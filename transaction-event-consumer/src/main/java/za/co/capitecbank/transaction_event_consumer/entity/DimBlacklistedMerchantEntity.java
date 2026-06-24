package za.co.capitecbank.transaction_event_consumer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import za.co.capitecbank.transaction_event_consumer.domain.BlacklistSource;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "dim_blacklisted_merchant", schema = "rules_engine")
public class DimBlacklistedMerchantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blacklist_key")
    private Long blacklistKey;

    @Column(name = "merchant_name", nullable = false)
    private String merchantName;

    @Column(name = "merchant_category_code")
    private Integer merchantCategoryCode;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private BlacklistSource source;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "blacklisted_at", nullable = false, updatable = false)
    private LocalDateTime blacklistedAt;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Embedded
    private HouseKeeping houseKeeping;
}
