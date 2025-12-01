package com.mindquest.llm.providers.local;

import com.mindquest.llm.*;
import com.mindquest.llm.exception.LlmException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;

/**
 * Local LLM provider using OpenAI-compatible API.
 * Compatible with LM Studio, Ollama, and other local inference servers.
 * 
 * Default endpoint: http://localhost:11434/v1
 * 
 * Key differences from cloud providers:
 * - No API key required (local inference)
 * - Latency depends on your GPU/CPU, not network speed
 * - Zero cost (uses your electricity instead of API credits)
 * 
 * @see <a href="https://lmstudio.ai/docs/developer/openai-compat">LM Studio API Docs</a>
 */
public class LocalLlmProvider implements LlmProvider {
    
    private static final String DEFAULT_ENDPOINT = "http://localhost:11434/v1";
    private static final String DEFAULT_MODEL = "local-model";
    private static final int DEFAULT_TIMEOUT_SECONDS = 120; // Local models can be slow on CPU
    
    private final HttpClient httpClient;
    private final String endpoint;
    private final String model;
    private final int timeoutSeconds;
    private final Gson gson;
    
    private volatile boolean closed = false;
    
    // Track active async requests for cancellation
    private final ConcurrentHashMap<String, CompletableFuture<CompletionResult>> activeRequests = new ConcurrentHashMap<>();
    
    /**
     * Creates a LocalLlmProvider with specified configuration.
     * 
     * @param endpoint The base URL of the local LLM server (e.g., "http://localhost:11434/v1")
     * @param model The model identifier (LM Studio ignores this if only one model is loaded)
     * @param options Additional provider options (timeout, headers, etc.)
     */
    public LocalLlmProvider(String endpoint, String model, ProviderOptions options) {
        this.endpoint = endpoint != null ? endpoint : DEFAULT_ENDPOINT;
        this.model = model != null ? model : DEFAULT_MODEL;
        this.timeoutSeconds = options != null ? options.getTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;
        this.gson = new Gson();
        
        // Build HTTP client with reasonable timeouts
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        System.out.println("[LocalLlmProvider] Initialized with endpoint: " + this.endpoint);
        System.out.println("[LocalLlmProvider] Model: " + this.model + ", Timeout: " + this.timeoutSeconds + "s");
    }
    
    @Override
    public ProviderMetadata getMetadata() {
        return new ProviderMetadata(
            "local",
            "Local LLM (LM Studio)",
            model,
            true, // Supports streaming (SSE)
            endpoint
        );
    }
    
    @Override
    public CompletionResult complete(Prompt prompt) throws LlmException {
        if (closed) {
            throw new LlmException(
                LlmException.Category.PROVIDER_ERROR,
                "local",
                "Provider already closed"
            );
        }
        
        String requestUrl = endpoint + "/chat/completions";
        System.out.println("[LocalLlmProvider] Sending request to: " + requestUrl);
        System.out.println("[LocalLlmProvider] Timeout set to: " + timeoutSeconds + " seconds");
        long startTime = System.currentTimeMillis();
        
        // Use legacy HttpURLConnection for better localhost compatibility on Windows
        java.net.HttpURLConnection conn = null;
        try {
            // Build the OpenAI-compatible request body
            JsonObject requestBody = buildChatCompletionRequest(prompt, false);
            String jsonBody = gson.toJson(requestBody);
            System.out.println("[LocalLlmProvider] Request body length: " + jsonBody.length() + " chars");
            
            java.net.URL url = new java.net.URL(requestUrl);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000); // 10 second connect timeout
            conn.setReadTimeout(timeoutSeconds * 1000); // Use configured timeout for read
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            // Write request body
            System.out.println("[LocalLlmProvider] Sending request body...");
            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            System.out.println("[LocalLlmProvider] Request sent, waiting for response...");
            
            int responseCode = conn.getResponseCode();
            long afterResponseCode = System.currentTimeMillis();
            System.out.println("[LocalLlmProvider] Got response code: " + responseCode + " (took " + (afterResponseCode - startTime) + "ms)");
            
            if (responseCode != 200) {
                // Read error response
                String errorBody = "";
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    errorBody = sb.toString();
                } catch (Exception ignored) {}
                throw mapHttpError(responseCode, errorBody);
            }
            
