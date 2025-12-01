package com.mindquest.loader.source;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mindquest.loader.QuestionSource;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.llm.CompletionResult;
import com.mindquest.llm.LlmProvider;
import com.mindquest.llm.Prompt;
import com.mindquest.llm.ProviderRegistry;
import com.mindquest.llm.exception.LlmException;
import com.mindquest.llm.prompts.PromptTemplates;
import com.mindquest.model.question.EasyQuestion;
import com.mindquest.model.question.HardQuestion;
import com.mindquest.model.question.MediumQuestion;
import com.mindquest.model.question.Question;
import com.mindquest.model.QuestionBank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * QuestionSource implementation that generates questions via Local LLM (LM Studio).
 * Similar to GeminiQuestionSource but uses the local LLM provider for offline operation.
 * 
 * This enables completely offline question generation using models like:
 * - Llama 3.2, Llama 3.1
 * - Mistral, Mixtral
 * - Phi-3, Phi-4
 * - Any other model loaded in LM Studio
 */
public class LocalLlmQuestionSource implements QuestionSource {
    
    private static final int DEFAULT_QUESTION_COUNT = 5;
    private static final Gson gson = new Gson();
    private static int questionCounter = 1;
    
    @Override
    public List<Question> loadQuestions(SourceConfig config) throws IOException {
        String topic = config.getTopic();
        String difficulty = config.getDifficulty();
        
        // Get question count from config or use default
        int questionCount = getQuestionCount(config);
        
        try {
            return generateQuestions(topic, difficulty, questionCount);
        } catch (Exception e) {
            // Fallback to cached/hardcoded questions if local LLM fails
            System.err.println("[LocalLlmQuestionSource] Local LLM generation failed: " + e.getMessage());
            System.err.println("[LocalLlmQuestionSource] Attempting fallback to cached sources...");
            
            // Try cached JSON first
            try {
                String folder = mapTopicToFolder(topic);
                List<Question> cached = JsonQuestionLoader.loadQuestions(folder, difficulty.toLowerCase());
                System.out.println("[LocalLlmQuestionSource] Using cached built-in JSON questions (fallback)");
                return cached;
            } catch (Exception ex) {
                // Final fallback: hardcoded QuestionBank
                try {
                    QuestionBank bank = new QuestionBank();
                    List<Question> hard = bank.getQuestionsByTopicAndDifficulty(topic, difficulty);
                    System.out.println("[LocalLlmQuestionSource] Using hardcoded QuestionBank (final fallback)");
                    return hard;
                } catch (Exception ex2) {
                    throw new IOException("All fallbacks failed: " + ex2.getMessage(), ex2);
                }
            }
        }
    }
    
    @Override
    public String getSourceName() {
        return "Local LLM (LM Studio)";
    }
    
