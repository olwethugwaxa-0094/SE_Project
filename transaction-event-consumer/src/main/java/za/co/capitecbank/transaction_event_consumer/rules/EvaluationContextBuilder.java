package za.co.capitecbank.transaction_event_consumer.rules;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.capitecbank.transaction_event_consumer.domain.TransactionEvent;
import za.co.capitecbank.transaction_event_consumer.repository.DimBlacklistedMerchantRepository;
import za.co.capitecbank.transaction_event_consumer.velocity.VelocityResult;
import za.co.capitecbank.transaction_event_consumer.velocity.VelocityService;

@Component
@RequiredArgsConstructor
public class EvaluationContextBuilder {

    private final VelocityService velocityService;
    private final DimBlacklistedMerchantRepository blacklistRepository;

    public EvaluationContext build(TransactionEvent event) {
        var meta    = event.transactionMetadata();
        var payment = event.paymentDetails();
        var client  = event.clientData();
        var merchant = event.merchantData();
        var auth    = event.authentication();

        EvaluationContext ctx = new EvaluationContext();

        // Raw transaction fields
        ctx.put("transactionId",       meta.transactionId());
        ctx.put("cifNr",               client.cifNr());
        ctx.put("accountNr",           client.accountNr());
        ctx.put("transactionDate",     meta.transactionDate());
        ctx.put("postingDate",         meta.postingDate());
        ctx.put("amount",              meta.amount());
        ctx.put("balance",             meta.balance());
        ctx.put("channel",             payment.channel());
        ctx.put("trancode",            payment.trancode());
        ctx.put("trantypedesc",        payment.trantypedesc());
        ctx.put("moneyIn",             payment.moneyIn());
        ctx.put("cardAuthStatus",      auth != null ? auth.cardAuthStatus() : null);
        ctx.put("transactionEventType", event.transactionEventType() != null ? event.transactionEventType().name() : null);

        if (merchant != null) {
            ctx.put("merchantName",         merchant.merchantName());
            ctx.put("merchantCategoryCode", merchant.merchantCategoryCode());
            ctx.put("city",                 merchant.city());
            ctx.put("province",             merchant.province());
        }

        // Derived: time fields
        if (meta.transactionDate() != null) {
            ctx.put("hourOfDay",  meta.transactionDate().getHour());
            ctx.put("dayOfWeek",  meta.transactionDate().getDayOfWeek().getValue());
        }

        // Derived: blacklist check
        boolean isBlacklisted = merchant != null
                && merchant.merchantName() != null
                && blacklistRepository.findByMerchantNameAndIsActiveTrue(merchant.merchantName()).isPresent();
        ctx.put("isBlacklisted", isBlacklisted);

        // Derived: velocity counts
        String cifNrStr = client.cifNr() != null ? client.cifNr().toString() : "unknown";
        VelocityResult velocity = velocityService.fetchAndRecord(
                cifNrStr, meta.transactionDate(), meta.transactionId());
        ctx.put("recentTxnCount10m",  velocity.recentTxnCount10m());
        ctx.put("recentTxnCount1h",   velocity.recentTxnCount1h());
        ctx.put("recentTxnCount24h",  velocity.recentTxnCount24h());
        ctx.put("_degradedMode",      velocity.degradedMode());

        return ctx;
    }
}
