package za.co.capitecbank.transaction_event_consumer.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import za.co.capitecbank.transaction_event_consumer.domain.*;
import za.co.capitecbank.transaction_event_consumer.entity.SrcTransactionEventEntity;

import java.time.LocalDateTime;

@Component
public class SrcTransactionEventMapper {

    public SrcTransactionEventEntity toEntity(
            ConsumerRecord<String, String> record,
            ValidationResult result) {

        SrcTransactionEventEntity entity = new SrcTransactionEventEntity();
        entity.setKafkaKey(record.key());
        entity.setKafkaTopic(record.topic());
        entity.setKafkaPartition(record.partition());
        entity.setKafkaOffset(record.offset());
        entity.setRawPayload(record.value());
        entity.setReceivedAt(LocalDateTime.now());

        if (result instanceof ValidationResult.Valid valid && valid.event() != null) {
            TransactionEvent e = valid.event();
            entity.setEventId(e.transactionEventId());
            entity.setEventType(e.transactionEventType() != null ? e.transactionEventType().name() : null);

            if (e.transactionMetadata() != null) {
                entity.setTransactionId(e.transactionMetadata().transactionId());
                entity.setTransactionDate(e.transactionMetadata().transactionDate());
                entity.setPostingDate(e.transactionMetadata().postingDate());
                entity.setAmount(e.transactionMetadata().amount());
                entity.setBalance(e.transactionMetadata().balance());
            }

            if (e.paymentDetails() != null) {
                entity.setChannel(e.paymentDetails().channel());
                entity.setTrancode(e.paymentDetails().trancode());
                entity.setTrantypedesc(e.paymentDetails().trantypedesc());
                entity.setMoneyIn(e.paymentDetails().moneyIn());
                entity.setCardNr(e.paymentDetails().cardNr());
            }

            if (e.merchantData() != null) {
                entity.setMerchantName(e.merchantData().merchantName());
                entity.setMerchantDesc(e.merchantData().merchantDesc());
                entity.setMerchantCategoryCode(e.merchantData().merchantCategoryCode());
                entity.setCity(e.merchantData().city());
                entity.setProvince(e.merchantData().province());
            }

            if (e.clientData() != null) {
                entity.setCifNr(e.clientData().cifNr());
                entity.setAccountNr(e.clientData().accountNr());
                entity.setBranch(e.clientData().branch());
            }

            if (e.authentication() != null) {
                entity.setAuthTraceId(e.authentication().authTraceId());
                entity.setCardAuthStatus(e.authentication().cardAuthStatus());
            }
        }

        return entity;
    }
}
