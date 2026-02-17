package co.com.kura.enterprise.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// MOCK INTEGRATION: AWS SES - Logs email content to console instead of sending real emails.
// Phase 2: Replace with actual AWS SES SDK integration.
@Component
public class MockSesEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(MockSesEmailProvider.class);

    @Override
    public void sendOtp(String toEmail, String otpCode) {
        log.info("=== MOCK SES EMAIL ===");
        log.info("To: {}", toEmail);
        log.info("Subject: KURA - Código de verificación");
        log.info("Body: Su código OTP es: {}", otpCode);
        log.info("=== END MOCK EMAIL ===");
    }

    @Override
    public void sendPasswordReset(String toEmail, String resetLink) {
        log.info("=== MOCK SES EMAIL ===");
        log.info("To: {}", toEmail);
        log.info("Subject: KURA - Restablecer contraseña");
        log.info("Body: Haga clic en el siguiente enlace para restablecer su contraseña: {}", resetLink);
        log.info("=== END MOCK EMAIL ===");
    }
}
