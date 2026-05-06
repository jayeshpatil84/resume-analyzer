package com.resumeai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single spell / grammar error found in the resume.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpellError {
    private String word;                  // The misspelled word
    private String context;               // Surrounding sentence snippet
    private List<String> suggestions;     // Top replacement suggestions
    private String ruleDescription;       // Human-readable description of the error
}
