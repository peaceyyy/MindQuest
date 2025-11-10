package com.mindquest.tests;

import com.mindquest.loader.ExcelQuestionLoader;
import com.mindquest.loader.JsonQuestionLoader;
import com.mindquest.loader.QuestionSource;
import com.mindquest.loader.SourceConfig;
import com.mindquest.model.Question;
import java.util.List;

/**
 * Quick test harness to verify QuestionSource implementations.
 */
public class QuestionSourceTest {
    
    public static void main(String[] args) {
        System.out.println("=== Testing QuestionSource Implementations ===\n");
        
        // Test 1: JSON Loader
        testJsonLoader();
        
        // Test 2: Excel Loader
        testExcelLoader();
        
        System.out.println("\n=== All Tests Complete ===");
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
                Question first = questions.get(0);
                System.out.println("Sample question: " + first.getQuestionText());
                System.out.println("Choices: " + first.getChoices().size());
            }
            
            System.out.println("✓ JSON Loader test PASSED\n");
            
        } catch (Exception e) {
            System.err.println("✗ JSON Loader test FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testExcelLoader() {
        System.out.println("--- Testing Excel Loader ---");
        try {
            QuestionSource excelLoader = new ExcelQuestionLoader();
            
            SourceConfig config = new SourceConfig.Builder()
                .type(SourceConfig.SourceType.CUSTOM_EXCEL)
                .filePath("resources/samples/questions.xlsx")
                .build();
            
            List<Question> questions = excelLoader.loadQuestions(config);
            
            System.out.println("Source: " + excelLoader.getSourceName());
            System.out.println("Loaded: " + questions.size() + " questions");
            
            if (!questions.isEmpty()) {
                Question first = questions.get(0);
                System.out.println("Sample question: " + first.getQuestionText());
                System.out.println("Topic: " + first.getTopic());
                System.out.println("Difficulty: " + first.getDifficulty());
                System.out.println("Choices: " + first.getChoices().size());
            }
            
            System.out.println("✓ Excel Loader test PASSED\n");
            
        } catch (Exception e) {
            System.err.println("✗ Excel Loader test FAILED: " + e.getMessage());
            System.err.println("Note: Make sure resources/samples/questions.xlsx exists");
            e.printStackTrace();
        }
    }
}
