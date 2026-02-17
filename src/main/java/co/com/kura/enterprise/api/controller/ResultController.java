package co.com.kura.enterprise.api.controller;

import co.com.kura.enterprise.domain.entity.PatientResult;
import co.com.kura.enterprise.domain.entity.ShareLink;
import co.com.kura.enterprise.service.ResultService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @PostMapping("/results/{resultId}/sample-taken")
    public ResponseEntity<PatientResult> markSampleTaken(@PathVariable UUID resultId) {
        return ResponseEntity.ok(resultService.markSampleTaken(resultId));
    }

    @PostMapping("/results/{resultId}/complete")
    public ResponseEntity<PatientResult> completeResult(
            @PathVariable UUID resultId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(resultService.completeResult(
                resultId, body.get("resultData"), body.get("notes")));
    }

    @PostMapping(value = "/results/{resultId}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadAudio(
            @PathVariable UUID resultId,
            @RequestParam("file") MultipartFile file) throws IOException {
        String url = resultService.uploadAudioNote(resultId, file.getInputStream(), file.getContentType());
        return ResponseEntity.ok(Map.of("audioUrl", url));
    }

    @PostMapping("/results/{resultId}/share")
    public ResponseEntity<ShareLink> createShareLink(
            @PathVariable UUID resultId,
            @RequestParam(required = false) UUID createdBy) {
        return ResponseEntity.ok(resultService.createShareLink(resultId, createdBy));
    }

    @GetMapping("/share/{shareUuid}")
    public ResponseEntity<PatientResult> getByShareLink(@PathVariable UUID shareUuid) {
        return ResponseEntity.ok(resultService.getByShareLink(shareUuid));
    }

    @GetMapping("/results/order/{orderId}")
    public ResponseEntity<List<PatientResult>> getByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(resultService.getResultsByOrder(orderId));
    }

    @GetMapping("/results/patient/{patientId}")
    public ResponseEntity<List<PatientResult>> getByPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(resultService.getResultsByPatient(patientId));
    }
}
