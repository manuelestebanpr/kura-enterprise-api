package co.com.kura.enterprise.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "walkin_tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalkinTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 20)
    private String ticketCode;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "pos_id", nullable = false)
    private UUID posId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "redeemed_at")
    private OffsetDateTime redeemedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
