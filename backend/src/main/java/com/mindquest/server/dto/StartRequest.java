package com.mindquest.server.dto;

import java.util.List;

/**
 * Request DTO for starting a new game round.
 */
public class StartRequest {
    public String topic;
    public String difficulty;
    public List<InlineQuestion> questions; // Optional: For Gemini/LLM generated questions
}
