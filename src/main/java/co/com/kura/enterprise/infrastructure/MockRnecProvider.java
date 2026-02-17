package co.com.kura.enterprise.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// MOCK INTEGRATION: RNEC (Registraduría Nacional) - Always returns MATCH for identity verification.
// Phase 2: Replace with real RNEC SOAP/REST integration for Colombian national ID validation.
@Component
public class MockRnecProvider implements IdentityVerificationProvider {

    private static final Logger log = LoggerFactory.getLogger(MockRnecProvider.class);

    @Override
    public VerificationResult verifyCedula(String cedula, String fullName) {
        log.info("=== MOCK RNEC VERIFICATION ===");
        log.info("Cedula: {}, Name: {}", cedula, fullName);
        log.info("Result: MATCH (mocked)");
        log.info("=== END MOCK RNEC ===");
        return new VerificationResult(true, "Identity verified (MOCK)");
    }
}
