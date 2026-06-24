package za.co.capitecbank.transaction_event_consumer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dim_card_auth_status", schema = "rules_engine")
public class DimCardAuthStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_status_key")
    private Long authStatusKey;

    @Column(name = "status_code", nullable = false, unique = true)
    private String statusCode;

    @Column(name = "status_desc", nullable = false)
    private String statusDesc;

    @Embedded
    private HouseKeeping houseKeeping;
}
