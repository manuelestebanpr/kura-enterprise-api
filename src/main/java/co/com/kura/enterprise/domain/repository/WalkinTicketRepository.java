package co.com.kura.enterprise.domain.repository;

import co.com.kura.enterprise.domain.entity.WalkinTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalkinTicketRepository extends JpaRepository<WalkinTicket, UUID> {
    Optional<WalkinTicket> findByTicketCode(String ticketCode);
    Optional<WalkinTicket> findByOrderId(UUID orderId);
}
