package com.mindquest.server.dto;

import java.util.List;

/**
 * DTO representing a saved question set (AI-generated questions persisted for reuse).
 */
public class SavedQuestionSet {
    public String id;
    public String name;
    public String topic;
    public String difficulty;
    public String provider;
    public long createdAt;
    public List<InlineQuestion> questions;
}
