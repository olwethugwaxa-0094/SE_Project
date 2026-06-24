package za.co.capitecbank.transaction_event_consumer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.capitecbank.transaction_event_consumer.entity.DimAccountEntity;

import java.util.Optional;

public interface DimAccountRepository extends JpaRepository<DimAccountEntity, Long> {

    Optional<DimAccountEntity> findByAccountNrAndCurrentTrue(Long accountNr);
}
