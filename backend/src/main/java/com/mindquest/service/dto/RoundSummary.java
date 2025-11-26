package com.mindquest.service.dto;


public class RoundSummary {
    private final int hpBonus;
    private final int roundScore;
    private final int newGlobalPointsTotal;
    private final int totalQuestions;
    private final int correctAnswers;
    private final int incorrectAnswers;
    private final double accuracyPercentage;
    private final long averageAnswerTimeMs;

    public RoundSummary(int hpBonus, int roundScore, int newGlobalPointsTotal, 
                       int totalQuestions, int correctAnswers, int incorrectAnswers, 
                       double accuracyPercentage, long averageAnswerTimeMs) {
        this.hpBonus = hpBonus;
        this.roundScore = roundScore;
        this.newGlobalPointsTotal = newGlobalPointsTotal;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.incorrectAnswers = incorrectAnswers;
        this.accuracyPercentage = accuracyPercentage;
        this.averageAnswerTimeMs = averageAnswerTimeMs;
    }

    public int getHpBonus() { return hpBonus; }
    public int getRoundScore() { return roundScore; }
    public int getNewGlobalPointsTotal() { return newGlobalPointsTotal; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getIncorrectAnswers() { return incorrectAnswers; }
    public double getAccuracyPercentage() { return accuracyPercentage; }
    public long getAverageAnswerTimeMs() { return averageAnswerTimeMs; }
}
