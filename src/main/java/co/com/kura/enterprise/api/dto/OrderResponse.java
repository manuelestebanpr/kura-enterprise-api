package co.com.kura.enterprise.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private String status;
    private String paymentMethod;
    private BigDecimal subtotal;
    private BigDecimal total;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
    private List<ItemResponse> items;
    private TicketResponse ticket;
    private PaymentInfo payment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResponse {
        private UUID serviceId;
        private String serviceName;
        private BigDecimal price;
        private int quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketResponse {
        private String ticketCode;
        private String status;
        private OffsetDateTime expiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private String preferenceId;
        private String checkoutUrl;
        private String status;
    }
}
