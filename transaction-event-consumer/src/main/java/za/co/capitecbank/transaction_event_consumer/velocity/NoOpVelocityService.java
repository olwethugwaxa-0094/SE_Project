package za.co.capitecbank.transaction_event_consumer.velocity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@ConditionalOnProperty(name = "velocity.redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpVelocityService implements VelocityService {

    @Override
    public VelocityResult fetchAndRecord(String cifNr, LocalDateTime transactionDate, String transactionId) {
        log.debug("NoOpVelocityService: returning zero counts for cifNr={}", cifNr);
        return VelocityResult.zeros();
    }
}
