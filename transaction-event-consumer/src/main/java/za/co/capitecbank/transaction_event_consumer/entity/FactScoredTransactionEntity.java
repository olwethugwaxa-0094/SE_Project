package za.co.capitecbank.transaction_event_consumer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
    name = "fact_scored_transaction",
    schema = "rules_engine",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_fact_scored_correlation_id",
        columnNames = {"correlation_id"}
    )
)
public class FactScoredTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scored_key")
    private Long scoredKey;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    // TransactionEvent header
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "event_type")
    private String eventType;

    // TransactionMetadata
    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "posting_date")
    private LocalDate postingDate;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance", precision = 19, scale = 4)
    private BigDecimal balance;

    // PaymentDetails
    @Column(name = "channel")
    private String channel;

    @Column(name = "trancode")
    private Integer trancode;

    @Column(name = "trantypedesc")
    private String trantypedesc;

    @Column(name = "money_in")
    private Boolean moneyIn;

    @Column(name = "card_nr")
    private String cardNr;

    // MerchantData
    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "merchant_desc")
    private String merchantDesc;

    @Column(name = "merchant_category_code")
    private Integer merchantCategoryCode;

    @Column(name = "city")
    private String city;

    @Column(name = "province")
    private String province;

    // ClientData
    @Column(name = "cif_nr")
    private Long cifNr;

    @Column(name = "account_nr")
    private Long accountNr;

    @Column(name = "branch")
    private Integer branch;

    // Authentication
    @Column(name = "auth_trace_id")
    private String authTraceId;

    @Column(name = "card_auth_status")
    private String cardAuthStatus;

    // Kafka provenance
    @Column(name = "kafka_key")
    private String kafkaKey;

    // Scoring output
    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "matched_rules", nullable = false, columnDefinition = "TEXT")
    private String matchedRules;

    @Column(name = "rule_set_version", nullable = false)
    private String ruleSetVersion;

    @Column(name = "degraded_mode", nullable = false)
    private Boolean degradedMode;

    @Column(name = "routed_to", nullable = false)
    private String routedTo;

    @Column(name = "scored_at", nullable = false, updatable = false)
    private LocalDateTime scoredAt;

    @Embedded
    private HouseKeeping houseKeeping;
}
