package com.resumeai.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads the skills dataset from src/main/resources/dataset/skills.txt
 * Provides O(1) lookup for skill keyword matching.
 */
@Service
public class SkillDatasetLoader {

    private static final Logger log = LoggerFactory.getLogger(SkillDatasetLoader.class);

    @Value("${app.dataset.skills-path:dataset/skills.txt}")
    private String skillsPath;

    private final Set<String> knownSkills = new LinkedHashSet<>();
    // Map from lowercase → canonical display name
    private final Map<String, String> displayNameMap = new HashMap<>();

    @PostConstruct
    public void load() {
        try {
            ClassPathResource resource = new ClassPathResource(skillsPath);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        knownSkills.add(line.toLowerCase());
                        displayNameMap.put(line.toLowerCase(), line);
                    }
                }
            }
            log.info("Loaded {} skills from dataset: {}", knownSkills.size(), skillsPath);
        } catch (Exception e) {
            log.error("Failed to load skills dataset: {}", e.getMessage());
        }
    }

    public Set<String> getKnownSkills() {
        return Collections.unmodifiableSet(knownSkills);
    }

    public String getDisplayName(String rawSkill) {
        return displayNameMap.getOrDefault(rawSkill.toLowerCase(), rawSkill);
    }
}
