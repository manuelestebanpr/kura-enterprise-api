package co.com.kura.enterprise.domain.repository;

import co.com.kura.enterprise.domain.entity.PatientResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientResultRepository extends JpaRepository<PatientResult, UUID> {
    List<PatientResult> findByOrderId(UUID orderId);
    List<PatientResult> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
    List<PatientResult> findByPosIdAndStatus(UUID posId, String status);
}
