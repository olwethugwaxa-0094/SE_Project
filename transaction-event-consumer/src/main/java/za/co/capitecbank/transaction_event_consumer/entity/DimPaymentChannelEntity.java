package za.co.capitecbank.transaction_event_consumer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dim_payment_channel", schema = "rules_engine")
public class DimPaymentChannelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_key")
    private Long channelKey;

    @Column(name = "channel_code", nullable = false, unique = true)
    private String channelCode;

    @Column(name = "channel_desc", nullable = false)
    private String channelDesc;

    @Embedded
    private HouseKeeping houseKeeping;
}
