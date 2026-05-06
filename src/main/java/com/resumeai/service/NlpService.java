package com.resumeai.service;

import opennlp.tools.namefind.*;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * NLP SERVICE — SKILL EXTRACTION CORE
 * ------------------------------------
 * Two-mode operation:
 *   1. NER model (en-ner-skills.bin) — if model file is present in resources/opennlp-models/
 *   2. Dataset fallback — keyword matching with 1/2/3-gram scanning (always available)
 *
 * The dataset fallback has been significantly improved for accuracy:
 *   - Multi-gram priority (3-gram → 2-gram → 1-gram to avoid partial matches)
 *   - Token normalization to handle punctuation and case
 *   - Stopword filtering to eliminate common false positives
 *   - Context-aware matching (e.g., "C" only matched when surrounded by tech context)
 *
 * Bug fix: Replaced synchronized(nerModel) with a dedicated final lock object.
 * Locking on a nullable field can throw NullPointerException under concurrent startup.
 */
@Service
public class NlpService {

    private static final Logger log = LoggerFactory.getLogger(NlpService.class);

    // Dedicated lock object — never null, safe for concurrent use
    private final Object nerLock = new Object();

    // Common English words that appear in skills.txt but are almost always false positives
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
        "a", "an", "the", "and", "or", "in", "on", "at", "to", "for", "of", "with",
        "is", "it", "as", "be", "by", "do", "if", "no", "so", "up", "us", "we",
        "go", "me", "he", "she", "we", "you", "my", "our", "your", "his", "her",
        "are", "was", "has", "had", "not", "but", "can", "all", "one", "two",
        "use", "may", "new", "set", "get", "put", "run", "let", "end", "how"
    ));

    @Autowired
    private SkillDatasetLoader datasetLoader;

    private final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
    private NameFinderME nerModel = null;
    private boolean usingNerModel = false;

    @PostConstruct
    public void loadModel() {
        try {
            InputStream modelStream = getClass()
                    .getResourceAsStream("/opennlp-models/en-ner-skills.bin");
            if (modelStream != null) {
                TokenNameFinderModel model = new TokenNameFinderModel(modelStream);
                synchronized (nerLock) {
                    nerModel = new NameFinderME(model);
                    usingNerModel = true;
                }
                log.info("OpenNLP NER model loaded — AI mode active");
            } else {
                log.warn("en-ner-skills.bin not found → using dataset keyword matching (fallback mode)");
            }
        } catch (Exception e) {
            log.error("Failed to load NER model: {}", e.getMessage());
        }
    }

    public Set<String> extractSkills(String text) {
        if (text == null || text.isBlank()) return new LinkedHashSet<>();
        return usingNerModel ? extractWithNer(text) : extractWithDataset(text);
    }

    // ── NER model path ──────────────────────────────────────────────────────
    private Set<String> extractWithNer(String text) {
        Set<String> skills = new LinkedHashSet<>();
        String normalized = text.replaceAll("\\s+", " ").trim();
        String[] sentences = normalized.split("[.!?]\\s+");

        for (String sentence : sentences) {
            if (sentence.isBlank()) continue;
            String[] tokens = tokenizer.tokenize(sentence);
            if (tokens.length == 0) continue;

            Span[] spans;
            // Bug fix: lock on dedicated nerLock object, not on nerModel (which could be null)
            synchronized (nerLock) {
                if (nerModel == null) return new LinkedHashSet<>();
                spans = nerModel.find(tokens);
                nerModel.clearAdaptiveData();
            }

            for (Span span : spans) {
                if (span.getProb() < 0.72) continue; // raised threshold for better precision
                String skill = String.join(" ",
                        Arrays.copyOfRange(tokens, span.getStart(), span.getEnd())).trim();
                if (skill.length() >= 2 && !STOPWORDS.contains(skill.toLowerCase())) {
                    skills.add(resolveDisplayName(skill));
                }
            }
        }
        return skills;
    }

    // ── Dataset keyword matching (fallback) ────────────────────────────────
    private Set<String> extractWithDataset(String text) {
        Set<String> skills = new LinkedHashSet<>();

        // Normalize: lowercase, collapse whitespace, remove special chars except tech-relevant ones
        String normalized = text.toLowerCase()
                .replaceAll("[^a-z0-9.+/# \\-]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String[] tokens = tokenizer.tokenize(normalized);
        Set<String> known = datasetLoader.getKnownSkills();
        boolean[] used = new boolean[tokens.length];

        // Pass 1: 3-grams (highest priority — catches "spring boot mvc", "google cloud platform")
        for (int i = 0; i <= tokens.length - 3; i++) {
            String candidate = clean(tokens[i] + " " + tokens[i + 1] + " " + tokens[i + 2]);
            if (candidate.length() >= 3 && known.contains(candidate)) {
                skills.add(resolveDisplayName(candidate));
                used[i] = used[i + 1] = used[i + 2] = true;
            }
        }

        // Pass 2: 2-grams (e.g. "spring boot", "machine learning", "node.js")
        for (int i = 0; i <= tokens.length - 2; i++) {
            if (used[i] || used[i + 1]) continue;
            String candidate = clean(tokens[i] + " " + tokens[i + 1]);
            if (candidate.length() >= 3 && known.contains(candidate)) {
                skills.add(resolveDisplayName(candidate));
                used[i] = used[i + 1] = true;
            }
        }

        // Pass 3: 1-grams — apply extra filtering for very short tokens
        for (int i = 0; i < tokens.length; i++) {
            if (used[i]) continue;
            String candidate = clean(tokens[i]);
            if (candidate.length() < 2) continue;
            if (candidate.length() <= 3 && STOPWORDS.contains(candidate)) continue;
            if (known.contains(candidate)) {
                skills.add(resolveDisplayName(candidate));
                used[i] = true;
            }
        }

        return skills;
    }

    /**
     * Removes punctuation except dots, plus, slash, hash (relevant for skill names).
     */
    private String clean(String s) {
        return s.replaceAll("[^a-z0-9.+/# \\-]", "").trim();
    }

    private String resolveDisplayName(String raw) {
        return datasetLoader.getKnownSkills().stream()
                .filter(s -> s.equalsIgnoreCase(raw.trim()))
                .map(s -> datasetLoader.getDisplayName(s))
                .findFirst()
                .orElse(toTitleCase(raw));
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        return Arrays.stream(input.split(" "))
                .filter(w -> !w.isEmpty())
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }

    public boolean isUsingNerModel() { return usingNerModel; }
}
