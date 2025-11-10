package com.mindquest.loader;

import com.mindquest.model.Question;
import com.mindquest.model.QuestionBank;

import java.io.IOException;
import java.util.List;

/*
 * Toggle between:
 * - Hardcoded questions (QuestionBank.java)
 * - JSON file-based questions (resources/questions/)
 * - Future: API-based questions (Gemini, etc.)
 */

public class QuestionBankFactory {

    public enum LoaderMode {
        HARDCODED,  // Use QuestionBank.java
        JSON,       // Use JSON files from resources/
        API         // Future: Use external API
    }

    // Global toggle - change this to switch between loading strategies
    private static final LoaderMode MODE = LoaderMode.HARDCODED;
    public static List<Question> getQuestions(String topic, String difficulty) {
        switch (MODE) {
            case HARDCODED:
                return getQuestionsFromHardcoded(topic, difficulty);
            
            case JSON:
                return getQuestionsFromJson(topic, difficulty);
            
            case API:
                // Future implementation
                System.out.println("API mode not yet implemented. Falling back to hardcoded.");
                return getQuestionsFromHardcoded(topic, difficulty);
            
            default:
                return getQuestionsFromHardcoded(topic, difficulty);
        }
    }

    /**
     * Loads questions from the original hardcoded QuestionBank.
     */
    private static List<Question> getQuestionsFromHardcoded(String topic, String difficulty) {
        QuestionBank bank = new QuestionBank();
        return bank.getQuestionsByTopicAndDifficulty(topic, difficulty);
    }

    /**
     * Loads questions from JSON files in resources/questions/.
     */
    private static List<Question> getQuestionsFromJson(String topic, String difficulty) {
        try {
            String topicFolder = getTopicFolder(topic);
            String difficultyFile = difficulty.toLowerCase();
            
            return JsonQuestionLoader.loadQuestions(topicFolder, difficultyFile);
            
        } catch (IOException e) {
            System.err.println("Error loading JSON questions: " + e.getMessage());
            System.out.println("Falling back to hardcoded questions.");
            return getQuestionsFromHardcoded(topic, difficulty);
        }
    }

    /**
     * Maps Topic string to folder name in resources/questions/.
     */
    private static String getTopicFolder(String topic) {
        switch (topic) {
            case "Computer Science":
                return "cs";
            case "Artificial Intelligence":
                return "ai";
            case "Philosophy":
                return "philosophy";
            default:
                return "cs";
        }
    }


    public static String getCurrentMode() {
        return "Question Loading Mode: " + MODE.toString();
    }
}
