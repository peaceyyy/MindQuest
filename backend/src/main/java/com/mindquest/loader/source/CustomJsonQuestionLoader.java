package com.mindquest.loader.source;

import com.mindquest.loader.QuestionSource;
import com.mindquest.loader.TopicScanner;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.model.question.EasyQuestion;
import com.mindquest.model.question.HardQuestion;
import com.mindquest.model.question.MediumQuestion;
import com.mindquest.model.question.Question;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Loads questions from custom JSON files uploaded by users.
 * Expected structure: {"topic": "...", "difficulty": "...", "questions": [...]}
 */
public class CustomJsonQuestionLoader implements QuestionSource {

    @Override
    public List<Question> loadQuestions(SourceConfig config) throws IOException {
        String topic = config.getTopic();
        String difficulty = config.getDifficulty();
        
        String filePath = TopicScanner.getTopicFilePath(topic, SourceConfig.SourceType.CUSTOM_JSON);
        
        System.out.println("[Custom JSON Loader] Loading from: " + filePath);
        
        return loadQuestionsFromFile(filePath, difficulty);
    }

    @Override
    public String getSourceName() {
        return "Custom JSON File";
    }

    private List<Question> loadQuestionsFromFile(String filePath, String difficulty) throws IOException {
        try (InputStream is = getInputStream(filePath)) {
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            String jsonContent = new String(bytes, StandardCharsets.UTF_8);
            return parseJson(jsonContent, difficulty);
        }
    }

    private InputStream getInputStream(String filePath) throws IOException {
        // Try file system first for custom uploads
        return new FileInputStream(filePath);
    }

    /**
     * Manual JSON parsing (reused from JsonQuestionLoader logic).
     */
    private static List<Question> parseJson(String jsonContent, String difficulty) {
        List<Question> questions = new ArrayList<>();
        
        int questionsStart = jsonContent.indexOf("\"questions\":");
        if (questionsStart == -1) return questions;

        int arrayStart = jsonContent.indexOf("[", questionsStart);
        int arrayEnd = jsonContent.lastIndexOf("]");
        
        if (arrayStart == -1 || arrayEnd == -1) return questions;

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

    private static Question parseQuestion(String questionJson, String difficulty) {
        try {
            String questionText = extractStringValue(questionJson, "questionText");
            String[] choices = extractArrayValues(questionJson, "choices");
            int correctIndex = extractIntValue(questionJson, "correctIndex");
            
            // If difficulty is not provided in the question, use the requested difficulty
            // But typically custom files might have mixed difficulties or we filter.
            // For now, we assume the file matches the requested difficulty or we assign it.
            // Actually, the existing JsonQuestionLoader assigns the requested difficulty.
            
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
                    return new MediumQuestion(id, questionText, choicesList, correctIndex, difficulty);
            }
        } catch (Exception e) {
            System.err.println("Error parsing question: " + e.getMessage());
            return null;
        }
    }

    private static String extractStringValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return "";
        
        start = json.indexOf("\"", start + searchKey.length()) + 1;
        int end = json.indexOf("\"", start);
        
        return json.substring(start, end);
    }

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

    private static int extractIntValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return 0;
        
        start += searchKey.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        
        String numberStr = json.substring(start, end).trim();
        return numberStr.isEmpty() ? 0 : Integer.parseInt(numberStr);
    }

    private static int questionCounter = 1;
    
    private static String generateQuestionId(String difficulty) {
        return "C_JSON_" + difficulty.toUpperCase() + "_" + String.format("%03d", questionCounter++);
    }
}
