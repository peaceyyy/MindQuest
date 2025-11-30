package com.mindquest.server.dto;

/**
 * Request DTO for Gemini AI question generation.
 */
public class GeminiGenerateRequest {
    public String topic;
    public String difficulty;
    public int count = 5;  // Default to 5 questions
}
