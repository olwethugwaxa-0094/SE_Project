package za.co.capitecbank.transaction_event_consumer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.capitecbank.transaction_event_consumer.entity.DimTransactionAuthEntity;

import java.util.Optional;

public interface DimTransactionAuthRepository extends JpaRepository<DimTransactionAuthEntity, Long> {

    Optional<DimTransactionAuthEntity> findByAuthTraceIdAndCurrentTrue(String authTraceId);
}
