package com.mindquest.util;

import com.mindquest.loader.factory.QuestionBankFactory;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.model.question.Question;

import java.util.List;

/**
 * Diagnostic utility to test all question loaders.
 * Run this to verify JSON, CSV, Excel, and Gemini loaders work correctly.
 */
public class QuestionLoaderDiagnostic {

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("    QUESTION LOADER DIAGNOSTIC TOOL");
        System.out.println("===========================================\n");

        // Test JSON loader
        testSource("JSON", SourceConfig.SourceType.BUILTIN_JSON, "philosophy", "Medium");
        testSource("JSON", SourceConfig.SourceType.BUILTIN_JSON, "cs", "easy");
        testSource("JSON", SourceConfig.SourceType.BUILTIN_JSON, "ai", "hard");
        
        // Test CSV loader
        testSource("CSV", SourceConfig.SourceType.CUSTOM_CSV, "philosophy", "Medium");
        testSource("CSV", SourceConfig.SourceType.CUSTOM_CSV, "cs", "Easy");
        
        // Test Excel loader
        testSource("Excel", SourceConfig.SourceType.CUSTOM_EXCEL, "philosophy", "Medium");
        testSource("Excel", SourceConfig.SourceType.CUSTOM_EXCEL, "ai", "Hard");
        
        System.out.println("\n===========================================");
        System.out.println("    DIAGNOSTIC COMPLETE");
        System.out.println("===========================================");
    }

    private static void testSource(String loaderName, SourceConfig.SourceType type, 
                                    String topic, String difficulty) {
        System.out.println("─────────────────────────────────────────");
        System.out.println("Testing: " + loaderName + " | " + topic + " | " + difficulty);
        System.out.println("─────────────────────────────────────────");

        try {
            SourceConfig config = new SourceConfig.Builder()
                .type(type)
                .topic(topic)
                .difficulty(difficulty)
                .build();

            List<Question> questions = QuestionBankFactory.getQuestions(config);

            if (questions == null || questions.isEmpty()) {
                System.out.println("❌ FAILED: No questions loaded");
            } else {
                System.out.println("✅ SUCCESS: Loaded " + questions.size() + " questions");
                
                // Show first question details
                Question first = questions.get(0);
                System.out.println("   Sample ID: " + first.getId());
                System.out.println("   Sample Q: " + 
                    first.getQuestionText().substring(0, Math.min(60, first.getQuestionText().length())) + 
                    (first.getQuestionText().length() > 60 ? "..." : ""));
                System.out.println("   Choices: " + first.getChoices().size());
                System.out.println("   Correct: " + first.getCorrectIndex());
            }
        } catch (Exception e) {
            System.out.println("❌ EXCEPTION: " + e.getClass().getSimpleName());
            System.out.println("   Message: " + e.getMessage());
            
            // Show first 3 stack trace lines for context
            StackTraceElement[] trace = e.getStackTrace();
            for (int i = 0; i < Math.min(3, trace.length); i++) {
                System.out.println("   at " + trace[i]);
            }
        }

        System.out.println();
    }
}
