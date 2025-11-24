package com.mindquest.llm;

/**
 * Metadata about an LLM provider.
 * Describes capabilities, model info, and endpoint configuration.
 */
public final class ProviderMetadata {
    
    private final String providerId;
    private final String displayName;
    private final String modelName;
    private final boolean supportsStreaming;
    private final String endpoint;
    
    public ProviderMetadata(String providerId, String displayName, String modelName, 
                           boolean supportsStreaming, String endpoint) {
        this.providerId = providerId;
        this.displayName = displayName;
        this.modelName = modelName;
        this.supportsStreaming = supportsStreaming;
        this.endpoint = endpoint;
    }
    
    public String getProviderId() { return providerId; }
    public String getDisplayName() { return displayName; }
    public String getModelName() { return modelName; }
    public boolean supportsStreaming() { return supportsStreaming; }
    public String getEndpoint() { return endpoint; }
    
    @Override
    public String toString() {
        return String.format("ProviderMetadata{id='%s', name='%s', model='%s', streaming=%b}",
                providerId, displayName, modelName, supportsStreaming);
    }
}
