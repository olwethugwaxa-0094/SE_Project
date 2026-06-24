package za.co.capitecbank.transaction_event_consumer.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.capitecbank.transaction_event_consumer.rules.model.Condition;
import za.co.capitecbank.transaction_event_consumer.rules.model.Operator;
import za.co.capitecbank.transaction_event_consumer.rules.model.Rule;
import za.co.capitecbank.transaction_event_consumer.rules.model.RuleSet;
import za.co.capitecbank.transaction_event_consumer.rules.model.ScoringResult;
import za.co.capitecbank.transaction_event_consumer.service.RulesEngineMetrics;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RulesEngineTest {

    @Mock private RuleSetLoader loader;
    @Mock private RulesEngineMetrics metrics;

    private RulesEngine engine;
    private EvaluationContext ctx;

    private static final Rule HIGH_AMOUNT = new Rule(
            "HIGH_AMOUNT", "Large txn", "fraud", true, 50,
            new Condition.Leaf("amount", Operator.GT, new BigDecimal("10000"), null));

    private static final Rule BLACKLISTED = new Rule(
            "BLACKLISTED_MERCHANT", "Blacklisted", "fraud", true, 80,
            new Condition.Leaf("isBlacklisted", Operator.EQUALS, true, null));

    private static final Rule DISABLED = new Rule(
            "DISABLED_RULE", "Should not run", "fraud", false, 100,
            new Condition.Leaf("amount", Operator.GT, new BigDecimal("1"), null));

    @BeforeEach
    void setUp() {
        when(loader.load()).thenReturn(new RuleSet(List.of(HIGH_AMOUNT, BLACKLISTED, DISABLED), "v1", 70));
        engine = new RulesEngine(loader, new ConditionEvaluator(), metrics);
        engine.init();

        ctx = new EvaluationContext();
        ctx.put("amount", new BigDecimal("15000"));
        ctx.put("isBlacklisted", false);
    }

    @Test
    void matches_rules_whose_condition_passes() {
        ScoringResult result = engine.evaluate(ctx);

        assertThat(result.totalScore()).isEqualTo(50);
        assertThat(result.matchedRules()).hasSize(1);
        assertThat(result.matchedRules().get(0).id()).isEqualTo("HIGH_AMOUNT");
    }

    @Test
    void accumulates_score_from_multiple_matched_rules() {
        ctx.put("isBlacklisted", true);
        ScoringResult result = engine.evaluate(ctx);

        assertThat(result.totalScore()).isEqualTo(130);
        assertThat(result.matchedRules()).hasSize(2);
    }

    @Test
    void disabled_rules_are_skipped() {
        ScoringResult result = engine.evaluate(ctx);

        assertThat(result.matchedRules()).extracting("id").doesNotContain("DISABLED_RULE");
    }

    @Test
    void no_rules_match_returns_zero_score() {
        ctx.put("amount", new BigDecimal("100"));
        ScoringResult result = engine.evaluate(ctx);

        assertThat(result.totalScore()).isZero();
        assertThat(result.matchedRules()).isEmpty();
    }

    @Test
    void degraded_mode_propagated_from_context() {
        ctx.put("_degradedMode", true);
        ScoringResult result = engine.evaluate(ctx);

        assertThat(result.degradedMode()).isTrue();
    }

    @Test
    void records_match_metric_for_each_matched_rule() {
        ctx.put("isBlacklisted", true);
        engine.evaluate(ctx);

        verify(metrics).recordRuleMatch("HIGH_AMOUNT");
        verify(metrics).recordRuleMatch("BLACKLISTED_MERCHANT");
        verifyNoMoreInteractions(metrics);
    }

    @Test
    void rule_evaluation_exception_does_not_abort_remaining_rules() {
        // Replace HIGH_AMOUNT with a rule that has a null condition to force exception
        Rule badRule = new Rule("BAD_RULE", "", "fraud", true, 50, null);
        when(loader.load()).thenReturn(new RuleSet(List.of(badRule, BLACKLISTED), "v2", 70));
        engine.init();
        ctx.put("isBlacklisted", true);

        ScoringResult result = engine.evaluate(ctx);

        // BAD_RULE threw, BLACKLISTED still ran
        assertThat(result.matchedRules()).extracting("id").containsExactly("BLACKLISTED_MERCHANT");
    }

    @Test
    void reload_hot_swaps_active_rule_set() {
        RuleSet newSet = new RuleSet(List.of(DISABLED), "v2", 60);
        engine.reload(newSet);

        assertThat(engine.activeRuleSet().version()).isEqualTo("v2");
    }
}
