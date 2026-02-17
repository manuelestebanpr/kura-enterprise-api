package co.com.kura.enterprise.service;

import co.com.kura.enterprise.domain.entity.*;
import co.com.kura.enterprise.domain.repository.*;
import co.com.kura.enterprise.infrastructure.StorageProvider;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ResultService {

    private static final Logger log = LoggerFactory.getLogger(ResultService.class);
    private static final int SHARE_LINK_HOURS = 48;

    private final PatientResultRepository resultRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final TestDependencyRepository testDependencyRepository;
    private final WarehouseInventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final StorageProvider storageProvider;

    public ResultService(PatientResultRepository resultRepository,
                         ShareLinkRepository shareLinkRepository,
                         TestDependencyRepository testDependencyRepository,
                         WarehouseInventoryRepository inventoryRepository,
                         OrderRepository orderRepository,
                         StorageProvider storageProvider) {
        this.resultRepository = resultRepository;
        this.shareLinkRepository = shareLinkRepository;
        this.testDependencyRepository = testDependencyRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.storageProvider = storageProvider;
    }

    @Transactional
    public PatientResult markSampleTaken(UUID resultId) {
        PatientResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new EntityNotFoundException("Result not found"));

        result.setStatus("SAMPLE_TAKEN");
        result.setSampleTakenAt(OffsetDateTime.now());
        result = resultRepository.save(result);

        // Async stock deduction based on BOM
        deductInventory(result);

        return result;
    }

    @Transactional
    public PatientResult completeResult(UUID resultId, String resultData, String notes) {
        PatientResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new EntityNotFoundException("Result not found"));

        result.setStatus("COMPLETED");
        result.setResultData(resultData);
        result.setNotes(notes);
        result.setCompletedAt(OffsetDateTime.now());

        return resultRepository.save(result);
    }

    public String uploadAudioNote(UUID resultId, InputStream audio, String contentType) {
        PatientResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new EntityNotFoundException("Result not found"));

        // MOCK INTEGRATION: AWS S3
        String key = "audio/" + resultId + "/" + UUID.randomUUID() + ".webm";
        String url = storageProvider.upload(key, audio, contentType);

        result.setAudioUrl(url);
        resultRepository.save(result);

        return url;
    }

    @Transactional
    public ShareLink createShareLink(UUID resultId, UUID createdBy) {
        resultRepository.findById(resultId)
                .orElseThrow(() -> new EntityNotFoundException("Result not found"));

        ShareLink link = ShareLink.builder()
                .resultId(resultId)
                .createdBy(createdBy)
                .expiresAt(OffsetDateTime.now().plusHours(SHARE_LINK_HOURS))
                .accessedCount(0)
                .build();

        return shareLinkRepository.save(link);
    }

    public PatientResult getByShareLink(UUID shareUuid) {
        ShareLink link = shareLinkRepository.findByShareUuid(shareUuid)
                .orElseThrow(() -> new EntityNotFoundException("Share link not found"));

        if (link.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Share link has expired");
        }

        link.setAccessedCount(link.getAccessedCount() + 1);
        shareLinkRepository.save(link);

        return resultRepository.findById(link.getResultId())
                .orElseThrow(() -> new EntityNotFoundException("Result not found"));
    }

    public List<PatientResult> getResultsByOrder(UUID orderId) {
        return resultRepository.findByOrderId(orderId);
    }

    public List<PatientResult> getResultsByPatient(UUID patientId) {
        return resultRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    private void deductInventory(PatientResult result) {
        if (result.getOrderItemId() == null) return;

        // Find the service's BOM (test dependencies)
        List<TestDependency> deps = testDependencyRepository.findByServiceId(result.getOrderItemId());

        for (TestDependency dep : deps) {
            inventoryRepository.findByPosIdAndItemCode(result.getPosId(), dep.getItemCode())
                    .ifPresent(inventory -> {
                        int newQty = inventory.getQuantity() - dep.getQuantityNeeded();
                        if (newQty < 0) {
                            log.warn("Negative stock for item {} at PoS {}: {} -> {}",
                                    dep.getItemCode(), result.getPosId(), inventory.getQuantity(), newQty);
                        }
                        inventory.setQuantity(Math.max(0, newQty));
                        inventoryRepository.save(inventory);

                        if (newQty <= inventory.getMinThreshold()) {
                            log.warn("LOW STOCK ALERT: item={}, pos={}, remaining={}",
                                    dep.getItemCode(), result.getPosId(), Math.max(0, newQty));
                        }
                    });
        }
    }
}
