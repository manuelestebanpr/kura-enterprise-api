package co.com.kura.enterprise.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

// MOCK INTEGRATION: MercadoPago — Returns fake Preference ID and checkout URL.
// Phase 2: Replace with real MercadoPago SDK integration for Colombian Pesos (COP).
@Component
public class MockMercadoPagoProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(MockMercadoPagoProvider.class);

    @Override
    public PaymentResult createPreference(String orderNumber, BigDecimal amount, String currency, String description) {
        String fakePreferenceId = "MOCK-PREF-" + UUID.randomUUID().toString().substring(0, 8);
        String fakeCheckoutUrl = "https://sandbox.mercadopago.com.co/checkout/v1/redirect?pref_id=" + fakePreferenceId;

        log.info("=== MOCK MERCADOPAGO ===");
        log.info("Order: {}, Amount: {} {}", orderNumber, amount, currency);
        log.info("Description: {}", description);
        log.info("Preference ID: {}", fakePreferenceId);
        log.info("Checkout URL: {}", fakeCheckoutUrl);
        log.info("=== END MOCK MERCADOPAGO ===");

        return new PaymentResult(fakePreferenceId, fakeCheckoutUrl, "PENDING");
    }
}
