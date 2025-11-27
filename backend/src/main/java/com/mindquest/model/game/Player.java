package com.mindquest.model.game;

public class Player {
    private int hp;
    private int score;
    private int hints;
    private int maxHints; // Difficulty-based max hints
    private final int MAX_HP = 100;

    public Player() {
        this.hp = MAX_HP;
        this.score = 0;
        this.hints = 2; // Default to medium difficulty
        this.maxHints = 2;
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
        if (hints < maxHints) {
            hints++;
        }
    }
    
    /**
     * Initialize hints based on difficulty.
     * Easy: 3 hints (questions are simple, but hints help learning)
     * Medium: 2 hints (balanced)
     * Hard: 1 hint (limited help for difficult questions)
     */
    public void setHintsForDifficulty(String difficulty) {
        if (difficulty == null) {
            this.maxHints = 2;
            this.hints = 2;
            return;
        }
        
        switch (difficulty.toLowerCase()) {
            case "easy":
                this.maxHints = 3;
                this.hints = 3;
                break;
            case "medium":
                this.maxHints = 2;
                this.hints = 2;
                break;
            case "hard":
                this.maxHints = 1;
                this.hints = 1;
                break;
            default:
                this.maxHints = 2;
                this.hints = 2;
        }
    }

    public void resetForRound() {
        this.hp = MAX_HP;
        // Hints are set by setHintsForDifficulty() when round starts
        // Score is global and only resets with a new session (killing the terminal)
    }
    
    public int getMaxHints() {
        return maxHints;
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
