package za.co.capitecbank.transaction_event_consumer.rules.model;

public record Rule(
        String id,
        String description,
        String owner,
        boolean enabled,
        int score,
        Condition condition
) {}
