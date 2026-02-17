package co.com.kura.enterprise.infrastructure;

public interface EmailProvider {
    void sendOtp(String toEmail, String otpCode);
    void sendPasswordReset(String toEmail, String resetLink);
}
