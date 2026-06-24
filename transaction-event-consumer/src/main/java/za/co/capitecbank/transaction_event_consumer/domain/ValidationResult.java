package za.co.capitecbank.transaction_event_consumer.domain;

public sealed interface ValidationResult {

    record Valid(TransactionEvent event) implements ValidationResult {}

    record Invalid(String reason, FailureCategory category, TransactionEvent event) implements ValidationResult {}

    enum FailureCategory {
        MISSING_FIELDS,
        CHECKSUM_ABSENT,
        CHECKSUM_MISMATCH
    }
}
