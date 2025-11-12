package com.mindquest.loader;

import com.mindquest.loader.SourceConfig.SourceType;
import java.util.List;

/**
 * Quick diagnostic to verify TopicScanner functionality.
 * Tests plug-and-play topic discovery for external sources.
 */
public class TopicScannerTest {
    
    public static void main(String[] args) {
        System.out.println("=== TopicScanner Diagnostic ===\n");
        
        // Test CSV topic discovery
        System.out.println("CSV Topics:");
        List<String> csvTopics = TopicScanner.getAvailableTopics(SourceType.CUSTOM_CSV);
        if (csvTopics.isEmpty()) {
            System.out.println("  (No CSV files found)");
        } else {
            for (String topic : csvTopics) {
                String path = TopicScanner.getTopicFilePath(topic, SourceType.CUSTOM_CSV);
                System.out.println("  - " + topic + " → " + path);
            }
        }
        
        // Test Excel topic discovery
        System.out.println("\nExcel Topics:");
        List<String> xlsxTopics = TopicScanner.getAvailableTopics(SourceType.CUSTOM_EXCEL);
        if (xlsxTopics.isEmpty()) {
            System.out.println("  (No Excel files found)");
        } else {
            for (String topic : xlsxTopics) {
                String path = TopicScanner.getTopicFilePath(topic, SourceType.CUSTOM_EXCEL);
                System.out.println("  - " + topic + " → " + path);
            }
        }
        
        // Test built-in JSON topics
        System.out.println("\nBuilt-in JSON Topics:");
        List<String> jsonTopics = TopicScanner.getAvailableTopics(SourceType.BUILTIN_JSON);
        for (String topic : jsonTopics) {
            System.out.println("  - " + topic);
        }
        
        // Test hardcoded topics
        System.out.println("\nHardcoded Topics:");
        List<String> hardcodedTopics = TopicScanner.getAvailableTopics(SourceType.BUILTIN_HARDCODED);
        for (String topic : hardcodedTopics) {
            System.out.println("  - " + topic);
        }
        
        System.out.println("\n=== Scan Complete ===");
    }
}
