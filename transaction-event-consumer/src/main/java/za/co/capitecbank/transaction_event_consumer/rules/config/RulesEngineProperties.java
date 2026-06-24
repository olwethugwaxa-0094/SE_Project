package za.co.capitecbank.transaction_event_consumer.rules.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "rules-engine")
public class RulesEngineProperties {

    private int scoreThreshold = 70;
    private String version = "local";
    private List<RuleProperties> rules = List.of();

    public int getScoreThreshold() { return scoreThreshold; }
    public void setScoreThreshold(int scoreThreshold) { this.scoreThreshold = scoreThreshold; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public List<RuleProperties> getRules() { return rules; }
    public void setRules(List<RuleProperties> rules) { this.rules = rules; }
}
