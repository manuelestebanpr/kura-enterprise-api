package co.com.kura.enterprise.infrastructure;

import java.math.BigDecimal;

public interface PaymentProvider {
    PaymentResult createPreference(String orderNumber, BigDecimal amount, String currency, String description);

    record PaymentResult(String preferenceId, String checkoutUrl, String status) {}
}
