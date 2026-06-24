package za.co.capitecbank.transaction_event_consumer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.capitecbank.transaction_event_consumer.entity.FactScoredTransactionEntity;

public interface FactScoredTransactionRepository extends JpaRepository<FactScoredTransactionEntity, Long> {

    boolean existsByCorrelationId(String correlationId);
}
