package za.co.capitecbank.transaction_event_consumer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.capitecbank.transaction_event_consumer.entity.DimCardAuthStatusEntity;

import java.util.Optional;

public interface DimCardAuthStatusRepository extends JpaRepository<DimCardAuthStatusEntity, Long> {

    Optional<DimCardAuthStatusEntity> findByStatusCode(String statusCode);
}
