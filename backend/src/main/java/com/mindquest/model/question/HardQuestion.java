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
        return 10; // Incorrect hard questions deduct 10 HP
    }
}
