package com.mindquest.server.util;

import com.mindquest.model.question.Question;
import com.mindquest.model.question.EasyQuestion;
import com.mindquest.model.question.MediumQuestion;
import com.mindquest.model.question.HardQuestion;

import java.util.List;

/**
 * Factory class for creating Question instances based on difficulty.
 * Since Question is abstract, we must use the appropriate subclass.
 */
public final class QuestionFactory {

    private QuestionFactory() {
        // Utility class - no instantiation
    }

    /**
     * Create the appropriate Question subclass based on difficulty.
     * 
     * @param id           Question identifier
     * @param questionText The question text
     * @param choices      List of answer choices
     * @param correctIndex Index of the correct answer (0-based)
     * @param difficulty   Difficulty level (easy/medium/hard)
     * @param topic        The topic/category of the question
     * @return A Question instance of the appropriate subclass
     */
    public static Question createForDifficulty(
            String id,
            String questionText,
            List<String> choices,
            int correctIndex,
            String difficulty,
            String topic) {
        
        String lowerDiff = difficulty != null ? difficulty.toLowerCase() : "medium";
        
        switch (lowerDiff) {
            case "easy":
                return new EasyQuestion(id, questionText, choices, correctIndex, topic);
            case "medium":
                return new MediumQuestion(id, questionText, choices, correctIndex, topic);
            case "hard":
                return new HardQuestion(id, questionText, choices, correctIndex, topic);
            default:
                // Default to Medium if unknown difficulty
                return new MediumQuestion(id, questionText, choices, correctIndex, topic);
        }
    }
}
