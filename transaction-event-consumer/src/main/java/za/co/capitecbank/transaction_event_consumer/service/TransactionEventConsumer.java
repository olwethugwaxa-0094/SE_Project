package za.co.capitecbank.transaction_event_consumer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import za.co.capitecbank.transaction_event_consumer.domain.ScoredEvent;
import za.co.capitecbank.transaction_event_consumer.domain.ValidationResult;
import za.co.capitecbank.transaction_event_consumer.entity.HouseKeeping;
import za.co.capitecbank.transaction_event_consumer.repository.SrcTransactionEventRepository;
import za.co.capitecbank.transaction_event_consumer.validator.TransactionEventValidator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionEventValidator validator;
    private final SrcTransactionEventRepository srcTransactionEventRepository;
    private final SrcTransactionEventMapper srcTransactionEventMapper;
    private final TransactionProcessingService transactionProcessingService;
    private final ScoredEventProducer scoredEventProducer;
    private final RulesEngineMetrics metrics;

    @KafkaListener(topics = "${spring.kafka.topics.source}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, String> record) {
        log.info("Received record: key={}, partition={}, offset={}",
                record.key(), record.partition(), record.offset());

        String correlationId = UUID.randomUUID().toString();

        // Step 1: persist raw source mirror — skip if already recorded (replay guard)
        if (!srcTransactionEventRepository.existsByKafkaTopicAndKafkaPartitionAndKafkaOffset(
                record.topic(), record.partition(), record.offset())) {
            var src = srcTransactionEventMapper.toEntity(record, null);
            src.setHouseKeeping(HouseKeeping.builder()
                    .eBatchId(correlationId)
                    .eIngestId(UUID.randomUUID())
                    .eOperation("INSERT")
                    .eSourceSystem("transaction-event-consumer")
                    .eRowHash(Long.toHexString(record.offset() + record.partition()))
                    .eLoadedAt(OffsetDateTime.now())
                    .build());
            srcTransactionEventRepository.save(src);
        }

        // Step 2: validate
        var validationSample = metrics.startValidation();
        ValidationResult result = validator.validate(record);
        metrics.stopValidation(validationSample);

        // Step 3: persist fact + evaluate rules — transaction commits before produce
        ScoredEvent scored;
        switch (result) {
            case ValidationResult.Invalid invalid -> {
                metrics.recordValidationFailure();
                log.warn("Validation failed — key={}, category={}, reason={}",
                        record.key(), invalid.category(), invalid.reason());
                scored = transactionProcessingService.processInvalid(invalid, record, correlationId);
            }
            case ValidationResult.Valid valid ->
                    scored = transactionProcessingService.processValid(valid.event(), record, correlationId);
        }

        // Step 4: dual-write scored event — runs in its own REQUIRES_NEW transaction
        if (scored != null) {
            scoredEventProducer.produce(scored, record.key());
        }
    }
}
