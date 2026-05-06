package com.resumeai.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SCORING SERVICE
 * ---------------
 * Calculates how well resume skills match the job description using
 * Jaccard Similarity — a standard set-overlap similarity metric.
 *
 *   Score = |intersection| / |union| × 100
 *
 * Also generates context-aware recommendations based on which skills
 * are missing and the overall score level.
 */
@Service
public class ScoringService {

    public int computeMatchScore(Set<String> resumeSkills, Set<String> jdSkills) {
        if (jdSkills.isEmpty()) return 0;

        Set<String> resumeLower = toLower(resumeSkills);
        Set<String> jdLower = toLower(jdSkills);

        Set<String> intersection = new HashSet<>(resumeLower);
        intersection.retainAll(jdLower);

        Set<String> union = new HashSet<>(resumeLower);
        union.addAll(jdLower);

        if (union.isEmpty()) return 0;
        return (int) Math.round((double) intersection.size() / union.size() * 100);
    }

    public List<String> getMatchedSkills(Set<String> resumeSkills, Set<String> jdSkills) {
        Set<String> resumeLower = toLower(resumeSkills);
        return jdSkills.stream()
                .filter(s -> resumeLower.contains(s.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> getMissingSkills(Set<String> resumeSkills, Set<String> jdSkills) {
        Set<String> resumeLower = toLower(resumeSkills);
        return jdSkills.stream()
                .filter(s -> !resumeLower.contains(s.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> getExtraSkills(Set<String> resumeSkills, Set<String> jdSkills) {
        Set<String> jdLower = toLower(jdSkills);
        return resumeSkills.stream()
                .filter(s -> !jdLower.contains(s.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    public String getScoreLabel(int score) {
        if (score >= 80) return "Excellent Match";
        if (score >= 60) return "Good Match";
        if (score >= 40) return "Fair Match";
        if (score >= 20) return "Weak Match";
        return "Poor Match";
    }

    public List<String> generateRecommendations(List<String> missingSkills, int score) {
        List<String> recommendations = new ArrayList<>();

        if (score == 100) {
            recommendations.add("Perfect match! Your resume covers all required skills. Focus on tailoring your summary.");
            return recommendations;
        }

        if (score >= 80) {
            recommendations.add("Strong profile! Highlight your matched skills prominently in your summary section.");
        } else if (score >= 60) {
            recommendations.add("Good match. Close the gap by adding a few missing skills to strengthen your application.");
        } else if (score < 40 && !missingSkills.isEmpty()) {
            recommendations.add("Your resume needs significant alignment with this role. Prioritize the missing skills below.");
        }

        // Skill-specific recommendations (limit to top 5)
        for (String skill : missingSkills) {
            String s = skill.toLowerCase();
            String tip;
            if (isDevOps(s)) {
                tip = "Learn " + skill + " — DevOps skills appear in 70%+ of backend job descriptions. Start with official docs or a hands-on project.";
            } else if (isCloud(s)) {
                tip = "Get hands-on with " + skill + " — most companies are cloud-first. Free tier accounts are a great starting point.";
            } else if (isDatabase(s)) {
                tip = "Practice " + skill + " — database proficiency is tested heavily in technical interviews.";
            } else if (isFramework(s)) {
                tip = "Build a small project using " + skill + " and add it to your GitHub to demonstrate the skill.";
            } else if (isLanguage(s)) {
                tip = "Add " + skill + " to your skills section if you have working knowledge — it's explicitly required for this role.";
            } else {
                tip = "Add '" + skill + "' to your resume — it's listed as a requirement in this job description.";
            }
            recommendations.add(tip);
            if (recommendations.size() >= 6) break;
        }

        if (missingSkills.isEmpty() && score < 80) {
            recommendations.add("Great skill coverage! Focus on quantifying your achievements with numbers and metrics.");
        }

        return recommendations;
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private Set<String> toLower(Set<String> skills) {
        return skills.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    private boolean isDevOps(String s) {
        return s.contains("docker") || s.contains("kubernetes") || s.contains("jenkins")
                || s.contains("ci/cd") || s.contains("ansible") || s.contains("terraform")
                || s.contains("helm") || s.contains("argocd");
    }

    private boolean isCloud(String s) {
        return s.contains("aws") || s.contains("azure") || s.contains("gcp")
                || s.contains("google cloud") || s.contains("cloud");
    }

    private boolean isDatabase(String s) {
        return s.contains("sql") || s.contains("mongo") || s.contains("redis")
                || s.contains("cassandra") || s.contains("oracle") || s.contains("postgres")
                || s.contains("elasticsearch");
    }

    private boolean isFramework(String s) {
        return s.contains("spring") || s.contains("react") || s.contains("angular")
                || s.contains("django") || s.contains("flask") || s.contains("node")
                || s.contains("fastapi") || s.contains("express") || s.contains("vue");
    }

    private boolean isLanguage(String s) {
        return s.equals("java") || s.equals("python") || s.equals("go") || s.equals("rust")
                || s.equals("kotlin") || s.equals("scala") || s.equals("typescript")
                || s.equals("javascript") || s.equals("c++") || s.equals("c#");
    }
}
