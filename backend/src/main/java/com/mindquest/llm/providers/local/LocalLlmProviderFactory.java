package com.mindquest.llm.providers.local;

import com.mindquest.llm.*;
import com.mindquest.llm.exception.LlmException;
import com.mindquest.llm.util.SecretResolver;

/**
 * Factory for creating LocalLlmProvider instances.
 * Registered via ServiceLoader (SPI) in META-INF/services.
 * 
 * Configuration is read from:
 * 1. ProviderOptions passed at creation time
 * 2. Environment variables / .env file (via SecretResolver)
 * 3. Defaults (localhost:1234)
 */
public class LocalLlmProviderFactory implements LlmProviderFactory {
    
    @Override
    public String getProviderId() {
        return "local";
    }
    
    @Override
    public String getDisplayName() {
        return "Local LLM (LM Studio)";
    }
    
    @Override
    public LlmProvider create(String apiKey, ProviderOptions options) throws LlmException {
        SecretResolver secrets = new SecretResolver();
        
        // Determine endpoint: options override > env var > default
        String endpoint;
        if (options != null && options.getEndpoint() != null) {
            endpoint = options.getEndpoint();
        } else {
            endpoint = secrets.getLocalLlmEndpoint();
        }
        
        // Get model name from env (optional)
        String model = secrets.getLocalLlmModel();
        
        // apiKey is ignored for local providers (no auth required)
        return new LocalLlmProvider(endpoint, model, options);
    }
}
