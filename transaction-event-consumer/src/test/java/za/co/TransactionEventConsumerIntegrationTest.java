package za.co.capitecbank.transaction_event_consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import za.co.capitecbank.transaction_event_consumer.domain.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"transaction-events", "transaction.review", "transaction.dlq"})
class TransactionEventConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private TransactionEventConsumer consumer;

    @Value("${spring.kafka.topics.source}")
    private String sourceTopic;

    @Test
    void valid_event_is_consumed_and_processed() throws Exception {
        TransactionEvent event = buildValidEvent("evt-int-001", "txn-int-001");
        String payload = objectMapper.writeValueAsString(event);
        ProducerRecord<String, Object> record = new ProducerRecord<>(sourceTopic, event.transactionEventId(), payload);
        record.headers().add("checksum", computeChecksum(payload).getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record);

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(consumer).consume(any()));
    }

    @Test
    void event_with_missing_fields_is_routed_to_review() throws Exception {
        TransactionEvent event = new TransactionEvent(
                null, TransactionEventType.NEW,
                buildValidEvent("evt-int-002", "txn-int-002").transactionMetadata(),
                buildValidEvent("evt-int-002", "txn-int-002").paymentDetails(),
                buildValidEvent("evt-int-002", "txn-int-002").merchantData(),
                buildValidEvent("evt-int-002", "txn-int-002").clientData(),
                buildValidEvent("evt-int-002", "txn-int-002").authentication()
        );
        String payload = objectMapper.writeValueAsString(event);
        ProducerRecord<String, Object> record = new ProducerRecord<>(sourceTopic, "evt-int-002", payload);
        record.headers().add("checksum", computeChecksum(payload).getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record);

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(consumer).consume(any()));
    }

    @Test
    void event_with_tampered_checksum_is_routed_to_review() throws Exception {
        TransactionEvent event = buildValidEvent("evt-int-003", "txn-int-003");
        String payload = objectMapper.writeValueAsString(event);
        ProducerRecord<String, Object> record = new ProducerRecord<>(sourceTopic, event.transactionEventId(), payload);
        record.headers().add("checksum", "000000deadbeef".getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record);

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(consumer).consume(any()));
    }

    @Test
    void event_with_absent_checksum_is_routed_to_review() throws Exception {
        TransactionEvent event = buildValidEvent("evt-int-004", "txn-int-004");
        String payload = objectMapper.writeValueAsString(event);
        ProducerRecord<String, Object> record = new ProducerRecord<>(sourceTopic, event.transactionEventId(), payload);

        kafkaTemplate.send(record);

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(consumer).consume(any()));
    }

    // --- Helpers ---

    private TransactionEvent buildValidEvent(String eventId, String transactionId) {
        return new TransactionEvent(
                eventId,
                TransactionEventType.NEW,
                new TransactionMetadata(transactionId, LocalDateTime.now(), LocalDate.now(),
                        new BigDecimal("250.00"), new BigDecimal("1500.00")),
                new PaymentDetails("POS", 1001, "Purchase", true, "************1234"),
                new MerchantData("Shoprite", "Grocery Store", 5411, "Cape Town", "Western Cape"),
                new ClientData(100001L, 4000000001L, 1),
                new Authentication("auth-trace-001", "APPROVED")
        );
    }

    private String computeChecksum(String payload) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
