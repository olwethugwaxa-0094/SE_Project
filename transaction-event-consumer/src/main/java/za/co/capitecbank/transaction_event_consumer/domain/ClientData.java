package za.co.capitecbank.transaction_event_consumer.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClientData(
        @JsonProperty("cifNr")     Long cifNr,
        @JsonProperty("accountNr") Long accountNr,
        @JsonProperty("branch")    Integer branch
) {
}
