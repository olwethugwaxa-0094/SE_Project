package za.co.capitecbank.transaction_event_consumer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dim_transaction_auth", schema = "rules_engine")
public class DimTransactionAuthEntity extends ScdBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_key")
    private Long authKey;

    @Column(name = "auth_trace_id", nullable = false)
    private String authTraceId;

    @Column(name = "card_nr")
    private String cardNr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_status_key", nullable = false)
    private DimCardAuthStatusEntity cardAuthStatus;
}
