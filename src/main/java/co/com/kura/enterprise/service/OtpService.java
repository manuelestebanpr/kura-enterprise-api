package co.com.kura.enterprise.service;

import co.com.kura.enterprise.infrastructure.EmailProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class OtpService {

    private static final String OTP_PREFIX = "otp:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final int OTP_LENGTH = 6;

    private final StringRedisTemplate redisTemplate;
    private final EmailProvider emailProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(StringRedisTemplate redisTemplate, EmailProvider emailProvider) {
        this.redisTemplate = redisTemplate;
        this.emailProvider = emailProvider;
    }

    public void generateAndSend(String email) {
        String code = generateCode();
        redisTemplate.opsForValue().set(OTP_PREFIX + email, code, OTP_TTL);
        emailProvider.sendOtp(email, code);
    }

    public boolean verify(String email, String code) {
        String key = OTP_PREFIX + email;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored != null && stored.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    private String generateCode() {
        int code = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
