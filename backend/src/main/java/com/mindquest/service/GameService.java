package com.mindquest.service;

import com.mindquest.controller.SessionManager;
import com.mindquest.loader.config.SourceConfig;
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
    
    // Round statistics tracking
    private int correctAnswersCount = 0;
    private int incorrectAnswersCount = 0;
    private long totalAnswerTimeMs = 0;
    private int answersWithTime = 0;
    
    // Streak tracking
    private int correctStreak = 0;
    private int wrongStreak = 0;
    
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
 
        // Reset round statistics
        correctAnswersCount = 0;
        incorrectAnswersCount = 0;
        totalAnswerTimeMs = 0;
        answersWithTime = 0;
        
        // Reset streaks
        correctStreak = 0;
        wrongStreak = 0;
        
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

    public int getHints() {
        return player.getHints();
    }
    
    public int getMaxHints() {
        return player.getMaxHints();
    }

  
    public void restoreHint() {
        player.restoreHint();
    }

    
    public AnswerResult evaluateAnswer(Question question, int answerIndex, boolean isFinalChance, Long answerTimeMs) {
        boolean correct = (answerIndex == question.getCorrectIndex());
        int pointsAwarded = 0;
        int damageTaken = 0;
        boolean isCritical = false;
        boolean isCounterattack = false;
        boolean isHotStreak = false;

        // Check for critical hit (answer in less than 5 seconds)
        if (correct && answerTimeMs != null && answerTimeMs < 5000) {
            isCritical = true;
        }

        // Track statistics
        if (correct) {
            correctAnswersCount++;
            correctStreak++;
            wrongStreak = 0; // Reset wrong streak on correct answer
        } else {
            incorrectAnswersCount++;
            wrongStreak++;
            correctStreak = 0; // Reset correct streak on wrong answer
        }
        
        if (answerTimeMs != null && answerTimeMs > 0) {
            totalAnswerTimeMs += answerTimeMs;
            answersWithTime++;
        }

        if (correct) {
            pointsAwarded = question.calculateScore();
            
            // Apply difficulty XP multiplier (Easy: 1.0×, Medium: 1.5×, Hard: 2.5×)
            double xpMultiplier = getDifficultyXpMultiplier(question.getDifficulty());
            pointsAwarded = (int) Math.round(pointsAwarded * xpMultiplier);
            
            // INVERTED CRIT BONUSES: Easy 5%, Medium 15%, Hard 25%
            if (isCritical) {
                double critMultiplier = getCritMultiplier(question.getDifficulty());
                pointsAwarded = (int) Math.round(pointsAwarded * critMultiplier);
            }
            
            // HOT STREAK BONUS: 3 correct answers in a row = +10% XP
            if (correctStreak >= 3) {
                isHotStreak = true;
                pointsAwarded = (int) Math.round(pointsAwarded * 1.10);
            }
            
            player.addScore(pointsAwarded);
            if (correctStreak >= 5) {
                // "Perfect Form!" - Full HP restore at 5 correct streak
                player.restoreHp(player.getMaxHp());
            } else if (correctStreak >= 3) {
                // "Hot Streak!" - +10% XP bonus at 3 correct streak
                pointsAwarded = (int) Math.round(pointsAwarded * 1.10);
            }
            
            player.addScore(pointsAwarded);
            if (isFinalChance) {
                player.restoreHp(30);
            }
        } else {
            damageTaken = question.calculateDamage();
            
            // Check for 3-wrong streak: Apply critical damage from enemy
            if (wrongStreak >= 3) {
                // Boss counterattack! 1.5x damage on 3rd consecutive mistake
                damageTaken = (int) Math.round(damageTaken * 1.5);
                isCounterattack = true;
                // Reset streak after delivering critical damage
                wrongStreak = 0;
            }
            
            player.takeDamage(damageTaken);
        }

        // Calculate current accuracy for live gauge
        int totalAnswered = correctAnswersCount + incorrectAnswersCount;
        double currentAccuracy = 0.0;
        if (totalAnswered > 0) {
            currentAccuracy = Math.min(100.0, (correctAnswersCount * 100.0) / totalAnswered);
        }

        return new AnswerResult(
            correct, 
            pointsAwarded, 
            damageTaken, 
            player.getHp(), 
            isCritical, 
            isCounterattack,
            correctAnswersCount,
            incorrectAnswersCount,
            currentAccuracy,
            correctStreak,
            wrongStreak,
            isHotStreak
        );
    }
    
    /**
     * Get critical hit multiplier based on difficulty.
     * Easy: 1.05 (5% bonus - minimal reward for speed on simple questions)
     * Medium: 1.15 (15% bonus - current baseline)
     * Hard: 1.25 (25% bonus - reward careful, fast reading on difficult content)
     */
    private double getCritMultiplier(String difficulty) {
        if (difficulty == null) return 1.15; // Default to medium
        
        switch (difficulty.toLowerCase()) {
            case "easy":
                return 1.05;
            case "medium":
                return 1.15;
            case "hard":
                return 1.25;
            default:
                return 1.15;
        }
    }
    
    /**
     * Get XP multiplier based on difficulty.
     * Easy: 1.0× (baseline - fundamentals)
     * Medium: 1.5× (moderate reward)
     * Hard: 2.5× (high reward for mastering difficult content)
     */
    private double getDifficultyXpMultiplier(String difficulty) {
        if (difficulty == null) return 1.0; // Default to easy
        
        switch (difficulty.toLowerCase()) {
            case "easy":
                return 1.0;
            case "medium":
                return 1.5;
            case "hard":
                return 2.5;
            default:
                return 1.0;
        }
    }

    /**
     * Compute round summary on success (when HP > 0 at end of round).
     */
    public RoundSummary completeRoundAndSummarize() {
        int hpBonus = (int) (player.getHp() * 0.5);
        int roundScore = player.getScore() + hpBonus;
        player.addScore(hpBonus);
        sessionManager.addToGlobalPoints(roundScore);
        
        // Calculate statistics
        int totalQuestions = sessionManager.getCurrentRoundQuestionCount();
        int correctAnswers = correctAnswersCount;
        int incorrectAnswers = incorrectAnswersCount;
        
        // Calculate accuracy percentage (capped at 100)
        double accuracyPercentage = 0.0;
        if (totalQuestions > 0) {
            accuracyPercentage = Math.min(100.0, (correctAnswers * 100.0) / totalQuestions);
        }
        
        // Calculate average answer time
        long averageAnswerTimeMs = 0;
        if (answersWithTime > 0) {
            averageAnswerTimeMs = totalAnswerTimeMs / answersWithTime;
        }
        
        snapshotHp = null;
        snapshotScore = null;
        
        return new RoundSummary(
            hpBonus, 
            roundScore, 
            sessionManager.getGlobalPoints(),
            totalQuestions,
            correctAnswers,
            incorrectAnswers,
            accuracyPercentage,
            averageAnswerTimeMs
        );
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
    
    public int getCorrectStreak() { return correctStreak; }
    
    public int getWrongStreak() { return wrongStreak; }

    public void setSourceConfig(SourceConfig config) {
        sessionManager.setSourceConfig(config);
    }
}
