package za.co.capitecbank.transaction_event_consumer.rules.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import za.co.capitecbank.transaction_event_consumer.rules.RuleSetLoader;
import za.co.capitecbank.transaction_event_consumer.rules.RulesEngine;
import za.co.capitecbank.transaction_event_consumer.rules.model.RuleSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class RulesRefreshListener {

    private final RulesEngine rulesEngine;
    private final RuleSetLoader ruleSetLoader;

    @EventListener
    public void onEnvironmentChange(EnvironmentChangeEvent event) {
        boolean rulesChanged = event.getKeys().stream()
                .anyMatch(k -> k.startsWith("rules-engine."));

        if (!rulesChanged) {
            return;
        }

        log.info("rules-engine config changed (keys={}), reloading rule set", event.getKeys());
        try {
            RuleSet newRuleSet = ruleSetLoader.load();
            rulesEngine.reload(newRuleSet);
        } catch (Exception e) {
            log.error("Failed to reload rule set after config change — keeping existing rules", e);
        }
    }
}
