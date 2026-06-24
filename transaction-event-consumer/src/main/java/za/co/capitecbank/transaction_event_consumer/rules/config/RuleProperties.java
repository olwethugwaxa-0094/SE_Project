package za.co.capitecbank.transaction_event_consumer.rules.config;

public class RuleProperties {

    private String id;
    private String description;
    private String owner;
    private boolean enabled;
    private int score;
    private RuleConditionProperties condition;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public RuleConditionProperties getCondition() { return condition; }
    public void setCondition(RuleConditionProperties condition) { this.condition = condition; }
}
