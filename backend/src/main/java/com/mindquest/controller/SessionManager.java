package com.mindquest.controller;

import com.mindquest.loader.factory.QuestionBankFactory;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.loader.config.MixedTopicsConfig;
import com.mindquest.model.game.Player;
import com.mindquest.model.question.Question;
import com.mindquest.model.QuestionBank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

public class SessionManager {
    private static final int DEFAULT_QUESTIONS_PER_ROUND = 5;
    
    private final Player player;
    private final QuestionBank questionBank;
    
    private final AtomicReference<SessionState> state;
    private record SessionState(
        List<Question> currentRoundQuestions,
        Set<String> usedQuestionIds,
        String currentTopic,
        String currentDifficulty,
        int currentQuestionIndex,
        int globalPoints,
        SourceConfig sourceConfig
    ) {
        /**
         * Compact constructor with defensive copying to ensure immutability.
         * Prevents external modification of mutable collections.
         */
        SessionState {
            currentRoundQuestions = currentRoundQuestions != null 
                ? Collections.unmodifiableList(new ArrayList<>(currentRoundQuestions))
                : Collections.emptyList();
            usedQuestionIds = usedQuestionIds != null
                ? Collections.unmodifiableSet(new HashSet<>(usedQuestionIds))
                : Collections.emptySet();
        }
        
        /**
         * Factory method for creating an empty initial state.
         * 
         * @return A new SessionState with default values
         */
        static SessionState empty() {
            return new SessionState(
                Collections.emptyList(), 
                new HashSet<>(), 
                null, 
                null, 
                0, 
                0, 
                null
            );
        }
        
        /**
         * @param newConfig The new source configuration
         * @return A new SessionState with updated config
         */
        SessionState withConfig(SourceConfig newConfig) {
            return new SessionState(
                currentRoundQuestions, 
                usedQuestionIds, 
                currentTopic, 
                currentDifficulty, 
                currentQuestionIndex, 
                globalPoints, 
                newConfig
            );
        }
    }

    public SessionManager(Player player, QuestionBank questionBank) {
        this.player = player;
        this.questionBank = questionBank;
        this.state = new AtomicReference<>(SessionState.empty());
    }
    
    /**
     * Sets the question source configuration for this session.
     * Allows switching between built-in, JSON, CSV, or Excel question sources.
     * 
     * @param config The source configuration to use
     */
    public void setSourceConfig(SourceConfig config) {
        state.updateAndGet(s -> s.withConfig(config));
    }
    
    /**
     * Gets the current question source configuration.
     */
    public SourceConfig getSourceConfig() {
        return state.get().sourceConfig;
    }

    /**
     * Starts a new round with questions from the specified topic and difficulty.
     * Resets player stats, loads fresh questions, and shuffles them.
     * 
     * @param topic The question topic (e.g., "Computer Science")
     * @param difficulty The difficulty level ("Easy", "Medium", "Hard")
     */
    public void startNewRound(String topic, String difficulty) {
        // Reset player for round (Note: Player is not thread-safe, assuming single-user session or external sync)
        player.resetForRound();
        player.setHintsForDifficulty(difficulty);
        
        // Load questions outside the atomic update to avoid blocking
        List<Question> newQuestions = loadQuestionsInternal(topic, difficulty, state.get().sourceConfig);
        
        state.updateAndGet(s -> {
            Set<String> newUsedIds = new HashSet<>(s.usedQuestionIds);
            for (Question q : newQuestions) {
                newUsedIds.add(q.getId());
            }
            return new SessionState(
                newQuestions,
                newUsedIds,
                topic,
                difficulty,
                0,
                s.globalPoints,
                s.sourceConfig
            );
        });
    }
    
  
    public void startNewRoundWithQuestions(String topic, String difficulty, List<Question> questions) {
        // Reset player for round
        player.resetForRound();
        player.setHintsForDifficulty(difficulty);
        
        // Shuffle questions for variety on replay
        List<Question> shuffledQuestions = new ArrayList<>(questions);
        Collections.shuffle(shuffledQuestions);
        
        state.updateAndGet(s -> {
            // Don't add inline questions to usedQuestionIds - they're replayable
            return new SessionState(
                shuffledQuestions,
                s.usedQuestionIds,  // Keep existing used IDs, don't add new ones
                topic,
                difficulty,
                0,
                s.globalPoints,
                s.sourceConfig
            );
        });
    }

    /**
     * Starts a mixed-topics round using the provided configuration.
     */
    public void startMixedTopicsRound(MixedTopicsConfig config) {
        String topic = "Mixed Topics";
        String difficulty = config.getDifficultyMode() == MixedTopicsConfig.DifficultyMode.UNIFIED ? "mixed" : "varied";
        
        player.resetForRound();
        
        // Load questions
        List<Question> newQuestions = loadMixedQuestionsInternal(config, state.get().usedQuestionIds);
        
        state.updateAndGet(s -> {
            Set<String> newUsedIds = new HashSet<>(s.usedQuestionIds);
            for (Question q : newQuestions) {
                newUsedIds.add(q.getId());
            }
            return new SessionState(
                newQuestions,
                newUsedIds,
                topic,
                difficulty,
                0,
                s.globalPoints,
                s.sourceConfig
            );
        });
    }

