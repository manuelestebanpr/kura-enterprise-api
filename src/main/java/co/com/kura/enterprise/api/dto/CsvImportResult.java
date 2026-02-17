package co.com.kura.enterprise.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvImportResult {
    private int totalRows;
    private int imported;
    private int updated;
    private int errors;
    private List<String> errorDetails;
}
