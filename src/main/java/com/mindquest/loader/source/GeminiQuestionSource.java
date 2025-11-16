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
import com.mindquest.llm.util.SecretResolver;
import com.mindquest.model.question.EasyQuestion;
import com.mindquest.model.question.HardQuestion;
import com.mindquest.model.question.MediumQuestion;
import com.mindquest.model.question.Question;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * QuestionSource implementation that generates questions via Gemini API.
 * Leverages existing LLM infrastructure (ProviderRegistry, PromptTemplates, etc.)
 * to dynamically create questions on demand.
 */
public class GeminiQuestionSource implements QuestionSource {
    
    private static final int DEFAULT_QUESTION_COUNT = 5;
    private static final Gson gson = new Gson();
    private static int questionCounter = 1;
    
    @Override
    public List<Question> loadQuestions(SourceConfig config) throws IOException {
        String topic = config.getTopic();
        String difficulty = config.getDifficulty();
        
        // Get API key from config or environment
        String apiKey = getApiKey(config);
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("your_")) {
            throw new IOException("No valid Gemini API key found. Please set GEMINI_API_KEY in .env file");
        }
        
        // Determine question count from config or use default
        int questionCount = getQuestionCount(config);
        
        try {
            return generateQuestions(apiKey, topic, difficulty, questionCount);
        } catch (LlmException e) {
            throw new IOException("Failed to generate questions from Gemini: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getSourceName() {
        return "Gemini AI Generated";
    }
    
    /**
     * Generates questions using Gemini API.
     */
    private List<Question> generateQuestions(String apiKey, String topic, String difficulty, int count) throws LlmException, IOException {
        // Create Gemini provider
        try (LlmProvider provider = new ProviderRegistry().createProvider("gemini", apiKey, null)) {
            
            // Build prompt using existing template
            String promptText = PromptTemplates.generateQuestionsPrompt(topic, difficulty, count);
            
            Prompt prompt = new Prompt.Builder()
                .id("game-" + topic.toLowerCase().replace(" ", "-"))
                .instruction(promptText)
                .maxTokens(2000)
                .temperature(0.7)
                .build();
            
            // Call Gemini
            CompletionResult result = provider.complete(prompt);
            
            // Extract and sanitize JSON response
            String rawResponse = result.getText();
            String cleanJson = PromptTemplates.extractJson(rawResponse);
            String sanitizedJson = PromptTemplates.sanitizeQuestionJson(cleanJson);
            
            // Parse JSON to Question objects
            return parseJsonToQuestions(sanitizedJson, difficulty);
            
        } catch (Exception e) {
            if (e instanceof LlmException) {
                throw (LlmException) e;
            }
            throw new IOException("Error during Gemini API call: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses Gemini's JSON response into Question objects.
     * Reuses JSON parsing logic similar to JsonQuestionLoader.
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
            throw new IOException("Failed to parse Gemini JSON response: " + e.getMessage(), e);
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
     * Retrieves API key from config or environment.
     */
    private String getApiKey(SourceConfig config) {
        // First check config extra params
        if (config.getExtraParam("apiKey") != null) {
            return config.getExtraParam("apiKey");
        }
        
        // Fall back to environment variable
        SecretResolver secrets = new SecretResolver();
        return secrets.getGeminiApiKey();
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
        return "GEMINI_" + difficulty.toUpperCase() + "_" + String.format("%03d", questionCounter++);
    }
}
