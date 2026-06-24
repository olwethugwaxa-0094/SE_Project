package za.co.capitecbank.transaction_event_consumer.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionMetadata(
        @JsonProperty("transactionId")   String transactionId,
        @JsonProperty("transactionDate") LocalDateTime transactionDate,
        @JsonProperty("postingDate")     LocalDate postingDate,
        @JsonProperty("amount")          BigDecimal amount,
        @JsonProperty("balance")         BigDecimal balance
) {
}
