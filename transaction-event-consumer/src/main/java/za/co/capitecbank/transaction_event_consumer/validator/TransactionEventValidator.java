package za.co.capitecbank.transaction_event_consumer.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Component;
import za.co.capitecbank.transaction_event_consumer.constants.KafkaMessageConstants;
import za.co.capitecbank.transaction_event_consumer.constants.ValidationMetricsConstants;
import za.co.capitecbank.transaction_event_consumer.domain.Authentication;
import za.co.capitecbank.transaction_event_consumer.domain.ClientData;
import za.co.capitecbank.transaction_event_consumer.domain.PaymentDetails;
import za.co.capitecbank.transaction_event_consumer.domain.TransactionEvent;
import za.co.capitecbank.transaction_event_consumer.domain.TransactionMetadata;
import za.co.capitecbank.transaction_event_consumer.domain.ValidationResult;
import za.co.capitecbank.transaction_event_consumer.domain.ValidationResult.FailureCategory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Validates incoming Kafka transaction events before they are processed.
 *
 * Validation is performed in three sequential stages:
 *   1. Checksum  — verifies the SHA-256 header written by the producer against the raw wire payload.
 *   2. Deserialize — parses the raw JSON string into a TransactionEvent.
 *   3. Field checks — ensures all required fields are present and non-blank.
 *
 * Any failure short-circuits the remaining stages and returns an Invalid result carrying
 * a reason and a FailureCategory. The caller routes Invalid events to transaction.review.
 * Each outcome increments a Micrometer counter observable via /actuator/prometheus.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventValidator {

    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    /**
     * Entry point. Runs checksum → deserialize → field checks in order.
     * Returns Valid(event) on success, Invalid(reason, category) on first failure.
     */
    public ValidationResult validate(ConsumerRecord<String, String> consumerRecord) {
        Optional<ValidationResult.Invalid> checksumFailure = validateChecksum(consumerRecord);
        if (checksumFailure.isPresent()) {
            recordInvalid(checksumFailure.get().category());
            return checksumFailure.get();
        }

        TransactionEvent event = deserialize(consumerRecord.value());
        if (event == null) {
            recordInvalid(FailureCategory.MISSING_FIELDS);
            return new ValidationResult.Invalid("payload could not be deserialized", FailureCategory.MISSING_FIELDS, null);
        }

        Optional<ValidationResult.Invalid> fieldFailure = validateRequiredFields(event);
        if (fieldFailure.isPresent()) {
            var failure = new ValidationResult.Invalid(fieldFailure.get().reason(), fieldFailure.get().category(), event);
            recordInvalid(failure.category());
            return failure;
        }

        meterRegistry.counter(ValidationMetricsConstants.METRIC_VALIDATION,
                ValidationMetricsConstants.TAG_RESULT, ValidationMetricsConstants.TAG_RESULT_VALID).increment();
        return new ValidationResult.Valid(event);
    }

    /**
     * Recomputes SHA-256 over the raw wire payload and compares against the
     * "checksum" header written by the producer. This verifies the exact bytes
     * received — no re-serialization drift is possible.
     *
     * An absent or empty header signals either a producer signing failure or
     * a stripped header in transit — both are flagged as CHECKSUM_ABSENT and
     * routed to transaction.review rather than the DLQ, since we cannot
     * distinguish a producer bug from a tamper attempt at this point.
     */
    private Optional<ValidationResult.Invalid> validateChecksum(ConsumerRecord<String, String> consumerRecord) {
        Header checksumHeader = consumerRecord.headers().lastHeader(KafkaMessageConstants.HEADER_CHECKSUM);

        if (checksumHeader == null || checksumHeader.value() == null || checksumHeader.value().length == 0) {
            return Optional.of(new ValidationResult.Invalid("checksum header is absent or empty", FailureCategory.CHECKSUM_ABSENT, null));
        }

        String receivedChecksum = new String(checksumHeader.value(), StandardCharsets.UTF_8);
        String computedChecksum = computeChecksum(consumerRecord.value());

        if (!computedChecksum.equals(receivedChecksum)) {
            return Optional.of(new ValidationResult.Invalid(
                    "checksum mismatch: received=%s computed=%s".formatted(receivedChecksum, computedChecksum),
                    FailureCategory.CHECKSUM_MISMATCH, null));
        }

        return Optional.empty();
    }

    /**
     * Orchestrates field-level validation across all domain objects.
     * Uses Optional.or() to short-circuit — evaluation stops at the first failure.
     */
    private Optional<ValidationResult.Invalid> validateRequiredFields(TransactionEvent event) {
        return validateEventHeader(event)
                .or(() -> validateMetadata(event.transactionMetadata()))
                .or(() -> validatePaymentDetails(event.paymentDetails()))
                .or(() -> validateClientData(event.clientData()))
                .or(() -> validateAuthentication(event.authentication(), event.paymentDetails()));
    }

    /** Validates top-level event identity fields. */
    private Optional<ValidationResult.Invalid> validateEventHeader(TransactionEvent event) {
        if (event.transactionEventId() == null || event.transactionEventId().isBlank())
            return missing("transactionEventId is missing");
        if (event.transactionEventType() == null)
            return missing("transactionEventType is missing");
        return Optional.empty();
    }

    /** Validates transaction metadata — financial and temporal fields required for processing. */
    private Optional<ValidationResult.Invalid> validateMetadata(TransactionMetadata meta) {
        if (meta == null)
            return missing("transactionMetadata is missing");
        if (meta.transactionId() == null || meta.transactionId().isBlank())
            return missing("transactionMetadata.transactionId is missing");
        if (meta.transactionDate() == null)
            return missing("transactionMetadata.transactionDate is missing");
        if (meta.postingDate() == null)
            return missing("transactionMetadata.postingDate is missing");
        if (meta.amount() == null)
            return missing("transactionMetadata.amount is missing");
        if (meta.balance() == null)
            return missing("transactionMetadata.balance is missing");
        return Optional.empty();
    }

    /** Validates payment channel and transaction type fields required for routing. */
    private Optional<ValidationResult.Invalid> validatePaymentDetails(PaymentDetails payment) {
        if (payment == null)
            return missing("paymentDetails is missing");
        if (payment.channel() == null || payment.channel().isBlank())
            return missing("paymentDetails.channel is missing");
        if (payment.trancode() == null)
            return missing("paymentDetails.trancode is missing");
        if (payment.moneyIn() == null)
            return missing("paymentDetails.moneyIn is missing");
        return Optional.empty();
    }

    /** Validates client identity fields required for account linkage. */
    private Optional<ValidationResult.Invalid> validateClientData(ClientData client) {
        if (client == null)
            return missing("clientData is missing");
        if (client.cifNr() == null)
            return missing("clientData.cifNr is missing");
        if (client.accountNr() == null)
            return missing("clientData.accountNr is missing");
        return Optional.empty();
    }

    /** Validates authentication fields required for fraud assessment. */
    private Optional<ValidationResult.Invalid> validateAuthentication(Authentication auth, PaymentDetails payment) {
        if (auth == null)
            return missing("authentication is missing");
        if (auth.authTraceId() == null || auth.authTraceId().isBlank())
            return missing("authentication.authTraceId is missing");
        if (!isCardlessTransaction(payment) && (auth.cardAuthStatus() == null || auth.cardAuthStatus().isBlank()))
            return missing("authentication.cardAuthStatus is missing");
        return Optional.empty();
    }

    private boolean isCardlessTransaction(PaymentDetails payment) {
        if (payment == null) return false;
        if (Boolean.TRUE.equals(payment.moneyIn())) return true;
        if (payment.channel() == null) return false;
        return switch (payment.channel().toUpperCase()) {
            case "ATM", "CASHSEND", "TRANSFER" -> true;
            default -> false;
        };
    }

    /**
     * Computes SHA-256 hash over the raw payload string bytes (UTF-8).
     * Must use the same encoding as the producer to produce an identical hash.
     */
    private String computeChecksum(String payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the Java platform spec — this branch is unreachable
            log.error("SHA-256 unavailable — checksum computation failed", e);
            return "";
        }
    }

    /** Deserializes the raw JSON string into a TransactionEvent using the Spring-managed ObjectMapper. */
    private TransactionEvent deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, TransactionEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize payload: {}", e.getMessage());
            return null;
        }
    }

    /** Increments the invalid counter tagged by failure category for Prometheus/Grafana observability. */
    private void recordInvalid(FailureCategory category) {
        meterRegistry.counter(ValidationMetricsConstants.METRIC_VALIDATION,
                ValidationMetricsConstants.TAG_RESULT,   ValidationMetricsConstants.TAG_RESULT_INVALID,
                ValidationMetricsConstants.TAG_CATEGORY, category.name()).increment();
    }

    private static Optional<ValidationResult.Invalid> missing(String reason) {
        return Optional.of(new ValidationResult.Invalid(reason, FailureCategory.MISSING_FIELDS, null));
    }
}
