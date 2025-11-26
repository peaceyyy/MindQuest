package com.mindquest.service.dto;


public class AnswerResult {
    private final boolean correct;
    private final int pointsAwarded;
    private final int damageTaken;
    private final int playerHpAfter;
    private final boolean isCritical;

    public AnswerResult(boolean correct, int pointsAwarded, int damageTaken, int playerHpAfter, boolean isCritical) {
        this.correct = correct;
        this.pointsAwarded = pointsAwarded;
        this.damageTaken = damageTaken;
        this.playerHpAfter = playerHpAfter;
        this.isCritical = isCritical;
    }

    public boolean isCorrect() { return correct; }
    public int getPointsAwarded() { return pointsAwarded; }
    public int getDamageTaken() { return damageTaken; }
    public int getPlayerHpAfter() { return playerHpAfter; }
    public boolean isCritical() { return isCritical; }
}
