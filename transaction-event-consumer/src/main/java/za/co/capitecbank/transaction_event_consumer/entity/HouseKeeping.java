package za.co.capitecbank.transaction_event_consumer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
@Data
public class HouseKeeping {

    @Column(name = "e_batch_id", nullable = false)
    private String eBatchId;

    @Column(name = "e_ingest_id", nullable = false)
    private UUID eIngestId;

    @Column(name = "e_operation", nullable = false)
    private String eOperation;

    @Column(name = "e_source_system", nullable = false)
    private String eSourceSystem;

    @Column(name = "e_row_hash", nullable = false)
    private String eRowHash;

    @Column(name = "e_loaded_at", nullable = false, updatable = false)
    private OffsetDateTime eLoadedAt;

    @Column(name = "e_updated_at")
    private OffsetDateTime eUpdatedAt;

    @PrePersist
    protected void onCreate() {
        eLoadedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        eUpdatedAt = OffsetDateTime.now();
    }
}
