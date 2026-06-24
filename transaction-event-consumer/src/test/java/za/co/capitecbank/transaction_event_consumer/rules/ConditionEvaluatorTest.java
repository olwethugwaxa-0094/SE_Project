package za.co.capitecbank.transaction_event_consumer.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.capitecbank.transaction_event_consumer.rules.model.Condition;
import za.co.capitecbank.transaction_event_consumer.rules.model.Operator;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionEvaluatorTest {

    private ConditionEvaluator evaluator;
    private EvaluationContext ctx;

    @BeforeEach
    void setUp() {
        evaluator = new ConditionEvaluator();
        ctx = new EvaluationContext();
        ctx.put("amount",   new BigDecimal("15000"));
        ctx.put("channel",  "ONLINE");
        ctx.put("hourOfDay", 3);
        ctx.put("isBlacklisted", true);
        ctx.put("recentTxnCount10m", 8L);
        ctx.put("merchantCategoryCode", 7995);
    }

    // --- Leaf operators ---

    @Test
    void leaf_GT_matches_when_value_above_threshold() {
        assertThat(leaf("amount", Operator.GT, new BigDecimal("10000"))).isTrue();
    }

    @Test
    void leaf_GT_does_not_match_when_value_equal() {
        ctx.put("amount", new BigDecimal("10000"));
        assertThat(leaf("amount", Operator.GT, new BigDecimal("10000"))).isFalse();
    }

    @Test
    void leaf_GTE_matches_when_value_equal() {
        ctx.put("amount", new BigDecimal("10000"));
        assertThat(leaf("amount", Operator.GTE, new BigDecimal("10000"))).isTrue();
    }

    @Test
    void leaf_EQUALS_matches_string() {
        assertThat(leaf("channel", Operator.EQUALS, "ONLINE")).isTrue();
    }

    @Test
    void leaf_EQUALS_no_match_different_string() {
        assertThat(leaf("channel", Operator.EQUALS, "ATM")).isFalse();
    }

    @Test
    void leaf_NOT_EQUALS_matches_when_values_differ() {
        assertThat(leaf("channel", Operator.NOT_EQUALS, "ATM")).isTrue();
    }

    @Test
    void leaf_EQUALS_boolean() {
        assertThat(leaf("isBlacklisted", Operator.EQUALS, true)).isTrue();
    }

    @Test
    void leaf_IN_matches_when_value_in_list() {
        var cond = new Condition.Leaf("merchantCategoryCode", Operator.IN, null, List.of(7993, 7995, 7801));
        assertThat(evaluator.evaluate(cond, ctx)).isTrue();
    }

    @Test
    void leaf_NOT_IN_matches_when_value_not_in_list() {
        var cond = new Condition.Leaf("merchantCategoryCode", Operator.NOT_IN, null, List.of(1111, 2222));
        assertThat(evaluator.evaluate(cond, ctx)).isTrue();
    }

    @Test
    void leaf_BETWEEN_matches_inclusive_bounds() {
        var cond = new Condition.Leaf("hourOfDay", Operator.BETWEEN, null, List.of(0, 5));
        assertThat(evaluator.evaluate(cond, ctx)).isTrue();
    }

    @Test
    void leaf_BETWEEN_fails_outside_bounds() {
        ctx.put("hourOfDay", 6);
        var cond = new Condition.Leaf("hourOfDay", Operator.BETWEEN, null, List.of(0, 5));
        assertThat(evaluator.evaluate(cond, ctx)).isFalse();
    }

    @Test
    void leaf_null_field_evaluates_to_false() {
        ctx.put("cardAuthStatus", null);
        assertThat(leaf("cardAuthStatus", Operator.EQUALS, "APPROVED")).isFalse();
    }

    @Test
    void leaf_missing_field_evaluates_to_false() {
        assertThat(leaf("nonExistentField", Operator.EQUALS, "X")).isFalse();
    }

    // --- Compound conditions ---

    @Test
    void all_matches_when_all_leaves_pass() {
        var cond = new Condition.All(List.of(
                leafCond("amount", Operator.GT, new BigDecimal("10000")),
                leafCond("channel", Operator.EQUALS, "ONLINE")));
        assertThat(evaluator.evaluate(cond, ctx)).isTrue();
    }

    @Test
    void all_fails_when_one_leaf_fails() {
        var cond = new Condition.All(List.of(
                leafCond("amount", Operator.GT, new BigDecimal("10000")),
                leafCond("channel", Operator.EQUALS, "ATM")));
        assertThat(evaluator.evaluate(cond, ctx)).isFalse();
    }

    @Test
    void any_matches_when_at_least_one_leaf_passes() {
        var cond = new Condition.Any(List.of(
                leafCond("channel", Operator.EQUALS, "ATM"),      // false
                leafCond("amount", Operator.GT, new BigDecimal("10000")))); // true
        assertThat(evaluator.evaluate(cond, ctx)).isTrue();
    }

    @Test
    void any_fails_when_no_leaf_passes() {
        var cond = new Condition.Any(List.of(
                leafCond("channel", Operator.EQUALS, "ATM"),
                leafCond("amount", Operator.GT, new BigDecimal("99999"))));
        assertThat(evaluator.evaluate(cond, ctx)).isFalse();
    }

    @Test
    void not_inverts_true_to_false() {
        var inner = leafCond("channel", Operator.EQUALS, "ONLINE");
        assertThat(evaluator.evaluate(new Condition.Not(inner), ctx)).isFalse();
    }

    @Test
    void not_inverts_false_to_true() {
        var inner = leafCond("channel", Operator.EQUALS, "ATM");
        assertThat(evaluator.evaluate(new Condition.Not(inner), ctx)).isTrue();
    }

    @Test
    void nested_all_inside_any() {
        // ANY[ ALL[amount>10k, channel=ONLINE], ALL[isBlacklisted=true] ]
        var cond = new Condition.Any(List.of(
                new Condition.All(List.of(
                        leafCond("amount", Operator.GT, new BigDecimal("10000")),
                        leafCond("channel", Operator.EQUALS, "ONLINE"))),
                new Condition.All(List.of(
                        leafCond("isBlacklisted", Operator.EQUALS, true)))));
        assertThat(evaluator.evaluate(cond, ctx)).isTrue();
    }

    @Test
    void spel_expression_evaluates_correctly() {
        ctx.put("balance", new BigDecimal("500"));
        var cond = new Condition.Leaf(null, Operator.EXPRESSION, "#amount > 10000 && #balance < 1000", null);
        assertThat(evaluator.evaluate(cond, ctx)).isTrue();
    }

    // --- Helpers ---

    private boolean leaf(String field, Operator op, Object value) {
        return evaluator.evaluate(new Condition.Leaf(field, op, value, null), ctx);
    }

    private Condition.Leaf leafCond(String field, Operator op, Object value) {
        return new Condition.Leaf(field, op, value, null);
    }
}
