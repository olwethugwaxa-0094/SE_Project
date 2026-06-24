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
@Table(name = "fact_transaction", schema = "rules_engine")
public class FactTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fact_key")
    private Long factKey;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // TransactionEvent header
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "event_type")
    private String eventType;

    // TransactionMetadata
    @Column(name = "transaction_id", nullable = false, unique = true)
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

    // Dimension FKs (nullable for INVALID)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_key")
    private DimClientEntity client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_key")
    private DimAccountEntity account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_key")
    private DimMerchantEntity merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_key")
    private DimPaymentChannelEntity channelDim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_key")
    private DimTransactionAuthEntity auth;

    @Embedded
    private HouseKeeping houseKeeping;
}
