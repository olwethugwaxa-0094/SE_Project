package za.co.capitecbank.transaction_event_consumer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import za.co.capitecbank.transaction_event_consumer.domain.MatchedRule;
import za.co.capitecbank.transaction_event_consumer.domain.ScoredEvent;
import za.co.capitecbank.transaction_event_consumer.domain.TransactionEvent;
import za.co.capitecbank.transaction_event_consumer.domain.ValidationResult;
import za.co.capitecbank.transaction_event_consumer.entity.FactTransactionEntity;
import za.co.capitecbank.transaction_event_consumer.entity.HouseKeeping;
import za.co.capitecbank.transaction_event_consumer.repository.FactTransactionRepository;
import za.co.capitecbank.transaction_event_consumer.rules.EvaluationContext;
import za.co.capitecbank.transaction_event_consumer.rules.EvaluationContextBuilder;
import za.co.capitecbank.transaction_event_consumer.rules.RulesEngine;
import za.co.capitecbank.transaction_event_consumer.rules.model.ScoringResult;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProcessingService {

    private final FactTransactionRepository factTransactionRepository;
    private final DimensionResolutionService dimensionResolutionService;
    private final EvaluationContextBuilder contextBuilder;
    private final RulesEngine rulesEngine;
    private final RulesEngineMetrics metrics;

    public ScoredEvent processValid(TransactionEvent event, ConsumerRecord<String, String> record, String correlationId) {
        String transactionId = event.transactionMetadata().transactionId();

        if (factTransactionRepository.existsByTransactionId(transactionId)) {
            log.info("Skipping duplicate transactionId={}", transactionId);
            metrics.recordIdempotencySkip();
            return null;
        }

        var procSample = metrics.startProcessing();

        FactTransactionEntity fact = buildFact(event, correlationId, "VALID");
        factTransactionRepository.save(fact);

        log.info("Persisted fact_transaction VALID transactionId={}", transactionId);

        var evalSample = metrics.startRulesEval();
        EvaluationContext ctx = contextBuilder.build(event);
        ctx.put("correlationId", correlationId);
        ScoringResult result = rulesEngine.evaluate(ctx);
        metrics.stopRulesEval(evalSample);

        metrics.stopProcessing(procSample);

        return new ScoredEvent(
                correlationId,
                transactionId,
                event.clientData().cifNr(),
                event.transactionMetadata().transactionDate(),
                result.totalScore(),
                result.matchedRules(),
                rulesEngine.activeRuleSet().version(),
                result.degradedMode(),
                event
        );
    }

    public ScoredEvent processInvalid(ValidationResult.Invalid invalid, ConsumerRecord<String, String> record, String correlationId) {
        TransactionEvent event = invalid.event();

        String transactionId = event != null
                && event.transactionMetadata() != null
                && event.transactionMetadata().transactionId() != null
                ? event.transactionMetadata().transactionId()
                : (record.key() != null ? record.key() : correlationId);

        if (factTransactionRepository.existsByTransactionId(transactionId)) {
            log.info("Skipping duplicate invalid transactionId={}", transactionId);
            metrics.recordIdempotencySkip();
            return null;
        }

        FactTransactionEntity fact;
        if (event != null) {
            fact = buildFact(event, correlationId, "INVALID");
        } else {
            fact = new FactTransactionEntity();
            fact.setTransactionId(transactionId);
            fact.setCorrelationId(correlationId);
            fact.setStatus("INVALID");
            fact.setHouseKeeping(buildHouseKeeping(correlationId));
        }
        factTransactionRepository.save(fact);

        log.warn("Persisted fact_transaction INVALID transactionId={}, reason={}", transactionId, invalid.reason());

        Long cifNr = event != null && event.clientData() != null ? event.clientData().cifNr() : null;
        LocalDateTime txnDate = event != null && event.transactionMetadata() != null
                ? event.transactionMetadata().transactionDate() : null;

        return new ScoredEvent(
                correlationId,
                transactionId,
                cifNr,
                txnDate,
                100,
                List.of(new MatchedRule("VALIDATION_FAILURE: " + invalid.reason(), 100)),
                "SYSTEM",
                false,
                event
        );
    }

    private FactTransactionEntity buildFact(TransactionEvent event, String correlationId, String status) {
        var meta    = event.transactionMetadata();
        var payment = event.paymentDetails();
        var client  = event.clientData();
        var merchant = event.merchantData();
        var auth    = event.authentication();

        FactTransactionEntity fact = new FactTransactionEntity();
        fact.setCorrelationId(correlationId);
        fact.setStatus(status);
        fact.setEventId(event.transactionEventId());
        fact.setEventType(event.transactionEventType() != null ? event.transactionEventType().name() : null);
        fact.setHouseKeeping(buildHouseKeeping(correlationId));

        // TransactionMetadata
        fact.setTransactionId(meta.transactionId());
        fact.setTransactionDate(meta.transactionDate());
        fact.setPostingDate(meta.postingDate());
        fact.setAmount(meta.amount());
        fact.setBalance(meta.balance());

        // PaymentDetails
        fact.setChannel(payment.channel());
        fact.setTrancode(payment.trancode());
        fact.setTrantypedesc(payment.trantypedesc());
        fact.setMoneyIn(payment.moneyIn());
        fact.setCardNr(payment.cardNr());

        // MerchantData
        if (merchant != null) {
            fact.setMerchantName(merchant.merchantName());
            fact.setMerchantDesc(merchant.merchantDesc());
            fact.setMerchantCategoryCode(merchant.merchantCategoryCode());
            fact.setCity(merchant.city());
            fact.setProvince(merchant.province());
        }

        // ClientData
        fact.setCifNr(client.cifNr());
        fact.setAccountNr(client.accountNr());
        fact.setBranch(client.branch());

        // Authentication
        if (auth != null) {
            fact.setAuthTraceId(auth.authTraceId());
            fact.setCardAuthStatus(auth.cardAuthStatus());
        }

        // Dimension FKs
        fact.setClient(dimensionResolutionService.resolveClient(client.cifNr()));
        fact.setAccount(dimensionResolutionService.resolveAccount(client.accountNr(), client.cifNr(), client.branch()));
        fact.setMerchant(dimensionResolutionService.resolveMerchant(merchant));
        if (payment != null) {
            fact.setChannelDim(dimensionResolutionService.resolveChannel(payment.channel()));
        }
        fact.setAuth(dimensionResolutionService.resolveAuth(auth));

        return fact;
    }

    private HouseKeeping buildHouseKeeping(String correlationId) {
        return HouseKeeping.builder()
                .eBatchId(correlationId)
                .eIngestId(UUID.randomUUID())
                .eOperation("INSERT")
                .eSourceSystem("transaction-event-consumer")
                .eRowHash(Integer.toHexString(correlationId.hashCode()))
                .eLoadedAt(OffsetDateTime.now())
                .build();
    }
}
