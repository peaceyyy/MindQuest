package com.mindquest.loader;

import com.mindquest.model.Question;
import java.io.IOException;
import java.util.List;

/**
 * Simple diagnostic to verify external source loaders work correctly.
 * Run this via: mvn compile exec:java -Dexec.mainClass="com.mindquest.loader.LoaderDiagnostic"
 */
public class LoaderDiagnostic {

    public static void main(String[] args) {
        System.out.println("=== MindQuest External Source Diagnostic ===\n");
        
        // Test CSV Loader
        System.out.println("--- Testing CSV Loader ---");
        testCsvLoader();
        
        System.out.println("\n--- Testing Excel Loader ---");
        testExcelLoader();
        
        System.out.println("\n=== Diagnostic Complete ===");
    }
    
    private static void testCsvLoader() {
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
                    
                    System.out.printf("✓ %s (%s): %d questions%n", 
                        topic, difficulty, questions.size());
                    
                } catch (IOException e) {
                    System.out.printf("✗ %s (%s): %s%n", 
                        topic, difficulty, e.getMessage());
                }
            }
        }
    }
    
    private static void testExcelLoader() {
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
                    
                    System.out.printf("✓ %s (%s): %d questions%n", 
                        topic, difficulty, questions.size());
                    
                } catch (IOException e) {
                    System.out.printf("✗ %s (%s): %s%n", 
                        topic, difficulty, e.getMessage());
                }
            }
        }
    }
}
