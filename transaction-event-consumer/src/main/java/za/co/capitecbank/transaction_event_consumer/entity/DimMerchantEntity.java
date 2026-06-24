package za.co.capitecbank.transaction_event_consumer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dim_merchant", schema = "rules_engine")
public class DimMerchantEntity extends ScdBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merchant_key")
    private Long merchantKey;

    @Column(name = "merchant_name", nullable = false)
    private String merchantName;

    @Column(name = "merchant_desc")
    private String merchantDesc;

    @Column(name = "merchant_category_code")
    private Integer merchantCategoryCode;

    @Column(name = "city")
    private String city;

    @Column(name = "province")
    private String province;
}
