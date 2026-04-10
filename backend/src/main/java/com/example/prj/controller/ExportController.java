package com.example.prj.controller;

import com.example.prj.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExportController {

    private final ExportService exportService;

    /**
     * Tải PDF hồ sơ tư vấn nghề nghiệp.
     */
    @GetMapping("/pdf/{submissionId}")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long submissionId) {
        byte[] pdf = exportService.generatePdf(submissionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=career-profile-" + submissionId + ".pdf")
                .body(pdf);
    }
}
