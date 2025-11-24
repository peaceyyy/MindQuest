package com.mindquest.loader;

import com.mindquest.loader.QuestionSource;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.loader.source.JsonQuestionLoader;
import com.mindquest.loader.source.CsvQuestionLoader;
import com.mindquest.loader.source.ExcelQuestionLoader;
import com.mindquest.model.question.Question;
import java.util.List;

/**
 * Test harness to verify all question loaders are working correctly.
 * Place in tests folder and run to diagnose loading issues.
 */
public class LoaderDiagnosticTest {
    
    public static void main(String[] args) {
        System.out.println("=== Question Loader Diagnostic Test ===\n");
        
        testJsonLoader();
        testCsvLoader();
        testExcelLoader();
        
        System.out.println("\n=== Test Complete ===");
    }
    
    private static void testJsonLoader() {
        System.out.println("--- Testing JSON Loader ---");
        try {
            QuestionSource jsonLoader = new JsonQuestionLoader();
            
            SourceConfig config = new SourceConfig.Builder()
                .type(SourceConfig.SourceType.BUILTIN_JSON)
                .topic("Artificial Intelligence")
                .difficulty("Easy")
                .build();
            
            List<Question> questions = jsonLoader.loadQuestions(config);
            
            System.out.println("Source: " + jsonLoader.getSourceName());
            System.out.println("Loaded: " + questions.size() + " questions");
            
            if (!questions.isEmpty()) {
                System.out.println("\nFirst 3 questions:");
                for (int i = 0; i < Math.min(3, questions.size()); i++) {
                    Question q = questions.get(i);
                    System.out.println((i + 1) + ". " + q.getQuestionText());
                    System.out.println("   Topic: " + q.getTopic());
                    System.out.println("   Difficulty: " + q.getDifficulty());
                    System.out.println("   Choices: " + q.getChoices().size());
                    System.out.println("   Correct: " + q.getCorrectIndex());
                }
            }
            
            System.out.println("✓ JSON Loader test PASSED\n");
            
        } catch (Exception e) {
            System.err.println("✗ JSON Loader test FAILED");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
        }
    }
    
    private static void testCsvLoader() {
        System.out.println("--- Testing CSV Loader ---");
        try {
            QuestionSource csvLoader = new CsvQuestionLoader();
            
            SourceConfig config = new SourceConfig.Builder()
                .type(SourceConfig.SourceType.CUSTOM_CSV)
                .build();
            
            List<Question> questions = csvLoader.loadQuestions(config);
            
            System.out.println("Source: " + csvLoader.getSourceName());
            System.out.println("Loaded: " + questions.size() + " questions");
            
            if (!questions.isEmpty()) {
                System.out.println("\nFirst 3 questions:");
                for (int i = 0; i < Math.min(3, questions.size()); i++) {
                    Question q = questions.get(i);
                    System.out.println((i + 1) + ". " + q.getQuestionText());
                    System.out.println("   Topic: " + q.getTopic());
                    System.out.println("   Difficulty: " + q.getDifficulty());
                    System.out.println("   Choices: " + q.getChoices().size());
                    System.out.println("   Correct: " + q.getCorrectIndex());
                }
            }
            
            System.out.println("✓ CSV Loader test PASSED\n");
            
        } catch (Exception e) {
            System.err.println("✗ CSV Loader test FAILED");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
        }
    }
    
    private static void testExcelLoader() {
        System.out.println("--- Testing Excel Loader ---");
        try {
            QuestionSource excelLoader = new ExcelQuestionLoader();
            
            SourceConfig config = new SourceConfig.Builder()
                .type(SourceConfig.SourceType.CUSTOM_EXCEL)
                .build();
            
            List<Question> questions = excelLoader.loadQuestions(config);
            
            System.out.println("Source: " + excelLoader.getSourceName());
            System.out.println("Loaded: " + questions.size() + " questions");
            
            if (!questions.isEmpty()) {
                System.out.println("\nFirst 3 questions:");
                for (int i = 0; i < Math.min(3, questions.size()); i++) {
                    Question q = questions.get(i);
                    System.out.println((i + 1) + ". " + q.getQuestionText());
                    System.out.println("   Topic: " + q.getTopic());
                    System.out.println("   Difficulty: " + q.getDifficulty());
                    System.out.println("   Choices: " + q.getChoices().size());
                    System.out.println("   Correct: " + q.getCorrectIndex());
                }
            }
            
            System.out.println("✓ Excel Loader test PASSED\n");
            
        } catch (Exception e) {
            System.err.println("✗ Excel Loader test FAILED");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
        }
    }
}
