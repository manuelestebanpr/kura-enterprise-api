package co.com.kura.enterprise.domain.repository;

import co.com.kura.enterprise.domain.entity.Laboratory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LaboratoryRepository extends JpaRepository<Laboratory, UUID> {
    Optional<Laboratory> findByNitAndDeletedAtIsNull(String nit);
    boolean existsByNit(String nit);
}
