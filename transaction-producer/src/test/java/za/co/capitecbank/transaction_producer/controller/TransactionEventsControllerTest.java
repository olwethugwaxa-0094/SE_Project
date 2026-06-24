package za.co.capitecbank.transaction_producer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import za.co.capitecbank.transaction_producer.domain.TransactionEvent;
import za.co.capitecbank.transaction_producer.service.TransactionEventProducer;
import za.co.capitecbank.transaction_producer.utils.TestUtils;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionEventsController.class)
class TransactionEventsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionEventProducer transactionEventProducer;

    @Test
    void postTransactionEvent_returns202_when_send_succeeds() throws Exception {
        var json = objectMapper.writeValueAsString(TestUtils.transactionEventRecord());

        when(transactionEventProducer.sendTransactionEvent(isA(TransactionEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/transactionevent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    void postTransactionEvent_returns202_when_send_fails_async() throws Exception {
        var json = objectMapper.writeValueAsString(TestUtils.transactionEventRecord());

        when(transactionEventProducer.sendTransactionEvent(isA(TransactionEvent.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable")));

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/transactionevent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    void postTransactionEvent_returns500_when_serialization_fails() throws Exception {
        var json = objectMapper.writeValueAsString(TestUtils.transactionEventRecord());

        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("Serialization failed"));

        when(transactionEventProducer.sendTransactionEvent(isA(TransactionEvent.class)))
                .thenReturn(failed);

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/transactionevent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }
}