package za.co.capitecbank.transaction_event_consumer.rules.model;

import java.util.List;

public record RuleSet(List<Rule> rules, String version, int scoreThreshold) {

    public RuleSet {
        rules = List.copyOf(rules);
    }

    public List<Rule> enabledRules() {
        return rules.stream().filter(Rule::enabled).toList();
    }
}
