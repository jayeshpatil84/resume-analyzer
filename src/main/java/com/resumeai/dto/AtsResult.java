package com.resumeai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * ATS (Applicant Tracking System) compatibility result.
 * Evaluates how well the resume will parse through automated systems.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtsResult {
    private int atsScore;                
    private String atsLabel;             // "ATS Friendly", "Needs Work", etc.
    private List<String> passedChecks;   // Things the resume does well for ATS
    private List<String> failedChecks;   // Issues that may cause ATS rejection
    private List<String> atsSuggestions; // Actionable improvements
}
