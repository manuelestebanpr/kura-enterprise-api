package co.com.kura.enterprise.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "lab_offerings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LabOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "laboratory_id", nullable = false)
    private UUID laboratoryId;

    @Column(name = "pos_id", nullable = false)
    private UUID posId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "turnaround_hours")
    private Integer turnaroundHours;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        isAvailable = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
