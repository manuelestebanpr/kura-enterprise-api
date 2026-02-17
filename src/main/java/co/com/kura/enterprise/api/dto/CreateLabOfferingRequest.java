package co.com.kura.enterprise.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateLabOfferingRequest {
    @NotNull
    private UUID laboratoryId;

    @NotNull
    private UUID posId;

    @NotNull
    private UUID serviceId;

    @NotNull
    private BigDecimal price;

    private Integer turnaroundHours;
}
