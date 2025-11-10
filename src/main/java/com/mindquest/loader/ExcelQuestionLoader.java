package com.mindquest.loader;

import com.mindquest.model.EasyQuestion;
import com.mindquest.model.HardQuestion;
import com.mindquest.model.MediumQuestion;
import com.mindquest.model.Question;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads questions from Excel (.xlsx) files using Apache POI.
 * Expected columns: topic, difficulty, questionText, choice0, choice1, choice2, choice3, correctIndex
 * Implements QuestionSource for unified loading interface.
 * Default file location: src/questions/external_source/xlsx/{topic}.xlsx
 */
public class ExcelQuestionLoader implements QuestionSource {

    private static final String BASE_PATH = "src/questions/external_source/xlsx/";
    private static int questionCounter = 1;

    /**
     * Implements QuestionSource interface.
     * Loads questions from Excel file based on topic in the configuration.
     */
    @Override
    public List<Question> loadQuestions(SourceConfig config) throws IOException {
        String topic = config.getTopic();
        String difficulty = config.getDifficulty();
        
        // Map topic to filename
        String topicFile = getTopicFileName(topic);
        String filePath = BASE_PATH + topicFile + ".xlsx";
        
        System.out.println("[Excel Loader] Loading from: " + filePath);
        System.out.println("[Excel Loader] Filtering for difficulty: " + difficulty);
        
        List<Question> result = loadQuestionsFromFile(filePath, difficulty);
        System.out.println("[Excel Loader] Loaded " + result.size() + " questions");
        
        return result;
    }
    
    /**
     * Returns the source name for logging and user feedback.
     */
    @Override
    public String getSourceName() {
        return "Excel (.xlsx) File";
    }

    /**
     * Maps display topic name to Excel filename (without extension).
     * Example: "Artificial Intelligence" → "ai"
     */
    private static String getTopicFileName(String topic) {
        if (topic == null) return "unknown";
        
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
     * Loads questions from an Excel file and filters by difficulty.
     * 
     * @param filePath Path to the Excel file
     * @param difficulty Difficulty level to filter (null = load all)
     * @return List of Question objects matching the difficulty
     * @throws IOException if file cannot be read
     */
    private static List<Question> loadQuestionsFromFile(String filePath, String difficulty) throws IOException {
        List<Question> allQuestions = loadQuestions(filePath);
        
        // If no difficulty filter, return all questions
        if (difficulty == null || difficulty.isEmpty()) {
            return allQuestions;
        }
        
        // Filter by difficulty
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
     * Loads questions from an Excel file (.xlsx).
     * Legacy static method maintained for backward compatibility.
     * 
     * @param filePath Path to the Excel file
     * @return List of Question objects
     * @throws IOException if file cannot be read
     */

    public static List<Question> loadQuestions(String filePath) throws IOException {
        List<Question> questions = new ArrayList<>();
        
        try (InputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0); // Read first sheet
            
            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue; // Skip empty rows
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
        }
        
        return questions;
    }

    /**
     * Parses a single Excel row into a Question object.
     * Expected format: topic | difficulty | questionText | choice0 | choice1 | choice2 | choice3 | correctIndex
     */
    private static Question parseRow(Row row) {
        try {
            String topic = getCellValueAsString(row.getCell(0));
            String difficulty = getCellValueAsString(row.getCell(1));
            String questionText = getCellValueAsString(row.getCell(2));
            
            // Read choices (choice0, choice1, choice2, choice3)
            List<String> choices = new ArrayList<>();
            for (int i = 3; i <= 6; i++) {
                Cell cell = row.getCell(i);
                if (cell != null) {
                    String choice = getCellValueAsString(cell);
                    if (!choice.isEmpty()) {
                        choices.add(choice);
                    }
                }
            }
            
            // Read correct index
            int correctIndex = (int) getCellValueAsNumber(row.getCell(7));
            
            // Validate required fields
            if (questionText.isEmpty() || choices.isEmpty()) {
                return null;
            }
            
            // Generate unique ID
            String id = generateQuestionId(difficulty);
            
            // Create appropriate Question subclass based on difficulty
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
     * Extracts cell value as String, handling different cell types.
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // Handle numeric values (convert to string without decimals if whole number)
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((int) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // Evaluate formula and return result
                return cell.getStringCellValue();
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    /**
     * Extracts cell value as numeric, handling different cell types.
     */
    private static double getCellValueAsNumber(Cell cell) {
        if (cell == null) {
            return 0;
        }
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            default:
                return 0;
        }
    }

    /**
     * Checks if a row is empty (all cells are blank or null).
     */
    private static boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (!value.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Generates a unique question ID.
     */
    private static String generateQuestionId(String difficulty) {
        return "EXCEL_" + difficulty.toUpperCase() + "_" + String.format("%03d", questionCounter++);
    }
}
