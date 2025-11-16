package com.mindquest.llm;

import com.mindquest.llm.exception.LlmException;
import java.util.concurrent.Flow;

/**
 * Core interface for LLM providers.
 * Implementations MUST be thread-safe or document their concurrency behavior.
 * 
 * Providers can support synchronous completion or streaming responses.
 * Use try-with-resources to ensure proper cleanup.
 */
public interface LlmProvider extends AutoCloseable {
    
    /**
     * Returns metadata about this provider (id, model, capabilities).
     */
    ProviderMetadata getMetadata();
    
    /**
     * Sends a prompt and returns the complete result (blocking call).
     * 
     * @param prompt the prompt request
     * @return completion result with generated text
     * @throws LlmException if the request fails (see category for details)
     */
    CompletionResult complete(Prompt prompt) throws LlmException;
    
    /**
     * Sends a prompt and returns a streaming publisher for partial results.
     * Implementations should emit StreamEvent objects as text is generated.
     * The publisher emits a final StreamEvent with done=true when complete.
     * 
     * @param prompt the prompt request (must have stream=true)
     * @return Flow.Publisher that emits StreamEvent objects
     * @throws LlmException if the request fails before streaming starts
     * @throws UnsupportedOperationException if provider does not support streaming
     */
    Flow.Publisher<StreamEvent> stream(Prompt prompt) throws LlmException;
    
    /**
     * Tests provider connectivity and authentication.
     * Sends a minimal test request and returns true if successful.
     * 
     * @return true if provider is reachable and authenticated
     */
    boolean testConnection();
    
    /**
     * Cleanup resources (HTTP clients, connections, etc.).
     * Called when the provider is no longer needed.
     */
    @Override
    void close();
}
