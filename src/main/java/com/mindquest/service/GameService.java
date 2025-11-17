package com.mindquest.service;

import com.mindquest.controller.SessionManager;
import com.mindquest.model.QuestionBank;
import com.mindquest.model.game.Player;
import com.mindquest.model.question.Question;
import com.mindquest.service.dto.AnswerResult;
import com.mindquest.service.dto.RoundSummary;


public class GameService {
    private final SessionManager sessionManager;
    private final Player player;
    private final QuestionBank questionBank;

    private Integer snapshotHp = null;
    private Integer snapshotScore = null;

    public GameService(SessionManager sessionManager, Player player, QuestionBank questionBank) {
        this.sessionManager = sessionManager;
        this.player = player;
        this.questionBank = questionBank;
    }

    public void startNewRound(String topic, String difficulty) {
        sessionManager.startNewRound(topic, difficulty);
 
        // (hints are per-round and reset automatically, so not snapshotted)
        snapshotHp = player.getHp();
        snapshotScore = player.getScore();
    }

    public Question getCurrentQuestion() {
        return sessionManager.getCurrentQuestion();
    }

    public boolean hasMoreQuestions() {
        return sessionManager.hasMoreQuestions();
    }

    public void moveToNextQuestion() {
        sessionManager.moveToNextQuestion();
    }

    public int getGlobalPoints() {
        return sessionManager.getGlobalPoints();
    }

    public boolean useHint() {
        return player.useHint();
    }

  
    public void restoreHint() {
        player.restoreHint();
    }

    
    public AnswerResult evaluateAnswer(Question question, int answerIndex, boolean isFinalChance) {
        boolean correct = (answerIndex == question.getCorrectIndex());
        int pointsAwarded = 0;
        int damageTaken = 0;

        if (correct) {
            pointsAwarded = question.calculateScore();
            player.addScore(pointsAwarded);
            if (isFinalChance) {
                player.restoreHp(30);
            }
        } else {
            damageTaken = question.calculateDamage();
            player.takeDamage(damageTaken);
        }

        return new AnswerResult(correct, pointsAwarded, damageTaken, player.getHp());
    }

    /**
     * Compute round summary on success (when HP > 0 at end of round).
     */
    public RoundSummary completeRoundAndSummarize() {
        int hpBonus = (int) (player.getHp() * 0.5);
        int roundScore = player.getScore() + hpBonus;
        player.addScore(hpBonus);
        sessionManager.addToGlobalPoints(roundScore);
        snapshotHp = null;
        snapshotScore = null;
        return new RoundSummary(hpBonus, roundScore, sessionManager.getGlobalPoints());
    }

    /**
     * Rollback any provisional changes made during the current round.
     * Restores player HP and score to the values captured at
     * the start of the round. 
     */
    public void rollbackRound() {
        if (snapshotHp != null && snapshotScore != null) {
            player.restoreState(snapshotHp, snapshotScore, player.getHints());
        }
        snapshotHp = null;
        snapshotScore = null;
    }

    /**
     * Select a hard question for final chance from the current topic.
     */
    public Question getFinalChanceQuestion(String topic) {
        var hardQuestions = questionBank.getQuestionsByTopicAndDifficulty(topic, "Hard");
        if (hardQuestions != null && !hardQuestions.isEmpty()) {
            int randomIndex = (int) (Math.random() * hardQuestions.size());
            return hardQuestions.get(randomIndex);
        }
        return null;
    }

    public String getCurrentTopic() { return sessionManager.getCurrentTopic(); }
}
