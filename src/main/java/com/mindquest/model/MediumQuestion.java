package com.mindquest.model;

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
        return 15; // Incorrect medium questions deduct 15 HP
    }
}