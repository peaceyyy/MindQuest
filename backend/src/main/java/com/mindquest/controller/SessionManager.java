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
    private final Player player;
    private final QuestionBank questionBank;
    
    // Thread-safe state container
    private final AtomicReference<SessionState> state;

    // Immutable state class
    private static class SessionState {
        final List<Question> currentRoundQuestions;
        final Set<String> usedQuestionIds;
        final String currentTopic;
        final String currentDifficulty;
        final int currentQuestionIndex;
        final int globalPoints;
        final SourceConfig sourceConfig;

        SessionState() {
            this(Collections.emptyList(), new HashSet<>(), null, null, 0, 0, null);
        }

        SessionState(List<Question> questions, Set<String> usedIds, String topic, String diff, int index, int points, SourceConfig config) {
            this.currentRoundQuestions = questions != null ? Collections.unmodifiableList(new ArrayList<>(questions)) : Collections.emptyList();
            this.usedQuestionIds = usedIds != null ? Collections.unmodifiableSet(new HashSet<>(usedIds)) : Collections.emptySet();
            this.currentTopic = topic;
            this.currentDifficulty = diff;
            this.currentQuestionIndex = index;
            this.globalPoints = points;
            this.sourceConfig = config;
        }
        
        // Helper to create a new state with updated config
        SessionState withConfig(SourceConfig newConfig) {
            return new SessionState(currentRoundQuestions, usedQuestionIds, currentTopic, currentDifficulty, currentQuestionIndex, globalPoints, newConfig);
        }
    }

    public SessionManager(Player player, QuestionBank questionBank) {
        this.player = player;
        this.questionBank = questionBank;
        this.state = new AtomicReference<>(new SessionState());
    }
    
    /**
     * Sets the question source configuration for this session.
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

        // We need the current used IDs to filter, but we can't access state inside this method easily without passing it.
        // However, we can just filter later or pass a snapshot.
        // Let's pass a snapshot of used IDs to this method? 
        // Actually, for simplicity, let's just filter against the *current* state at the start of this method.
        // It's a slight race if usedIds changes, but usedIds only changes when starting a new round, which is what we are doing.
        Set<String> usedIdsSnapshot = state.get().usedQuestionIds;

        List<Question> freshQuestions = new ArrayList<>();
        for (Question q : availableQuestions) {
            if (!usedIdsSnapshot.contains(q.getId())) {
                freshQuestions.add(q);
            }
        }

        Collections.shuffle(freshQuestions);
        
        int count = Math.min(5, freshQuestions.size());
        return freshQuestions.subList(0, count);
    }

    public Question getCurrentQuestion() {
        SessionState s = state.get();
        if (s.currentQuestionIndex < s.currentRoundQuestions.size()) {
            return s.currentRoundQuestions.get(s.currentQuestionIndex);
        }
        return null;
    }

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

    public int getGlobalPoints() {
        return state.get().globalPoints;
    }

    public void addToGlobalPoints(int points) {
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
        state.set(new SessionState());
    }

    /**
     * Returns the number of questions loaded for the current round.
     */
    public int getCurrentRoundQuestionCount() {
        return state.get().currentRoundQuestions.size();
    }
}