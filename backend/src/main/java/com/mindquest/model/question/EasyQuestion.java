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
        // INVERTED MODEL: Easy questions are strict - high damage for mistakes
    
        return 40; // 30% mistake tolerance (2.5 mistakes allowed in 5 questions)
    }
}
