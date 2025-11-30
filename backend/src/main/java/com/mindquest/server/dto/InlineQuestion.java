package com.mindquest.server.dto;

import java.util.List;

/**
 * DTO representing a question passed inline (e.g., from Gemini AI generation).
 */
public class InlineQuestion {
    public String questionText;
    public List<String> choices;
    public int correctIndex;
    public String id;
    public String difficulty;
    public String topic;
}
