package com.mindquest.server.handler;

import com.mindquest.loader.config.SourceConfig;
import com.mindquest.loader.factory.QuestionBankFactory;
import com.mindquest.llm.util.SecretResolver;
import com.mindquest.model.question.Question;
import com.mindquest.server.dto.GeminiGenerateRequest;
import com.mindquest.server.util.NetworkUtils;
import com.mindquest.server.util.TopicNormalizer;
import io.javalin.http.Context;

import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;

/**
 * Handler for Gemini AI operations.
 * Manages status checks, network tests, and question generation.
 */
public class GeminiHandler {

    public GeminiHandler() {
        // No dependencies needed
    }

    /**
     * GET /api/gemini/status - Check if Gemini API is available.
     */
    public void getStatus(Context ctx) {
        SecretResolver secrets = new SecretResolver();
        boolean hasKey = secrets.hasSecret("GOOGLE_API_KEY") || secrets.hasSecret("GEMINI_API_KEY");
        String maskedKey = "";
        
        if (hasKey) {
            String key = secrets.getGeminiApiKey();
            if (key != null && key.length() > 8) {
                maskedKey = key.substring(0, 4) + "..." + key.substring(key.length() - 4);
            }
        }
        
        ctx.json(Map.of(
            "available", hasKey,
            "keyConfigured", hasKey,
            "maskedKey", maskedKey,
            "message", hasKey ? "Gemini API is configured" : "No API key found. Set GOOGLE_API_KEY in .env file."
        ));
    }

    /**
     * GET /api/gemini/network-test - Test network connectivity to Google APIs.
     */
    public void testNetwork(Context ctx) {
        Map<String, Object> results = new HashMap<>();
        
        // Test 1: Basic Google connectivity
        results.put("test1_google", NetworkUtils.testHttpConnection("https://www.google.com", 5000));
        
        // Test 2: Generative AI endpoint (just connectivity, not auth)
        results.put("test2_generativelanguage", NetworkUtils.testHttpConnection("https://generativelanguage.googleapis.com", 5000));
        
        // Test 3: Check SSL/TLS version
        results.put("java_version", System.getProperty("java.version"));
        results.put("java_vendor", System.getProperty("java.vendor"));
        
        // Test 4: Try to see which TLS versions are available
        try {
            SSLContext sslContext = SSLContext.getDefault();
            results.put("ssl_protocol", sslContext.getProtocol());
            results.put("ssl_providers", Arrays.toString(Security.getProviders()));
        } catch (Exception e) {
            results.put("ssl_error", e.getMessage());
        }
        
        ctx.json(results);
    }

    /**
     * POST /api/gemini/generate - Generate questions using Gemini AI.
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
        
        // Enforce topic length limit (max 100 chars to prevent prompt injection)
        String topic = req.topic.trim();
        if (topic.length() > 100) {
            topic = topic.substring(0, 100);
        }
        
        // Sanitize topic - remove special characters that could affect prompt
        topic = topic.replaceAll("[^a-zA-Z0-9\\s\\-]", "");
        
        // Normalize difficulty
        String difficulty = TopicNormalizer.normalizeDifficulty(req.difficulty);
        
        // Clamp question count (5-10)
        int count = req.count;
        if (count < 5) count = 5;
        if (count > 10) count = 10;
        
        System.out.println("[Gemini] Generating " + count + " questions for topic '" + topic + "' at " + difficulty + " difficulty");
        
        try {
            // Build config with question count as extra param
            SourceConfig config = new SourceConfig.Builder()
                .type(SourceConfig.SourceType.GEMINI_API)
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
                    "message", "Gemini returned no valid questions. Try a different topic."
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
            
            System.out.println("[Gemini] Generated " + questions.size() + " questions in " + elapsed + "ms");
            
            ctx.json(Map.of(
                "success", true,
                "topic", topic,
                "difficulty", difficulty,
                "count", questions.size(),
                "generationTimeMs", elapsed,
                "questions", questionList
            ));
            
        } catch (Exception e) {
            System.err.println("[Gemini] Generation failed: " + e.getMessage());
            e.printStackTrace();
            ctx.status(500).json(Map.of(
                "error", "Generation failed",
                "message", e.getMessage()
            ));
        }
    }
}
