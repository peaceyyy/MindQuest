package com.mindquest.loader;

import com.mindquest.loader.config.SourceConfig;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Scans external source directories (csv/ and xlsx/) to discover available topics.
 * Enables plug-and-play functionality: just add a file, and it appears in the menu.
 * 
 * Automatically detects JAR vs IDE environment:
 * - Development: Uses src/questions/external_source/...
 * - Production (JAR): Uses ./data/... (external to JAR)
 */
public class TopicScanner {
    
    private static final String DEV_CSV_PATH = "src/questions/external_source/csv/";
    private static final String DEV_XLSX_PATH = "src/questions/external_source/xlsx/";
    private static final String PROD_CSV_PATH = "./data/csv/";
    private static final String PROD_XLSX_PATH = "./data/xlsx/";
    
    private static final String CSV_BASE_PATH = getBasePath(DEV_CSV_PATH, PROD_CSV_PATH);
    private static final String XLSX_BASE_PATH = getBasePath(DEV_XLSX_PATH, PROD_XLSX_PATH);
    
    /**
     * Detects if running from JAR and returns appropriate base path.
     */
    private static String getBasePath(String devPath, String prodPath) {
        try {
            String classPath = TopicScanner.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath();
            
            // Running from JAR → use external data directory
            if (classPath.endsWith(".jar")) {
                return prodPath;
            }
        } catch (Exception e) {
            // Fallback to dev path if detection fails
        }
        
        // Development or detection failed → use src/ directory
        return devPath;
    }
    

    public static List<String> getAvailableTopics(SourceConfig.SourceType sourceType) {
        Set<String> topics = new HashSet<>();
        
        switch (sourceType) {
            case CUSTOM_CSV:
                System.out.println("[TopicScanner] Scanning CSV from: " + CSV_BASE_PATH);
                topics.addAll(scanDirectory(CSV_BASE_PATH, ".csv"));
                break;
                
            case CUSTOM_EXCEL:
                System.out.println("[TopicScanner] Scanning Excel from: " + XLSX_BASE_PATH);
                topics.addAll(scanDirectory(XLSX_BASE_PATH, ".xlsx"));
                break;
                
            case BUILTIN_JSON:
                // For JSON, scan built-in resources (fixed structure)
                topics.addAll(Arrays.asList("cs", "ai", "philosophy"));
                break;
                
            case GEMINI_API:
                // Gemini can generate questions for any topic
                // Return standard topics as suggestions
                topics.addAll(Arrays.asList("Computer Science", "Artificial Intelligence", "Philosophy"));
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
     * Returns the full file path for a given topic and source type. Used by loaders under the loader/ folder
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
