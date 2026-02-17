package co.com.kura.enterprise.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// MOCK INTEGRATION: AWS S3 — Saves files to local /tmp/kura-storage instead of S3.
// Phase 2: Replace with real AWS S3 SDK integration.
@Component
public class MockS3StorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(MockS3StorageProvider.class);
    private static final String LOCAL_STORAGE_DIR = "/tmp/kura-storage";

    @Override
    public String upload(String key, InputStream data, String contentType) {
        try {
            Path dir = Path.of(LOCAL_STORAGE_DIR);
            Files.createDirectories(dir);
            Path filePath = dir.resolve(key);
            Files.createDirectories(filePath.getParent());
            Files.copy(data, filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("=== MOCK S3 UPLOAD ===");
            log.info("Key: {}", key);
            log.info("Content-Type: {}", contentType);
            log.info("Local path: {}", filePath);
            log.info("=== END MOCK S3 ===");

            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Mock S3 upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getUrl(String key) {
        return "/tmp/kura-storage/" + key;
    }
}
