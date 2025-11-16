package com.mindquest.controller;

import com.mindquest.loader.factory.QuestionBankFactory;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.model.game.Player;
import com.mindquest.model.question.Question;
import com.mindquest.model.QuestionBank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
    private SourceConfig sourceConfig; // Current source configuration

    public SessionManager(Player player, QuestionBank questionBank) {
        this.player = player;
        this.questionBank = questionBank;
        this.usedQuestionIds = new HashSet<>();
        this.currentRoundQuestions = new ArrayList<>();
        this.currentQuestionIndex = 0;
        this.globalPoints = 0;
        this.sourceConfig = null; // No source config by default (uses hardcoded)
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

    private void loadQuestionsForRound(String topic, String difficulty) {
        List<Question> availableQuestions;
        
        // Use QuestionBankFactory with SourceConfig if available
        if (sourceConfig != null) {
            // Build a config with topic and difficulty for this round
            SourceConfig roundConfig = new SourceConfig.Builder()
                .type(sourceConfig.getType())
                .topic(topic)
                .difficulty(difficulty)
                .filePath(sourceConfig.getFilePath())
                .extraParams(sourceConfig.getExtraParams())
                .build();
            
            availableQuestions = QuestionBankFactory.getQuestions(roundConfig);
        } else {
            // Fallback to hardcoded QuestionBank
            availableQuestions = questionBank.getQuestionsByTopicAndDifficulty(topic, difficulty);
        }
        if (availableQuestions == null || availableQuestions.isEmpty()) {
            System.out.println("No questions available for " + topic + " - " + difficulty);
            return;
        }

        // Filter out already used questions (if any, though for a new round, this set should be empty)
        List<Question> freshQuestions = new ArrayList<>();
        for (Question q : availableQuestions) {
            if (!usedQuestionIds.contains(q.getId())) {
                freshQuestions.add(q);
            }
        }

        Collections.shuffle(freshQuestions);

        // Select 5 questions for the round, or fewer if not enough fresh questions
        currentRoundQuestions.clear();
        for (int i = 0; i < Math.min(5, freshQuestions.size()); i++) {
            currentRoundQuestions.add(freshQuestions.get(i));
            usedQuestionIds.add(freshQuestions.get(i).getId()); // Mark as used for this session
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
        player = new Player(); // Completely reset player state
        usedQuestionIds.clear(); // Clear all used questions
        currentRoundQuestions.clear();
        currentQuestionIndex = 0;
        currentTopic = null;
        currentDifficulty = null;
        globalPoints = 0; // Reset global points when session fully resets
    }
}