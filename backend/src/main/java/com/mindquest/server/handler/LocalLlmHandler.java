package com.mindquest.server.handler;

import com.mindquest.llm.LlmProvider;
import com.mindquest.llm.ProviderRegistry;
import com.mindquest.llm.providers.local.LocalLlmProvider;
import com.mindquest.llm.util.SecretResolver;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.loader.factory.QuestionBankFactory;
import com.mindquest.model.question.Question;
import com.mindquest.server.dto.GeminiGenerateRequest;
import com.mindquest.server.util.TopicNormalizer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for Local LLM provider endpoints.
 * Provides status, model discovery, and provider availability information.
 */
public class LocalLlmHandler {
    
    private final ProviderRegistry registry;
    private final SecretResolver secrets;
    
    public LocalLlmHandler() {
        this.registry = new ProviderRegistry();
        this.secrets = new SecretResolver();
    }
    
    /**
     * GET /api/llm/providers
     * Returns a list of all registered LLM providers and their availability status.
     * 
     * This is how the frontend knows which providers can be used.
     * Similar to how VS Code extensions detect available models.
     */
    public void getProviders(Context ctx) {
        List<Map<String, Object>> providers = new ArrayList<>();
        
        for (String providerId : registry.listProviderIds()) {
            Map<String, Object> providerInfo = new HashMap<>();
            providerInfo.put("id", providerId);
            
            switch (providerId) {
                case "gemini":
                    boolean hasGeminiKey = secrets.getGeminiApiKey() != null;
                    providerInfo.put("name", "Gemini AI");
                    providerInfo.put("available", hasGeminiKey);
                    providerInfo.put("type", "cloud");
                    providerInfo.put("requiresApiKey", true);
                    providerInfo.put("description", "Google's cloud-based AI (requires internet)");
                    if (!hasGeminiKey) {
                        providerInfo.put("unavailableReason", "API key not configured. Set GOOGLE_API_KEY in .env file.");
                    }
                    break;
                    
                case "local":
                    // Test connection to local LM Studio server
                    boolean localAvailable = false;
                    String loadedModel = null;
                    String endpoint = secrets.getLocalLlmEndpoint();
                    
                    try {
                        LlmProvider localProvider = registry.createProvider("local", null, null);
                        localAvailable = localProvider.testConnection();
                        
                        // Try to get model info
                        if (localAvailable && localProvider instanceof LocalLlmProvider) {
                            JsonObject models = ((LocalLlmProvider) localProvider).getLoadedModels();
                            if (models != null && models.has("data")) {
                                JsonArray data = models.getAsJsonArray("data");
                                if (data.size() > 0) {
                                    loadedModel = data.get(0).getAsJsonObject().get("id").getAsString();
                                }
                            }
                        }
                        
                        localProvider.close();
                    } catch (Exception e) {
                        System.err.println("[LocalLlmHandler] Error checking local provider: " + e.getMessage());
                    }
                    
                    providerInfo.put("name", "Local LLM (LM Studio)");
                    providerInfo.put("available", localAvailable);
                    providerInfo.put("type", "local");
                    providerInfo.put("requiresApiKey", false);
                    providerInfo.put("endpoint", endpoint);
                    providerInfo.put("description", "Run AI locally on your machine (offline capable)");
                    
                    if (loadedModel != null) {
                        providerInfo.put("loadedModel", loadedModel);
                    }
                    
                    if (!localAvailable) {
                        providerInfo.put("unavailableReason", 
                            "LM Studio server not running. Start it from Developer → Start Server in LM Studio.");
                    }
                    break;
                    
                case "mock":
                    providerInfo.put("name", "Mock Provider");
                    providerInfo.put("available", true);
                    providerInfo.put("type", "mock");
                    providerInfo.put("requiresApiKey", false);
                    providerInfo.put("description", "Testing provider with canned responses");
                    break;
                    
                default:
                    providerInfo.put("name", providerId);
                    providerInfo.put("available", false);
                    providerInfo.put("type", "unknown");
            }
            
            providers.add(providerInfo);
        }
        
        ctx.json(Map.of(
            "providers", providers,
            "defaultProvider", secrets.getDefaultProvider(),
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * GET /api/llm/local/status
     * Returns detailed status of the local LLM server.
     * Includes loaded models and server health.
     */
    public void getLocalStatus(Context ctx) {
        String endpoint = secrets.getLocalLlmEndpoint();
        
        try {
            LlmProvider localProvider = registry.createProvider("local", null, null);
            boolean connected = localProvider.testConnection();
            
            if (!connected) {
                ctx.json(Map.of(
                    "status", "offline",
                    "endpoint", endpoint,
                    "message", "LM Studio server is not running",
                    "instructions", List.of(
                        "1. Open LM Studio",
                        "2. Load a model (e.g., Llama 3, LLaVA, Mistral)",
                        "3. Go to Developer → Local Server",
                        "4. Click 'Start Server'",
                        "5. Ensure port is 1234 (default)"
                    )
                ));
                localProvider.close();
                return;
            }
            
            // Get loaded models
            JsonObject models = null;
            if (localProvider instanceof LocalLlmProvider) {
                models = ((LocalLlmProvider) localProvider).getLoadedModels();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "online");
            response.put("endpoint", endpoint);
            response.put("message", "LM Studio server is running and ready");
            
            if (models != null && models.has("data")) {
                JsonArray data = models.getAsJsonArray("data");
                List<String> modelNames = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    modelNames.add(data.get(i).getAsJsonObject().get("id").getAsString());
                }
                response.put("loadedModels", modelNames);
            }
            
            localProvider.close();
            ctx.json(response);
            
        } catch (Exception e) {
            ctx.status(500).json(Map.of(
                "status", "error",
                "endpoint", endpoint,
                "message", "Error checking local LLM: " + e.getMessage()
            ));
        }
    }
    
    /**
     * POST /api/llm/local/test
     * Sends a simple test prompt to the local LLM to verify it's working.
     * Returns the response along with timing information.
     */
    public void testLocalLlm(Context ctx) {
        String endpoint = secrets.getLocalLlmEndpoint();
        
        try {
            LlmProvider localProvider = registry.createProvider("local", null, null);
            
            if (!localProvider.testConnection()) {
                ctx.status(503).json(Map.of(
                    "success", false,
                    "message", "LM Studio server is not running at " + endpoint
                ));
                localProvider.close();
                return;
            }
            
            // Send a simple test prompt
            long startTime = System.currentTimeMillis();
            
            com.mindquest.llm.Prompt testPrompt = new com.mindquest.llm.Prompt.Builder()
                .id("local-test-" + System.currentTimeMillis())
                .instruction("Say 'Hello from LM Studio!' in exactly one sentence.")
                .maxTokens(50)
                .temperature(0.7)
                .build();
            
            com.mindquest.llm.CompletionResult result = localProvider.complete(testPrompt);
            
            long elapsed = System.currentTimeMillis() - startTime;
            
            localProvider.close();
            
            ctx.json(Map.of(
                "success", true,
                "response", result.getText(),
                "elapsedMs", elapsed,
                "metadata", result.getMetadata(),
                "message", "Local LLM is working correctly!"
            ));
            
        } catch (Exception e) {
            ctx.status(500).json(Map.of(
                "success", false,
                "message", "Test failed: " + e.getMessage(),
                "endpoint", endpoint
            ));
        }
    }
    
    /**
     * POST /api/llm/local/generate
     * Generate questions using the local LLM (LM Studio).
     * Uses the same request format as the Gemini endpoint for consistency.
     */
    public void generateQuestions(Context ctx) {
        GeminiGenerateRequest req;
        try {
            req = ctx.bodyAsClass(GeminiGenerateRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "Invalid request format", "message", e.getMessage()));
            return;
        }
        
        // Validate inputs
        if (req.topic == null || req.topic.trim().isEmpty()) {
            ctx.status(400).json(Map.of("error", "Topic is required"));
            return;
        }
        
        if (req.difficulty == null || req.difficulty.trim().isEmpty()) {
            ctx.status(400).json(Map.of("error", "Difficulty is required"));
            return;
        }
        
        // Check if local LLM is available first
        try {
            LlmProvider localProvider = registry.createProvider("local", null, null);
            if (!localProvider.testConnection()) {
                localProvider.close();
                ctx.status(503).json(Map.of(
                    "error", "LM Studio not running",
                    "message", "Start LM Studio and enable the local server (Developer → Start Server)",
                    "endpoint", secrets.getLocalLlmEndpoint()
                ));
                return;
            }
            localProvider.close();
        } catch (Exception e) {
            ctx.status(503).json(Map.of(
                "error", "LM Studio not available",
                "message", e.getMessage()
            ));
            return;
        }
        
        // Enforce topic length limit (max 100 chars)
        String topic = req.topic.trim();
        if (topic.length() > 100) {
            topic = topic.substring(0, 100);
        }
        
        // Sanitize topic
        topic = topic.replaceAll("[^a-zA-Z0-9\\s\\-]", "");
        
        // Normalize difficulty
        String difficulty = TopicNormalizer.normalizeDifficulty(req.difficulty);
        
        // Clamp question count (5-10)
        int count = req.count;
        if (count < 5) count = 5;
        if (count > 10) count = 10;
        
        System.out.println("[LocalLLM] Generating " + count + " questions for topic '" + topic + "' at " + difficulty + " difficulty");
        
        try {
            // Build config for LOCAL_LLM source type
            SourceConfig config = new SourceConfig.Builder()
                .type(SourceConfig.SourceType.LOCAL_LLM)
                .topic(topic)
                .difficulty(difficulty)
                .addExtraParam("questionCount", String.valueOf(count))
                .build();
            
            // Generate questions
            long startTime = System.currentTimeMillis();
            List<Question> questions = QuestionBankFactory.getQuestions(config);
            long elapsed = System.currentTimeMillis() - startTime;
            
            if (questions == null || questions.isEmpty()) {
                ctx.status(500).json(Map.of(
                    "error", "No questions generated",
                    "message", "Local LLM returned no valid questions. Try a different topic or check the model."
                ));
                return;
            }
            
            // Convert questions to JSON-friendly format
            List<Map<String, Object>> questionList = new ArrayList<>();
            for (Question q : questions) {
                Map<String, Object> qMap = new HashMap<>();
                qMap.put("id", q.getId());
                qMap.put("questionText", q.getQuestionText());
                qMap.put("choices", q.getChoices());
                qMap.put("correctIndex", q.getCorrectIndex());
                qMap.put("difficulty", q.getDifficulty());
                qMap.put("topic", q.getTopic());
                questionList.add(qMap);
            }
            
            System.out.println("[LocalLLM] Generated " + questions.size() + " questions in " + elapsed + "ms");
            
            ctx.json(Map.of(
                "success", true,
                "topic", topic,
                "difficulty", difficulty,
                "count", questions.size(),
                "generationTimeMs", elapsed,
                "provider", "local",
                "questions", questionList
            ));
            
        } catch (Exception e) {
            System.err.println("[LocalLLM] Generation failed: " + e.getMessage());
            e.printStackTrace();
            ctx.status(500).json(Map.of(
                "error", "Generation failed",
                "message", e.getMessage(),
                "provider", "local"
            ));
        }
    }
}
