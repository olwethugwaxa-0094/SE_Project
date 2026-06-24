package za.co.capitecbank.transaction_producer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import za.co.capitecbank.transaction_producer.constants.KafkaMessageConstants;
import za.co.capitecbank.transaction_producer.domain.TransactionEvent;
import za.co.capitecbank.transaction_producer.repository.TransactionRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventProducer {

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionEventMapper mapper;
    private final ObjectMapper objectMapper;

    @Value("${app.event-producer.sleep-delay-sec:5}")
    private int sleepDelaySec;

    @Value("${spring.kafka.topic}")
    private String topic;

    public CompletableFuture<SendResult<String, String>> sendTransactionEvent(TransactionEvent transactionEvent) {
        String transactionId = transactionEvent.transactionMetadata().transactionId();

        String payload = serialize(transactionEvent);
        if (payload == null) {
            log.error("Failed to serialize event for transactionId={}, skipping", transactionId);
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Serialization failed for transactionId=" + transactionId));
        }

        ProducerRecord<String, String> producerRecord = buildProducerRecord(transactionId, payload);

        return kafkaTemplate.send(producerRecord)
                .whenComplete((sendResult, throwable) -> {
                    if (throwable != null) {
                        handleFailure(transactionId, throwable);
                    } else {
                        handleSuccess(transactionId, sendResult);
                    }
                });
    }

    private ProducerRecord<String, String> buildProducerRecord(String transactionId, String payload) {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, transactionId, payload);

        // Checksum is computed over the exact JSON string sent on the wire.
        // Consumer verifies against the same raw bytes — no re-serialization drift.
        producerRecord.headers()
                .add(KafkaMessageConstants.HEADER_CHECKSUM,       computeChecksum(payload))
                .add(KafkaMessageConstants.HEADER_SCHEMA_VERSION, "v1".getBytes(StandardCharsets.UTF_8))
                .add(KafkaMessageConstants.HEADER_SOURCE,         KafkaMessageConstants.SOURCE_TRANSACTION_PRODUCER.getBytes(StandardCharsets.UTF_8));

        return producerRecord;
    }

    private String serialize(TransactionEvent transactionEvent) {
        try {
            return objectMapper.writeValueAsString(transactionEvent);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize event for transactionId={}",
                    transactionEvent.transactionMetadata().transactionId(), e);
            return null;
        }
    }

    private byte[] computeChecksum(String payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).getBytes(StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 unavailable — checksum not computed", e);
            return new byte[0];
        }
    }

    private void handleSuccess(String transactionId, SendResult<String, String> sendResult) {
        log.info("Transaction id {} sent successfully. Partition = {}.", transactionId,
                sendResult.getRecordMetadata().partition());
    }

    private void handleFailure(String transactionId, Throwable throwable) {
        log.error("Error sending transaction id {}. Exception: {}", transactionId, throwable.getMessage(), throwable);
    }

    @Scheduled(initialDelayString = "${app.event-producer.initial-delay-ms:0}", fixedDelay = Long.MAX_VALUE)
    public void produce() {
        List<TransactionEvent> events = transactionRepository.findAll()
                .stream()
                .map(mapper::toEvent)
                .sorted(Comparator.comparing(e -> e.transactionMetadata().transactionDate()))
                .toList();

        log.info("Producing {} transaction events", events.size());
        for (TransactionEvent event : events) {
            try {
                sendTransactionEvent(event).get(30, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.error("Timeout sending event for transactionId={}",
                        event.transactionMetadata().transactionId());
            } catch (ExecutionException e) {
                log.error("Failed to send event for transactionId={}",
                        event.transactionMetadata().transactionId(), e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Event producer interrupted");
                return;
            }
            try {
                TimeUnit.SECONDS.sleep(sleepDelaySec);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Event producer interrupted");
                return;
            }
        }
    }
}
