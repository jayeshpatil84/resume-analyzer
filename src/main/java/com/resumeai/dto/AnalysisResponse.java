package com.resumeai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full API response returned from POST /api/analyze.
 * Serialized to JSON by Jackson and consumed by the frontend.
 *
 * NOTE: @NoArgsConstructor + @AllArgsConstructor are required alongside @Builder
 * so Jackson can deserialize this class correctly (e.g., in tests or if used as input).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {

    private String analysisId;
    private String status;          // "SUCCESS" or "ERROR"
    private String timestamp;
    private String errorMessage;    // only set on ERROR

    // ── JD Match ────────────────────────────────
    private int matchScore;         // 0-100 Jaccard similarity
    private String scoreLabel;      // "Excellent Match", "Good Match", etc.
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> extraSkills;
    private int totalResumeSkills;
    private int totalRequiredSkills;
    private List<String> recommendations;

    // ── ATS Score ───────────────────────────────
    private AtsResult atsResult;

    // ── Spell Check ─────────────────────────────
    private List<SpellError> spellErrors;
    private int spellErrorCount;
    private String spellCheckSummary;

    // ── Meta ────────────────────────────────────
    private String resumeFileName;
    private String jobTitle;
    private long processingTimeMs;
}
