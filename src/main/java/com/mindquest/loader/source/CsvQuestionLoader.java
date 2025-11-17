package com.mindquest.loader.source;

import com.mindquest.loader.QuestionSource;
import com.mindquest.loader.TopicScanner;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.model.question.EasyQuestion;
import com.mindquest.model.question.HardQuestion;
import com.mindquest.model.question.MediumQuestion;
import com.mindquest.model.question.Question;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Loads questions from CSV files using OpenCSV.
 * Expected columns: topic, difficulty, questionText, choice0, choice1, choice2, choice3, correctIndex
 * Default file location: src/questions/external_source/csv/{topic}.csv
 */
public class CsvQuestionLoader implements QuestionSource {

    private static int questionCounter = 1;

    /**
     * Implements QuestionSource interface.
     * Loads questions from CSV file based on topic in the configuration.
     */
    @Override
    public List<Question> loadQuestions(SourceConfig config) throws IOException {
        String topic = config.getTopic();
        String difficulty = config.getDifficulty();
        
        String filePath = TopicScanner.getTopicFilePath(getTopicFileName(topic), SourceConfig.SourceType.CUSTOM_CSV);
        
        System.out.println("[CSV Loader] Loading from: " + filePath);
        System.out.println("[CSV Loader] Filtering for difficulty: " + difficulty);
        
        List<Question> result = loadQuestionsFromFile(filePath, difficulty);
        System.out.println("[CSV Loader] Loaded " + result.size() + " questions");
        
        return result;
    }
    
    /**
     * Returns the source name for logging and user feedback.
     */
    @Override
    public String getSourceName() {
        return "CSV File";
    }

    /**
     * Maps display topic name to CSV filename (without extension).
     * Example: "Artificial Intelligence" → "ai"
     * If topic is already a filename (e.g., "ai"), returns as-is.
     */
    private static String getTopicFileName(String topic) {
        if (topic == null) return "unknown";
        
        if (topic.equals(topic.toLowerCase()) && !topic.contains(" ")) {
            return topic;
        }
        
        switch (topic.toLowerCase()) {
            case "artificial intelligence":
                return "ai";
            case "computer science":
                return "cs";
            case "philosophy":
                return "philosophy";
            default:
                return topic.toLowerCase().replace(" ", "_");
        }
    }

    /**
     * Loads questions from a CSV file and filters by difficulty.
     * 
     * @param filePath Path to the CSV file
     * @param difficulty Difficulty level to filter (null = load all)
     * @return List of Question objects matching the difficulty
     * @throws IOException if file cannot be read
     */
    private static List<Question> loadQuestionsFromFile(String filePath, String difficulty) throws IOException {
        List<Question> allQuestions = loadQuestions(filePath);
        
        if (difficulty == null || difficulty.isEmpty()) {
            return allQuestions;
        }
        
        List<Question> filtered = new ArrayList<>();
        for (Question q : allQuestions) {
            if (matchesDifficulty(q, difficulty)) {
                filtered.add(q);
            }
        }
        return filtered;
    }

    /**
     * Checks if a question matches the specified difficulty level.
     */
    private static boolean matchesDifficulty(Question q, String difficulty) {
        if (difficulty == null) return true;
        
        String lowerDiff = difficulty.toLowerCase();
        if (lowerDiff.equals("easy") && q instanceof EasyQuestion) return true;
        if (lowerDiff.equals("medium") && q instanceof MediumQuestion) return true;
        if (lowerDiff.equals("hard") && q instanceof HardQuestion) return true;
        
        return false;
    }

    /**
     * Loads questions from a CSV file.
     * Legacy static method maintained for backward compatibility.
     * 
     * @param filePath Path to the CSV file
     * @return List of Question objects
     * @throws IOException if file cannot be read
     */
    public static List<Question> loadQuestions(String filePath) throws IOException {
        List<Question> questions = new ArrayList<>();
        
        java.io.Reader reader = getReader(filePath);
        
        try (CSVReader csvReader = new CSVReader(reader)) {
            List<String[]> rows = csvReader.readAll();
            
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                
                if (row == null || row.length == 0 || isRowEmpty(row)) {
                    continue;
                }
                
                try {
                    Question question = parseRow(row);
                    if (question != null) {
                        questions.add(question);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing row " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (CsvException e) {
            throw new IOException("Error reading CSV file: " + e.getMessage(), e);
        }
        
        return questions;
    }
    
    /**
     * Gets a Reader for the CSV file, trying classpath first, then file system.
     */
    private static java.io.Reader getReader(String filePath) throws IOException {
        String classpathPath = filePath.replace("src/", "");
        
        java.io.InputStream is = CsvQuestionLoader.class.getClassLoader().getResourceAsStream(classpathPath);
        if (is != null) {
            System.out.println("[CSV Loader] Loading from classpath: " + classpathPath);
            return new java.io.InputStreamReader(is);
        }
        
        System.out.println("[CSV Loader] Loading from file system: " + filePath);
        return new FileReader(filePath);
    }

    /**
     * Parses a single CSV row into a Question object.
     * Expected format: topic, difficulty, questionText, choice0, choice1, choice2, choice3, correctIndex
     */
    private static Question parseRow(String[] row) {
        try {
            if (row.length < 8) {
                System.err.println("Row has insufficient columns: " + Arrays.toString(row));
                return null;
            }
            
            String topic = row[0].trim();
            String difficulty = row[1].trim();
            String questionText = row[2].trim();
            
            List<String> choices = new ArrayList<>();
            for (int i = 3; i <= 6; i++) {
                if (i < row.length && !row[i].trim().isEmpty()) {
                    choices.add(row[i].trim());
                }
            }
            
            int correctIndex = Integer.parseInt(row[7].trim());
            
            if (questionText.isEmpty() || choices.isEmpty()) {
                return null;
            }
            
            String id = generateQuestionId(difficulty);
            
            switch (difficulty.toLowerCase()) {
                case "easy":
                    return new EasyQuestion(id, questionText, choices, correctIndex, topic);
                case "medium":
                    return new MediumQuestion(id, questionText, choices, correctIndex, topic);
                case "hard":
                    return new HardQuestion(id, questionText, choices, correctIndex, topic);
                default:
                    System.err.println("Unknown difficulty: " + difficulty);
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Error parsing question: " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if a row is empty (all cells are blank or whitespace).
     */
    private static boolean isRowEmpty(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Generates a unique question ID.
     */
    private static String generateQuestionId(String difficulty) {
        return "CSV_" + difficulty.toUpperCase() + "_" + String.format("%03d", questionCounter++);
    }
}
