package com.mindquest.model.game;

public class Player {
    private int hp;
    private int score;
    private int hints;
    private final int MAX_HP = 100;
    private final int INITIAL_HINTS = 1;

    public Player() {
        this.hp = MAX_HP;
        this.score = 0;
        this.hints = INITIAL_HINTS;
    }

    public int getHp() {
        return hp;
    }

    public void takeDamage(int damage) {
        this.hp = Math.max(0, this.hp - damage);
    }

    public void restoreHp(int amount) {
        this.hp = Math.min(MAX_HP, this.hp + amount);
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public int getHints() {
        return hints;
    }

    public boolean useHint() {
        if (hints > 0) {
            hints--;
            return true;
        } else {
            return false;
        }
    }

    public void restoreHint() {
        if (hints < INITIAL_HINTS) {
            hints++;
        }
    }

    public void resetForRound() {
        this.hp = MAX_HP;
        this.hints = INITIAL_HINTS;
        // Score is global and only resets with a new session (killing the terminal)
    }

  
    public void restoreState(int hp, int score, int hints) {
        this.hp = Math.max(0, Math.min(MAX_HP, hp));
        this.score = score;
        this.hints = hints;
    }

    public int getMaxHp() {
        return MAX_HP;
    }
}
