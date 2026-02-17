package co.com.kura.enterprise.domain.repository;

import co.com.kura.enterprise.domain.entity.MasterService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterServiceRepository extends JpaRepository<MasterService, UUID> {
    Optional<MasterService> findByCodeAndDeletedAtIsNull(String code);
    List<MasterService> findByServiceTypeAndDeletedAtIsNull(String serviceType);
    List<MasterService> findByCategoryAndDeletedAtIsNull(String category);

    @Query(value = "SELECT * FROM master_services WHERE deleted_at IS NULL AND name % :query ORDER BY similarity(name, :query) DESC LIMIT :limit", nativeQuery = true)
    List<MasterService> searchByName(String query, int limit);
}
