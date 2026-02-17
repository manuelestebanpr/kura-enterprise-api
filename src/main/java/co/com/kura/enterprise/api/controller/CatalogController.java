package co.com.kura.enterprise.api.controller;

import co.com.kura.enterprise.api.dto.CreateLabOfferingRequest;
import co.com.kura.enterprise.api.dto.CreateServiceRequest;
import co.com.kura.enterprise.api.dto.ServiceResponse;
import co.com.kura.enterprise.domain.entity.LabOffering;
import co.com.kura.enterprise.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/services")
    public ResponseEntity<ServiceResponse> createService(@Valid @RequestBody CreateServiceRequest request) {
        return ResponseEntity.ok(catalogService.createService(request));
    }

    @GetMapping("/services/{code}")
    public ResponseEntity<ServiceResponse> getService(@PathVariable String code) {
        return ResponseEntity.ok(catalogService.getServiceByCode(code));
    }

    @GetMapping("/services/search")
    public ResponseEntity<List<ServiceResponse>> searchServices(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(catalogService.searchServices(q, limit));
    }

    @GetMapping("/services/type/{type}")
    public ResponseEntity<List<ServiceResponse>> getByType(@PathVariable String type) {
        return ResponseEntity.ok(catalogService.getServicesByType(type));
    }

    @PostMapping("/offerings")
    public ResponseEntity<LabOffering> createOffering(@Valid @RequestBody CreateLabOfferingRequest request) {
        return ResponseEntity.ok(catalogService.createLabOffering(request));
    }
}
