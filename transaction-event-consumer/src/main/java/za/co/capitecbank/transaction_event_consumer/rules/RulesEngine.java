package za.co.capitecbank.transaction_event_consumer.rules;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.co.capitecbank.transaction_event_consumer.domain.MatchedRule;
import za.co.capitecbank.transaction_event_consumer.rules.model.RuleSet;
import za.co.capitecbank.transaction_event_consumer.rules.model.ScoringResult;
import za.co.capitecbank.transaction_event_consumer.service.RulesEngineMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class RulesEngine {

    private final RuleSetLoader loader;
    private final ConditionEvaluator evaluator;
    private final RulesEngineMetrics metrics;
    private final AtomicReference<RuleSet> activeRuleSet = new AtomicReference<>();

    @PostConstruct
    public void init() {
        RuleSet ruleSet = loader.load();
        activeRuleSet.set(ruleSet);
        log.info("Rules engine initialised — {} enabled rules, version={}",
                ruleSet.enabledRules().size(), ruleSet.version());
    }

    public ScoringResult evaluate(EvaluationContext ctx) {
        RuleSet ruleSet = activeRuleSet.get();
        boolean degradedMode = Boolean.TRUE.equals(ctx.get("_degradedMode"));

        List<MatchedRule> matched = new ArrayList<>();
        int totalScore = 0;

        for (var rule : ruleSet.enabledRules()) {
            try {
                log.debug("Evaluating rule '{}' condition '{}'", rule.id(), rule.condition());
                if (evaluator.evaluate(rule.condition(), ctx)) {
                    matched.add(new MatchedRule(rule.id(), rule.score()));
                    totalScore += rule.score();
                    metrics.recordRuleMatch(rule.id());
                }
            } catch (Exception e) {
                log.error("Error evaluating rule '{}': {}", rule.id(), e.getMessage(), e);
            }
        }

        log.info("Scoring complete — transactionId={}, score={}, matchedRules={}, version={}, degraded={}",
                ctx.get("transactionId"), totalScore, matched.stream().map(MatchedRule::id).toList(),
                ruleSet.version(), degradedMode);

        return new ScoringResult(totalScore, matched, degradedMode);
    }

    public RuleSet activeRuleSet() {
        return activeRuleSet.get();
    }

    public void reload(RuleSet newRuleSet) {
        activeRuleSet.set(newRuleSet);
        log.info("Rule set hot-swapped — version={}, rules={}", newRuleSet.version(), newRuleSet.enabledRules().size());
    }
}
