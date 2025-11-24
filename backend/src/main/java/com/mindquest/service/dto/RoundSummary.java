package com.mindquest.service.dto;


public class RoundSummary {
    private final int hpBonus;
    private final int roundScore;
    private final int newGlobalPointsTotal;

    public RoundSummary(int hpBonus, int roundScore, int newGlobalPointsTotal) {
        this.hpBonus = hpBonus;
        this.roundScore = roundScore;
        this.newGlobalPointsTotal = newGlobalPointsTotal;
    }

    public int getHpBonus() { return hpBonus; }
    public int getRoundScore() { return roundScore; }
    public int getNewGlobalPointsTotal() { return newGlobalPointsTotal; }
}
