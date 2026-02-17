package co.com.kura.enterprise.service;

import co.com.kura.enterprise.api.dto.*;
import co.com.kura.enterprise.domain.entity.AuditLog;
import co.com.kura.enterprise.domain.entity.User;
import co.com.kura.enterprise.domain.repository.AuditLogRepository;
import co.com.kura.enterprise.domain.repository.UserRepository;
import co.com.kura.enterprise.infrastructure.EmailProvider;
import co.com.kura.enterprise.infrastructure.IdentityVerificationProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final String RESET_PREFIX = "reset:";
    private static final Duration RESET_TTL = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final IdentityVerificationProvider identityProvider;
    private final EmailProvider emailProvider;
    private final StringRedisTemplate redisTemplate;

    public AuthService(UserRepository userRepository,
                       AuditLogRepository auditLogRepository,
                       PasswordEncoder passwordEncoder,
                       OtpService otpService,
                       IdentityVerificationProvider identityProvider,
                       EmailProvider emailProvider,
                       StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.identityProvider = identityProvider;
        this.emailProvider = emailProvider;
        this.redisTemplate = redisTemplate;
    }

    public void sendOtp(String email) {
        otpService.generateAndSend(email);
    }

    public boolean verifyOtp(String email, String code) {
        return otpService.verify(email, code);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.isConsentLey1581()) {
            throw new IllegalArgumentException("Debe aceptar la política de tratamiento de datos (Ley 1581)");
        }

        if (userRepository.existsByCedula(request.getCedula())) {
            throw new IllegalArgumentException("La cédula ya está registrada");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado");
        }

        // MOCK INTEGRATION: RNEC — Verify cedula against national registry
        var verification = identityProvider.verifyCedula(request.getCedula(), request.getFullName());
        if (!verification.match()) {
            throw new IllegalArgumentException("Identity verification failed: " + verification.message());
        }

        User user = User.builder()
                .cedula(request.getCedula())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role("PATIENT")
                .consentLey1581(true)
                .consentDate(OffsetDateTime.now())
                .isActive(true)
                .build();

        user = userRepository.save(user);

        auditLog(user.getId(), "REGISTER", "USER", user.getId());

        return AuthResponse.builder()
                .token("kura-jwt-" + user.getId().toString())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .message("Registration successful")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("Account is disabled");
        }

        auditLog(user.getId(), "LOGIN", "USER", user.getId());

        String token = "kura-jwt-" + user.getId().toString();
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .message("Login successful")
                .build();
    }

    public void requestPasswordReset(String email) {
        userRepository.findByEmailAndDeletedAtIsNull(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(RESET_PREFIX + token, user.getId().toString(), RESET_TTL);
            // MOCK INTEGRATION: AWS SES — Send password reset email
            String resetLink = "https://kura.com.co/reset?token=" + token;
            emailProvider.sendPasswordReset(email, resetLink);
            auditLog(user.getId(), "PASSWORD_RESET_REQUEST", "USER", user.getId());
        });
        // Silent fail if email not found (security best practice)
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirm request) {
        String key = RESET_PREFIX + request.getToken();
        String userIdStr = redisTemplate.opsForValue().get(key);

        if (userIdStr == null) {
            throw new IllegalArgumentException("Invalid or expired reset link");
        }

        UUID userId = UUID.fromString(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        redisTemplate.delete(key);

        auditLog(userId, "PASSWORD_RESET_CONFIRM", "USER", userId);
    }

    private void auditLog(UUID userId, String action, String entityType, UUID entityId) {
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .build();
        auditLogRepository.save(log);
    }
}
