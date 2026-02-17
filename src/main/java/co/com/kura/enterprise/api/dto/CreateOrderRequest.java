package co.com.kura.enterprise.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {
    private UUID userId; // null for guest checkout

    @NotNull
    private UUID posId;

    @NotNull
    private String paymentMethod; // PAY_AT_LAB or MERCADOPAGO

    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private String notes;

    @NotEmpty
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        @NotNull
        private UUID serviceId;
        private int quantity;
    }
}
