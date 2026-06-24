package za.co.capitecbank.transaction_event_consumer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.capitecbank.transaction_event_consumer.entity.DimClientEntity;

import java.util.Optional;

public interface DimClientRepository extends JpaRepository<DimClientEntity, Long> {

    Optional<DimClientEntity> findByCifNrAndCurrentTrue(Long cifNr);
}
