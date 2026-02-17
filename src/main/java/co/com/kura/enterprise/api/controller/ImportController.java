package co.com.kura.enterprise.api.controller;

import co.com.kura.enterprise.api.dto.CsvImportResult;
import co.com.kura.enterprise.service.PatientImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/import")
public class ImportController {

    private final PatientImportService importService;

    public ImportController(PatientImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/patients", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CsvImportResult> importPatients(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mapping_cedula", defaultValue = "cedula") String mappingCedula,
            @RequestParam(value = "mapping_full_name", defaultValue = "full_name") String mappingFullName,
            @RequestParam(value = "mapping_email", defaultValue = "email") String mappingEmail,
            @RequestParam(value = "mapping_phone", defaultValue = "phone") String mappingPhone
    ) throws IOException {

        Map<String, String> columnMapping = new HashMap<>();
        columnMapping.put("cedula", mappingCedula);
        columnMapping.put("full_name", mappingFullName);
        columnMapping.put("email", mappingEmail);
        columnMapping.put("phone", mappingPhone);

        CsvImportResult result = importService.importPatients(file.getInputStream(), columnMapping);
        return ResponseEntity.ok(result);
    }
}
