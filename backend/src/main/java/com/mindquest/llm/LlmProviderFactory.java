package com.mindquest.llm;

import com.mindquest.llm.exception.LlmException;

/**
 * Factory interface for creating LlmProvider instances.
 */
public interface LlmProviderFactory {
    
    /**
     * Unique provider identifier (e.g., "gemini", "openai", "local-llm").
     * Used for provider selection and logging.
     */
    String getProviderId();
    
    /**
     * Human-readable display name for the provider.
     */
    String getDisplayName();
    
    /**
     * Creates a configured provider instance.
     * 
     */
    LlmProvider create(String apiKey, ProviderOptions options) throws LlmException;
}
