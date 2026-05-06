package com.resumeai.service;

import com.resumeai.dto.AnalysisResponse;
import com.resumeai.dto.AtsResult;
import com.resumeai.dto.SpellError;
import com.resumeai.model.AnalysisResult;
import com.resumeai.repository.AnalysisResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ANALYZER SERVICE — ORCHESTRATOR
 * --------------------------------
 * Full pipeline:
 *
 *   PDF File
 *     → PdfExtractorService  → raw text
 *     → NlpService           → resume skills
 *     → NlpService           → JD skills
 *     → ScoringService       → match score, gaps, recommendations
 *     → AtsService           → ATS compatibility score + checks
 *     → SpellCheckService    → spelling errors + suggestions
 *     → Repository           → saved to MySQL
 *     → AnalysisResponse     → JSON to frontend
 */
@Service
public class AnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(AnalyzerService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Autowired private PdfExtractorService pdfExtractor;
    @Autowired private NlpService nlpService;
    @Autowired private ScoringService scoringService;
    @Autowired private AtsService atsService;
    @Autowired private SpellCheckService spellCheckService;
    @Autowired private AnalysisResultRepository repository;

    public AnalysisResponse analyze(MultipartFile resumeFile, String jobDescription, String jobTitle) {
        long startTime = System.currentTimeMillis();
        String analysisId = UUID.randomUUID().toString();

        try {
            // ── STEP 1: Extract text from PDF ──────────────────────────────
            log.info("[{}] Extracting PDF text from: {}", analysisId, resumeFile.getOriginalFilename());
            String resumeText = pdfExtractor.extractText(resumeFile);

            // ── STEP 2: Skill extraction from resume ───────────────────────
            log.info("[{}] Extracting skills from resume...", analysisId);
            Set<String> resumeSkills = nlpService.extractSkills(resumeText);

            // ── STEP 3: Skill extraction from JD ──────────────────────────
            log.info("[{}] Extracting skills from job description...", analysisId);
            Set<String> jdSkills = nlpService.extractSkills(jobDescription);

            // ── STEP 4: Compute match score ────────────────────────────────
            int score = scoringService.computeMatchScore(resumeSkills, jdSkills);
            List<String> matched       = scoringService.getMatchedSkills(resumeSkills, jdSkills);
            List<String> missing       = scoringService.getMissingSkills(resumeSkills, jdSkills);
            List<String> extra         = scoringService.getExtraSkills(resumeSkills, jdSkills);
            List<String> recommendations = scoringService.generateRecommendations(missing, score);

            // ── STEP 5: ATS evaluation ─────────────────────────────────────
            log.info("[{}] Running ATS evaluation...", analysisId);
            AtsResult atsResult = atsService.evaluate(resumeText, jobDescription, score);

            // ── STEP 6: Spell check ────────────────────────────────────────
            log.info("[{}] Running spell check...", analysisId);
            List<SpellError> spellErrors = spellCheckService.checkSpelling(resumeText);
            String spellSummary = spellCheckService.buildSummary(spellErrors);

            long elapsed = System.currentTimeMillis() - startTime;

            // ── STEP 7: Persist to MySQL ───────────────────────────────────
            AnalysisResult entity = AnalysisResult.builder()
                    .analysisId(analysisId)
                    .jobTitle(jobTitle)
                    .matchScore(score)
                    .atsScore(atsResult.getAtsScore())
                    .matchedSkills(String.join(",", matched))
                    .missingSkills(String.join(",", missing))
                    .extraSkills(String.join(",", extra))
                    .spellErrorCount(spellErrors.size())
                    .resumeFileName(resumeFile.getOriginalFilename())
                    .processingTimeMs(elapsed)
                    .build();
            repository.save(entity);

            log.info("[{}] Analysis complete in {}ms — Score: {}%, ATS: {}/100, Spell errors: {}",
                    analysisId, elapsed, score, atsResult.getAtsScore(), spellErrors.size());

            // ── STEP 8: Build and return response ─────────────────────────
            return AnalysisResponse.builder()
                    .analysisId(analysisId)
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now().format(FORMATTER))
                    .matchScore(score)
                    .scoreLabel(scoringService.getScoreLabel(score))
                    .matchedSkills(matched)
                    .missingSkills(missing)
                    .extraSkills(extra)
                    .totalResumeSkills(resumeSkills.size())
                    .totalRequiredSkills(jdSkills.size())
                    .recommendations(recommendations)
                    .atsResult(atsResult)
                    .spellErrors(spellErrors)
                    .spellErrorCount(spellErrors.size())
                    .spellCheckSummary(spellSummary)
                    .resumeFileName(resumeFile.getOriginalFilename())
                    .jobTitle(jobTitle)
                    .processingTimeMs(elapsed)
                    .build();

        } catch (Exception e) {
            log.error("[{}] Analysis failed: {}", analysisId, e.getMessage(), e);
            return AnalysisResponse.builder()
                    .analysisId(analysisId)
                    .status("ERROR")
                    .timestamp(LocalDateTime.now().format(FORMATTER))
                    .errorMessage("Analysis failed: " + e.getMessage())
                    .build();
        }
    }

    public List<AnalysisResult> getHistory() {
        return repository.findAllByOrderByCreatedAtDesc();
    }
}
