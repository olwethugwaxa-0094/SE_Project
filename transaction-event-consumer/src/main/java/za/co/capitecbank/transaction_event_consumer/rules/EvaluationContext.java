package za.co.capitecbank.transaction_event_consumer.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class EvaluationContext {

    private final Map<String, Object> fields = new HashMap<>();

    public void put(String field, Object value) {
        fields.put(field, value);
    }

    public Object get(String field) {
        return fields.get(field);
    }

    public boolean contains(String field) {
        return fields.containsKey(field);
    }

    public Map<String, Object> asMap() {
        return Map.copyOf(fields);
    }

    // Known field names — used by validator to reject unknown field references
    public static final java.util.Set<String> KNOWN_FIELDS = java.util.Set.of(
            "transactionId", "cifNr", "accountNr",
            "transactionDate", "postingDate",
            "amount", "balance",
            "channel", "trancode", "trantypedesc", "moneyIn",
            "merchantName", "merchantCategoryCode", "city", "province",
            "cardAuthStatus", "transactionEventType",
            "hourOfDay", "dayOfWeek",
            "isBlacklisted",
            "recentTxnCount10m", "recentTxnCount1h", "recentTxnCount24h"
    );
}
