package com.mindquest.model.question;

import java.util.List;

public class MediumQuestion extends Question {
    public MediumQuestion(String id, String questionText, List<String> choices, int correctIndex, String topic) {
        super(id, questionText, choices, correctIndex, "Medium", topic);
    }

    @Override
    public int calculateScore() {
        return 20; // Medium questions give 20 points
    }

    @Override
    public int calculateDamage() {
        // GOLDEN MEAN: Medium is balanced
        // Fair damage for moderate difficulty content
        return 25; // 40% mistake tolerance (2 mistakes allowed in 5 questions)
    }
}
