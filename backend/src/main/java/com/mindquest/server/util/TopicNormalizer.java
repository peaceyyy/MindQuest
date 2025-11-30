package com.mindquest.server.util;

/**
 * Utility class for normalizing topic and difficulty strings
 * to match QuestionBank's expected format.
 */
public final class TopicNormalizer {

    private TopicNormalizer() {
        // Utility class - no instantiation
    }

    /**
     * Normalize topic name to standard format.
     * Maps short forms and variations to full names.
     */
    public static String normalizeTopic(String topic) {
        if (topic == null) return null;
        
        String lower = topic.toLowerCase().trim();
        
        // Map short forms and variations to full names
        switch (lower) {
            case "ai":
            case "artificial intelligence":
                return "Artificial Intelligence";
            case "cs":
            case "computer science":
                return "Computer Science";
            case "philosophy":
            case "phil":
                return "Philosophy";
            default:
                // Capitalize first letter of each word for unknown topics
                return capitalizeWords(topic.trim());
        }
    }

    /**
     * Normalize difficulty to standard format (capitalized).
     * e.g., "easy" -> "Easy", "MEDIUM" -> "Medium"
     */
    public static String normalizeDifficulty(String difficulty) {
        if (difficulty == null) return null;
        
        String lower = difficulty.toLowerCase().trim();
        
        // Capitalize first letter: easy -> Easy, medium -> Medium, hard -> Hard
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }

    /**
     * Capitalize the first letter of each word in the input string.
     */
    public static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return input;
        
        String[] words = input.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
}
