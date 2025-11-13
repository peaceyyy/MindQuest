package com.mindquest.llm;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a prompt request sent to an LLM provider.
 * Immutable via builder pattern.
 */
public final class Prompt {
    
    private final String id;
    private final String instruction;
    private final String context;
    private final int maxTokens;
    private final double temperature;
    private final boolean stream;
    private final Map<String, String> hints;
    
    private Prompt(Builder builder) {
        this.id = builder.id;
        this.instruction = builder.instruction;
        this.context = builder.context;
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;
        this.stream = builder.stream;
        this.hints = new HashMap<>(builder.hints);
    }
    
    public String getId() { return id; }
    public String getInstruction() { return instruction; }
    public String getContext() { return context; }
    public int getMaxTokens() { return maxTokens; }
    public double getTemperature() { return temperature; }
    public boolean isStream() { return stream; }
    public Map<String, String> getHints() { return new HashMap<>(hints); }
    
    public static class Builder {
        private String id;
        private String instruction = "";
        private String context = "";
        private int maxTokens = 1000;
        private double temperature = 0.7;
        private boolean stream = false;
        private Map<String, String> hints = new HashMap<>();
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder instruction(String instruction) {
            this.instruction = Objects.requireNonNull(instruction, "instruction cannot be null");
            return this;
        }
        
        public Builder context(String context) {
            this.context = context;
            return this;
        }
        
        public Builder maxTokens(int maxTokens) {
            if (maxTokens <= 0) throw new IllegalArgumentException("maxTokens must be positive");
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Builder temperature(double temperature) {
            if (temperature < 0 || temperature > 2) {
                throw new IllegalArgumentException("temperature must be between 0 and 2");
            }
            this.temperature = temperature;
            return this;
        }
        
        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }
        
        public Builder addHint(String key, String value) {
            this.hints.put(key, value);
            return this;
        }
        
        public Builder hints(Map<String, String> hints) {
            this.hints = new HashMap<>(hints);
            return this;
        }
        
        public Prompt build() {
            if (instruction == null || instruction.isEmpty()) {
                throw new IllegalStateException("instruction is required");
            }
            return new Prompt(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("Prompt{id='%s', instruction='%s', maxTokens=%d, temperature=%.2f, stream=%b}",
                id, instruction.substring(0, Math.min(50, instruction.length())), maxTokens, temperature, stream);
    }
}
