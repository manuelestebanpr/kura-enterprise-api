package co.com.kura.enterprise.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatientResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "order_item_id")
    private UUID orderItemId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "pos_id", nullable = false)
    private UUID posId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "result_data", columnDefinition = "jsonb")
    private String resultData;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "sample_taken_at")
    private OffsetDateTime sampleTakenAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
