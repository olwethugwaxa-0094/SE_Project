package za.co.capitecbank.transaction_event_consumer.rules;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import za.co.capitecbank.transaction_event_consumer.rules.model.Condition;
import za.co.capitecbank.transaction_event_consumer.rules.model.Operator;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ConditionEvaluator {

    private final ExpressionParser spel = new SpelExpressionParser();

    public boolean evaluate(Condition condition, EvaluationContext ctx) {
        return switch (condition) {
            case Condition.All all   -> all.conditions().stream().allMatch(c -> evaluate(c, ctx));
            case Condition.Any any   -> any.conditions().stream().anyMatch(c -> evaluate(c, ctx));
            case Condition.Not not   -> !evaluate(not.condition(), ctx);
            case Condition.Leaf leaf -> evaluateLeaf(leaf, ctx);
        };
    }

    private boolean evaluateLeaf(Condition.Leaf leaf, EvaluationContext ctx) {
        Object fieldValue = ctx.get(leaf.field());

        if (leaf.op() == Operator.EXPRESSION) {
            return evaluateSpel(leaf.value().toString(), ctx);
        }

        if (fieldValue == null) {
            log.debug("Field '{}' is null — leaf evaluates to false", leaf.field());
            return false;
        }

        return switch (leaf.op()) {
            case EQUALS     -> compare(fieldValue, leaf.value()) == 0;
            case NOT_EQUALS -> compare(fieldValue, leaf.value()) != 0;
            case GT         -> compare(fieldValue, leaf.value()) > 0;
            case GTE        -> compare(fieldValue, leaf.value()) >= 0;
            case LT         -> compare(fieldValue, leaf.value()) < 0;
            case LTE        -> compare(fieldValue, leaf.value()) <= 0;
            case BETWEEN    -> evaluateBetween(fieldValue, leaf.values());
            case IN         -> evaluateIn(fieldValue, leaf.values(), true);
            case NOT_IN     -> evaluateIn(fieldValue, leaf.values(), false);
            case MATCHES    -> evaluateMatches(fieldValue, leaf.value().toString());
            case EXPRESSION -> false; // handled above
        };
    }

    private int compare(Object fieldValue, Object ruleValue) {
        // Try numeric comparison first
        BigDecimal left  = toBigDecimal(fieldValue);
        BigDecimal right = toBigDecimal(ruleValue);
        if (left != null && right != null) {
            return left.compareTo(right);
        }

        // Boolean equality
        if (fieldValue instanceof Boolean bField && ruleValue instanceof Boolean bRule) {
            return bField.compareTo(bRule);
        }
        if (fieldValue instanceof Boolean bField) {
            return bField.compareTo(Boolean.parseBoolean(ruleValue.toString()));
        }

        // Fall back to string comparison
        return fieldValue.toString().compareTo(ruleValue.toString());
    }

    private boolean evaluateBetween(Object fieldValue, List<Object> bounds) {
        if (bounds == null || bounds.size() != 2) {
            log.warn("BETWEEN requires a 2-element values list");
            return false;
        }
        return compare(fieldValue, bounds.get(0)) >= 0
                && compare(fieldValue, bounds.get(1)) <= 0;
    }

    private boolean evaluateIn(Object fieldValue, List<Object> list, boolean inclusive) {
        if (list == null || list.isEmpty()) {
            log.warn("IN/NOT_IN requires a non-empty values list");
            return !inclusive;
        }
        boolean found = list.stream().anyMatch(v -> compare(fieldValue, v) == 0);
        return inclusive ? found : !found;
    }

    private boolean evaluateMatches(Object fieldValue, String pattern) {
        return Pattern.compile(pattern).matcher(fieldValue.toString()).matches();
    }

    private boolean evaluateSpel(String expression, EvaluationContext ctx) {
        StandardEvaluationContext spelCtx = new StandardEvaluationContext();
        ctx.asMap().forEach(spelCtx::setVariable);
        Boolean result = spel.parseExpression(expression).getValue(spelCtx, Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n)    return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(value.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}