            // Read success response
            String responseBody;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                responseBody = sb.toString();
            }
            
            // Parse OpenAI-format response
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            String content = extractMessageContent(jsonResponse);
            
            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("model", model);
            metadata.put("provider", "local");
            
            // Extract token usage if present
            if (jsonResponse.has("usage")) {
                JsonObject usage = jsonResponse.getAsJsonObject("usage");
                if (usage.has("prompt_tokens")) {
                    metadata.put("promptTokens", usage.get("prompt_tokens").getAsInt());
                }
                if (usage.has("completion_tokens")) {
                    metadata.put("completionTokens", usage.get("completion_tokens").getAsInt());
                }
                if (usage.has("total_tokens")) {
                    metadata.put("totalTokens", usage.get("total_tokens").getAsInt());
                }
            }
            
            System.out.println("[LocalLlmProvider] ✅ Received response, content length: " + content.length());
            
            return new CompletionResult(prompt.getId(), content, metadata);
            
        } catch (LlmException e) {
            throw e;
        } catch (java.net.ConnectException e) {
            throw new LlmException(
                LlmException.Category.NETWORK,
                "local",
                "Cannot connect to local LLM server at " + endpoint + ". " +
                "Ensure LM Studio is running with the server enabled (Developer → Start Server).",
                e
            );
        } catch (java.net.SocketTimeoutException e) {
            throw new LlmException(
                LlmException.Category.TIMEOUT,
                "local",
                "Request timed out after " + timeoutSeconds + " seconds. " +
                "Local inference can be slow on CPU. Try loading a smaller model or ensure GPU acceleration is enabled.",
                e
            );
        } catch (Exception e) {
            throw new LlmException(
                LlmException.Category.PROVIDER_ERROR,
                "local",
                "Local LLM request failed: " + e.getMessage(),
                e
            );
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    @Override
    public CompletableFuture<CompletionResult> completeAsync(Prompt prompt) {
        if (closed) {
            return CompletableFuture.failedFuture(
                new LlmException(
                    LlmException.Category.PROVIDER_ERROR,
                    "local",
                    "Provider already closed"
                )
            );
        }
        
        JsonObject requestBody = buildChatCompletionRequest(prompt, false);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint + "/chat/completions"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();
        
        CompletableFuture<CompletionResult> future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new RuntimeException(mapHttpError(response.statusCode(), response.body()));
                }
                
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                String content = extractMessageContent(jsonResponse);
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("model", model);
                metadata.put("provider", "local");
                
                return new CompletionResult(prompt.getId(), content, metadata);
            })
            .whenComplete((result, error) -> activeRequests.remove(prompt.getId()));
        
        activeRequests.put(prompt.getId(), future);
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
        // Streaming implementation using SSE can be added in Phase 2
        // For now, throw UnsupportedOperationException
        throw new UnsupportedOperationException(
            "Streaming not yet implemented for local provider. Use complete() instead."
        );
    }
    
    /**
     * Tests connection by hitting the /v1/models endpoint.
     * This is exactly how VS Code extensions like "Continue" detect LM Studio.
     * Uses legacy HttpURLConnection for better localhost compatibility.
     */
    @Override
    public boolean testConnection() {
        if (closed) return false;
        
        String testUrl = endpoint + "/models";
        System.out.println("[LocalLlmProvider] Testing connection to: " + testUrl);
        
        // Try using legacy HttpURLConnection which handles localhost better
        java.net.HttpURLConnection conn = null;
        try {
            java.net.URL url = new java.net.URL(testUrl);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                // Read response body
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    System.out.println("[LocalLlmProvider] ✅ Connection test successful!");
                    System.out.println("[LocalLlmProvider] Available models: " + response.toString());
                }
                return true;
            }
            
            System.out.println("[LocalLlmProvider] ❌ Connection test returned status: " + responseCode);
            return false;
            
        } catch (java.net.ConnectException e) {
            System.err.println("[LocalLlmProvider] ❌ Connection refused at " + testUrl);
            System.err.println("[LocalLlmProvider] Is LM Studio running with server enabled on port 11434?");
            return false;
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("[LocalLlmProvider] ❌ Connection timed out at " + testUrl);
            return false;
        } catch (Exception e) {
            System.err.println("[LocalLlmProvider] ❌ Connection test exception: " + e.getClass().getName());
            System.err.println("[LocalLlmProvider] Message: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    /**
     * Retrieves the list of loaded models from the local server.
     * Useful for displaying in the UI what models are available.
     * 
     * @return JsonObject containing the models list, or null if unavailable
     */
    public JsonObject getLoadedModels() {
        String modelsUrl = endpoint + "/models";
        java.net.HttpURLConnection conn = null;
        try {
            java.net.URL url = new java.net.URL(modelsUrl);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return JsonParser.parseString(response.toString()).getAsJsonObject();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    @Override
    public void close() {
        closed = true;
        // Cancel all in-flight requests
        activeRequests.forEach((id, future) -> future.cancel(true));
        activeRequests.clear();
        System.out.println("[LocalLlmProvider] Closed");
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Builds an OpenAI-compatible chat completion request body.
     * Format: https://lmstudio.ai/docs/developer/openai-compat
     */
    private JsonObject buildChatCompletionRequest(Prompt prompt, boolean stream) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("stream", stream);
        body.addProperty("temperature", prompt.getTemperature());
        body.addProperty("max_tokens", prompt.getMaxTokens());
        
        JsonArray messages = new JsonArray();
        
        // Add system context if provided
        if (prompt.getContext() != null && !prompt.getContext().isEmpty()) {
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", prompt.getContext());
            messages.add(systemMsg);
        }
        
        // Add user instruction
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt.getInstruction());
        messages.add(userMsg);
        
        body.add("messages", messages);
        
        return body;
    }
    
    /**
     * Extracts the assistant's message content from an OpenAI-format response.
     */
    private String extractMessageContent(JsonObject response) {
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            return "";
        }
        
        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");
        
        if (message != null && message.has("content")) {
            return message.get("content").getAsString();
        }
        
        return "";
    }
    
    /**
     * Maps HTTP error codes to appropriate LlmException categories.
     */
    private LlmException mapHttpError(int statusCode, String body) {
        LlmException.Category category;
        String message;
        
        switch (statusCode) {
            case 400:
                category = LlmException.Category.INVALID_REQUEST;
                message = "Invalid request: " + body;
                break;
            case 404:
                category = LlmException.Category.INVALID_REQUEST;
                message = "Model not found. Ensure a model is loaded in LM Studio. " +
                         "Go to the 'Local Server' tab and click 'Start Server' after loading a model.";
                break;
            case 500:
            case 503:
                category = LlmException.Category.PROVIDER_ERROR;
                message = "Local LLM server error: " + body;
                break;
            default:
                category = LlmException.Category.PROVIDER_ERROR;
                message = "HTTP " + statusCode + ": " + body;
        }
        
        return new LlmException(category, "local", message);
    }
}
