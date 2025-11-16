package com.mindquest.llm.providers;

import com.mindquest.llm.*;
import com.mindquest.llm.exception.LlmException;

/**
 * Factory for creating GeminiProvider instances.
 * Registered via ServiceLoader.
 */
public class GeminiProviderFactory implements LlmProviderFactory {
    
    @Override
    public String getProviderId() {
        return "gemini";
    }
    
    @Override
    public String getDisplayName() {
        return "Google Gemini";
    }
    
    @Override
    public LlmProvider create(String apiKey, ProviderOptions options) throws LlmException {
        return new GeminiProvider(apiKey, options);
    }
}
