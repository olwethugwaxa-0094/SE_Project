package za.co.capitecbank.transaction_event_consumer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import za.co.capitecbank.transaction_event_consumer.domain.MatchedRule;
import za.co.capitecbank.transaction_event_consumer.domain.ScoredEvent;
import za.co.capitecbank.transaction_event_consumer.entity.FactScoredTransactionEntity;
import za.co.capitecbank.transaction_event_consumer.entity.HouseKeeping;
import za.co.capitecbank.transaction_event_consumer.repository.FactScoredTransactionRepository;
import za.co.capitecbank.transaction_event_consumer.rules.config.RulesEngineProperties;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoredEventProducer {

    private final FactScoredTransactionRepository scoredRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RulesEngineProperties rulesEngineProperties;
    private final RulesEngineMetrics metrics;

    @Value("${spring.kafka.topics.scored}")
    private String scoredTopic;

    @Value("${spring.kafka.topics.review}")
    private String reviewTopic;

    @Value("${spring.kafka.topics.approved}")
    private String approvedTopic;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void produce(ScoredEvent event, String kafkaKey) {
        if (scoredRepository.existsByCorrelationId(event.correlationId())) {
            log.info("Skipping duplicate scored event correlationId={}", event.correlationId());
            return;
        }

        int threshold = rulesEngineProperties.getScoreThreshold();
        String routedTo = event.score() >= threshold ? reviewTopic : approvedTopic;

        var persistSample = metrics.startScoredPersist();
        FactScoredTransactionEntity entity = toEntity(event, kafkaKey, routedTo);
        scoredRepository.save(entity);
        metrics.stopScoredPersist(persistSample);

        log.info("Persisted fact_scored_transaction correlationId={}, score={}, routedTo={}",
                event.correlationId(), event.score(), routedTo);

        String payload = serialise(event);

        // Dual-write: DB committed above, publish after
        var publishSample = metrics.startScoredPublish();
        kafkaTemplate.send(scoredTopic, kafkaKey, payload)
                .whenComplete((r, ex) -> {
                    metrics.stopScoredPublish(publishSample);
                    if (ex != null) {
                        log.error("Failed to publish to {} for correlationId={}: {}",
                                scoredTopic, event.correlationId(), ex.getMessage(), ex);
                    }
                });

        kafkaTemplate.send(routedTo, kafkaKey, payload)
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish to {} for correlationId={}: {}",
                                routedTo, event.correlationId(), ex.getMessage(), ex);
                    }
                });
    }

    private FactScoredTransactionEntity toEntity(ScoredEvent event, String kafkaKey, String routedTo) {
        FactScoredTransactionEntity entity = new FactScoredTransactionEntity();
        entity.setCorrelationId(event.correlationId());
        entity.setKafkaKey(kafkaKey);
        entity.setScore(event.score());
        entity.setMatchedRules(serialiseRules(event.matchedRules()));
        entity.setRuleSetVersion(event.ruleSetVersion());
        entity.setDegradedMode(event.degradedMode());
        entity.setRoutedTo(routedTo);
        entity.setScoredAt(LocalDateTime.now());

        // Flatten all TransactionEvent fields when available
        var txn = event.originalEvent();
        if (txn != null) {
            entity.setEventId(txn.transactionEventId());
            entity.setEventType(txn.transactionEventType() != null ? txn.transactionEventType().name() : null);

            if (txn.transactionMetadata() != null) {
                entity.setTransactionId(txn.transactionMetadata().transactionId());
                entity.setTransactionDate(txn.transactionMetadata().transactionDate());
                entity.setPostingDate(txn.transactionMetadata().postingDate());
                entity.setAmount(txn.transactionMetadata().amount());
                entity.setBalance(txn.transactionMetadata().balance());
            }

            if (txn.paymentDetails() != null) {
                entity.setChannel(txn.paymentDetails().channel());
                entity.setTrancode(txn.paymentDetails().trancode());
                entity.setTrantypedesc(txn.paymentDetails().trantypedesc());
                entity.setMoneyIn(txn.paymentDetails().moneyIn());
                entity.setCardNr(txn.paymentDetails().cardNr());
            }

            if (txn.merchantData() != null) {
                entity.setMerchantName(txn.merchantData().merchantName());
                entity.setMerchantDesc(txn.merchantData().merchantDesc());
                entity.setMerchantCategoryCode(txn.merchantData().merchantCategoryCode());
                entity.setCity(txn.merchantData().city());
                entity.setProvince(txn.merchantData().province());
            }

            if (txn.clientData() != null) {
                entity.setCifNr(txn.clientData().cifNr());
                entity.setAccountNr(txn.clientData().accountNr());
                entity.setBranch(txn.clientData().branch());
            }

            if (txn.authentication() != null) {
                entity.setAuthTraceId(txn.authentication().authTraceId());
                entity.setCardAuthStatus(txn.authentication().cardAuthStatus());
            }
        } else {
            // INVALID path — use what we have from ScoredEvent
            entity.setTransactionId(event.transactionId());
            entity.setCifNr(event.cifNr());
            entity.setTransactionDate(event.transactionDate());
        }

        entity.setHouseKeeping(HouseKeeping.builder()
                .eBatchId(event.correlationId())
                .eIngestId(UUID.randomUUID())
                .eOperation("INSERT")
                .eSourceSystem("transaction-event-consumer")
                .eRowHash(Integer.toHexString(event.correlationId().hashCode()))
                .eLoadedAt(OffsetDateTime.now())
                .build());
        return entity;
    }

    private String serialise(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise scored event", e);
        }
    }

    private String serialiseRules(List<MatchedRule> rules) {
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
