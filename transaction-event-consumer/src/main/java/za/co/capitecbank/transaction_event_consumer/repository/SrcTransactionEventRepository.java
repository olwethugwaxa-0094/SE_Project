package za.co.capitecbank.transaction_event_consumer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.capitecbank.transaction_event_consumer.entity.SrcTransactionEventEntity;

import java.util.Optional;

public interface SrcTransactionEventRepository extends JpaRepository<SrcTransactionEventEntity, Long> {

    Optional<SrcTransactionEventEntity> findByKafkaTopicAndKafkaPartitionAndKafkaOffset(
            String kafkaTopic, Integer kafkaPartition, Long kafkaOffset);

    boolean existsByKafkaTopicAndKafkaPartitionAndKafkaOffset(
            String kafkaTopic, Integer kafkaPartition, Long kafkaOffset);
}
