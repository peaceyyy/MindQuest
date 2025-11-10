package com.mindquest.loader;

import com.mindquest.model.Question;
import java.io.IOException;
import java.util.List;

/**
 * Quick diagnostic test to verify external source loaders work correctly.
 * This tests CSV and Excel loaders with the topic-based file structure.
 */
public class ExternalSourceTest {

    public static void main(String[] args) {
        System.out.println("=== Testing External Source Loaders ===\n");
        
        // Test CSV Loader
        testCsvLoader();
        
        // Test Excel Loader
        testExcelLoader();
    }
    
    private static void testCsvLoader() {
        System.out.println("--- CSV Loader Test ---");
        
        String[] topics = {"Artificial Intelligence", "Computer Science", "Philosophy"};
        String[] difficulties = {"Easy", "Medium", "Hard"};
        
        for (String topic : topics) {
            for (String difficulty : difficulties) {
                try {
                    SourceConfig config = new SourceConfig.Builder()
                        .type(SourceConfig.SourceType.CUSTOM_CSV)
                        .topic(topic)
                        .difficulty(difficulty)
                        .build();
                    
                    CsvQuestionLoader loader = new CsvQuestionLoader();
                    List<Question> questions = loader.loadQuestions(config);
                    
                    System.out.printf("  %s - %s: %d questions loaded%n", 
                        topic, difficulty, questions.size());
                    
                    if (!questions.isEmpty()) {
                        Question first = questions.get(0);
                        System.out.printf("    Sample: %s%n", 
                            first.getQuestionText().substring(0, Math.min(50, first.getQuestionText().length())) + "...");
                    }
                } catch (IOException e) {
                    System.out.printf("  %s - %s: ERROR - %s%n", 
                        topic, difficulty, e.getMessage());
                }
            }
        }
        System.out.println();
    }
    
    private static void testExcelLoader() {
        System.out.println("--- Excel Loader Test ---");
        
        String[] topics = {"Artificial Intelligence", "Computer Science", "Philosophy"};
        String[] difficulties = {"Easy", "Medium", "Hard"};
        
        for (String topic : topics) {
            for (String difficulty : difficulties) {
                try {
                    SourceConfig config = new SourceConfig.Builder()
                        .type(SourceConfig.SourceType.CUSTOM_EXCEL)
                        .topic(topic)
                        .difficulty(difficulty)
                        .build();
                    
                    ExcelQuestionLoader loader = new ExcelQuestionLoader();
                    List<Question> questions = loader.loadQuestions(config);
                    
                    System.out.printf("  %s - %s: %d questions loaded%n", 
                        topic, difficulty, questions.size());
                    
                    if (!questions.isEmpty()) {
                        Question first = questions.get(0);
                        System.out.printf("    Sample: %s%n", 
                            first.getQuestionText().substring(0, Math.min(50, first.getQuestionText().length())) + "...");
                    }
                } catch (IOException e) {
                    System.out.printf("  %s - %s: ERROR - %s%n", 
                        topic, difficulty, e.getMessage());
                }
            }
        }
        System.out.println();
    }
}
