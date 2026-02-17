package co.com.kura.enterprise.api.controller;

import co.com.kura.enterprise.api.dto.*;
import co.com.kura.enterprise.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, String>> sendOtp(@Valid @RequestBody OtpRequest request) {
        authService.sendOtp(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        boolean valid = authService.verifyOtp(request.getEmail(), request.getCode());
        if (!valid) {
            return ResponseEntity.badRequest().body(Map.of(
                "valid", false,
                "message", "Invalid or expired OTP code"
            ));
        }
        return ResponseEntity.ok(Map.of(
            "valid", true,
            "message", "OTP verified successfully"
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        
        ResponseCookie cookie = ResponseCookie.from("KURA_SESSION", response.getToken())
                .httpOnly(true)
                .secure(true)
                .domain(".kura.com.co")
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofHours(24))
                .build();
        
        // Remove token from response body (cookie-only)
        response.setToken(null);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        
        ResponseCookie cookie = ResponseCookie.from("KURA_SESSION", response.getToken())
                .httpOnly(true)
                .secure(true)
                .domain(".kura.com.co")
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofHours(24))
                .build();
        
        // Remove token from response body (cookie-only)
        response.setToken(null);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, String>> requestReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "If the email exists, a reset link has been sent"));
    }

    @PostMapping("/password/reset/confirm")
    public ResponseEntity<Map<String, String>> confirmReset(@Valid @RequestBody PasswordResetConfirm request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }
}
