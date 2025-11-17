package com.mindquest.service.dto;


public class AnswerResult {
    private final boolean correct;
    private final int pointsAwarded;
    private final int damageTaken;
    private final int playerHpAfter;

    public AnswerResult(boolean correct, int pointsAwarded, int damageTaken, int playerHpAfter) {
        this.correct = correct;
        this.pointsAwarded = pointsAwarded;
        this.damageTaken = damageTaken;
        this.playerHpAfter = playerHpAfter;
    }

    public boolean isCorrect() { return correct; }
    public int getPointsAwarded() { return pointsAwarded; }
    public int getDamageTaken() { return damageTaken; }
    public int getPlayerHpAfter() { return playerHpAfter; }
}
