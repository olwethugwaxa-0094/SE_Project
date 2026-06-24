package za.co.capitecbank.transaction_event_consumer.rules.config;

import java.util.List;
import java.util.Map;

public class RuleConditionProperties {

    private String field;
    private String op;
    private Object value;
    private List<Object> values;
    private List<RuleConditionProperties> all;
    private List<RuleConditionProperties> any;
    private RuleConditionProperties not;

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }

    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }

    public List<Object> getValues() { return values; }
    public void setValues(List<Object> values) { this.values = values; }

    public List<RuleConditionProperties> getAll() { return all; }
    public void setAll(List<RuleConditionProperties> all) { this.all = all; }

    public List<RuleConditionProperties> getAny() { return any; }
    public void setAny(List<RuleConditionProperties> any) { this.any = any; }

    public RuleConditionProperties getNot() { return not; }
    public void setNot(RuleConditionProperties not) { this.not = not; }
}
