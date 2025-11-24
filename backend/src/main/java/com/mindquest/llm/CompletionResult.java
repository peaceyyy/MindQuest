package com.mindquest.llm;

import java.util.HashMap;
import java.util.Map;

/**
 * Result returned from a non-streaming LLM provider call.
 * Contains the generated text and provider metadata.
 */
public final class CompletionResult {
    
    private final String id;
    private final String text;
    private final Map<String, Object> metadata;
    
    public CompletionResult(String id, String text, Map<String, Object> metadata) {
        this.id = id;
        this.text = text;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    public CompletionResult(String id, String text) {
        this(id, text, null);
    }
    
    public String getId() { return id; }
    public String getText() { return text; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    
    @Override
    public String toString() {
        return String.format("CompletionResult{id='%s', text='%s...', metadata=%s}",
                id, text.substring(0, Math.min(50, text.length())), metadata);
    }
}
