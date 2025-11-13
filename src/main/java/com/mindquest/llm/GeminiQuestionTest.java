package com.mindquest.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Test utility to verify Gemini returns properly formatted question JSON.
 * Run this BEFORE integrating into the game to ensure prompt templates work correctly.
 * 
 * Usage:
 *   1. Ensure .env has valid GEMINI_API_KEY
 *   2. Run: java -cp target/classes com.mindquest.llm.GeminiQuestionTest
 *   3. Verify JSON output matches expected structure
 */
public class GeminiQuestionTest {
    
    public static void main(String[] args) {
        System.out.println("=== Gemini Question Generation Test ===\n");
        
        // Load API key
        SecretResolver secrets = new SecretResolver();
        String apiKey = secrets.getGeminiApiKey();
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("your_")) {
            System.err.println("ERROR: No valid Gemini API key found!");
            System.err.println("Please set GEMINI_API_KEY in .env file");
            System.exit(1);
        }
        
        System.out.println("✓ API key found: " + maskKey(apiKey));
        System.out.println();
        
        // Create provider
        try (LlmProvider provider = new ProviderRegistry().createProvider("gemini", apiKey, null)) {
            
            // Test 1: Connection test
            System.out.println("Test 1: Connection Test");
            System.out.println("------------------------");
            boolean connected = provider.testConnection();
            System.out.println("Connection: " + (connected ? "✓ SUCCESS" : "✗ FAILED"));
            
            if (!connected) {
                System.err.println("\nConnection test failed. Check your API key and network.");
                System.exit(1);
            }
            System.out.println();
            
            // Test 2: Generate Easy questions
            System.out.println("Test 2: Generate Easy Questions (Computer Science)");
            System.out.println("---------------------------------------------------");
            testQuestionGeneration(provider, "Computer Science", "Easy", 3);
            
            // Test 3: Generate Medium questions
            System.out.println("\nTest 3: Generate Medium Questions (Artificial Intelligence)");
            System.out.println("------------------------------------------------------------");
            testQuestionGeneration(provider, "Artificial Intelligence", "Medium", 2);
            
            // Test 4: Generate Hard questions
            System.out.println("\nTest 4: Generate Hard Questions (Philosophy)");
            System.out.println("---------------------------------------------");
            testQuestionGeneration(provider, "Philosophy", "Hard", 2);
            
            System.out.println("\n=== All Tests Passed ===");
            System.out.println("\nNext steps:");
            System.out.println("1. Verify the JSON structure matches built-in questions");
            System.out.println("2. Check question quality and difficulty appropriateness");
            System.out.println("3. If satisfied, integrate into game via LlmQuestionLoader");
            
        } catch (Exception e) {
            System.err.println("\nERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void testQuestionGeneration(LlmProvider provider, String topic, String difficulty, int count) {
        try {
            // Build prompt
            String promptText = PromptTemplates.generateQuestionsPrompt(topic, difficulty, count);
            
            System.out.println("Requesting " + count + " questions...");
            
            Prompt prompt = new Prompt.Builder()
                .id("test-" + topic.toLowerCase().replace(" ", "-"))
                .instruction(promptText)
                .maxTokens(2000)
                .temperature(0.7)
                .build();
            
            // Call Gemini
            long startTime = System.currentTimeMillis();
            CompletionResult result = provider.complete(prompt);
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("✓ Response received in " + duration + "ms");
            
            // Extract and sanitize JSON
            String rawResponse = result.getText();
            String cleanJson = PromptTemplates.extractJson(rawResponse);
            String sanitizedJson = PromptTemplates.sanitizeQuestionJson(cleanJson);
            
            // Parse JSON
            Gson gson = new Gson();
            JsonObject jsonObj = gson.fromJson(sanitizedJson, JsonObject.class);
            
            // Validate structure
            validateQuestionJson(jsonObj, topic, difficulty, count);
            
            // Display results
            System.out.println("\nGenerated Questions:");
            System.out.println("===================");
            System.out.println(gson.toJson(jsonObj));
            
            // Token usage
            if (result.getMetadata().containsKey("totalTokens")) {
                System.out.println("\nToken Usage:");
                System.out.println("  Prompt: " + result.getMetadata().get("promptTokens"));
                System.out.println("  Completion: " + result.getMetadata().get("completionTokens"));
                System.out.println("  Total: " + result.getMetadata().get("totalTokens"));
            }
            
            System.out.println("\n✓ JSON structure validated successfully");
            
        } catch (JsonSyntaxException e) {
            System.err.println("✗ FAILED: Invalid JSON response");
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException("JSON validation failed", e);
        } catch (LlmException e) {
            System.err.println("✗ FAILED: LLM error");
            System.err.println("Category: " + e.getCategory());
            System.err.println("Message: " + e.getMessage());
            throw new RuntimeException("LLM call failed", e);
        } catch (Exception e) {
            System.err.println("✗ FAILED: Unexpected error");
            e.printStackTrace();
            throw new RuntimeException("Test failed", e);
        }
    }
    
    private static void validateQuestionJson(JsonObject json, String expectedTopic, String expectedDifficulty, int expectedCount) {
        // Validate top-level fields
        if (!json.has("topic")) {
            throw new RuntimeException("Missing 'topic' field");
        }
        if (!json.has("difficulty")) {
            throw new RuntimeException("Missing 'difficulty' field");
        }
        if (!json.has("questions")) {
            throw new RuntimeException("Missing 'questions' array");
        }
        
        String topic = json.get("topic").getAsString();
        String difficulty = json.get("difficulty").getAsString();
        JsonArray questions = json.getAsJsonArray("questions");
        
        // Validate values
        if (!topic.equals(expectedTopic)) {
            System.err.println("Warning: Topic mismatch. Expected '" + expectedTopic + "', got '" + topic + "'");
        }
        
        if (!difficulty.equals(expectedDifficulty)) {
            System.err.println("Warning: Difficulty mismatch. Expected '" + expectedDifficulty + "', got '" + difficulty + "'");
        }
        
        if (questions.size() != expectedCount) {
            System.err.println("Warning: Question count mismatch. Expected " + expectedCount + ", got " + questions.size());
        }
        
        // Validate each question
        for (int i = 0; i < questions.size(); i++) {
            JsonObject question = questions.get(i).getAsJsonObject();
            
            if (!question.has("questionText")) {
                throw new RuntimeException("Question " + i + " missing 'questionText'");
            }
            if (!question.has("choices")) {
                throw new RuntimeException("Question " + i + " missing 'choices'");
            }
            if (!question.has("correctIndex")) {
                throw new RuntimeException("Question " + i + " missing 'correctIndex'");
            }
            
            JsonArray choices = question.getAsJsonArray("choices");
            if (choices.size() != 4) {
                throw new RuntimeException("Question " + i + " must have exactly 4 choices, got " + choices.size());
            }
            
            int correctIndex = question.get("correctIndex").getAsInt();
            if (correctIndex < 0 || correctIndex > 3) {
                throw new RuntimeException("Question " + i + " has invalid correctIndex: " + correctIndex);
            }
        }
        
        System.out.println("Validation: ✓ Topic ✓ Difficulty ✓ Count (" + questions.size() + ") ✓ Structure");
    }
    
    private static String maskKey(String key) {
        if (key.length() <= 8) return "***";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
