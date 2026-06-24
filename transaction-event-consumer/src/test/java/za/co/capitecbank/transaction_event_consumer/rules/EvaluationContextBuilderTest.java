package za.co.capitecbank.transaction_event_consumer.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.capitecbank.transaction_event_consumer.domain.*;
import za.co.capitecbank.transaction_event_consumer.entity.DimBlacklistedMerchantEntity;
import za.co.capitecbank.transaction_event_consumer.repository.DimBlacklistedMerchantRepository;
import za.co.capitecbank.transaction_event_consumer.velocity.VelocityResult;
import za.co.capitecbank.transaction_event_consumer.velocity.VelocityService;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationContextBuilderTest {

    @Mock private VelocityService velocityService;
    @Mock private DimBlacklistedMerchantRepository blacklistRepository;

    private EvaluationContextBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new EvaluationContextBuilder(velocityService, blacklistRepository);
        when(velocityService.fetchAndRecord(anyString(), any(), anyString()))
                .thenReturn(new VelocityResult(2, 5, 12, false));
        when(blacklistRepository.findByMerchantNameAndIsActiveTrue(anyString()))
                .thenReturn(Optional.empty());
    }

    @Test
    void raw_transaction_fields_are_mapped() {
        EvaluationContext ctx = builder.build(event("Shoprite"));

        assertThat(ctx.get("transactionId")).isEqualTo("txn-001");
        assertThat(ctx.get("amount")).isEqualTo(new BigDecimal("250.00"));
        assertThat(ctx.get("channel")).isEqualTo("POS");
        assertThat(ctx.get("cifNr")).isEqualTo(100001L);
    }

    @Test
    void derived_time_fields_are_populated() {
        LocalDateTime fixedDate = LocalDateTime.of(2026, 5, 20, 14, 30);
        EvaluationContext ctx = builder.build(eventWithDate(fixedDate));

        assertThat(ctx.get("hourOfDay")).isEqualTo(14);
        assertThat(ctx.get("dayOfWeek")).isEqualTo(DayOfWeek.WEDNESDAY.getValue());
    }

    @Test
    void is_blacklisted_true_when_merchant_found() {
        when(blacklistRepository.findByMerchantNameAndIsActiveTrue("BadMerchant"))
                .thenReturn(Optional.of(new DimBlacklistedMerchantEntity()));

        EvaluationContext ctx = builder.build(event("BadMerchant"));

        assertThat(ctx.get("isBlacklisted")).isEqualTo(true);
    }

    @Test
    void is_blacklisted_false_when_merchant_not_found() {
        EvaluationContext ctx = builder.build(event("Shoprite"));

        assertThat(ctx.get("isBlacklisted")).isEqualTo(false);
    }

    @Test
    void velocity_counts_are_populated_from_service() {
        EvaluationContext ctx = builder.build(event("Shoprite"));

        assertThat(ctx.get("recentTxnCount10m")).isEqualTo(2L);
        assertThat(ctx.get("recentTxnCount1h")).isEqualTo(5L);
        assertThat(ctx.get("recentTxnCount24h")).isEqualTo(12L);
        assertThat(ctx.get("_degradedMode")).isEqualTo(false);
    }

    @Test
    void degraded_mode_propagated_when_velocity_degraded() {
        when(velocityService.fetchAndRecord(anyString(), any(), anyString()))
                .thenReturn(VelocityResult.degraded(3, 7, 20));

        EvaluationContext ctx = builder.build(event("Shoprite"));

        assertThat(ctx.get("_degradedMode")).isEqualTo(true);
    }

    // --- Helpers ---

    private TransactionEvent event(String merchantName) {
        return eventWithDate(LocalDateTime.of(2026, 5, 20, 14, 30), merchantName);
    }

    private TransactionEvent eventWithDate(LocalDateTime date) {
        return eventWithDate(date, "Shoprite");
    }

    private TransactionEvent eventWithDate(LocalDateTime date, String merchantName) {
        return new TransactionEvent(
                "evt-001",
                TransactionEventType.NEW,
                new TransactionMetadata("txn-001", date, LocalDate.now(),
                        new BigDecimal("250.00"), new BigDecimal("1500.00")),
                new PaymentDetails("POS", 1001, "Purchase", true, "****1234"),
                new MerchantData(merchantName, "Grocery", 5411, "Cape Town", "Western Cape"),
                new ClientData(100001L, 4000000001L, 1),
                new Authentication("auth-001", "APPROVED"));
    }
}
