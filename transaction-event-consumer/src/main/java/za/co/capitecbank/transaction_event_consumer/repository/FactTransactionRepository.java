package za.co.capitecbank.transaction_event_consumer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.capitecbank.transaction_event_consumer.entity.FactTransactionEntity;

import java.util.Optional;

public interface FactTransactionRepository extends JpaRepository<FactTransactionEntity, Long> {

    Optional<FactTransactionEntity> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);
}
