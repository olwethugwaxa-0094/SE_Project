package za.co.capitecbank.transaction_producer.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Authentication(
        @JsonProperty("authTraceId")    String authTraceId,
        @JsonProperty("cardAuthStatus") String cardAuthStatus
) {
}
