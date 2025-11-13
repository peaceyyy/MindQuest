package com.mindquest.llm;

/**
 * Factory interface for creating LlmProvider instances.
 * Implementations are discovered via Java ServiceLoader (SPI).
 * 
 * To register a provider:
 * 1. Implement this interface
 * 2. Create META-INF/services/com.mindquest.llm.LlmProviderFactory
 * 3. Add the fully-qualified class name of your factory implementation
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
     * @param apiKey API key or authentication token (may be null for local providers)
     * @param options provider-specific options (endpoint override, timeouts, etc.)
     * @return configured LlmProvider instance
     * @throws LlmException if provider cannot be created (e.g., missing config)
     */
    LlmProvider create(String apiKey, ProviderOptions options) throws LlmException;
}
