package com.mindquest.server.util;

/**
 * Utility class for mapping answer formats.
 */
public final class AnswerMapper {

    private AnswerMapper() {
        // Utility class - no instantiation
    }

    /**
     * Convert letter answer (A/B/C/D) to zero-based index (0-3).
     * 
     * @param letter The letter answer (case-insensitive)
     * @return The zero-based index, or -1 if invalid
     */
    public static int letterToIndex(String letter) {
        if (letter == null || letter.isEmpty()) {
            return -1;
        }
        
        String upper = letter.trim().toUpperCase();
        switch (upper) {
            case "A": return 0;
            case "B": return 1;
            case "C": return 2;
            case "D": return 3;
            default: return -1;
        }
    }

    /**
     * Convert zero-based index (0-3) to letter answer (A/B/C/D).
     * 
     * @param index The zero-based index
     * @return The letter answer, or null if invalid
     */
    public static String indexToLetter(int index) {
        switch (index) {
            case 0: return "A";
            case 1: return "B";
            case 2: return "C";
            case 3: return "D";
            default: return null;
        }
    }
}
