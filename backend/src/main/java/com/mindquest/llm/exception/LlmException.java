package com.mindquest.llm.exception;

/**
 * Exception thrown by LLM providers.
 * Categorizes errors for easier handling and user feedback.
 */
public class LlmException extends Exception {
    
    public enum Category {
        AUTH,           // Invalid or missing API key
        NETWORK,        // Network connection issues
        RATE_LIMIT,     // Provider rate limit exceeded
        TIMEOUT,        // Operation exceeded timeout limit
        PARSE,          // Unable to parse provider response
        PROVIDER_ERROR, // Provider returned 5xx error
        INVALID_REQUEST // Invalid prompt or configuration
    }
    
    private final Category category;
    private final String providerId;
    
    public LlmException(Category category, String providerId, String message) {
        super(message);
        this.category = category;
        this.providerId = providerId;
    }
    
    public LlmException(Category category, String providerId, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
        this.providerId = providerId;
    }
    
    public Category getCategory() { return category; }
    public String getProviderId() { return providerId; }
    
    @Override
    public String toString() {
        return String.format("LlmException{category=%s, provider='%s', message='%s'}",
                category, providerId, getMessage());
    }
}
