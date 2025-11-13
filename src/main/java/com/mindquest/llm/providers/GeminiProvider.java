package com.mindquest.llm.providers;

import com.mindquest.llm.*;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateContentResponseUsageMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Flow;

/**
 * Gemini LLM provider using official Google GenAI SDK.
 * 
 * Uses the google-genai library for clean, production-ready API integration.
 * Supports automatic API key detection from GOOGLE_API_KEY environment variable.
 * 
 * API Documentation: https://ai.google.dev/gemini-api/docs
 */
public class GeminiProvider implements LlmProvider {
    
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    
    private final Client client;
    private final String modelName;
    private boolean closed = false;
    
    public GeminiProvider(String apiKey, ProviderOptions options) throws LlmException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new LlmException(
                LlmException.Category.AUTH,
                "gemini",
                "Gemini API key is required. Set GOOGLE_API_KEY in .env file or environment."
            );
        }
        
        this.modelName = DEFAULT_MODEL;
        
        try {
            // Initialize official Gemini client
            this.client = Client.builder()
                .apiKey(apiKey)
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
            // Categorize exceptions
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            
            if (message.contains("401") || message.contains("403") || message.contains("API key")) {
                throw new LlmException(
                    LlmException.Category.AUTH,
                    "gemini",
                    "Authentication failed: " + message,
                    e
                );
            } else if (message.contains("429") || message.contains("rate limit")) {
                throw new LlmException(
                    LlmException.Category.RATE_LIMIT,
                    "gemini",
                    "Rate limit exceeded: " + message,
                    e
                );
            } else if (message.contains("timeout") || message.contains("connection")) {
                throw new LlmException(
                    LlmException.Category.NETWORK,
                    "gemini",
                    "Network error: " + message,
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
    public Flow.Publisher<StreamEvent> stream(Prompt prompt) throws LlmException {
        if (closed) {
            throw new LlmException(
                LlmException.Category.PROVIDER_ERROR,
                "gemini",
                "Provider already closed"
            );
        }
        
        // TODO: Implement streaming using client.models.generateContentStream()
        // The official SDK provides ResponseStream<GenerateContentResponse>
        
        throw new UnsupportedOperationException(
            "Gemini streaming not yet implemented."
        );
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
        // Official SDK client doesn't require explicit closing
    }
    
    public boolean isClosed() {
        return closed;
    }
}
