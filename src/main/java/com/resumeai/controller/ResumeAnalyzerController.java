package com.resumeai.controller;

import com.resumeai.dto.AnalysisResponse;
import com.resumeai.model.AnalysisResult;
import com.resumeai.service.AnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST CONTROLLER
 * ───────────────
 * POST /api/analyze   → Upload resume PDF + JD → full analysis JSON
 * GET  /api/history   → All past analyses from MySQL
 * GET  /api/health    → Health check
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ResumeAnalyzerController {

    @Autowired
    private AnalyzerService analyzerService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisResponse> analyze(
            @RequestParam("resume") MultipartFile resume,
            @RequestParam("jobDescription") String jobDescription,
            @RequestParam(value = "jobTitle", defaultValue = "Not Specified") String jobTitle) {

        String filename = resume.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(
                    AnalysisResponse.builder()
                            .status("ERROR")
                            .errorMessage("Only PDF files are accepted.")
                            .build());
        }

        if (jobDescription == null || jobDescription.isBlank()) {
            return ResponseEntity.badRequest().body(
                    AnalysisResponse.builder()
                            .status("ERROR")
                            .errorMessage("Job description cannot be empty.")
                            .build());
        }

        AnalysisResponse response = analyzerService.analyze(resume, jobDescription, jobTitle);
        return "ERROR".equals(response.getStatus())
                ? ResponseEntity.internalServerError().body(response)
                : ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<AnalysisResult>> getHistory() {
        return ResponseEntity.ok(analyzerService.getHistory());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"UP\",\"service\":\"AI Resume Analyzer v2\"}");
    }
}
