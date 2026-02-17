package co.com.kura.enterprise.domain.repository;

import co.com.kura.enterprise.domain.entity.PointOfService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PointOfServiceRepository extends JpaRepository<PointOfService, UUID> {
    List<PointOfService> findByLaboratoryIdAndDeletedAtIsNull(UUID laboratoryId);
    List<PointOfService> findByCityAndDeletedAtIsNull(String city);
}
