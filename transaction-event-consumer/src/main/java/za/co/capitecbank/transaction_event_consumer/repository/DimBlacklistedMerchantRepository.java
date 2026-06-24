package za.co.capitecbank.transaction_event_consumer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.capitecbank.transaction_event_consumer.entity.DimBlacklistedMerchantEntity;
import za.co.capitecbank.transaction_event_consumer.domain.BlacklistSource;

import java.util.List;
import java.util.Optional;

public interface DimBlacklistedMerchantRepository extends JpaRepository<DimBlacklistedMerchantEntity, Long> {

    Optional<DimBlacklistedMerchantEntity> findByMerchantNameAndIsActiveTrue(String merchantName);

    List<DimBlacklistedMerchantEntity> findAllByIsActiveTrue();

    List<DimBlacklistedMerchantEntity> findAllBySourceAndIsActiveTrue(BlacklistSource source);
}
