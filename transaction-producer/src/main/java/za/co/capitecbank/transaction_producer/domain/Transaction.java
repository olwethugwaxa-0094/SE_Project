package za.co.capitecbank.transaction_producer.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "transactions", schema = "payments")
public class Transaction {

    @Id
    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance", precision = 19, scale = 4)
    private BigDecimal balance;

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

    @Column(name = "cif_nr", nullable = false)
    private Long cifNr;

    @Column(name = "account_nr", nullable = false)
    private Long accountNr;

    @Column(name = "branch")
    private Integer branch;

    @Column(name = "auth_trace_id")
    private String authTraceId;

    @Column(name = "card_auth_status")
    private String cardAuthStatus;
}
