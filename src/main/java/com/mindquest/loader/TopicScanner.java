package com.mindquest.loader;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Scans external source directories (csv/ and xlsx/) to discover available topics.
 * Enables plug-and-play functionality: just add a file, and it appears in the menu.
 */
public class TopicScanner {
    
    private static final String CSV_BASE_PATH = "src/questions/external_source/csv/";
    private static final String XLSX_BASE_PATH = "src/questions/external_source/xlsx/";
    
    /**
     * Scans directories for available topics based on the selected source type.
     * Returns a sorted list of unique topic names (without extensions).
     */
    public static List<String> getAvailableTopics(SourceConfig.SourceType sourceType) {
        Set<String> topics = new HashSet<>();
        
        switch (sourceType) {
            case CUSTOM_CSV:
                topics.addAll(scanDirectory(CSV_BASE_PATH, ".csv"));
                break;
                
            case CUSTOM_EXCEL:
                topics.addAll(scanDirectory(XLSX_BASE_PATH, ".xlsx"));
                break;
                
            case BUILTIN_JSON:
                // For JSON, scan built-in resources (fixed structure)
                topics.addAll(Arrays.asList("cs", "ai", "philosophy"));
                break;
                
            case BUILTIN_HARDCODED:
            default:
                // Hardcoded topics from QuestionBank
                topics.addAll(Arrays.asList("Computer Science", "Artificial Intelligence", "Philosophy"));
                break;
        }
        
        List<String> sortedTopics = new ArrayList<>(topics);
        Collections.sort(sortedTopics);
        return sortedTopics;
    }
    
    /**
     * Scans a directory for files with a specific extension.
     * Extracts topic names from filenames (without extension).
     */
    private static List<String> scanDirectory(String basePath, String extension) {
        List<String> topics = new ArrayList<>();
        
        try {
            Path path = Paths.get(basePath);
            File dir = path.toFile();
            
            if (!dir.exists() || !dir.isDirectory()) {
                System.err.println("[TopicScanner] Directory not found: " + basePath);
                return topics;
            }
            
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(extension));
            
            if (files != null) {
                for (File file : files) {
                    String filename = file.getName();
                    // Extract topic name (remove extension)
                    String topic = filename.substring(0, filename.lastIndexOf('.'));
                    topics.add(topic);
                }
            }
            
        } catch (Exception e) {
            System.err.println("[TopicScanner] Error scanning directory " + basePath + ": " + e.getMessage());
        }
        
        return topics;
    }
    
    /**
     * Checks if a topic exists in the specified source directory.
     */
    public static boolean topicExists(String topic, SourceConfig.SourceType sourceType) {
        List<String> availableTopics = getAvailableTopics(sourceType);
        return availableTopics.contains(topic);
    }
    
    /**
     * Returns the full file path for a given topic and source type.
     * Used by loaders to construct file paths dynamically.
     */
    public static String getTopicFilePath(String topic, SourceConfig.SourceType sourceType) {
        switch (sourceType) {
            case CUSTOM_CSV:
                return CSV_BASE_PATH + topic + ".csv";
                
            case CUSTOM_EXCEL:
                return XLSX_BASE_PATH + topic + ".xlsx";
                
            case BUILTIN_JSON:
        
                return "src/questions/built-in/" + topic + "/";
                
            default:
                return "";
        }
    }
}
