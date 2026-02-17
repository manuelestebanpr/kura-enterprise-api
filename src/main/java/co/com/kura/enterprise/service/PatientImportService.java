package co.com.kura.enterprise.service;

import co.com.kura.enterprise.api.dto.CsvImportResult;
import co.com.kura.enterprise.domain.entity.AuditLog;
import co.com.kura.enterprise.domain.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class PatientImportService {

    private static final Logger log = LoggerFactory.getLogger(PatientImportService.class);

    private static final String UPSERT_SQL = """
            INSERT INTO users (id, cedula, full_name, email, password_hash, phone, role, consent_ley1581, is_active, created_at, updated_at)
            VALUES (uuid_generate_v4(), ?, ?, ?, '$IMPORT_PLACEHOLDER$', ?, 'PATIENT', true, true, NOW(), NOW())
            ON CONFLICT (cedula) DO UPDATE SET
                full_name = EXCLUDED.full_name,
                email = EXCLUDED.email,
                phone = EXCLUDED.phone,
                updated_at = NOW()
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AuditLogRepository auditLogRepository;

    public PatientImportService(JdbcTemplate jdbcTemplate, AuditLogRepository auditLogRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public CsvImportResult importPatients(InputStream csvStream, Map<String, String> columnMapping) {
        List<String> errorDetails = new ArrayList<>();
        int totalRows = 0;
        int imported = 0;
        int updated = 0;
        int errors = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return CsvImportResult.builder()
                        .totalRows(0).imported(0).updated(0).errors(1)
                        .errorDetails(List.of("Empty CSV file"))
                        .build();
            }

            String[] headers = headerLine.split(",");
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndex.put(headers[i].trim().toLowerCase(), i);
            }

            // Resolve mapped columns
            int cedulaIdx = resolveIndex(headerIndex, columnMapping, "cedula");
            int nameIdx = resolveIndex(headerIndex, columnMapping, "full_name");
            int emailIdx = resolveIndex(headerIndex, columnMapping, "email");
            int phoneIdx = resolveIndex(headerIndex, columnMapping, "phone");

            if (cedulaIdx < 0 || nameIdx < 0 || emailIdx < 0) {
                return CsvImportResult.builder()
                        .totalRows(0).imported(0).updated(0).errors(1)
                        .errorDetails(List.of("Missing required column mappings: cedula, full_name, email"))
                        .build();
            }

            String line;
            while ((line = reader.readLine()) != null) {
                totalRows++;
                try {
                    String[] cols = line.split(",", -1);
                    String cedula = cols[cedulaIdx].trim();
                    String fullName = cols[nameIdx].trim();
                    String email = cols[emailIdx].trim();
                    String phone = phoneIdx >= 0 && phoneIdx < cols.length ? cols[phoneIdx].trim() : null;

                    if (cedula.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
                        errors++;
                        errorDetails.add("Row " + totalRows + ": missing required fields");
                        continue;
                    }

                    // Check if this is an insert or update
                    Integer existing = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM users WHERE cedula = ?",
                            Integer.class, cedula);

                    jdbcTemplate.update(UPSERT_SQL, cedula, fullName, email, phone);

                    if (existing != null && existing > 0) {
                        updated++;
                    } else {
                        imported++;
                    }
                } catch (Exception e) {
                    errors++;
                    errorDetails.add("Row " + totalRows + ": " + e.getMessage());
                    log.warn("CSV import error at row {}: {}", totalRows, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("CSV import failed: {}", e.getMessage());
            errorDetails.add("Fatal error: " + e.getMessage());
            errors++;
        }

        // Audit log
        AuditLog audit = AuditLog.builder()
                .action("CSV_PATIENT_IMPORT")
                .entityType("USER")
                .newValue(String.format("{\"total\":%d,\"imported\":%d,\"updated\":%d,\"errors\":%d}",
                        totalRows, imported, updated, errors))
                .build();
        auditLogRepository.save(audit);

        log.info("CSV import complete: total={}, imported={}, updated={}, errors={}", totalRows, imported, updated, errors);

        return CsvImportResult.builder()
                .totalRows(totalRows)
                .imported(imported)
                .updated(updated)
                .errors(errors)
                .errorDetails(errorDetails)
                .build();
    }

    private int resolveIndex(Map<String, Integer> headerIndex, Map<String, String> columnMapping, String field) {
        String mappedHeader = columnMapping.getOrDefault(field, field);
        return headerIndex.getOrDefault(mappedHeader.toLowerCase(), -1);
    }
}
