package com.mindquest.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public abstract class Question {
    private String id;
    private String questionText;
    private List<String> choices;
    private int correctIndex;
    private String difficulty;
    private String topic;

    public Question(String id, String questionText, List<String> choices, int correctIndex, String difficulty, String topic) {
        this.id = id;
        this.questionText = questionText;
        this.choices = new ArrayList<>(choices);
        this.correctIndex = correctIndex;
        this.difficulty = difficulty;
        this.topic = topic;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public List<String> getChoices() {
        return new ArrayList<>(choices); // copy to prevent external modification
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getTopic() {
        return topic;
    }

    // Method to shuffle choices and update correctIndex
    public void shuffleChoices() {
        String correctAnswer = choices.get(correctIndex);
        Collections.shuffle(choices);
        correctIndex = choices.indexOf(correctAnswer);
    }

    // Helper method for hints: removes two incorrect options
    public List<String> removeIncorrectOptions() {
        List<String> hintedChoices = new ArrayList<>();
        List<String> incorrectOptions = new ArrayList<>();

        for (int i = 0; i < choices.size(); i++) {
            if (i != correctIndex) {
                incorrectOptions.add(choices.get(i));
            }
        }

        Collections.shuffle(incorrectOptions);

        hintedChoices.add(choices.get(correctIndex));
        if (incorrectOptions.size() > 0) {
            hintedChoices.add(incorrectOptions.get(0));
        }
        Collections.shuffle(hintedChoices);
        return hintedChoices;
    }

    // Abstract method for difficulty-specific score/damage calculation (if needed later)
    public abstract int calculateScore();
    public abstract int calculateDamage();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(questionText).append("\n");
        for (int i = 0; i < choices.size(); i++) {
            sb.append(String.format("%d. %s\n", (i + 1), choices.get(i)));
        }
        return sb.toString();
    }
}
