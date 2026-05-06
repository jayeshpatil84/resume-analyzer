package com.resumeai.service;

import com.resumeai.dto.AtsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * ATS (Applicant Tracking System) COMPATIBILITY SERVICE
 * -------------------------------------------------------
 * Evaluates how well a resume will pass through automated ATS parsers
 * used by most companies (Workday, Greenhouse, Lever, iCIMS, Taleo, etc.)
 *
 * ATS systems commonly reject resumes for:
 *   - Missing standard section headers (Experience, Education, Skills)
 *   - No contact information (email, phone)
 *   - Low keyword density relative to the job description
 *   - Very short or very long content
 *   - Lack of dates in work experience
 *   - No measurable achievements (numbers/percentages)
 *
 * Each check contributes to the total ATS score (0-100).
 */
@Service
public class AtsService {

    private static final Logger log = LoggerFactory.getLogger(AtsService.class);

    // Section headers ATS systems look for
    private static final List<Pattern> SECTION_HEADERS = List.of(
        Pattern.compile("(?i)(work\\s*experience|experience|employment\\s*history|professional\\s*experience)"),
        Pattern.compile("(?i)(education|academic|qualification)"),
        Pattern.compile("(?i)(skills|technical\\s*skills|core\\s*competencies|technologies)"),
        Pattern.compile("(?i)(summary|profile|objective|about\\s*me)"),
        Pattern.compile("(?i)(projects?|personal\\s*projects?|key\\s*projects?)")
    );

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("(\\+?[0-9][\\s\\-.]?){8,15}");

    private static final Pattern DATE_PATTERN =
        Pattern.compile("(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|january|february|march|april|june|july|august|september|october|november|december|[12][0-9]{3})");

    private static final Pattern METRICS_PATTERN =
        Pattern.compile("\\d+\\s*(%|percent|x|\\+|times|users|clients|million|billion|k\\b|hours|days|months|years)");

    private static final Pattern ACTION_VERBS = Pattern.compile(
        "(?i)\\b(led|built|developed|designed|implemented|managed|improved|increased|reduced|" +
        "optimized|delivered|created|architected|deployed|mentored|collaborated|" +
        "launched|automated|migrated|scaled|refactored|integrated|analyzed|resolved)\\b"
    );

    /**
     * Runs all ATS checks against the resume text and the job description.
     *
     * @param resumeText    raw text extracted from the PDF
     * @param jobDescription the JD text (used for keyword density check)
     * @param matchScore    Jaccard skill match score (reused here)
     * @return AtsResult with score, passed/failed checks, and suggestions
     */
    public AtsResult evaluate(String resumeText, String jobDescription, int matchScore) {
        List<String> passed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        int score = 0;

        // ── CHECK 1: Contact info present (email) — 10 pts ──────────────────
        if (EMAIL_PATTERN.matcher(resumeText).find()) {
            passed.add("Email address detected");
            score += 10;
        } else {
            failed.add("No email address found");
            suggestions.add("Add your email address clearly at the top of the resume.");
        }

        // ── CHECK 2: Phone number — 5 pts ────────────────────────────────────
        if (PHONE_PATTERN.matcher(resumeText).find()) {
            passed.add("Phone number detected");
            score += 5;
        } else {
            failed.add("No phone number found");
            suggestions.add("Include your phone number for recruiters to contact you.");
        }

        // ── CHECK 3: Standard section headers — 20 pts (4 pts each) ──────────
        int sectionsFound = 0;
        String[] sectionNames = {"Work Experience", "Education", "Skills", "Summary/Profile", "Projects"};
        for (int i = 0; i < SECTION_HEADERS.size(); i++) {
            if (SECTION_HEADERS.get(i).matcher(resumeText).find()) {
                passed.add("Section detected: " + sectionNames[i]);
                sectionsFound++;
                score += 4;
            } else {
                failed.add("Missing section: " + sectionNames[i]);
                suggestions.add("Add a clear '" + sectionNames[i] + "' section with that exact heading.");
            }
        }

        // ── CHECK 4: Date ranges in experience — 10 pts ───────────────────────
        if (DATE_PATTERN.matcher(resumeText).find()) {
            passed.add("Date ranges found in experience");
            score += 10;
        } else {
            failed.add("No date ranges found in experience entries");
            suggestions.add("Add employment dates (e.g., 'Jan 2021 – Dec 2023') to each role.");
        }

        // ── CHECK 5: Quantified achievements — 10 pts ─────────────────────────
        if (METRICS_PATTERN.matcher(resumeText).find()) {
            passed.add("Quantified achievements detected (numbers/metrics)");
            score += 10;
        } else {
            failed.add("No quantified achievements found");
            suggestions.add("Add metrics to bullet points: 'Improved latency by 40%', 'Onboarded 3 engineers'.");
        }

        // ── CHECK 6: Action verbs — 10 pts ────────────────────────────────────
        if (ACTION_VERBS.matcher(resumeText).find()) {
            passed.add("Action verbs used in experience descriptions");
            score += 10;
        } else {
            failed.add("Weak or no action verbs in experience");
            suggestions.add("Start bullet points with strong action verbs: 'Built', 'Led', 'Optimized', 'Designed'.");
        }

        // ── CHECK 7: Resume length (word count) — 5 pts ───────────────────────
        int wordCount = resumeText.split("\\s+").length;
        if (wordCount >= 200 && wordCount <= 1200) {
            passed.add("Resume length is appropriate (" + wordCount + " words)");
            score += 5;
        } else if (wordCount < 200) {
            failed.add("Resume is too short (" + wordCount + " words)");
            suggestions.add("Expand your resume with more detail on projects and experience (aim for 400–800 words).");
        } else {
            failed.add("Resume may be too long (" + wordCount + " words)");
            suggestions.add("Trim your resume to 1-2 pages. ATS systems and recruiters prefer concise resumes.");
        }

        // ── CHECK 8: Keyword / skill match density — 20 pts ──────────────────
        // We reuse the already-computed Jaccard score here
        if (matchScore >= 60) {
            passed.add("Good keyword match with job description (" + matchScore + "%)");
            score += 20;
        } else if (matchScore >= 35) {
            passed.add("Moderate keyword match (" + matchScore + "%)");
            score += 10;
            suggestions.add("Mirror more keywords from the job description naturally in your resume.");
        } else {
            failed.add("Low keyword density vs job description (" + matchScore + "%)");
            suggestions.add("Add missing skills from the JD to your resume (where you genuinely have them).");
        }

        // ── CHECK 9: LinkedIn / GitHub URL — 5 pts ────────────────────────────
        if (resumeText.toLowerCase().contains("linkedin") || resumeText.toLowerCase().contains("github")) {
            passed.add("Professional profile URL (LinkedIn/GitHub) present");
            score += 5;
        } else {
            failed.add("No LinkedIn or GitHub profile URL found");
            suggestions.add("Add your LinkedIn and/or GitHub URL — many ATS systems parse and use these.");
        }

        score = Math.min(score, 100); // cap at 100

        log.info("ATS evaluation complete. Score: {}/100, Passed: {}, Failed: {}",
                score, passed.size(), failed.size());

        return AtsResult.builder()
                .atsScore(score)
                .atsLabel(getAtsLabel(score))
                .passedChecks(passed)
                .failedChecks(failed)
                .atsSuggestions(suggestions)
                .build();
    }

    private String getAtsLabel(int score) {
        if (score >= 80) return "ATS Friendly";
        if (score >= 60) return "Mostly ATS Compatible";
        if (score >= 40) return "Needs Improvement";
        return "ATS Risky";
    }
}
