package com.mindquest.loader.factory;

import com.mindquest.loader.QuestionSource;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.loader.source.*;
import com.mindquest.model.question.Question;
import com.mindquest.model.QuestionBank;

import java.util.List;

/**
 * Factory for loading questions from various sources.
 * Supports: Hardcoded QuestionBank, JSON files, CSV files, Excel files, and Gemini API.
 * Uses QuestionSource interface for unified loading.
 */
public class QuestionBankFactory {

    // Default mode - can be overridden during run
    private static SourceConfig.SourceType DEFAULT_MODE = SourceConfig.SourceType.BUILTIN_HARDCODED;
    

    public static List<Question> getQuestions(SourceConfig config) {
        try {
            QuestionSource loader = createLoader(config.getType());
            return loader.loadQuestions(config);
        } catch (Exception e) {
            System.err.println("Error loading questions from " + config.getType() + ": " + e.getMessage());
            System.out.println("Falling back to hardcoded questions.");
            return getQuestionsFromHardcoded(config.getTopic(), config.getDifficulty());
        }
    }
    
    /**
     * Uses default mode (BUILTIN_HARDCODED).
     */
    public static List<Question> getQuestions(String topic, String difficulty) {
        SourceConfig config = new SourceConfig.Builder()
            .type(DEFAULT_MODE)
            .topic(topic)
            .difficulty(difficulty)
            .build();
        return getQuestions(config);
    }
    
    /**
     * Creates the appropriate QuestionSource implementation based on source type.
     */
    private static QuestionSource createLoader(SourceConfig.SourceType type) {
        switch (type) {
            case BUILTIN_HARDCODED:
                return new HardcodedQuestionSource();
            
            case BUILTIN_JSON:
                return new JsonQuestionLoader();
            
            case CUSTOM_EXCEL:
                return new ExcelQuestionLoader();
            
            case CUSTOM_CSV:
                return new CsvQuestionLoader();
            
            case GEMINI_API:
                return new GeminiQuestionSource();
            
            default:
                return new HardcodedQuestionSource();
        }
    }

    /**
     * Loads questions from the original hardcoded QuestionBank.
     * Used for fallback when other loaders fail.
     */
    private static List<Question> getQuestionsFromHardcoded(String topic, String difficulty) {
        QuestionBank bank = new QuestionBank();
        return bank.getQuestionsByTopicAndDifficulty(topic, difficulty);
    }

    /**
     * Gets the current default mode for backward compatibility.
     */
    public static String getCurrentMode() {
        return "Question Loading Mode: " + DEFAULT_MODE.toString();
    }
    
    /**
     * Sets the default mode for backward compatibility.
     */
    public static void setDefaultMode(SourceConfig.SourceType mode) {
        DEFAULT_MODE = mode;
    }
}
