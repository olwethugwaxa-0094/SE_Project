package za.co.capitecbank.transaction_event_consumer.domain;

import java.time.LocalDateTime;
import java.util.List;

public record ScoredEvent(
        String correlationId,
        String transactionId,
        Long cifNr,
        LocalDateTime transactionDate,
        int score,
        List<MatchedRule> matchedRules,
        String ruleSetVersion,
        boolean degradedMode,
        TransactionEvent originalEvent
) {}
