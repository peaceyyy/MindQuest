package com.mindquest.llm.prompts;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Prompt templates for generating questions in the format expected 
 */
public class PromptTemplates {
    
    /**
     * Creates a prompt that instructs the LLM to generate questions in strict JSON format.
     * The JSON structure matches the built-in question files exactly.
     */
    public static String generateQuestionsPrompt(String topic, String difficulty, int count) {
        return String.format(
            "Generate %d multiple-choice questions about %s at %s difficulty level.\n\n" +
            "CRITICAL CONSTRAINT: Every question must have EXACTLY 4 choices. Not 3, not 5, exactly 4.\n\n" +
            "You MUST respond with ONLY valid JSON matching this EXACT structure:\n\n" +
            "{\n" +
            "  \"topic\": \"%s\",\n" +
            "  \"difficulty\": \"%s\",\n" +
            "  \"questions\": [\n" +
            "    {\n" +
            "      \"questionText\": \"Example question?\",\n" +
            "      \"choices\": [\n" +
            "        \"First choice\",\n" +
            "        \"Second choice\",\n" +
            "        \"Third choice\",\n" +
            "        \"Fourth choice\"\n" +
            "      ],\n" +
            "      \"correctIndex\": 0\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            "MANDATORY RULES - FOLLOW EXACTLY:\n" +
            "1. EXACTLY 4 choices per question - count them: [0, 1, 2, 3]\n" +
            "2. correctIndex must be 0, 1, 2, or 3 (matching one of the 4 choices)\n" +
            "3. NO markdown formatting - pure JSON only\n" +
            "4. NO text outside the JSON structure\n" +
            "5. Each \"choices\" array MUST contain exactly 4 strings\n\n" +
            "Difficulty guidelines:\n" +
            "- Easy: Basic definitions, simple facts\n" +
            "- Medium: Applied concepts, moderate reasoning\n" +
            "- Hard: Deep analysis, complex scenarios\n\n" +
            "REMEMBER: 4 choices per question. Always 4. Never more, never less.\n\n" +
            "Generate valid JSON now:",
            count, topic, difficulty, topic, difficulty
        );
    }
    
    /**
     * Creates a simpler test prompt for validating LLM connectivity.
     */
    public static String testPrompt() {
        return "Respond with exactly this JSON: {\"status\":\"ok\",\"message\":\"Connection successful\"}";
    }
    
    /**
     * Extracts clean JSON from LLM response
     */
    public static String extractJson(String llmResponse) {
        if (llmResponse == null) return null;
        
        String cleaned = llmResponse.trim();
        
        // Remove markdown code blocks if present
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        return cleaned.trim();
    }
    
    /**
     * Post-processes LLM-generated question JSON to fix errors.
     * 
     * 
     * @param questionJson The raw JSON from LLM
     * @return Sanitized JSON that matches expected format
     */
    public static String sanitizeQuestionJson(String questionJson) {
        if (questionJson == null) return null;
        
        try {
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(questionJson, JsonObject.class);
            
            if (!root.has("questions")) return questionJson;
            
            JsonArray questions = root.getAsJsonArray("questions");
            boolean modified = false;
            
            for (int i = 0; i < questions.size(); i++) {
                JsonObject q = questions.get(i).getAsJsonObject();
                
                if (q.has("choices")) {
                    JsonArray choices = q.getAsJsonArray("choices");
                    
                    // Fix: Too many choices (truncate to 4)
                    if (choices.size() > 4) {
                        JsonArray fixed = new JsonArray();
                        for (int j = 0; j < 4; j++) {
                            fixed.add(choices.get(j));
                        }
                        q.add("choices", fixed);
                        modified = true;
                        
                     
                        if (q.has("correctIndex")) {
                            int idx = q.get("correctIndex").getAsInt();
                            if (idx >= 4) {
                                q.addProperty("correctIndex", 0);
                            }
                        }
                    }
                  
                    else if (choices.size() < 4) {
                        while (choices.size() < 4) {
                            choices.add("Additional option");
                        }
                        modified = true;
                    }
                }
                
             
                if (q.has("correctIndex")) {
                    int idx = q.get("correctIndex").getAsInt();
                    if (idx < 0 || idx > 3) {
                        q.addProperty("correctIndex", Math.max(0, Math.min(3, idx)));
                        modified = true;
                    }
                }
            }
            
            if (modified) {
                System.err.println("[PromptTemplates] Auto-fixed malformed LLM response");
            }
            
            return gson.toJson(root);
            
        } catch (Exception e) {
            
            System.err.println("[PromptTemplates] Sanitization failed: " + e.getMessage());
            return questionJson;
        }
    }
}
