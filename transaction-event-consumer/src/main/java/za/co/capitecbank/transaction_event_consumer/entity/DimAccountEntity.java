package za.co.capitecbank.transaction_event_consumer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dim_account", schema = "rules_engine")
public class DimAccountEntity extends ScdBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_key")
    private Long accountKey;

    @Column(name = "account_nr", nullable = false)
    private Long accountNr;

    @Column(name = "cif_nr", nullable = false)
    private Long cifNr;

    @Column(name = "branch")
    private Integer branch;
}