    /**
     * Generates questions using the local LLM provider.
     */
    private List<Question> generateQuestions(String topic, String difficulty, int count) throws LlmException, IOException {
        // Create local LLM provider (no API key needed)
        try (LlmProvider provider = new ProviderRegistry().createProvider("local", null, null)) {
            
            // Check connection first
            if (!provider.testConnection()) {
                throw new IOException("Local LLM server is not running. Start LM Studio and enable the server.");
            }
            
            // Build prompt using existing template (same as Gemini)
            String promptText = PromptTemplates.generateQuestionsPrompt(topic, difficulty, count);
            
            // Add extra context for local models (they may need clearer instructions)
            String enhancedPrompt = 
                "You are a question generator for a quiz game. " +
                "Generate questions in valid JSON format ONLY. " +
                "Do not include any explanation or markdown, just the JSON.\n\n" + 
                promptText;
            
            Prompt prompt = new Prompt.Builder()
                .id("local-game-" + topic.toLowerCase().replace(" ", "-"))
                .instruction(enhancedPrompt)
                .maxTokens(2500) // Local models may need more tokens
                .temperature(0.7)
                .build();
            
            System.out.println("[LocalLlmQuestionSource] Generating " + count + " questions for '" + topic + "' at " + difficulty);
            
            // Call local LLM
            CompletionResult result = provider.complete(prompt);
            
            // Extract and sanitize JSON response
            String rawResponse = result.getText();
            String cleanJson = PromptTemplates.extractJson(rawResponse);
            String sanitizedJson = PromptTemplates.sanitizeQuestionJson(cleanJson);
            
            System.out.println("[LocalLlmQuestionSource] Received response, parsing JSON...");
            
            // Parse JSON to Question objects
            return parseJsonToQuestions(sanitizedJson, difficulty);
            
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Error during local LLM call: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses the LLM's JSON response into Question objects.
     * Same logic as GeminiQuestionSource.
     */
    private List<Question> parseJsonToQuestions(String jsonContent, String difficulty) throws IOException {
        List<Question> questions = new ArrayList<>();
        
        try {
            JsonObject jsonObj = gson.fromJson(jsonContent, JsonObject.class);
            
            if (!jsonObj.has("questions")) {
                throw new IOException("Invalid JSON: missing 'questions' array");
            }
            
            // Extract topic from JSON
            String topic = jsonObj.has("topic") ? jsonObj.get("topic").getAsString() : "General";
            
            JsonArray questionsArray = jsonObj.getAsJsonArray("questions");
            
            for (int i = 0; i < questionsArray.size(); i++) {
                JsonObject questionJson = questionsArray.get(i).getAsJsonObject();
                Question question = parseQuestion(questionJson, difficulty, topic);
                if (question != null) {
                    questions.add(question);
                }
            }
            
        } catch (Exception e) {
            throw new IOException("Failed to parse local LLM JSON response: " + e.getMessage(), e);
        }
        
        return questions;
    }
    
    /**
     * Parses a single question from JSON object.
     */
    private Question parseQuestion(JsonObject questionJson, String difficulty, String topic) {
        try {
            String questionText = questionJson.get("questionText").getAsString();
            JsonArray choicesArray = questionJson.getAsJsonArray("choices");
            int correctIndex = questionJson.get("correctIndex").getAsInt();
            
            // Convert choices to List
            List<String> choicesList = new ArrayList<>();
            for (int i = 0; i < choicesArray.size(); i++) {
                choicesList.add(choicesArray.get(i).getAsString());
            }
            
            // Validate
            if (choicesList.size() != 4) {
                System.err.println("Warning: Question has " + choicesList.size() + " choices, expected 4. Skipping.");
                return null;
            }
            
            if (correctIndex < 0 || correctIndex > 3) {
                System.err.println("Warning: Invalid correctIndex " + correctIndex + ". Skipping.");
                return null;
            }
            
            // Create appropriate Question subclass based on difficulty
            String questionId = generateQuestionId(difficulty);
            
            switch (difficulty.toLowerCase()) {
                case "easy":
                    return new EasyQuestion(questionId, questionText, choicesList, correctIndex, topic);
                case "medium":
                    return new MediumQuestion(questionId, questionText, choicesList, correctIndex, topic);
                case "hard":
                    return new HardQuestion(questionId, questionText, choicesList, correctIndex, topic);
                default:
                    System.err.println("Warning: Unknown difficulty '" + difficulty + "'. Defaulting to Medium.");
                    return new MediumQuestion(questionId, questionText, choicesList, correctIndex, topic);
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing question: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Map UI topic names to built-in folder names.
     */
    private String mapTopicToFolder(String topic) {
        if (topic == null) return "";
        if (topic.equalsIgnoreCase("Computer Science") || topic.equalsIgnoreCase("cs")) return "cs";
        if (topic.equalsIgnoreCase("Artificial Intelligence") || topic.equalsIgnoreCase("ai")) return "ai";
        if (topic.equalsIgnoreCase("Philosophy")) return "philosophy";
        return topic.toLowerCase();
    }
    
    /**
     * Gets question count from config or returns default.
     */
    private int getQuestionCount(SourceConfig config) {
        String countStr = config.getExtraParam("questionCount");
        if (countStr != null) {
            try {
                return Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid questionCount '" + countStr + "'. Using default.");
            }
        }
        return DEFAULT_QUESTION_COUNT;
    }
    
    /**
     * Generates unique question ID.
     */
    private static String generateQuestionId(String difficulty) {
        return "LOCAL_" + difficulty.toUpperCase() + "_" + String.format("%03d", questionCounter++);
    }
}
