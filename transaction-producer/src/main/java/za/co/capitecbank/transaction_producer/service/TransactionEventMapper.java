package za.co.capitecbank.transaction_producer.service;

import org.springframework.stereotype.Component;
import za.co.capitecbank.transaction_producer.domain.*;

import java.util.UUID;

@Component
public class TransactionEventMapper {

    public TransactionEvent toEvent(Transaction t) {
        return new TransactionEvent(
                UUID.randomUUID().toString(),
                TransactionEventType.NEW,
                new TransactionMetadata(
                        t.getTransactionId(),
                        t.getTransactionDate(),
                        t.getPostingDate(),
                        t.getAmount(),
                        t.getBalance()
                ),
                new PaymentDetails(
                        t.getChannel(),
                        t.getTrancode(),
                        t.getTrantypedesc(),
                        t.getMoneyIn(),
                        t.getCardNr()
                ),
                new MerchantData(
                        t.getMerchantName(),
                        t.getMerchantDesc(),
                        t.getMerchantCategoryCode(),
                        t.getCity(),
                        t.getProvince()
                ),
                new ClientData(
                        t.getCifNr(),
                        t.getAccountNr(),
                        t.getBranch()
                ),
                new Authentication(
                        t.getAuthTraceId(),
                        t.getCardAuthStatus()
                )
        );
    }
}
