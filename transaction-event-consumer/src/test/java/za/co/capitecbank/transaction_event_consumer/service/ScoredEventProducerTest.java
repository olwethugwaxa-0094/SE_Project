package za.co.capitecbank.transaction_event_consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import za.co.capitecbank.transaction_event_consumer.domain.MatchedRule;
import za.co.capitecbank.transaction_event_consumer.domain.ScoredEvent;
import za.co.capitecbank.transaction_event_consumer.entity.FactScoredTransactionEntity;
import za.co.capitecbank.transaction_event_consumer.repository.FactScoredTransactionRepository;
import za.co.capitecbank.transaction_event_consumer.rules.config.RulesEngineProperties;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoredEventProducerTest {

    @Mock private FactScoredTransactionRepository repository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;
    @Mock private RulesEngineProperties properties;
    @Mock private RulesEngineMetrics metrics;

    private ScoredEventProducer producer;

    private static final String SCORED_TOPIC   = "transaction.scored";
    private static final String REVIEW_TOPIC   = "transaction.review";
    private static final String APPROVED_TOPIC = "transaction.approved";

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        producer = new ScoredEventProducer(repository, kafkaTemplate, mapper, properties, metrics);

        // Inject @Value fields via reflection
        setField(producer, "scoredTopic",   SCORED_TOPIC);
        setField(producer, "reviewTopic",   REVIEW_TOPIC);
        setField(producer, "approvedTopic", APPROVED_TOPIC);

        lenient().when(properties.getScoreThreshold()).thenReturn(70);
        lenient().when(repository.existsByCorrelationId(anyString())).thenReturn(false);
        lenient().when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        lenient().when(metrics.startScoredPersist()).thenReturn(io.micrometer.core.instrument.Timer.start());
        lenient().when(metrics.startScoredPublish()).thenReturn(io.micrometer.core.instrument.Timer.start());
    }

    @Test
    void high_score_event_is_routed_to_review() {
        ScoredEvent event = scoredEvent("corr-001", 90);

        producer.produce(event, "key-001");

        verify(kafkaTemplate).send(eq(SCORED_TOPIC),  anyString(), anyString());
        verify(kafkaTemplate).send(eq(REVIEW_TOPIC),  anyString(), anyString());
        verify(kafkaTemplate, never()).send(eq(APPROVED_TOPIC), anyString(), anyString());
    }

    @Test
    void low_score_event_is_routed_to_approved() {
        ScoredEvent event = scoredEvent("corr-002", 30);

        producer.produce(event, "key-002");

        verify(kafkaTemplate).send(eq(SCORED_TOPIC),    anyString(), anyString());
        verify(kafkaTemplate).send(eq(APPROVED_TOPIC),  anyString(), anyString());
        verify(kafkaTemplate, never()).send(eq(REVIEW_TOPIC), anyString(), anyString());
    }

    @Test
    void duplicate_correlation_id_is_skipped() {
        when(repository.existsByCorrelationId("corr-dup")).thenReturn(true);

        producer.produce(scoredEvent("corr-dup", 50), "key-dup");

        verify(repository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void entity_is_persisted_before_publishing() {
        ScoredEvent event = scoredEvent("corr-003", 80);
        ArgumentCaptor<FactScoredTransactionEntity> captor =
                ArgumentCaptor.forClass(FactScoredTransactionEntity.class);

        producer.produce(event, "key-003");

        verify(repository).save(captor.capture());
        FactScoredTransactionEntity saved = captor.getValue();
        assertThat(saved.getCorrelationId()).isEqualTo("corr-003");
        assertThat(saved.getScore()).isEqualTo(80);
        assertThat(saved.getRoutedTo()).isEqualTo(REVIEW_TOPIC);
    }

    @Test
    void persist_and_publish_metrics_are_recorded() {
        producer.produce(scoredEvent("corr-004", 50), "key-004");

        verify(metrics).startScoredPersist();
        verify(metrics).startScoredPublish();
    }

    // --- Helpers ---

    private ScoredEvent scoredEvent(String correlationId, int score) {
        return new ScoredEvent(
                correlationId, "txn-" + correlationId, 100001L,
                LocalDateTime.now(), score,
                List.of(new MatchedRule("TEST_RULE", score)),
                "v1", false, null);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

}
