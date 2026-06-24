package za.co.capitecbank.transaction_event_consumer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.capitecbank.transaction_event_consumer.entity.DimMerchantEntity;

import java.util.Optional;

public interface DimMerchantRepository extends JpaRepository<DimMerchantEntity, Long> {

    Optional<DimMerchantEntity> findByMerchantNameAndCurrentTrue(String merchantName);
}
