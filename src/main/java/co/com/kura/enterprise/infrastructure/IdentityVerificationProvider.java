package co.com.kura.enterprise.infrastructure;

public interface IdentityVerificationProvider {
    VerificationResult verifyCedula(String cedula, String fullName);

    record VerificationResult(boolean match, String message) {}
}
