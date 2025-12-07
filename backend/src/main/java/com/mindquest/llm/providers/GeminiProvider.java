package com.mindquest.llm.providers;

import com.mindquest.llm.*;
import com.mindquest.llm.exception.LlmException;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateContentResponseUsageMetadata;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Gemini LLM provider using official Google GenAI SDK.

 * 
 * API Documentation: https://ai.google.dev/gemini-api/docs
 */
public class GeminiProvider implements LlmProvider {
    
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    
    private final Client client;
    private final String modelName;
    private final int timeoutSeconds;
    private boolean closed = false;
    
    // Track in-flight async requests for cancellation
    private final ConcurrentHashMap<String, CompletableFuture<CompletionResult>> activeRequests = new ConcurrentHashMap<>();
    
    // Executor for streaming operations
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("GeminiStream-" + t.hashCode());
        return t;
    });
    
    // Scheduler for timeout enforcement
    private final ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("GeminiTimeout");
        return t;
    });
    
    private static final int STREAMING_TIMEOUT_SECONDS = 120; // 2 minutes for long-form content
    
    public GeminiProvider(String apiKey, ProviderOptions options) throws LlmException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new LlmException(
                LlmException.Category.AUTH,
                "gemini",
                "Gemini API key is required. Set GOOGLE_API_KEY in .env file or environment."
            );
        }
        
        this.modelName = DEFAULT_MODEL;
        this.timeoutSeconds = options != null ? options.getTimeoutSeconds() : 60;  // Default 60 seconds
        
        try {
            // Initialize official Gemini client with timeout configuration
            // NOTE: HttpOptions.timeout() expects MILLISECONDS, not seconds!
            int timeoutMillis = timeoutSeconds * 1000;
            
            // Configure retry options for transient failures (rate limits, timeouts)
            HttpRetryOptions retryOptions = HttpRetryOptions.builder()
                .attempts(3)                    // Retry up to 3 times
                .httpStatusCodes(408, 429, 503) // Retry on timeout, rate limit, service unavailable
                .build();
            
            HttpOptions httpOptions = HttpOptions.builder()
                .timeout(timeoutMillis)
                .retryOptions(retryOptions)
                .build();
            
            System.out.println("[GeminiProvider] Initializing with timeout: " + timeoutSeconds + "s (" + timeoutMillis + "ms), retries: 3");
            
            this.client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(httpOptions)
                .build();
        } catch (Exception e) {
            throw new LlmException(
                LlmException.Category.PROVIDER_ERROR,
                "gemini",
                "Failed to initialize Gemini client: " + e.getMessage(),
                e
            );
        }
    }
    
    @Override
    public ProviderMetadata getMetadata() {
        return new ProviderMetadata(
            "gemini",
            "Google Gemini",
            modelName,
            true, // Gemini supports streaming
            "https://generativelanguage.googleapis.com/v1beta"
        );
    }
    
    @Override
    public CompletionResult complete(Prompt prompt) throws LlmException {
        if (closed) {
            throw new LlmException(
                LlmException.Category.PROVIDER_ERROR,
                "gemini",
                "Provider already closed"
            );
        }
        
        try {
            // Build configuration
            GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder()
                .temperature((float) prompt.getTemperature())
                .maxOutputTokens(prompt.getMaxTokens());
            
            // Disable thinking mode for faster, cheaper responses
            configBuilder.thinkingConfig(
                com.google.genai.types.ThinkingConfig.builder()
                    .thinkingBudget(0)
                    .build()
            );
            
            GenerateContentConfig config = configBuilder.build();
            
            // Combine instruction and context
            String fullPrompt = prompt.getInstruction();
            if (prompt.getContext() != null && !prompt.getContext().isEmpty()) {
                fullPrompt = prompt.getContext() + "\n\n" + fullPrompt;
            }
            
            // Call Gemini API
            GenerateContentResponse response = client.models.generateContent(
                modelName,
                fullPrompt,
                config
            );
            
            // Extract response text
            String responseText = response.text();
            
            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("model", modelName);
            
            // Add token usage if available
            if (response.usageMetadata().isPresent()) {
                GenerateContentResponseUsageMetadata usage = response.usageMetadata().get();
                if (usage.promptTokenCount().isPresent()) {
                    metadata.put("promptTokens", usage.promptTokenCount().get());
                }
                if (usage.candidatesTokenCount().isPresent()) {
                    metadata.put("completionTokens", usage.candidatesTokenCount().get());
                }
                if (usage.totalTokenCount().isPresent()) {
                    metadata.put("totalTokens", usage.totalTokenCount().get());
                }
            }
            
            return new CompletionResult(prompt.getId(), responseText, metadata);
            
        } catch (Exception e) {
            // Categorize exceptions with detailed diagnostics
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            String rootMessage = rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.getClass().getSimpleName();
            
            // Log detailed error for debugging
            System.err.println("[GeminiProvider] API call failed:");
            System.err.println("  Message: " + message);
            System.err.println("  Root cause: " + rootCause.getClass().getSimpleName() + " - " + rootMessage);
            System.err.println("  Model: " + modelName);
            System.err.println("  Prompt length: " + (prompt.getInstruction() != null ? prompt.getInstruction().length() : 0) + " chars");
            
            if (message.contains("401") || message.contains("403") || message.contains("API key") || message.contains("PERMISSION_DENIED")) {
                throw new LlmException(
                    LlmException.Category.AUTH,
                    "gemini",
                    "Authentication failed: " + message,
                    e
                );
            } else if (message.contains("429") || message.contains("rate limit") || message.contains("RESOURCE_EXHAUSTED")) {
                throw new LlmException(
                    LlmException.Category.RATE_LIMIT,
                    "gemini",
                    "Rate limit exceeded: " + message,
                    e
                );
            } else if (message.contains("timeout") || message.contains("connection") || 
                       rootMessage.contains("Canceled") || rootMessage.contains("SocketTimeoutException")) {
                // "Canceled" IOException typically means the request timed out (callTimeout exceeded)
                throw new LlmException(
                    LlmException.Category.TIMEOUT,
                    "gemini",
                    "Request timed out or connection error: " + message + " (root: " + rootMessage + ")",
                    e
                );
            } else {
                throw new LlmException(
                    LlmException.Category.PROVIDER_ERROR,
                    "gemini",
                    "Gemini API error: " + message,
                    e
                );
            }
        }
    }
    
    @Override
    public CompletableFuture<CompletionResult> completeAsync(Prompt prompt) {
        if (closed) {
            return CompletableFuture.failedFuture(
                new LlmException(
                    LlmException.Category.PROVIDER_ERROR,
                    "gemini",
                    "Provider already closed"
                )
            );
        }
        
        // Build configuration
        GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder()
            .temperature((float) prompt.getTemperature())
            .maxOutputTokens(prompt.getMaxTokens());
        
        // Disable thinking mode for faster responses
        configBuilder.thinkingConfig(
            com.google.genai.types.ThinkingConfig.builder()
                .thinkingBudget(0)
                .build()
        );
        
        GenerateContentConfig config = configBuilder.build();
        
        // Combine instruction and context
        String fullPrompt = prompt.getInstruction();
        if (prompt.getContext() != null && !prompt.getContext().isEmpty()) {
            fullPrompt = prompt.getContext() + "\n\n" + fullPrompt;
        }
        
        // Use SDK's built-in async API
        CompletableFuture<GenerateContentResponse> responseFuture = 
            client.async.models.generateContent(modelName, fullPrompt, config);
        
        // Transform to CompletionResult and track for cancellation
        CompletableFuture<CompletionResult> resultFuture = responseFuture
            .thenApply(response -> {
                String responseText = response.text();
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("model", modelName);
                
                // Add token usage if available
                if (response.usageMetadata().isPresent()) {
                    GenerateContentResponseUsageMetadata usage = response.usageMetadata().get();
                    if (usage.promptTokenCount().isPresent()) {
                        metadata.put("promptTokens", usage.promptTokenCount().get());
                    }
                    if (usage.candidatesTokenCount().isPresent()) {
                        metadata.put("completionTokens", usage.candidatesTokenCount().get());
                    }
                    if (usage.totalTokenCount().isPresent()) {
                        metadata.put("totalTokens", usage.totalTokenCount().get());
                    }
                }
                
                return new CompletionResult(prompt.getId(), responseText, metadata);
            })
            .exceptionally(e -> {
                // Map exceptions to LlmException categories
                throw mapException(e.getCause() != null ? e.getCause() : e);
            })
            .whenComplete((result, error) -> {
                // Remove from active requests when done
                activeRequests.remove(prompt.getId());
            });
        
        // Track for cancellation
        activeRequests.put(prompt.getId(), resultFuture);
        
        // Apply application-level timeout
        return resultFuture.orTimeout(timeoutSeconds, TimeUnit.SECONDS);
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
    
    /**
     * Maps exceptions to LlmException with appropriate categories.
     */
    private RuntimeException mapException(Throwable e) {
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        
        LlmException.Category category;
        if (e instanceof TimeoutException) {
            category = LlmException.Category.TIMEOUT;
        } else if (message.contains("401") || message.contains("403") || message.contains("API key")) {
            category = LlmException.Category.AUTH;
        } else if (message.contains("429") || message.contains("rate limit")) {
            category = LlmException.Category.RATE_LIMIT;
        } else if (message.contains("timeout") || message.contains("connection")) {
            category = LlmException.Category.NETWORK;
        } else {
            category = LlmException.Category.PROVIDER_ERROR;
        }
        
        return new RuntimeException(
            new LlmException(category, "gemini", message, e)
        );
    }
    
    @Override
    public Flow.Publisher<StreamEvent> stream(Prompt prompt) throws LlmException {
        if (closed) {
            throw new LlmException(
                LlmException.Category.PROVIDER_ERROR,
                "gemini",
                "Provider already closed"
            );
        }
        
        // Build configuration
        GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder()
            .temperature((float) prompt.getTemperature())
            .maxOutputTokens(prompt.getMaxTokens());
        
        // Disable thinking mode for faster responses
        configBuilder.thinkingConfig(
            com.google.genai.types.ThinkingConfig.builder()
                .thinkingBudget(0)
                .build()
        );
        
        GenerateContentConfig config = configBuilder.build();
        
        // Combine instruction and context
        String fullPrompt = prompt.getInstruction();
        if (prompt.getContext() != null && !prompt.getContext().isEmpty()) {
            fullPrompt = prompt.getContext() + "\n\n" + fullPrompt;
        }
        
        // Create publisher with backpressure support
        SubmissionPublisher<StreamEvent> publisher = new SubmissionPublisher<>(
            streamExecutor,
            Flow.defaultBufferSize()
        );
        
        // Schedule timeout task
        var timeoutTask = timeoutScheduler.schedule(() -> {
            publisher.submit(StreamEvent.error(
                prompt.getId(),
                new TimeoutException("Streaming exceeded " + STREAMING_TIMEOUT_SECONDS + " seconds")
            ));
            publisher.close();
        }, STREAMING_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Start streaming in background
        final String finalPrompt = fullPrompt;
        streamExecutor.submit(() -> {
            com.google.genai.ResponseStream<GenerateContentResponse> responseStream = null;
            try {
                // Get streaming response from SDK
                responseStream = client.models.generateContentStream(
                    modelName,
                    finalPrompt,
                    config
                );
                
                // Iterate and emit stream events
                for (GenerateContentResponse chunk : responseStream) {
                    String partialText = chunk.text();
                    
                    if (partialText != null && !partialText.isEmpty()) {
                        StreamEvent event = StreamEvent.partial(prompt.getId(), partialText);
                        publisher.submit(event);
                    }
                }
                
                // Emit completion event
                publisher.submit(StreamEvent.done(prompt.getId()));
                timeoutTask.cancel(false); // Cancel timeout since we completed normally
                
            } catch (Exception e) {
                timeoutTask.cancel(false); // Cancel timeout on error too
                // Emit error event
                LlmException llmError = new LlmException(
                    LlmException.Category.PROVIDER_ERROR,
                    "gemini",
                    "Streaming error: " + e.getMessage(),
                    e
                );
                publisher.submit(StreamEvent.error(prompt.getId(), llmError));
            } finally {
                // Clean up stream
                if (responseStream != null) {
                    try {
                        responseStream.close();
                    } catch (Exception ignored) {}
                }
                publisher.close();
            }
        });
        
        return publisher;
    }
    
    @Override
    public boolean testConnection() {
        if (closed) return false;
        
        try {
            // Send a minimal test request
            Prompt testPrompt = new Prompt.Builder()
                .id("test")
                .instruction("Say 'OK' if you can read this.")
                .maxTokens(10)
                .temperature(0.1)
                .build();
            
            CompletionResult result = complete(testPrompt);
            return result != null && result.getText() != null && !result.getText().isEmpty();
        } catch (Exception e) {
            System.err.println("[GeminiProvider] Connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void close() {
        closed = true;
        // Cancel all in-flight requests
        activeRequests.forEach((id, future) -> future.cancel(true));
        activeRequests.clear();
        // Shutdown executors
        streamExecutor.shutdown();
        timeoutScheduler.shutdown();
        // Official SDK client doesn't require explicit closing
    }
    
    public boolean isClosed() {
        return closed;
    }
}
