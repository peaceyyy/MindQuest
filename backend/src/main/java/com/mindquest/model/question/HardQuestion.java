package com.mindquest.model.question;

import java.util.List;

public class HardQuestion extends Question {
    public HardQuestion(String id, String questionText, List<String> choices, int correctIndex, String topic) {
        super(id, questionText, choices, correctIndex, "Hard", topic);
    }

    @Override
    public int calculateScore() {
        return 30; // Hard questions give 30 points
    }

    @Override
    public int calculateDamage() {
        // FORGIVING MODEL: Hard questions are genuinely difficult
        // Mistakes are understandable - lower punishment
        return 20; // 50% mistake tolerance (2-3 mistakes allowed in 5 questions)
    }
}
