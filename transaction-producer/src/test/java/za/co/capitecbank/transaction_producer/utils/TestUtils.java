package za.co.capitecbank.transaction_producer.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import za.co.capitecbank.transaction_producer.domain.Authentication;
import za.co.capitecbank.transaction_producer.domain.ClientData;
import za.co.capitecbank.transaction_producer.domain.MerchantData;
import za.co.capitecbank.transaction_producer.domain.PaymentDetails;
import za.co.capitecbank.transaction_producer.domain.TransactionEvent;
import za.co.capitecbank.transaction_producer.domain.TransactionEventType;
import za.co.capitecbank.transaction_producer.domain.TransactionMetadata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestUtils {

    public static TransactionMetadata transactionMetadataRecord() {
        return new TransactionMetadata(
                "TXN-98234510B",
                LocalDateTime.parse("2023-10-27T14:22:01"),
                LocalDate.parse("2023-10-27"),
                new BigDecimal("125.50"),
                new BigDecimal("4520.75")
        );
    }

    public static TransactionMetadata transactionMetadataWithInvalidValues() {
        return new TransactionMetadata(null, null, null, null, null);
    }

    public static PaymentDetails paymentDetailsRecord() {
        return new PaymentDetails("CARD", 5401, "POS PURCHASE", false, "************4412");
    }

    public static MerchantData merchantDataRecord() {
        return new MerchantData("Blanda-Schaefer", "Office Supplies Co", 5943, "Washington", "DC");
    }

    public static ClientData clientDataRecord() {
        return new ClientData(773210L, 6201234567L, 251);
    }

    public static Authentication authenticationRecord() {
        return new Authentication("091225", "APPROVED");
    }

    public static TransactionEvent transactionEventRecord() {
        return new TransactionEvent(
                null,
                TransactionEventType.NEW,
                transactionMetadataRecord(),
                paymentDetailsRecord(),
                merchantDataRecord(),
                clientDataRecord(),
                authenticationRecord()
        );
    }

    public static TransactionEvent newTransactionEventRecordWithTransactionEventId() {
        return new TransactionEvent(
                "123",
                TransactionEventType.NEW,
                transactionMetadataRecord(),
                paymentDetailsRecord(),
                merchantDataRecord(),
                clientDataRecord(),
                authenticationRecord()
        );
    }

    public static TransactionEvent transactionEventRecordUpdate() {
        return new TransactionEvent(
                "123",
                TransactionEventType.UPDATE,
                transactionMetadataRecord(),
                paymentDetailsRecord(),
                merchantDataRecord(),
                clientDataRecord(),
                authenticationRecord()
        );
    }

    public static TransactionEvent transactionEventRecordUpdateWithNullTransactionEventId() {
        return new TransactionEvent(
                null,
                TransactionEventType.UPDATE,
                transactionMetadataRecord(),
                paymentDetailsRecord(),
                merchantDataRecord(),
                clientDataRecord(),
                authenticationRecord()
        );
    }

    public static TransactionEvent transactionEventRecordWithInvalidMetadata() {
        return new TransactionEvent(
                null,
                TransactionEventType.NEW,
                transactionMetadataWithInvalidValues(),
                paymentDetailsRecord(),
                merchantDataRecord(),
                clientDataRecord(),
                authenticationRecord()
        );
    }

    public static TransactionEvent parseTransactionEventRecord(ObjectMapper objectMapper, String json) {
        try {
            return objectMapper.readValue(json, TransactionEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
