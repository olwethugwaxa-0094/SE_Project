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
    name = "src_transaction_event",
    schema = "rules_engine",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_src_event_partition_offset",
        columnNames = {"kafka_topic", "kafka_partition", "kafka_offset"}
    )
)
public class SrcTransactionEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Kafka provenance
    @Column(name = "kafka_key")
    private String kafkaKey;

    @Column(name = "kafka_topic", nullable = false)
    private String kafkaTopic;

    @Column(name = "kafka_partition", nullable = false)
    private Integer kafkaPartition;

    @Column(name = "kafka_offset", nullable = false)
    private Long kafkaOffset;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    // TransactionEvent header
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "event_type")
    private String eventType;

    // TransactionMetadata
    @Column(name = "transaction_id")
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

    @Embedded
    private HouseKeeping houseKeeping;
}
