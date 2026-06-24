package za.co.capitecbank.transaction_producer.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentDetails(
        @JsonProperty("channel")      String channel,
        @JsonProperty("trancode")     Integer trancode,
        @JsonProperty("trantypedesc") String trantypedesc,
        @JsonProperty("moneyIn")      Boolean moneyIn,
        @JsonProperty("cardNr")       String cardNr
) {
}
