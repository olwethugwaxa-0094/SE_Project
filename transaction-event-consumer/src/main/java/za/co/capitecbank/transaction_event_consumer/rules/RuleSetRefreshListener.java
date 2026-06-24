package za.co.capitecbank.transaction_event_consumer.rules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import za.co.capitecbank.transaction_event_consumer.rules.model.RuleSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleSetRefreshListener {

    private final RuleSetLoader loader;
    private final RuleSetValidator validator;
    private final RulesEngine rulesEngine;

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefresh(RefreshScopeRefreshedEvent event) {
        log.info("Spring Cloud Config refresh detected — reloading rule set");
        try {
            RuleSet newRuleSet = loader.load();
            validator.validate(newRuleSet.rules());
            rulesEngine.reload(newRuleSet);
            log.info("Rule set hot-swapped successfully — version={}, rules={}",
                    newRuleSet.version(), newRuleSet.enabledRules().size());
        } catch (RuleSetValidator.InvalidRuleSetException e) {
            log.error("Incoming rule set is invalid — keeping current active rule set. Reason: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error reloading rule set — keeping current active rule set", e);
        }
    }
}
