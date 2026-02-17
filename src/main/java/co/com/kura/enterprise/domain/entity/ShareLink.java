package co.com.kura.enterprise.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "share_links")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "share_uuid", nullable = false, unique = true)
    private UUID shareUuid;

    @Column(name = "result_id", nullable = false)
    private UUID resultId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "accessed_count", nullable = false)
    private int accessedCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (shareUuid == null) {
            shareUuid = UUID.randomUUID();
        }
    }
}
