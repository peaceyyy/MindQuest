package com.mindquest.llm.providers;

import com.mindquest.llm.*;

/**
 * Factory for creating MockProvider instances.
 * Registered via ServiceLoader for testing.
 */
public class MockProviderFactory implements LlmProviderFactory {
    
    @Override
    public String getProviderId() {
        return "mock";
    }
    
    @Override
    public String getDisplayName() {
        return "Mock Provider (Testing)";
    }
    
    @Override
    public LlmProvider create(String apiKey, ProviderOptions options) throws LlmException {
        // Mock provider doesn't need API key
        return new MockProvider();
    }
}
