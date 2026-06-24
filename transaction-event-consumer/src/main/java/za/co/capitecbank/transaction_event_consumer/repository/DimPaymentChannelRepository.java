package za.co.capitecbank.transaction_event_consumer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.capitecbank.transaction_event_consumer.entity.DimPaymentChannelEntity;

import java.util.Optional;

public interface DimPaymentChannelRepository extends JpaRepository<DimPaymentChannelEntity, Long> {

    Optional<DimPaymentChannelEntity> findByChannelCode(String channelCode);
}
