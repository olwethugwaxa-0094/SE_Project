package za.co.capitecbank.transaction_event_consumer.rules.model;

import java.util.List;

public sealed interface Condition {

    record Leaf(String field, Operator op, Object value, List<Object> values) implements Condition {}

    record All(List<Condition> conditions) implements Condition {}

    record Any(List<Condition> conditions) implements Condition {}

    record Not(Condition condition) implements Condition {}
}
