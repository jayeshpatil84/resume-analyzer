package com.resumeai.service;

import com.resumeai.dto.SpellError;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class SpellCheckService {

    private static final Logger log = LoggerFactory.getLogger(SpellCheckService.class);
    private static final int MAX_ERRORS = 20;

    private static final Set<String> TECH_WHITELIST = new HashSet<>(Arrays.asList(
        "java", "python", "kotlin", "golang", "rust", "scala", "typescript",
        "javascript", "nodejs", "npm", "webpack", "babel",
        "springboot", "springframework", "fastapi", "django", "flask",
        "react", "reactjs", "vuejs", "angularjs", "nextjs", "nuxtjs",
        "hibernate", "mybatis", "micronaut", "quarkus",
        "aws", "gcp", "azure", "kubernetes", "k8s", "docker", "terraform",
        "ansible", "jenkins", "gitlab", "github", "bitbucket", "ci", "cd",
        "devops", "sre", "iac", "argocd", "helm",
        "mysql", "postgresql", "mongodb", "redis", "elasticsearch",
        "cassandra", "dynamodb", "neo4j", "couchdb", "cockroachdb",
        "jira", "confluence", "sonarqube", "grafana", "kibana", "prometheus",
        "postman", "swagger", "openapi", "graphql", "grpc", "kafka",
        "rabbitmq", "activemq", "zookeeper",
        "agile", "scrum", "kanban", "tdd", "bdd", "ddd", "microservices",
        "monorepo", "backend", "frontend", "fullstack", "api", "rest",
        "restful", "oauth", "jwt", "saml", "ldap",
        "github", "linkedin", "leetcode", "hackerrank", "stackoverflow",
        "onboarded", "onboarding", "upskilled", "refactored", "refactoring",
        "mentored", "mentoring", "solutioning", "co-led", "co-built"
    ));

    private JLanguageTool languageTool;

    @PostConstruct
    public void init() {
        try {
            languageTool = new JLanguageTool(new AmericanEnglish());

            languageTool.getAllRules().stream()
                    .filter(r -> !r.isDictionaryBasedSpellingRule())
                    .forEach(r -> languageTool.disableRule(r.getId()));

            log.info("LanguageTool spell checker initialized");
        } catch (Exception e) {
            log.error("Failed to initialize LanguageTool: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        // No shutdown() method exists in JLanguageTool
        languageTool = null;
        log.info("LanguageTool instance cleaned up");
    }

    public List<SpellError> checkSpelling(String text) {
        List<SpellError> errors = new ArrayList<>();
        if (languageTool == null || text == null || text.isBlank()) return errors;

        try {
            List<RuleMatch> matches = languageTool.check(text);

            for (RuleMatch match : matches) {
                String word = text.substring(match.getFromPos(), match.getToPos()).trim();

                if (shouldSkip(word)) continue;

                String context = buildContext(text, match.getFromPos(), match.getToPos());

                List<String> suggestions = match.getSuggestedReplacements()
                        .stream()
                        .limit(3)
                        .toList();

                errors.add(SpellError.builder()
                        .word(word)
                        .context(context)
                        .suggestions(suggestions)
                        .ruleDescription(match.getMessage()
                                .replaceAll("<suggestion>", "'")
                                .replaceAll("</suggestion>", "'"))
                        .build());

                if (errors.size() >= MAX_ERRORS) break;
            }

        } catch (IOException e) {
            log.error("Spell check failed: {}", e.getMessage());
        }

        return errors;
    }

    public String buildSummary(List<SpellError> errors) {
        if (errors.isEmpty()) {
            return "No spelling errors found. Your resume looks clean!";
        }
        if (errors.size() == 1) {
            return "1 potential spelling issue found. Review and correct it.";
        }
        if (errors.size() <= 5) {
            return errors.size() + " spelling issues found. Fix them to polish your resume.";
        }
        return errors.size() + " spelling issues found. Proofread carefully.";
    }

    private boolean shouldSkip(String word) {
        if (word == null || word.length() <= 1) return true;

        if (word.equals(word.toUpperCase()) && word.length() <= 4) return true;

        if (word.matches("[0-9A-Za-z.\\-+]+") && word.matches(".*[0-9].*")) return true;

        if (TECH_WHITELIST.contains(word.toLowerCase())) return true;

        if (word.contains("@") || word.contains("://") || word.contains("www.")) return true;

        return false;
    }

    private String buildContext(String text, int from, int to) {
        int start = Math.max(0, from - 40);
        int end = Math.min(text.length(), to + 40);

        String snippet = text.substring(start, end)
                .replaceAll("\\s+", " ")
                .trim();

        String word = text.substring(from, to);

        return snippet.replace(word, "**" + word + "**");
    }
}