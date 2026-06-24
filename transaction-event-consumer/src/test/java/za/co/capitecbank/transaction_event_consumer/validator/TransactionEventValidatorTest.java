package za.co.capitecbank.transaction_event_consumer.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.capitecbank.transaction_event_consumer.domain.*;
import za.co.capitecbank.transaction_event_consumer.domain.ValidationResult.FailureCategory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionEventValidatorTest {

    private TransactionEventValidator validator;
    private ObjectMapper objectMapper;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        meterRegistry = new SimpleMeterRegistry();
        validator = new TransactionEventValidator(objectMapper, meterRegistry);
    }

    // --- Valid event ---

    @Test
    void valid_event_passes_validation() throws Exception {
        TransactionEvent event = buildValidEvent();
        ConsumerRecord<String, String> record = buildRecord(event, computeChecksum(event));

        ValidationResult result = validator.validate(record);
        assertThat(result).isInstanceOf(ValidationResult.Valid.class);
        assertThat(((ValidationResult.Valid) result).event().transactionEventId()).isEqualTo("evt-001");
    }

    // --- Null field checks ---

    @Test
    void missing_transactionEventId_fails_with_MISSING_FIELDS() throws Exception {
        TransactionEvent event = buildEventWithNullEventId();
        ConsumerRecord<String, String> record = buildRecord(event, computeChecksum(event));

        assertInvalid(validator.validate(record), FailureCategory.MISSING_FIELDS, "transactionEventId");
    }

    @Test
    void missing_transactionEventType_fails_with_MISSING_FIELDS() throws Exception {
        TransactionEvent event = buildEventWithNullType();
        ConsumerRecord<String, String> record = buildRecord(event, computeChecksum(event));

        assertInvalid(validator.validate(record), FailureCategory.MISSING_FIELDS, "transactionEventType");
    }

    @Test
    void missing_transactionMetadata_fails_with_MISSING_FIELDS() throws Exception {
        TransactionEvent event = buildEventWithNullMetadata();
        ConsumerRecord<String, String> record = buildRecord(event, computeChecksum(event));

        assertInvalid(validator.validate(record), FailureCategory.MISSING_FIELDS, "transactionMetadata");
    }

    @Test
    void missing_clientData_cifNr_fails_with_MISSING_FIELDS() throws Exception {
        TransactionEvent event = buildEventWithNullCifNr();
        ConsumerRecord<String, String> record = buildRecord(event, computeChecksum(event));

        assertInvalid(validator.validate(record), FailureCategory.MISSING_FIELDS, "clientData.cifNr");
    }

    @Test
    void missing_authentication_fails_with_MISSING_FIELDS() throws Exception {
        TransactionEvent event = buildEventWithNullAuthentication();
        ConsumerRecord<String, String> record = buildRecord(event, computeChecksum(event));

        assertInvalid(validator.validate(record), FailureCategory.MISSING_FIELDS, "authentication");
    }

    // --- Checksum checks ---

    @Test
    void absent_checksum_header_fails_with_CHECKSUM_ABSENT() throws Exception {
        TransactionEvent event = buildValidEvent();
        ConsumerRecord<String, String> record = buildRecord(event, null);

        assertInvalid(validator.validate(record), FailureCategory.CHECKSUM_ABSENT, "checksum header is absent");
    }

    @Test
    void empty_checksum_header_fails_with_CHECKSUM_ABSENT() throws Exception {
        TransactionEvent event = buildValidEvent();
        ConsumerRecord<String, String> record = buildRecord(event, "");

        assertInvalid(validator.validate(record), FailureCategory.CHECKSUM_ABSENT, "checksum header is absent");
    }

    @Test
    void tampered_checksum_fails_with_CHECKSUM_MISMATCH() throws Exception {
        TransactionEvent event = buildValidEvent();
        ConsumerRecord<String, String> record = buildRecord(event, "000000deadbeef");

        assertInvalid(validator.validate(record), FailureCategory.CHECKSUM_MISMATCH, "checksum mismatch");
    }

    // --- Metrics counter tests ---

    @Test
    void valid_event_increments_valid_counter() throws Exception {
        TransactionEvent event = buildValidEvent();
        ConsumerRecord<String, String> record = buildRecord(event, computeChecksum(event));

        validator.validate(record);

        assertThat(meterRegistry.counter("transaction.event.validation", "result", "valid").count())
                .isEqualTo(1.0);
    }

    @Test
    void missing_field_increments_invalid_counter_with_MISSING_FIELDS_category() throws Exception {
        TransactionEvent event = buildEventWithNullEventId();
        ConsumerRecord<String, String> record = buildRecord(event, computeChecksum(event));

        validator.validate(record);

        assertThat(meterRegistry.counter("transaction.event.validation",
                "result", "invalid", "category", "MISSING_FIELDS").count())
                .isEqualTo(1.0);
    }

    @Test
    void checksum_mismatch_increments_invalid_counter_with_CHECKSUM_MISMATCH_category() throws Exception {
        TransactionEvent event = buildValidEvent();
        ConsumerRecord<String, String> record = buildRecord(event, "000000deadbeef");

        validator.validate(record);

        assertThat(meterRegistry.counter("transaction.event.validation",
                "result", "invalid", "category", "CHECKSUM_MISMATCH").count())
                .isEqualTo(1.0);
    }

    @Test
    void absent_checksum_increments_invalid_counter_with_CHECKSUM_ABSENT_category() throws Exception {
        TransactionEvent event = buildValidEvent();
        ConsumerRecord<String, String> record = buildRecord(event, null);

        validator.validate(record);

        assertThat(meterRegistry.counter("transaction.event.validation",
                "result", "invalid", "category", "CHECKSUM_ABSENT").count())
                .isEqualTo(1.0);
    }

    // --- Helpers ---

    private void assertInvalid(ValidationResult result, FailureCategory expectedCategory, String reasonContains) {
        assertThat(result).isInstanceOf(ValidationResult.Invalid.class);
        ValidationResult.Invalid invalid = (ValidationResult.Invalid) result;
        assertThat(invalid.category()).isEqualTo(expectedCategory);
        assertThat(invalid.reason()).containsIgnoringCase(reasonContains);
    }

    private ConsumerRecord<String, String> buildRecord(TransactionEvent event, String checksum) throws Exception {
        String payload = objectMapper.writeValueAsString(event);
        RecordHeaders headers = new RecordHeaders();
        if (checksum != null && !checksum.isEmpty()) {
            headers.add("checksum", checksum.getBytes(StandardCharsets.UTF_8));
        } else if (checksum != null) {
            headers.add("checksum", new byte[0]);
        }
        return new ConsumerRecord<>("transaction-events", 0, 0L, 0L,
                TimestampType.CREATE_TIME, 0, 0,
                event.transactionEventId() != null ? event.transactionEventId() : "unknown",
                payload, headers, Optional.empty());
    }

    private String computeChecksum(TransactionEvent event) throws Exception {
        String payload = objectMapper.writeValueAsString(event);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private TransactionEvent buildValidEvent() {
        return new TransactionEvent(
                "evt-001",
                TransactionEventType.NEW,
                new TransactionMetadata("txn-001", LocalDateTime.now(), LocalDate.now(),
                        new BigDecimal("250.00"), new BigDecimal("1500.00")),
                new PaymentDetails("POS", 1001, "Purchase", true, "************1234"),
                new MerchantData("Shoprite", "Grocery Store", 5411, "Cape Town", "Western Cape"),
                new ClientData(100001L, 4000000001L, 1),
                new Authentication("auth-trace-001", "APPROVED")
        );
    }

    private TransactionEvent buildEventWithNullEventId() {
        return new TransactionEvent(null, TransactionEventType.NEW,
                buildValidEvent().transactionMetadata(), buildValidEvent().paymentDetails(),
                buildValidEvent().merchantData(), buildValidEvent().clientData(),
                buildValidEvent().authentication());
    }

    private TransactionEvent buildEventWithNullType() {
        return new TransactionEvent("evt-001", null,
                buildValidEvent().transactionMetadata(), buildValidEvent().paymentDetails(),
                buildValidEvent().merchantData(), buildValidEvent().clientData(),
                buildValidEvent().authentication());
    }

    private TransactionEvent buildEventWithNullMetadata() {
        return new TransactionEvent("evt-001", TransactionEventType.NEW,
                null, buildValidEvent().paymentDetails(),
                buildValidEvent().merchantData(), buildValidEvent().clientData(),
                buildValidEvent().authentication());
    }

    private TransactionEvent buildEventWithNullCifNr() {
        return new TransactionEvent("evt-001", TransactionEventType.NEW,
                buildValidEvent().transactionMetadata(), buildValidEvent().paymentDetails(),
                buildValidEvent().merchantData(),
                new ClientData(null, 4000000001L, 1),
                buildValidEvent().authentication());
    }

    private TransactionEvent buildEventWithNullAuthentication() {
        return new TransactionEvent("evt-001", TransactionEventType.NEW,
                buildValidEvent().transactionMetadata(), buildValidEvent().paymentDetails(),
                buildValidEvent().merchantData(), buildValidEvent().clientData(), null);
    }
}
