package za.co.capitecbank.transaction_event_consumer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicConfig {

    @Value("${spring.kafka.topics.dlq}")
    private String dlqTopic;

    @Value("${spring.kafka.topics.review}")
    private String reviewTopic;

    @Value("${spring.kafka.topics.approved}")
    private String approvedTopic;

    @Value("${spring.kafka.topics.scored}")
    private String scoredTopic;

    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name(dlqTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic reviewTopic() {
        return TopicBuilder.name(reviewTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic approvedTopic() {
        return TopicBuilder.name(approvedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic scoredTopic() {
        return TopicBuilder.name(scoredTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
