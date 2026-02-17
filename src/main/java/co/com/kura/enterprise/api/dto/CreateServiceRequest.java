package co.com.kura.enterprise.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateServiceRequest {
    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String serviceType; // SINGLE or BUNDLE

    private String category;

    private BigDecimal basePrice;

    private boolean isCustom;

    // Only for BUNDLE type
    private List<BundleItemRequest> bundleItems;

    @Data
    public static class BundleItemRequest {
        private String serviceCode;
        private int quantity;
        private int sortOrder;
    }
}
