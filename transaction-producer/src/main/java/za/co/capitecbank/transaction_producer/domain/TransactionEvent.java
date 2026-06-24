package za.co.capitecbank.transaction_producer.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TransactionEvent(
        @JsonProperty("transactionEventId")   String transactionEventId,
        @JsonProperty("transactionEventType") TransactionEventType transactionEventType,
        @JsonProperty("transactionMetadata")  TransactionMetadata transactionMetadata,
        @JsonProperty("paymentDetails")       PaymentDetails paymentDetails,
        @JsonProperty("merchantData")         MerchantData merchantData,
        @JsonProperty("clientData")           ClientData clientData,
        @JsonProperty("authentication")       Authentication authentication
        ) {
}
