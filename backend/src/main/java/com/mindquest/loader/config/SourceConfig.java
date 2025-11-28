package com.mindquest.loader.config;

import java.util.HashMap;
import java.util.Map;

public class SourceConfig {
    
    public enum SourceType {
        BUILTIN_HARDCODED,  // Use QuestionBank.java hardcoded questions
        BUILTIN_JSON,       // Use built-in JSON files from resources/questions/
        CUSTOM_CSV,         // Load from custom CSV file (requires path)
        CUSTOM_EXCEL,       // Load from custom Excel file (requires path)
        CUSTOM_JSON,        // Load from custom JSON file (requires path)
        GEMINI_API          // Generate questions via Gemini API
    }
    
    private final SourceType type;
    private final String topic;           // e.g., "Computer Science", "ai", etc.
    private final String difficulty;      // "Easy", "Medium", "Hard"
    private final String filePath;        // For CSV/Excel  files
    private final Map<String, String> extraParams;  // For API keys, prompt customization, etc.
    
    // Builder-style constructor
    private SourceConfig(Builder builder) {
        this.type = builder.type;
        this.topic = builder.topic;
        this.difficulty = builder.difficulty;
        this.filePath = builder.filePath;
        this.extraParams = builder.extraParams;
    }
    
    // Getters
    public SourceType getType() {
        return type;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public String getDifficulty() {
        return difficulty;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public Map<String, String> getExtraParams() {
        return extraParams;
    }
    
    public String getExtraParam(String key) {
        return extraParams.get(key);
    }
    

    public static class Builder {
        private SourceType type = SourceType.BUILTIN_HARDCODED;
        private String topic = "";
        private String difficulty = "";
        private String filePath = "";
        private Map<String, String> extraParams = new HashMap<>();
        
        public Builder type(SourceType type) {
            this.type = type;
            return this;
        }
        
        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }
        
        public Builder difficulty(String difficulty) {
            this.difficulty = difficulty;
            return this;
        }
        
        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }
        
        public Builder addExtraParam(String key, String value) {
            this.extraParams.put(key, value);
            return this;
        }
        
        public Builder extraParams(Map<String, String> params) {
            this.extraParams = new HashMap<>(params);
            return this;
        }
        
        public SourceConfig build() {
            return new SourceConfig(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("SourceConfig{type=%s, topic='%s', difficulty='%s', filePath='%s', extraParams=%s}",
                type, topic, difficulty, filePath, extraParams);
    }
}
