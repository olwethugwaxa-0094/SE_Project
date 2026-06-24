package za.co.capitecbank.transaction_event_consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TransactionEventConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionEventConsumerApplication.class, args);
    }
}
