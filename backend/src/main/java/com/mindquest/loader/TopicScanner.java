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
    private static final String DEV_JSON_PATH = "src/questions/external_source/json/";
    private static final String PROD_CSV_PATH = "data/csv/";
    private static final String PROD_XLSX_PATH = "data/xlsx/";
    private static final String PROD_JSON_PATH = "data/json/";

    private static final String CSV_BASE_PATH = resolveBasePath(DEV_CSV_PATH, PROD_CSV_PATH);
    private static final String XLSX_BASE_PATH = resolveBasePath(DEV_XLSX_PATH, PROD_XLSX_PATH);
    private static final String JSON_BASE_PATH = resolveBasePath(DEV_JSON_PATH, PROD_JSON_PATH);
    
    /**
     * Detects if running from JAR and returns appropriate base path.
     */
    private static String resolveBasePath(String devPath, String prodPath) {
        try {
            String userDir = System.getProperty("user.dir");
            List<Path> candidates = new ArrayList<>();

            // Common dev layout when running from backend/ folder
            candidates.add(Paths.get(userDir, devPath));

            // Common dev layout when running from project root (parent contains backend/)
            candidates.add(Paths.get(userDir, "backend", devPath));

            // Try parent directory of userDir (if running from backend/ or other nested location)
            Path parent = Paths.get(userDir).getParent();
            if (parent != null) {
                candidates.add(parent.resolve(devPath));
                candidates.add(parent.resolve("backend").resolve(devPath));
            }

            // Production external data directory candidate
            candidates.add(Paths.get(userDir, prodPath));

            // Evaluate candidates and return first existing directory
            for (Path p : candidates) {
                if (p != null) {
                    File f = p.toFile();
                    if (f.exists() && f.isDirectory()) {
                        return p.toString() + File.separator;
                    }
                }
            }

            // If none exist, return a deterministic absolute path (first dev candidate)
            Path fallback = Paths.get(userDir, devPath);
            return fallback.toString() + File.separator;
        } catch (Exception e) {
            // Best-effort fallback: return devPath as-is
            return devPath;
        }
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

            case CUSTOM_JSON:
                System.out.println("[TopicScanner] Scanning JSON from: " + JSON_BASE_PATH);
                topics.addAll(scanDirectory(JSON_BASE_PATH, ".json"));
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
            
            File[] files = dir.listFiles((d, name) -> {
                String lower = name.toLowerCase();
                // Ignore temporary files and hidden/system files
                if (lower.endsWith(".tmp") || lower.endsWith("~") || lower.startsWith(".")) {
                    return false;
                }
                return lower.endsWith(extension);
            });
            
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

            case CUSTOM_JSON:
                return JSON_BASE_PATH + topic + ".json";
                
            case BUILTIN_JSON:
        
                return "src/questions/built-in/" + topic + "/";
                
            default:
                return "";
        }
    }
}
