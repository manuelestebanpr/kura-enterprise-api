package co.com.kura.enterprise.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private String serviceType;
    private String category;
    private BigDecimal basePrice;
    private boolean isCustom;
    private boolean isActive;
    private List<BundleItemResponse> bundleItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BundleItemResponse {
        private UUID serviceId;
        private String serviceCode;
        private String serviceName;
        private int quantity;
        private int sortOrder;
    }
}
