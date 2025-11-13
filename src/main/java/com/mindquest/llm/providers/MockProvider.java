package com.mindquest.llm.providers;

import com.mindquest.llm.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * Mock LLM provider for testing.
 * Returns canned responses without making real API calls.
 */
public class MockProvider implements LlmProvider {
    
    private final String responseText;
    private final boolean simulateError;
    private boolean closed = false;
    
    public MockProvider(String responseText, boolean simulateError) {
        this.responseText = responseText;
        this.simulateError = simulateError;
    }
    
    public MockProvider(String responseText) {
        this(responseText, false);
    }
    
    public MockProvider() {
        this("Mock LLM response for testing", false);
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
    }
    
    public boolean isClosed() {
        return closed;
    }
}
