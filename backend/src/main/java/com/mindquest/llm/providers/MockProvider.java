package com.mindquest.llm.providers;

import com.mindquest.llm.*;
import com.mindquest.llm.exception.LlmException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * Mock LLM provider for testing.
 * Returns canned responses without making real API calls.
 * Thread-safe with async support and request cancellation.
 */
public class MockProvider implements LlmProvider {
    
    private final String responseText;
    private final boolean simulateError;
    private boolean closed = false;
    private final int simulatedDelayMs;
    
    // Track in-flight async requests for cancellation
    private final ConcurrentHashMap<String, CompletableFuture<CompletionResult>> activeRequests = new ConcurrentHashMap<>();
    
    public MockProvider(String responseText, boolean simulateError, int simulatedDelayMs) {
        this.responseText = responseText;
        this.simulateError = simulateError;
        this.simulatedDelayMs = simulatedDelayMs;
    }
    
    public MockProvider(String responseText, boolean simulateError) {
        this(responseText, simulateError, 50);
    }
    
    public MockProvider(String responseText) {
        this(responseText, false, 50);
    }
    
    public MockProvider() {
        this("Mock LLM response for testing", false, 50);
    }
    
    @Override
    public ProviderMetadata getMetadata() {
        return new ProviderMetadata(
            "mock",
            "Mock Provider (Testing)",
            "mock-model-v1",
            true,
            "mock://localhost"
        );
    }
    
    @Override
    public CompletionResult complete(Prompt prompt) throws LlmException {
        if (closed) {
            throw new LlmException(
                LlmException.Category.PROVIDER_ERROR,
                "mock",
                "Provider already closed"
            );
        }
        
        if (simulateError) {
            throw new LlmException(
                LlmException.Category.NETWORK,
                "mock",
                "Simulated network error"
            );
        }
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tokens", responseText.split("\\s+").length);
        metadata.put("model", "mock-model-v1");
        
        return new CompletionResult(prompt.getId(), responseText, metadata);
    }
    
    @Override
    public CompletableFuture<CompletionResult> completeAsync(Prompt prompt) {
        if (closed) {
            return CompletableFuture.failedFuture(
                new LlmException(
                    LlmException.Category.PROVIDER_ERROR,
                    "mock",
                    "Provider already closed"
                )
            );
        }
        
        if (simulateError) {
            return CompletableFuture.failedFuture(
                new LlmException(
                    LlmException.Category.NETWORK,
                    "mock",
                    "Simulated network error"
                )
            );
        }
        
        // Simulate async execution with delay
        CompletableFuture<CompletionResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(simulatedDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("tokens", responseText.split("\\s+").length);
            metadata.put("model", "mock-model-v1");
            
            return new CompletionResult(prompt.getId(), responseText, metadata);
        });
        
        // Track for cancellation
        activeRequests.put(prompt.getId(), future);
        future.whenComplete((result, error) -> activeRequests.remove(prompt.getId()));
        
        return future;
    }
    
    @Override
    public boolean cancel(String requestId) {
        CompletableFuture<CompletionResult> future = activeRequests.get(requestId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                activeRequests.remove(requestId);
            }
            return cancelled;
        }
        return false;
    }
    
    @Override
    public Flow.Publisher<StreamEvent> stream(Prompt prompt) throws LlmException {
        if (closed) {
            throw new LlmException(
                LlmException.Category.PROVIDER_ERROR,
                "mock",
                "Provider already closed"
            );
        }
        
        if (simulateError) {
            throw new LlmException(
                LlmException.Category.NETWORK,
                "mock",
                "Simulated network error"
            );
        }
        
        SubmissionPublisher<StreamEvent> publisher = new SubmissionPublisher<>();
        
        // Simulate streaming by breaking response into chunks
        new Thread(() -> {
            try {
                String[] words = responseText.split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    String chunk = words[i] + (i < words.length - 1 ? " " : "");
                    publisher.submit(StreamEvent.partial(prompt.getId(), chunk));
                    Thread.sleep(50); // Simulate network delay
                }
                publisher.submit(StreamEvent.done(prompt.getId()));
                publisher.close();
            } catch (InterruptedException e) {
                publisher.submit(StreamEvent.error(prompt.getId(), e));
                publisher.close();
            }
        }).start();
        
        return publisher;
    }
    
    @Override
    public boolean testConnection() {
        return !simulateError && !closed;
    }
    
    @Override
    public void close() {
        closed = true;
        // Cancel all in-flight requests
        activeRequests.forEach((id, future) -> future.cancel(true));
        activeRequests.clear();
    }
    
    public boolean isClosed() {
        return closed;
    }
}
