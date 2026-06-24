package za.co.capitecbank.transaction_event_consumer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"transaction-events"})
class TransactionEventConsumerApplicationTests {

    @Test
    void contextLoads() {
    }
}
