package com.resumeai.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * JPA Entity — maps to the `analysis_results` table in MySQL.
 * Spring Data JPA auto-creates/updates this table on startup.
 */
@Entity
@Table(name = "analysis_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String analysisId;

    private String jobTitle;

    // JD Match score (0-100, Jaccard similarity)
    private int matchScore;

    // ATS compatibility score (0-100)
    private int atsScore;

    @Column(columnDefinition = "TEXT")
    private String matchedSkills;

    @Column(columnDefinition = "TEXT")
    private String missingSkills;

    @Column(columnDefinition = "TEXT")
    private String extraSkills;

    // Number of spell errors found
    private int spellErrorCount;

    private String resumeFileName;
    private long processingTimeMs;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
