package za.co.capitecbank.transaction_event_consumer.rules.model;

import za.co.capitecbank.transaction_event_consumer.domain.MatchedRule;

import java.util.List;

public record ScoringResult(int totalScore, List<MatchedRule> matchedRules, boolean degradedMode) {}
