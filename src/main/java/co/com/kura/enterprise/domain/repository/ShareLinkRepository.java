package co.com.kura.enterprise.domain.repository;

import co.com.kura.enterprise.domain.entity.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, UUID> {
    Optional<ShareLink> findByShareUuid(UUID shareUuid);
}
