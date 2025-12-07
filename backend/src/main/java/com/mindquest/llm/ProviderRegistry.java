package com.mindquest.llm;

import com.mindquest.llm.exception.LlmException;
import com.mindquest.llm.util.SecretResolver;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry for discovering and managing LLM provider factories.
 */
public class ProviderRegistry {
    
    private final Map<String, LlmProviderFactory> factories;
    
    public ProviderRegistry() {
        this.factories = new HashMap<>();
        loadFactories();
    }
    
    /**
     * Discovers provider factories using ServiceLoader.
     */
    private void loadFactories() {
        ServiceLoader<LlmProviderFactory> loader = ServiceLoader.load(LlmProviderFactory.class);
        
        for (LlmProviderFactory factory : loader) {
            String id = factory.getProviderId();
            if (factories.containsKey(id)) {
                System.err.println("[ProviderRegistry] Warning: duplicate provider ID '" + id + "' - ignoring duplicate");
                continue;
            }
            factories.put(id, factory);
            System.out.println("[ProviderRegistry] Registered provider: " + id + " (" + factory.getDisplayName() + ")");
        }
        
        if (factories.isEmpty()) {
            System.err.println("[ProviderRegistry] Warning: no LLM providers found via ServiceLoader");
        }
    }
    
    /**
     * Lists all available provider IDs.
     */
    public List<String> listProviderIds() {
        return new ArrayList<>(factories.keySet());
    }
    
    /**
     * Lists metadata for all available providers.
     */
    public List<ProviderMetadata> listProviders() {
        return factories.values().stream()
                .map(f -> {
                    try {
                        // Create a temporary instance to get metadata (no API key needed for metadata)
                        LlmProvider temp = f.create(null, null);
                        ProviderMetadata meta = temp.getMetadata();
                        temp.close();
                        return meta;
                    } catch (Exception e) {
                        System.err.println("[ProviderRegistry] Failed to get metadata for " + f.getProviderId() + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Creates a provider instance by ID.
     */
    public LlmProvider createProvider(String providerId, String apiKey, ProviderOptions options) throws LlmException {
        LlmProviderFactory factory = factories.get(providerId);
        
        if (factory == null) {
            throw new LlmException(
                LlmException.Category.INVALID_REQUEST,
                providerId,
                "Provider not found: " + providerId + ". Available: " + listProviderIds()
            );
        }
        
        return factory.create(apiKey, options);
    }
    
    /**
     * Checks if a provider is registered.
     */
    public boolean hasProvider(String providerId) {
        return factories.containsKey(providerId);
    }
    
    /**
     * Manually registers a factory (useful for testing).
     */
    public void registerFactory(LlmProviderFactory factory) {
        factories.put(factory.getProviderId(), factory);
    }
}
