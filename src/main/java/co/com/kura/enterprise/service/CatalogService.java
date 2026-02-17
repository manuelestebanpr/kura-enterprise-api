package co.com.kura.enterprise.service;

import co.com.kura.enterprise.api.dto.CreateLabOfferingRequest;
import co.com.kura.enterprise.api.dto.CreateServiceRequest;
import co.com.kura.enterprise.api.dto.ServiceResponse;
import co.com.kura.enterprise.domain.entity.BundleItem;
import co.com.kura.enterprise.domain.entity.LabOffering;
import co.com.kura.enterprise.domain.entity.MasterService;
import co.com.kura.enterprise.domain.repository.BundleItemRepository;
import co.com.kura.enterprise.domain.repository.LabOfferingRepository;
import co.com.kura.enterprise.domain.repository.MasterServiceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final MasterServiceRepository serviceRepository;
    private final BundleItemRepository bundleItemRepository;
    private final LabOfferingRepository labOfferingRepository;

    public CatalogService(MasterServiceRepository serviceRepository,
                          BundleItemRepository bundleItemRepository,
                          LabOfferingRepository labOfferingRepository) {
        this.serviceRepository = serviceRepository;
        this.bundleItemRepository = bundleItemRepository;
        this.labOfferingRepository = labOfferingRepository;
    }

    @Transactional
    public ServiceResponse createService(CreateServiceRequest request) {
        if (request.isCustom()) {
            log.warn("Creating CUSTOM service '{}' — Not Recommended: custom services may have unpredictable pricing", request.getName());
        }

        MasterService service = MasterService.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .serviceType(request.getServiceType())
                .category(request.getCategory())
                .basePrice(request.getBasePrice())
                .isCustom(request.isCustom())
                .build();

        service = serviceRepository.save(service);

        if ("BUNDLE".equals(request.getServiceType()) && request.getBundleItems() != null) {
            for (CreateServiceRequest.BundleItemRequest item : request.getBundleItems()) {
                MasterService childService = serviceRepository.findByCodeAndDeletedAtIsNull(item.getServiceCode())
                        .orElseThrow(() -> new EntityNotFoundException("Service not found: " + item.getServiceCode()));

                BundleItem bundleItem = BundleItem.builder()
                        .bundleId(service.getId())
                        .serviceId(childService.getId())
                        .quantity(item.getQuantity())
                        .sortOrder(item.getSortOrder())
                        .build();
                bundleItemRepository.save(bundleItem);
            }
        }

        return toResponse(service);
    }

    public ServiceResponse getServiceByCode(String code) {
        MasterService service = serviceRepository.findByCodeAndDeletedAtIsNull(code)
                .orElseThrow(() -> new EntityNotFoundException("Service not found: " + code));
        return toResponse(service);
    }

    public List<ServiceResponse> searchServices(String query, int limit) {
        return serviceRepository.searchByName(query, limit).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ServiceResponse> getServicesByType(String type) {
        return serviceRepository.findByServiceTypeAndDeletedAtIsNull(type).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LabOffering createLabOffering(CreateLabOfferingRequest request) {
        LabOffering offering = LabOffering.builder()
                .laboratoryId(request.getLaboratoryId())
                .posId(request.getPosId())
                .serviceId(request.getServiceId())
                .price(request.getPrice())
                .turnaroundHours(request.getTurnaroundHours())
                .build();
        return labOfferingRepository.save(offering);
    }

    private ServiceResponse toResponse(MasterService service) {
        List<ServiceResponse.BundleItemResponse> bundleItems = null;

        if ("BUNDLE".equals(service.getServiceType())) {
            bundleItems = bundleItemRepository.findByBundleIdOrderBySortOrder(service.getId()).stream()
                    .map(item -> {
                        MasterService child = serviceRepository.findById(item.getServiceId()).orElse(null);
                        return ServiceResponse.BundleItemResponse.builder()
                                .serviceId(item.getServiceId())
                                .serviceCode(child != null ? child.getCode() : null)
                                .serviceName(child != null ? child.getName() : null)
                                .quantity(item.getQuantity())
                                .sortOrder(item.getSortOrder())
                                .build();
                    })
                    .toList();
        }

        return ServiceResponse.builder()
                .id(service.getId())
                .code(service.getCode())
                .name(service.getName())
                .description(service.getDescription())
                .serviceType(service.getServiceType())
                .category(service.getCategory())
                .basePrice(service.getBasePrice())
                .isCustom(service.isCustom())
                .isActive(service.isActive())
                .bundleItems(bundleItems)
                .build();
    }
}
