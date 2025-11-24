package com.mindquest.model.question;

import java.util.List;

public class EasyQuestion extends Question {
    public EasyQuestion(String id, String questionText, List<String> choices, int correctIndex, String topic) {
        super(id, questionText, choices, correctIndex, "Easy", topic);
    }

    @Override
    public int calculateScore() {
        return 10; // Easy questions give 10 points
    }

    @Override
    public int calculateDamage() {
        return 25; // Incorrect easy questions deduct 25 HP
    }
}
