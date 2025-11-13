package com.mindquest.llm;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration options for LLM provider instances.
 * Allows overriding endpoint, timeout, retry settings, etc.
 */
public final class ProviderOptions {
    
    private final String endpoint;
    private final int timeoutSeconds;
    private final int maxRetries;
    private final Map<String, String> customHeaders;
    
    private ProviderOptions(Builder builder) {
        this.endpoint = builder.endpoint;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.maxRetries = builder.maxRetries;
        this.customHeaders = new HashMap<>(builder.customHeaders);
    }
    
    public String getEndpoint() { return endpoint; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public int getMaxRetries() { return maxRetries; }
    public Map<String, String> getCustomHeaders() { return new HashMap<>(customHeaders); }
    
    public static class Builder {
        private String endpoint = null;
        private int timeoutSeconds = 30;
        private int maxRetries = 3;
        private Map<String, String> customHeaders = new HashMap<>();
        
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }
        
        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder addHeader(String key, String value) {
            this.customHeaders.put(key, value);
            return this;
        }
        
        public ProviderOptions build() {
            return new ProviderOptions(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