    private List<Question> loadMixedQuestionsInternal(MixedTopicsConfig config, Set<String> currentUsedIds) {
        List<String> selectedTopics = config.getSelectedTopics();
        SourceConfig sourceConfig = config.getSourceConfig();
        String difficulty = config.getDifficulty();
        int questionsPerRound = config.getQuestionsPerRound();
        int perTopicLimit = config.getPerTopicLimit();
        
        List<Question> allQuestions = new ArrayList<>();
        Set<String> seenQuestionTexts = new HashSet<>();
        
        for (String topic : selectedTopics) {
            List<Question> topicQuestions;
            
            if (sourceConfig != null) {
                SourceConfig roundConfig = new SourceConfig.Builder()
                    .type(sourceConfig.getType())
                    .topic(topic)
                    .difficulty(difficulty)
                    .filePath(sourceConfig.getFilePath())
                    .extraParams(sourceConfig.getExtraParams())
                    .build();
                topicQuestions = QuestionBankFactory.getQuestions(roundConfig);
            } else {
                topicQuestions = questionBank.getQuestionsByTopicAndDifficulty(topic, difficulty);
            }
            
            if (topicQuestions == null || topicQuestions.isEmpty()) {
                System.out.println("[MixedMode] No questions available for topic: " + topic);
                continue;
            }
            
            List<Question> freshQuestions = new ArrayList<>();
            for (Question q : topicQuestions) {
                String normalizedText = q.getQuestionText().trim().toLowerCase();
                if (!currentUsedIds.contains(q.getId()) && !seenQuestionTexts.contains(normalizedText)) {
                    freshQuestions.add(q);
                    seenQuestionTexts.add(normalizedText);
                }
            }
            
            Collections.shuffle(freshQuestions);
            int limit = Math.min(perTopicLimit, freshQuestions.size());
            allQuestions.addAll(freshQuestions.subList(0, limit));
        }
        Random rng = new Random(config.getSeed());
        Collections.shuffle(allQuestions, rng);
        
        int count = Math.min(questionsPerRound, allQuestions.size());
        if (count == 0) {
            System.out.println("[MixedMode] No questions available for mixed round!");
            return Collections.emptyList();
        }
        return allQuestions.subList(0, count);
    }

    private List<Question> loadQuestionsInternal(String topic, String difficulty, SourceConfig sourceConfig) {
        List<Question> availableQuestions;
        
        if (sourceConfig != null) {
            SourceConfig roundConfig = new SourceConfig.Builder()
                .type(sourceConfig.getType())
                .topic(topic)
                .difficulty(difficulty)
                .filePath(sourceConfig.getFilePath())
                .extraParams(sourceConfig.getExtraParams())
                .build();
            availableQuestions = QuestionBankFactory.getQuestions(roundConfig);
        } else {
            availableQuestions = questionBank.getQuestionsByTopicAndDifficulty(topic, difficulty);
        }
        
        if (availableQuestions == null || availableQuestions.isEmpty()) {
            System.out.println("No questions available for " + topic + " - " + difficulty);
            return Collections.emptyList();
        }

      
        Set<String> usedIdsSnapshot = state.get().usedQuestionIds;

        List<Question> freshQuestions = new ArrayList<>();
        for (Question q : availableQuestions) {
            if (!usedIdsSnapshot.contains(q.getId())) {
                freshQuestions.add(q);
            }
        }

        Collections.shuffle(freshQuestions);
        
        int count = Math.min(DEFAULT_QUESTIONS_PER_ROUND, freshQuestions.size());
        return freshQuestions.subList(0, count);
    }

    /**
     * Retrieves the current question in the active round.
     * 
     * @return The current Question object, or null if round is complete or not started
     */
    public Question getCurrentQuestion() {
        SessionState s = state.get();
        if (s.currentQuestionIndex < s.currentRoundQuestions.size()) {
            return s.currentRoundQuestions.get(s.currentQuestionIndex);
        }
        return null;
    }

    /**
     * Advances to the next question in the current round.
     * Thread-safe operation using atomic state update.
     */
    public void moveToNextQuestion() {
        state.updateAndGet(s -> new SessionState(
            s.currentRoundQuestions,
            s.usedQuestionIds,
            s.currentTopic,
            s.currentDifficulty,
            s.currentQuestionIndex + 1,
            s.globalPoints,
            s.sourceConfig
        ));
    }

    /**
     * Checks if there are remaining questions in the current round.
     * 
     * @return true if more questions are available, false otherwise
     */
    public boolean hasMoreQuestions() {
        SessionState s = state.get();
        return s.currentQuestionIndex < s.currentRoundQuestions.size();
    }

    public String getCurrentTopic() {
        return state.get().currentTopic;
    }

    public String getCurrentDifficulty() {
        return state.get().currentDifficulty;
    }

    /**
     * Retrieves the accumulated global points across all rounds.
     * 
     * @return The total career points earned
     */
    public int getGlobalPoints() {
        return state.get().globalPoints;
    }

    /**
     * Adds points to the global career points total.
     * 
     * @param points The points to add (must be non-negative)
     * @throws IllegalArgumentException if points is negative
     */
    public void addToGlobalPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Cannot add negative points: " + points);
        }
        
        state.updateAndGet(s -> new SessionState(
            s.currentRoundQuestions,
            s.usedQuestionIds,
            s.currentTopic,
            s.currentDifficulty,
            s.currentQuestionIndex,
            s.globalPoints + points,
            s.sourceConfig
        ));
    }

    public void resetSession() {
        player.resetForRound(); // Note: Player reset might need its own sync if shared
        state.set(SessionState.empty());
    }

    /**
     * Gets the total number of questions in the current round.
     * Used for calculating accuracy statistics.
     * 
     * @return The question count for the active round
     */
    public int getCurrentRoundQuestionCount() {
        return state.get().currentRoundQuestions.size();
    }
}