package za.co.capitecbank.transaction_event_consumer.velocity;

import java.time.LocalDateTime;

public interface VelocityService {

    VelocityResult fetchAndRecord(String cifNr, LocalDateTime transactionDate, String transactionId);
}
