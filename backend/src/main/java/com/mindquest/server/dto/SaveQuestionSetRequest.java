package com.mindquest.server.dto;

import java.util.List;

/**
 * Request DTO for saving a new question set.
 */
public class SaveQuestionSetRequest {
    public String name;
    public String topic;
    public String difficulty;
    public String provider;
    public List<InlineQuestion> questions;
}
