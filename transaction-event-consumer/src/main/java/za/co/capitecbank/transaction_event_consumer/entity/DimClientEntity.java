package za.co.capitecbank.transaction_event_consumer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dim_client", schema = "rules_engine")
public class DimClientEntity extends ScdBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_key")
    private Long clientKey;

    @Column(name = "cif_nr", nullable = false)
    private Long cifNr;
}
