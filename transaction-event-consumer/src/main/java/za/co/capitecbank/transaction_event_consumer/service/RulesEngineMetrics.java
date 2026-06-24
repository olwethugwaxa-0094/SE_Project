package za.co.capitecbank.transaction_event_consumer.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class RulesEngineMetrics {

    // Consumer
    private final Counter consumerDeserialisationFailures;

    // Validator
    private final Timer validatorDuration;
    private final Counter validatorFailures;

    // Processing
    private final Timer procDuration;
    private final Counter procIdempotencySkips;

    // Rules evaluation
    private final Timer rulesEvalDuration;

    // Velocity
    private final Timer velocityQueryDuration;
    private final Counter velocityFallbackCount;

    // Per-rule match rate (lazily registered, tagged by rule id)
    private final MeterRegistry registry;
    private final ConcurrentMap<String, Counter> ruleMatchCounters = new ConcurrentHashMap<>();

    // Scored producer
    private final Timer scoredPersistDuration;
    private final Timer scoredPublishDuration;

    public RulesEngineMetrics(MeterRegistry registry) {
        this.registry = registry;
        consumerDeserialisationFailures = Counter.builder("consumer.deserialisation.failures")
                .description("Number of Kafka message deserialisation failures")
                .register(registry);

        validatorDuration = Timer.builder("validator.duration")
                .description("Time spent in TransactionEventValidator")
                .register(registry);

        validatorFailures = Counter.builder("validator.failures")
                .description("Number of validation failures")
                .register(registry);

        procDuration = Timer.builder("processing.duration")
                .description("Time spent in TransactionProcessingService")
                .register(registry);

        procIdempotencySkips = Counter.builder("processing.idempotency.skips")
                .description("Number of duplicate transactions skipped")
                .register(registry);

        rulesEvalDuration = Timer.builder("rules.eval.duration")
                .description("Time spent evaluating fraud rules")
                .register(registry);

        velocityQueryDuration = Timer.builder("velocity.query.duration")
                .description("Time spent in VelocityService")
                .register(registry);

        velocityFallbackCount = Counter.builder("velocity.fallback.count")
                .description("Number of times velocity fell back to DW query")
                .register(registry);

        scoredPersistDuration = Timer.builder("scored.persist.duration")
                .description("Time to persist fact_scored_transaction")
                .register(registry);

        scoredPublishDuration = Timer.builder("scored.publish.duration")
                .description("Time to publish to transaction.scored")
                .register(registry);
    }

    public void recordDeserialisationFailure() { consumerDeserialisationFailures.increment(); }

    public Timer.Sample startValidation() { return Timer.start(); }
    public void stopValidation(Timer.Sample sample) { sample.stop(validatorDuration); }
    public void recordValidationFailure() { validatorFailures.increment(); }

    public Timer.Sample startProcessing() { return Timer.start(); }
    public void stopProcessing(Timer.Sample sample) { sample.stop(procDuration); }
    public void recordIdempotencySkip() { procIdempotencySkips.increment(); }

    public Timer.Sample startRulesEval() { return Timer.start(); }
    public void stopRulesEval(Timer.Sample sample) { sample.stop(rulesEvalDuration); }

    public Timer.Sample startVelocityQuery() { return Timer.start(); }
    public void stopVelocityQuery(Timer.Sample sample) { sample.stop(velocityQueryDuration); }
    public void recordVelocityFallback() { velocityFallbackCount.increment(); }

    public Timer.Sample startScoredPersist() { return Timer.start(); }
    public void stopScoredPersist(Timer.Sample sample) { sample.stop(scoredPersistDuration); }

    public Timer.Sample startScoredPublish() { return Timer.start(); }
    public void stopScoredPublish(Timer.Sample sample) { sample.stop(scoredPublishDuration); }

    public void recordRuleMatch(String ruleId) {
        ruleMatchCounters.computeIfAbsent(ruleId, id ->
                Counter.builder("rules.match.count")
                        .tag("rule_id", id)
                        .description("Number of times a fraud rule matched")
                        .register(registry))
                .increment();
    }
}
