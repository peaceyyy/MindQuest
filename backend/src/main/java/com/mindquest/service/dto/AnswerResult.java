package com.mindquest.service.dto;


public class AnswerResult {
    private final boolean correct;
    private final int pointsAwarded;
    private final int damageTaken;
    private final int playerHpAfter;
    private final boolean isCritical;
    private final boolean isCounterattack; // Enemy critical damage from 3-wrong streak
    
    // Live accuracy tracking for gauge widget
    private final int correctAnswers;
    private final int incorrectAnswers;
    private final double currentAccuracy; // Percentage (0-100)

    public AnswerResult(boolean correct, int pointsAwarded, int damageTaken, int playerHpAfter, 
                        boolean isCritical, boolean isCounterattack,
                        int correctAnswers, int incorrectAnswers, double currentAccuracy) {
        this.correct = correct;
        this.pointsAwarded = pointsAwarded;
        this.damageTaken = damageTaken;
        this.playerHpAfter = playerHpAfter;
        this.isCritical = isCritical;
        this.isCounterattack = isCounterattack;
        this.correctAnswers = correctAnswers;
        this.incorrectAnswers = incorrectAnswers;
        this.currentAccuracy = currentAccuracy;
    }

    public boolean isCorrect() { return correct; }
    public int getPointsAwarded() { return pointsAwarded; }
    public int getDamageTaken() { return damageTaken; }
    public int getPlayerHpAfter() { return playerHpAfter; }
    public boolean isCritical() { return isCritical; }
    public boolean isCounterattack() { return isCounterattack; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getIncorrectAnswers() { return incorrectAnswers; }
    public double getCurrentAccuracy() { return currentAccuracy; }
}
