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

public class SessionManager {
    private Player player;
    private QuestionBank questionBank;
    private List<Question> currentRoundQuestions;
    private Set<String> usedQuestionIds;
    private String currentTopic;
    private String currentDifficulty;
    private int currentQuestionIndex;
    private int globalPoints;
    private SourceConfig sourceConfig;

    public SessionManager(Player player, QuestionBank questionBank) {
        this.player = player;
        this.questionBank = questionBank;
        this.usedQuestionIds = new HashSet<>();
        this.currentRoundQuestions = new ArrayList<>();
        this.currentQuestionIndex = 0;
        this.globalPoints = 0;
        this.sourceConfig = null;
    }
    
    /**
     * Sets the question source configuration for this session.
     */
    public void setSourceConfig(SourceConfig config) {
        this.sourceConfig = config;
    }
    
    /**
     * Gets the current question source configuration.
     */
    public SourceConfig getSourceConfig() {
        return this.sourceConfig;
    }

    public void startNewRound(String topic, String difficulty) {
        this.currentTopic = topic;
        this.currentDifficulty = difficulty;
        player.resetForRound();
        loadQuestionsForRound(topic, difficulty);
        currentQuestionIndex = 0;
    }

    /**
     * Starts a mixed-topics round using the provided configuration.
     * Loads questions from multiple topics using a unified source and merges them.
     */
    public void startMixedTopicsRound(MixedTopicsConfig config) {
        this.currentTopic = "Mixed Topics";
        this.currentDifficulty = config.getDifficultyMode() == MixedTopicsConfig.DifficultyMode.UNIFIED 
            ? "mixed" : "varied";
        player.resetForRound();
        loadMixedQuestionsForRound(config);
        currentQuestionIndex = 0;
    }

    /**
     * Loads and merges questions from multiple topics based on MixedTopicsConfig.
     */
    private void loadMixedQuestionsForRound(MixedTopicsConfig config) {
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
                if (!usedQuestionIds.contains(q.getId()) && !seenQuestionTexts.contains(normalizedText)) {
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
        
        currentRoundQuestions.clear();
        int count = Math.min(questionsPerRound, allQuestions.size());
        for (int i = 0; i < count; i++) {
            Question q = allQuestions.get(i);
            currentRoundQuestions.add(q);
            usedQuestionIds.add(q.getId());
        }
        
        if (currentRoundQuestions.isEmpty()) {
            System.out.println("[MixedMode] No questions available for mixed round!");
        }
    }

    private void loadQuestionsForRound(String topic, String difficulty) {
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
            return;
        }

        List<Question> freshQuestions = new ArrayList<>();
        for (Question q : availableQuestions) {
            if (!usedQuestionIds.contains(q.getId())) {
                freshQuestions.add(q);
            }
        }

        Collections.shuffle(freshQuestions);

        currentRoundQuestions.clear();
        for (int i = 0; i < Math.min(5, freshQuestions.size()); i++) {
            currentRoundQuestions.add(freshQuestions.get(i));
            usedQuestionIds.add(freshQuestions.get(i).getId());
        }
    }

    public Question getCurrentQuestion() {
        if (currentQuestionIndex < currentRoundQuestions.size()) {
            return currentRoundQuestions.get(currentQuestionIndex);
        }
        return null;
    }

    public void moveToNextQuestion() {
        currentQuestionIndex++;
    }

    public boolean hasMoreQuestions() {
        return currentQuestionIndex < currentRoundQuestions.size();
    }

    public String getCurrentTopic() {
        return currentTopic;
    }

    public String getCurrentDifficulty() {
        return currentDifficulty;
    }

    public int getGlobalPoints() {
        return globalPoints;
    }

    public void addToGlobalPoints(int points) {
        this.globalPoints += points;
    }

    public void resetSession() {
        player = new Player();
        usedQuestionIds.clear();
        currentRoundQuestions.clear();
        currentQuestionIndex = 0;
        currentTopic = null;
        currentDifficulty = null;
        globalPoints = 0;
    }

    /**
     * Returns the number of questions loaded for the current round.
     */
    public int getCurrentRoundQuestionCount() {
        return currentRoundQuestions == null ? 0 : currentRoundQuestions.size();
    }
}