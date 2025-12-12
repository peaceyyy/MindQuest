package com.mindquest.service;

import com.mindquest.controller.SessionManager;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.model.QuestionBank;
import com.mindquest.model.game.Player;
import com.mindquest.model.question.Question;
import com.mindquest.service.dto.AnswerResult;
import com.mindquest.service.dto.RoundSummary;
import com.mindquest.service.scoring.DifficultyMultipliers;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class GameService {
    // Game Balance Constants
    private static final long CRITICAL_HIT_THRESHOLD_MS = 5000;
    private static final int HOT_STREAK_THRESHOLD = 3;
    private static final double HOT_STREAK_MULTIPLIER = 1.10;
    private static final int PERFECT_FORM_STREAK_THRESHOLD = 5;
    private static final int COUNTERATTACK_STREAK_THRESHOLD = 3;
    private static final double COUNTERATTACK_DAMAGE_MULTIPLIER = 1.5;
    private static final int FINAL_CHANCE_HP_RESTORE = 30;
    
    // Thread Pool Configuration
    private static final int MIN_THREAD_POOL_SIZE = 2;
    
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
        Math.max(MIN_THREAD_POOL_SIZE, Runtime.getRuntime().availableProcessors()/2),
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

    /**
     * Starts a new game round with the specified topic and difficulty.
     * Resets player stats, round statistics, and creates a snapshot for potential rollback.
     * 
     * @param topic The question topic (e.g., "Computer Science", "AI")
     * @param difficulty The difficulty level ("Easy", "Medium", "Hard")
     * @throws IllegalArgumentException if topic or difficulty is null or empty
     */
    public void startNewRound(String topic, String difficulty) {
        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be null or empty");
        }
        if (difficulty == null || difficulty.trim().isEmpty()) {
            throw new IllegalArgumentException("Difficulty cannot be null or empty");
        }
        
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
     * Starts a new round with pre-loaded questions (e.g., from AI generation).
     * Useful for testing LLM-generated questions without going through the QuestionBank.
     * 
     * @param topic The question topic for display purposes
     * @param difficulty The difficulty level for hint allocation
     * @param questions Pre-loaded list of Question objects to use for this round
     */
    public void startNewRoundWithQuestions(String topic, String difficulty, List<Question> questions) {
        sessionManager.startNewRoundWithQuestions(topic, difficulty, questions);
        
        // Reset round statistics
        correctAnswersCount = 0;
        incorrectAnswersCount = 0;
        totalAnswerTimeMs = 0;
        answersWithTime = 0;
        
        // Reset streaks
        correctStreak = 0;
        wrongStreak = 0;
        
        // Snapshot HP/score for revert
        snapshotHp = player.getHp();
        snapshotScore = player.getScore();
    }

    /**
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

    /**
     * Retrieves the current question in the active round.
     * 
     * @return The current Question object, or null if no round is active
     */
    public Question getCurrentQuestion() {
        return sessionManager.getCurrentQuestion();
    }

    /**
     * Checks if there are remaining questions in the current round.
     * 
     * @return true if more questions are available, false otherwise
     */
    public boolean hasMoreQuestions() {
        return sessionManager.hasMoreQuestions();
    }

    public void moveToNextQuestion() {
        sessionManager.moveToNextQuestion();
    }

    public int getGlobalPoints() {
        return sessionManager.getGlobalPoints();
    }

    /**
     * Attempts to use a hint, eliminating wrong answer choices.
     * 
     * @return true if hint was successfully used, false if no hints remain
     */
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

    /**
     * Evaluates a player's answer and calculates scoring with difficulty-based multipliers.
     * Implements streak bonuses, critical hits, and counterattack mechanics.
     * 
     * @param question The question being answered
     * @param answerIndex The player's selected answer (0-based index)
     * @param isFinalChance Whether this is the player's second attempt after using a hint
     * @param answerTimeMs Time taken to answer in milliseconds (null if not tracked)
     * @return AnswerResult containing scoring, damage, HP changes, and special effect flags
     */
    public AnswerResult evaluateAnswer(Question question, int answerIndex, boolean isFinalChance, Long answerTimeMs) {
        boolean correct = (answerIndex == question.getCorrectIndex());
        int pointsAwarded = 0;
        int damageTaken = 0;
        boolean isCritical = false;
        boolean isCounterattack = false;
        boolean isHotStreak = false;

        // Check for critical hit (answer in less than 5 seconds)
        if (correct && answerTimeMs != null && answerTimeMs < CRITICAL_HIT_THRESHOLD_MS) {
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
            double xpMultiplier = DifficultyMultipliers.getXpMultiplier(question.getDifficulty());
            pointsAwarded = (int) Math.round(pointsAwarded * xpMultiplier);
            
            // INVERTED CRIT BONUSES: Easy 5%, Medium 15%, Hard 25%
            if (isCritical) {
                double critMultiplier = DifficultyMultipliers.getCritMultiplier(question.getDifficulty());
                pointsAwarded = (int) Math.round(pointsAwarded * critMultiplier);
            }
            
            // HOT STREAK BONUS: 3 correct answers in a row = +10% XP
            if (correctStreak >= HOT_STREAK_THRESHOLD) {
                isHotStreak = true;
                pointsAwarded = (int) Math.round(pointsAwarded * HOT_STREAK_MULTIPLIER);
            }
            
            player.addScore(pointsAwarded);
            if (correctStreak >= PERFECT_FORM_STREAK_THRESHOLD) {
                // "Perfect Form!" - Full HP restore at 5 correct streak
                player.restoreHp(player.getMaxHp());
            } else if (correctStreak >= HOT_STREAK_THRESHOLD) {
                // "Hot Streak!" - +10% XP bonus at 3 correct streak
                pointsAwarded = (int) Math.round(pointsAwarded * HOT_STREAK_MULTIPLIER);
            }
            
            player.addScore(pointsAwarded);
            if (isFinalChance) {
                player.restoreHp(FINAL_CHANCE_HP_RESTORE);
            }
        } else {
            damageTaken = question.calculateDamage();
            
            // Check for 3-wrong streak: Apply critical damage from enemy
            if (wrongStreak >= COUNTERATTACK_STREAK_THRESHOLD) {
                // Boss counterattack! 1.5x damage on 3rd consecutive mistake
                damageTaken = (int) Math.round(damageTaken * COUNTERATTACK_DAMAGE_MULTIPLIER);
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
     * Calculates round completion summary and awards bonus points.
     * Applies HP-based bonus (50% of remaining HP), updates global points,
     * and compiles accuracy statistics.
     * 
     * @return RoundSummary containing HP bonus, total score, accuracy, and timing statistics
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
