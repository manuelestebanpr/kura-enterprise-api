package co.com.kura.enterprise.domain.repository;

import co.com.kura.enterprise.domain.entity.LabOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabOfferingRepository extends JpaRepository<LabOffering, UUID> {
    List<LabOffering> findByPosIdAndIsAvailableTrue(UUID posId);
    List<LabOffering> findByServiceIdAndIsAvailableTrue(UUID serviceId);
    Optional<LabOffering> findByPosIdAndServiceId(UUID posId, UUID serviceId);
}
