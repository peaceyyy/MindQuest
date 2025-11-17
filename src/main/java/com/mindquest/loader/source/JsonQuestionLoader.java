package com.mindquest.loader.source;

import com.mindquest.loader.QuestionSource;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.model.question.EasyQuestion;
import com.mindquest.model.question.HardQuestion;
import com.mindquest.model.question.MediumQuestion;
import com.mindquest.model.question.Question;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Loads questions from JSON files in the resources/questions directory.
 * Designed for "plug and play" - easily swap between hardcoded and file-based questions.
 * Implements QuestionSource for unified loading interface.
 */
public class JsonQuestionLoader implements QuestionSource {

    private static final String BASE_PATH = "src/questions/built-in/";

    /**
     * Implements QuestionSource interface.
     * Loads questions from JSON files based on the provided configuration.
     */
    @Override
    public List<Question> loadQuestions(SourceConfig config) throws IOException {
        String topic = config.getTopic();
        String difficulty = config.getDifficulty();
        
        String topicFolder = getTopicFolder(topic);
        
        return loadQuestions(topicFolder, difficulty.toLowerCase());
    }
    
    /**
     * Returns the source name for logging and user feedback.
     */
    @Override
    public String getSourceName() {
        return "Built-in JSON Files";
    }

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
        
        int questionsStart = jsonContent.indexOf("\"questions\":");
        int arrayStart = jsonContent.indexOf("[", questionsStart);
        int arrayEnd = jsonContent.lastIndexOf("]");
        
        String questionsArray = jsonContent.substring(arrayStart + 1, arrayEnd).trim();
        
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
            
            String id = generateQuestionId(difficulty);
            
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

    private static int questionCounter = 1;
    
    private static String generateQuestionId(String difficulty) {
        return "JSON_" + difficulty.toUpperCase() + "_" + String.format("%03d", questionCounter++);
    }
    
    /**
     * Maps topic string to folder name in resources/questions/.
     * Handles both full names and abbreviated folder names.
     */
    private static String getTopicFolder(String topic) {
        if (topic.equals("cs") || topic.equals("ai") || topic.equals("philosophy")) {
            return topic;
        }
        
        switch (topic) {
            case "Computer Science":
                return "cs";
            case "Artificial Intelligence":
                return "ai";
            case "Philosophy":
                return "philosophy";
            default:
                return topic.toLowerCase();
        }
    }
}
