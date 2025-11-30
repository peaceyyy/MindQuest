package com.mindquest.server.dto;

/**
 * Request DTO for submitting an answer.
 */
public class AnswerRequest {
    public int index;           // Legacy numeric index (0-3)
    public String answer;       // Letter answer (A/B/C/D)
    public Long answerTimeMs;   // Time taken to answer in milliseconds
}
