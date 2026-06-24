package za.co.capitecbank.transaction_producer.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MerchantData(
        @JsonProperty("merchantName")         String merchantName,
        @JsonProperty("merchantDesc")         String merchantDesc,
        @JsonProperty("merchantCategoryCode") Integer merchantCategoryCode,
        @JsonProperty("city")                 String city,
        @JsonProperty("province")             String province
) {
}
