package za.co.capitecbank.transaction_producer.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import za.co.capitecbank.transaction_producer.domain.TransactionEvent;
import za.co.capitecbank.transaction_producer.service.TransactionEventProducer;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionEventsController {

    private final TransactionEventProducer transactionEventProducer;

    @PostMapping("/v1/transactionevent")
    public ResponseEntity<TransactionEvent> postTransactionEvent(
            @RequestBody TransactionEvent transactionEvent) {

        var future = transactionEventProducer.sendTransactionEvent(transactionEvent);

        // Only serialization failures (IllegalStateException) are fatal — propagate as 500.
        // Transient Kafka send failures are async and fire-and-forget; return 202 regardless.
        if (future.isCompletedExceptionally()) {
            try {
                future.join();
            } catch (Exception e) {
                if (e.getCause() instanceof IllegalStateException) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            }
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(transactionEvent);
    }
}