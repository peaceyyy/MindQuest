package com.mindquest.loader;

import com.mindquest.model.EasyQuestion;
import com.mindquest.model.HardQuestion;
import com.mindquest.model.MediumQuestion;
import com.mindquest.model.Question;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Loads questions from JSON files in the resources/questions directory.
 * Designed for "plug and play" - easily swap between hardcoded and file-based questions.
 */
public class JsonQuestionLoader {

    private static final String BASE_PATH = "resources/questions/";

    /**
     * Loads questions from a JSON file for a specific topic and difficulty.
     * 
     * @param topic The topic folder (e.g., "cs", "ai", "philosophy")
     * @param difficulty The difficulty level ("easy", "medium", "hard")
     * @return List of Question objects loaded from the JSON file
     * @throws IOException if the file cannot be read
     */
    public static List<Question> loadQuestions(String topic, String difficulty) throws IOException {
        String filePath = BASE_PATH + topic + "/" + difficulty + ".json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
        
        return parseJson(jsonContent, difficulty);
    }

    /**
     * Manual JSON parsing to avoid external dependencies.
     * Parses the simple structure: {"topic": "...", "difficulty": "...", "questions": [...]}
     */
    private static List<Question> parseJson(String jsonContent, String difficulty) {
        List<Question> questions = new ArrayList<>();
        
        // Extract the questions array from the JSON
        int questionsStart = jsonContent.indexOf("\"questions\":");
        int arrayStart = jsonContent.indexOf("[", questionsStart);
        int arrayEnd = jsonContent.lastIndexOf("]");
        
        String questionsArray = jsonContent.substring(arrayStart + 1, arrayEnd).trim();
        
        // Split into individual question objects
        List<String> questionObjects = splitQuestionObjects(questionsArray);
        
        for (String questionJson : questionObjects) {
            Question q = parseQuestion(questionJson, difficulty);
            if (q != null) {
                questions.add(q);
            }
        }
        
        return questions;
    }

    /**
     * Splits the questions array into individual question objects.
     * Handles nested braces properly.
     */
    private static List<String> splitQuestionObjects(String questionsArray) {
        List<String> objects = new ArrayList<>();
        int braceCount = 0;
        int start = 0;
        
        for (int i = 0; i < questionsArray.length(); i++) {
            char c = questionsArray.charAt(i);
            
            if (c == '{') {
                if (braceCount == 0) {
                    start = i;
                }
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    objects.add(questionsArray.substring(start, i + 1));
                }
            }
        }
        
        return objects;
    }

    /**
     * Parses a single question JSON object.
     */
    private static Question parseQuestion(String questionJson, String difficulty) {
        try {
            String questionText = extractStringValue(questionJson, "questionText");
            String[] choices = extractArrayValues(questionJson, "choices");
            int correctIndex = extractIntValue(questionJson, "correctIndex");
            
            // Generate a unique ID (simple sequential approach)
            String id = generateQuestionId(difficulty);
            
            // Create appropriate Question subclass based on difficulty
            List<String> choicesList = Arrays.asList(choices);
            
            switch (difficulty.toLowerCase()) {
                case "easy":
                    return new EasyQuestion(id, questionText, choicesList, correctIndex, difficulty);
                case "medium":
                    return new MediumQuestion(id, questionText, choicesList, correctIndex, difficulty);
                case "hard":
                    return new HardQuestion(id, questionText, choicesList, correctIndex, difficulty);
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Error parsing question: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts a string value from JSON.
     */
    private static String extractStringValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return "";
        
        start = json.indexOf("\"", start + searchKey.length()) + 1;
        int end = json.indexOf("\"", start);
        
        return json.substring(start, end);
    }

    /**
     * Extracts an array of strings from JSON.
     */
    private static String[] extractArrayValues(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return new String[0];
        
        int arrayStart = json.indexOf("[", start) + 1;
        int arrayEnd = json.indexOf("]", arrayStart);
        
        String arrayContent = json.substring(arrayStart, arrayEnd);
        
        // Split by comma and clean up quotes
        String[] rawValues = arrayContent.split("\",\\s*\"");
        String[] cleanValues = new String[rawValues.length];
        
        for (int i = 0; i < rawValues.length; i++) {
            cleanValues[i] = rawValues[i].replace("\"", "").trim();
        }
        
        return cleanValues;
    }

    /**
     * Extracts an integer value from JSON.
     */
    private static int extractIntValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return 0;
        
        start += searchKey.length();
        int end = start;
        
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        
        return Integer.parseInt(json.substring(start, end).trim());
    }

    // Simple counter for generating unique IDs
    private static int questionCounter = 1;
    
    private static String generateQuestionId(String difficulty) {
        return "JSON_" + difficulty.toUpperCase() + "_" + String.format("%03d", questionCounter++);
    }
}
