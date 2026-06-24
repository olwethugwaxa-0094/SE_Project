package za.co.capitecbank.transaction_event_consumer.rules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.co.capitecbank.transaction_event_consumer.rules.config.RuleConditionProperties;
import za.co.capitecbank.transaction_event_consumer.rules.config.RulesEngineProperties;
import za.co.capitecbank.transaction_event_consumer.rules.model.Condition;
import za.co.capitecbank.transaction_event_consumer.rules.model.Operator;
import za.co.capitecbank.transaction_event_consumer.rules.model.Rule;
import za.co.capitecbank.transaction_event_consumer.rules.model.RuleSet;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleSetLoader {

    private final RulesEngineProperties properties;
    private final RuleSetValidator validator;

    public RuleSet load() {
        List<Rule> rules = properties.getRules().stream()
                .map(p -> new Rule(
                        p.getId(),
                        p.getDescription(),
                        p.getOwner(),
                        p.isEnabled(),
                        p.getScore(),
                        parseCondition(p.getCondition(), p.getId())))
                .toList();

        validator.validate(rules);

        log.info("Loaded {} rules, version={}, scoreThreshold={}",
                rules.size(), properties.getVersion(), properties.getScoreThreshold());

        return new RuleSet(rules, properties.getVersion(), properties.getScoreThreshold());
    }

    private Condition parseCondition(RuleConditionProperties props, String ruleId) {
        if (props == null)
            throw new RuleSetValidator.InvalidRuleSetException("Rule '" + ruleId + "' has null condition");

        if (props.getAll() != null)
            return new Condition.All(props.getAll().stream()
                    .map(c -> parseCondition(c, ruleId)).toList());

        if (props.getAny() != null)
            return new Condition.Any(props.getAny().stream()
                    .map(c -> parseCondition(c, ruleId)).toList());

        if (props.getNot() != null)
            return new Condition.Not(parseCondition(props.getNot(), ruleId));

        // Leaf
        if (props.getField() == null || props.getOp() == null)
            throw new RuleSetValidator.InvalidRuleSetException(
                    "Rule '" + ruleId + "': leaf condition missing field or op");

        Operator op = Operator.valueOf(props.getOp().toUpperCase());
        return new Condition.Leaf(props.getField(), op, props.getValue(), props.getValues());
    }
}
