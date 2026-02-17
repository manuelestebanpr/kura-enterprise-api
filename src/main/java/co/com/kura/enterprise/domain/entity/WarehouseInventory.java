package co.com.kura.enterprise.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "warehouse_inventory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WarehouseInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pos_id", nullable = false)
    private UUID posId;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "min_threshold", nullable = false)
    private int minThreshold;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(name = "last_restocked")
    private OffsetDateTime lastRestocked;

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
