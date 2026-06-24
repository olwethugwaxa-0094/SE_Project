package za.co.capitecbank.transaction_event_consumer.rules;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.co.capitecbank.transaction_event_consumer.rules.model.Condition;
import za.co.capitecbank.transaction_event_consumer.rules.model.Operator;
import za.co.capitecbank.transaction_event_consumer.rules.model.Rule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RuleSetValidator {

    public void validate(List<Rule> rules) {
        Set<String> seenIds = new HashSet<>();

        for (Rule rule : rules) {
            if (rule.id() == null || rule.id().isBlank())
                throw new InvalidRuleSetException("Rule has blank id");

            if (!seenIds.add(rule.id()))
                throw new InvalidRuleSetException("Duplicate rule id: " + rule.id());

            if (rule.score() < 0)
                throw new InvalidRuleSetException("Rule '" + rule.id() + "' has negative score: " + rule.score());

            if (rule.condition() == null)
                throw new InvalidRuleSetException("Rule '" + rule.id() + "' has no condition");

            validateCondition(rule.condition(), rule.id());
        }
    }

    private void validateCondition(Condition condition, String ruleId) {
        switch (condition) {
            case Condition.All all   -> all.conditions().forEach(c -> validateCondition(c, ruleId));
            case Condition.Any any   -> any.conditions().forEach(c -> validateCondition(c, ruleId));
            case Condition.Not not   -> validateCondition(not.condition(), ruleId);
            case Condition.Leaf leaf -> validateLeaf(leaf, ruleId);
        }
    }

    private void validateLeaf(Condition.Leaf leaf, String ruleId) {
        if (!EvaluationContext.KNOWN_FIELDS.contains(leaf.field()))
            throw new InvalidRuleSetException("Rule '" + ruleId + "' references unknown field: " + leaf.field());

        if (leaf.op() == Operator.BETWEEN) {
            if (leaf.values() == null || leaf.values().size() != 2)
                throw new InvalidRuleSetException("Rule '" + ruleId + "': BETWEEN requires 'values' with exactly 2 elements");
        }

        if (leaf.op() == Operator.IN || leaf.op() == Operator.NOT_IN) {
            if (leaf.values() == null || leaf.values().isEmpty())
                throw new InvalidRuleSetException("Rule '" + ruleId + "': IN/NOT_IN requires a non-empty 'values' list");
        }

        if (leaf.op() == Operator.MATCHES) {
            try {
                Pattern.compile(leaf.value().toString());
            } catch (Exception e) {
                throw new InvalidRuleSetException("Rule '" + ruleId + "': invalid MATCHES regex: " + leaf.value());
            }
        }

        if (leaf.op() == Operator.EXPRESSION) {
            try {
                new org.springframework.expression.spel.standard.SpelExpressionParser()
                        .parseExpression(leaf.value().toString());
            } catch (Exception e) {
                throw new InvalidRuleSetException("Rule '" + ruleId + "': invalid SpEL expression: " + leaf.value());
            }
        }
    }

    public static class InvalidRuleSetException extends RuntimeException {
        public InvalidRuleSetException(String message) { super(message); }
    }
}
