package za.co.capitecbank.transaction_event_consumer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public abstract class ScdBaseEntity {

    @Column(name = "effective_from", nullable = false, updatable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Embedded
    private HouseKeeping houseKeeping;
}
