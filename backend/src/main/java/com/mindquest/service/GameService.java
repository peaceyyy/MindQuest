package com.mindquest.service;

import com.mindquest.controller.SessionManager;
import com.mindquest.model.QuestionBank;
import com.mindquest.model.game.Player;
import com.mindquest.model.question.Question;
import com.mindquest.service.dto.AnswerResult;
import com.mindquest.service.dto.RoundSummary;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class GameService {
    private final SessionManager sessionManager;
    private final Player player;
    private final QuestionBank questionBank;

    private Integer snapshotHp = null;
    private Integer snapshotScore = null;
    private static final AtomicInteger poolThreadCounter = new AtomicInteger(1);
    private final ExecutorService backgroundPool = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors()/2),
        r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("GameService-" + poolThreadCounter.getAndIncrement());
            return t;
        }
    );

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

    /**
     * Async variant to start a new round without blocking caller (UI).
     * Times out after 60 seconds to prevent indefinite hangs.
     */
    public CompletableFuture<Void> startNewRoundAsync(String topic, String difficulty) {
        return CompletableFuture.runAsync(
            () -> startNewRound(topic, difficulty),
            backgroundPool
        ).orTimeout(60, TimeUnit.SECONDS);
    }

    /**
     * Shutdown the background executor used by this service.
     */
    public void shutdown() {
        backgroundPool.shutdown();
        try {
            if (!backgroundPool.awaitTermination(5, TimeUnit.SECONDS)) {
                backgroundPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            backgroundPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
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

    
    public AnswerResult evaluateAnswer(Question question, int answerIndex, boolean isFinalChance, Long answerTimeMs) {
        boolean correct = (answerIndex == question.getCorrectIndex());
        int pointsAwarded = 0;
        int damageTaken = 0;
        boolean isCritical = false;

        // Check for critical hit (answer in less than 5 seconds)
        if (correct && answerTimeMs != null && answerTimeMs < 5000) {
            isCritical = true;
        }

        if (correct) {
            pointsAwarded = question.calculateScore();
            // Bonus points for critical hit
            if (isCritical) {
                pointsAwarded = (int) Math.round(pointsAwarded * 1.15);
            }
            player.addScore(pointsAwarded);
            if (isFinalChance) {
                player.restoreHp(30);
            }
        } else {
            damageTaken = question.calculateDamage();
            player.takeDamage(damageTaken);
        }

        return new AnswerResult(correct, pointsAwarded, damageTaken, player.getHp(), isCritical);
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
