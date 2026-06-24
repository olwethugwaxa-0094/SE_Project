package za.co.capitecbank.transaction_event_consumer.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${spring.kafka.listener.concurrency}")
    private int concurrency;

    @Value("${spring.kafka.topics.dlq}")
    private String dlqTopic;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {

        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(dlqTopic, -1));

        var backoff = new ExponentialBackOff(1000L, 2.0);
        backoff.setMaxAttempts(3);

        var errorHandler = new DefaultErrorHandler(recoverer, backoff);
        // Config/data errors — retrying won't help, send straight to DLQ
        errorHandler.addNotRetryableExceptions(IllegalStateException.class);

        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.setConcurrency(concurrency);
        return factory;
    }

    /**
     * KafkaClientMetrics implements Closeable and closes its consumer on close().
     * Declaring it as a bean lets Spring call close() on context shutdown.
     */
    @Bean(name = "customKafkaConsumerMetrics", destroyMethod = "close")
    public KafkaClientMetrics kafkaConsumerMetrics(ConsumerFactory<String, String> consumerFactory) {
        Consumer<String, String> consumer = consumerFactory.createConsumer();
        KafkaClientMetrics binder = new KafkaClientMetrics(consumer);
        binder.bindTo(meterRegistry);
        return binder;
    }
}
