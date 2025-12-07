package com.mindquest.llm;

import com.mindquest.llm.exception.LlmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * Core interface for LLM providers.
 * Implementations MUST be thread-safe or document their concurrency behavior.
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
     * Sends a prompt asynchronously and returns a CompletableFuture.
     * Non-blocking variant of complete() for UI applications.
     * 
     * @param prompt the prompt request
     * @return CompletableFuture that will complete with the result
     */
    CompletableFuture<CompletionResult> completeAsync(Prompt prompt);
    
    /**
     * Cancels an in-flight request by request ID.
     */
    boolean cancel(String requestId);
    
    /**
     * Sends a prompt and returns a streaming publisher for partial results.
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
