package com.mindquest.controller;

import com.mindquest.loader.config.SourceConfig;
import com.mindquest.loader.config.MixedTopicsConfig;
import com.mindquest.loader.TopicScanner;
import com.mindquest.view.ConsoleUI;
import com.mindquest.model.game.Player;
import com.mindquest.model.question.Question;
import com.mindquest.model.QuestionBank;
import com.mindquest.service.GameService;
import com.mindquest.service.dto.AnswerResult;
import com.mindquest.service.dto.RoundSummary;

import java.util.ArrayList;
import java.util.List;

public class GameController {
    private SessionManager sessionManager;
    private Player player;
    private QuestionBank questionBank;
    private GameService gameService;
    private NavigationManager navigationManager;
    private boolean gameRunning;
    private boolean abortRound = false;

    public GameController() {
        this.player = new Player();
        this.questionBank = new QuestionBank();
        this.sessionManager = new SessionManager(player, questionBank);
        this.gameService = new GameService(sessionManager, player, questionBank);
        this.navigationManager = new NavigationManager();
        this.gameRunning = true;
    }

    public void startGame() {
        ConsoleUI.displayWelcome();
        while (gameRunning) {
            showMainMenu();
        }
        ConsoleUI.displayGoodbye();
        InputHandler.closeScanner();
    }

    private void showMainMenu() {
        navigationManager.push(MenuId.MAIN_MENU, null);
        ConsoleUI.displayMainMenu(sessionManager.getGlobalPoints());
        ConsoleUI.displayNavigationDebug(MenuId.MAIN_MENU, null);
        int choice = InputHandler.getIntInput(1, 2);
        switch (choice) {
            case 1:
                showTopicMenu();
                break;
            case 2:
                exitGame();
                break;
            default:
                ConsoleUI.displayInvalidChoice();
                break;
        }
    }

    private void showTopicMenu() {
        
        showSourceMenu();

        if (navigationManager.current().map(e -> e.getMenuId() == MenuId.MAIN_MENU).orElse(false)) {
            return;
        }

        navigationManager.push(MenuId.TOPIC_MENU, null);

        SourceConfig.SourceType selectedSource = sessionManager.getSourceConfig().getType();
        List<String> availableTopics = TopicScanner.getAvailableTopics(selectedSource);

        if (availableTopics.isEmpty()) {
            ConsoleUI.displayMessage("\nNo topics found for the selected source!");
            ConsoleUI.displayMessage("Please add files to the appropriate directory.");
            try { Thread.sleep(2500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            navigationManager.pop(); 
            return;
        }

        
        ConsoleUI.displayDynamicTopicMenu(availableTopics);
        ConsoleUI.displayNavigationDebug(MenuId.TOPIC_MENU, MenuId.SOURCE_MENU);
        Integer choice = InputHandler.getMenuChoice(1, availableTopics.size() + 1, true);

        if (choice == null) {
           
            navigationManager.goBack();
      
            if (navigationManager.current().map(e -> e.getMenuId() == MenuId.SOURCE_MENU).orElse(false)) {
                showSourceMenuInternal(false);
                return;
            }
            return;
        }

        if (choice == availableTopics.size() + 1) {
            showMixedModeFlow(availableTopics);
        } else {
            
            String selectedTopic = availableTopics.get(choice - 1);
            showDifficultyMenuAndStartRound(selectedTopic);
        }
    }

    /**
     * Handles the Mixed Mode topic selection
     * Allows user to select up to 3 topics or choose "All topics".
     */
    private void showMixedModeFlow(List<String> availableTopics) {
        navigationManager.push(MenuId.MIXED_TOPICS_SELECTION, null);
        ConsoleUI.displayMessage("\n" + ConsoleUI.formatColor("========== MIXED TOPICS MODE ==========", "boldOrange"));
        ConsoleUI.displayNavigationDebug(MenuId.MIXED_TOPICS_SELECTION, MenuId.TOPIC_MENU);
        ConsoleUI.displayMessage("Select up to 3 topics to mix, or type 'ALL' for all topics.");
        ConsoleUI.displayMessage("Enter topic numbers separated by commas (e.g., 1,2,3):\n");
        
        for (int i = 0; i < availableTopics.size(); i++) {
            ConsoleUI.displayMessage((i + 1) + ". " + availableTopics.get(i));
        }
        ConsoleUI.displayMessage("\nType 'ALL' for all topics, or enter numbers (e.g., 1,2,3): ");
        
        String input = InputHandler.getUserInput().trim();
        if (input.equalsIgnoreCase("BACK")) {
            
            navigationManager.goBack();
            return;
        }
        List<String> selectedTopics = new ArrayList<>();
        
        if (input.equalsIgnoreCase("ALL")) {
            // Use all available topics (limit to 3 for initial implementation)
            int limit = Math.min(3, availableTopics.size());
            selectedTopics.addAll(availableTopics.subList(0, limit));
            ConsoleUI.displayMessage("\nSelected all topics (limited to first 3): " + selectedTopics);
        } else {
            // Parse comma-separated input
            String[] parts = input.split(",");
            for (String part : parts) {
                try {
                    int idx = Integer.parseInt(part.trim()) - 1;
                    if (idx >= 0 && idx < availableTopics.size() && selectedTopics.size() < 3) {
                        String topic = availableTopics.get(idx);
                        if (!selectedTopics.contains(topic)) {
                            selectedTopics.add(topic);
                        }
                    }
                } catch (NumberFormatException e) {
                    
                }
            }
        }
        
        if (selectedTopics.isEmpty()) {
            ConsoleUI.displayMessage("\nNo valid topics selected. Returning to menu...");
            try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return;
        }
        
        if (selectedTopics.size() > 3) {
            ConsoleUI.displayMessage("\nToo many topics selected. Limited to first 3.");
            selectedTopics = selectedTopics.subList(0, 3);
        }
        
        ConsoleUI.displayMessage("\nMixed topics: " + selectedTopics);
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Select difficulty for mixed round
        showDifficultyMenuAndStartMixedRound(selectedTopics);
    }

    /**
     * Shows difficulty menu and starts a mixed-topics round.
     */
    private void showDifficultyMenuAndStartMixedRound(List<String> selectedTopics) {
        navigationManager.push(MenuId.MIXED_DIFFICULTY_MENU, selectedTopics);
        ConsoleUI.displayDifficultyMenu();
        ConsoleUI.displayNavigationDebug(MenuId.MIXED_DIFFICULTY_MENU, MenuId.MIXED_TOPICS_SELECTION);
        Integer difficultyChoice = InputHandler.getMenuChoice(1, 3, true);

        if (difficultyChoice == null) {
            // BACK -> return to topic selection
            navigationManager.goBack();
            return;
        }

        String difficulty = "";
        switch (difficultyChoice) {
            case 1:
                difficulty = "Easy";
                break;
            case 2:
                difficulty = "Medium";
                break;
            case 3:
                difficulty = "Hard";
                break;
        }
        
       
        MixedTopicsConfig config = new MixedTopicsConfig.Builder()
            .selectedTopics(selectedTopics)
            .maxTopics(3)
            .mixingStrategy(MixedTopicsConfig.MixingStrategy.RANDOM)
            .difficulty(difficulty)
            .sourceConfig(sessionManager.getSourceConfig())
            .questionsPerRound(5)
            .perTopicLimit(3) // Max 3 questions per topic
            .difficultyMode(MixedTopicsConfig.DifficultyMode.UNIFIED)
            .build();
        
        
        sessionManager.startMixedTopicsRound(config);
        
        
        if (sessionManager.getCurrentRoundQuestionCount() == 0) {
            ConsoleUI.displayMessage("\nNo questions available for the selected topics and difficulty.");
            ConsoleUI.displayMessage("Please ensure the selected source has questions for these topics/difficulty.");
            try { Thread.sleep(2500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return;
        }
        
        playRound();
    }

    private void showSourceMenu() {
        showSourceMenuInternal(true);
    }

  
    private void showSourceMenuInternal(boolean push) {
        if (push) navigationManager.push(MenuId.SOURCE_MENU, null);
        ConsoleUI.displaySourceMenu();
        ConsoleUI.displayNavigationDebug(MenuId.SOURCE_MENU, MenuId.MAIN_MENU);
        Integer choice = InputHandler.getMenuChoice(1, 5, true);

        if (choice == null) {
        
            navigationManager.goBack();
            return;
        }

        SourceConfig config = null;

        switch (choice) {
            case 1:
                // Built-in Hardcoded Questions (default)
                config = new SourceConfig.Builder()
                    .type(SourceConfig.SourceType.BUILTIN_HARDCODED)
                    .build();
                break;
            case 2:
                // Built-in JSON Files
                config = new SourceConfig.Builder()
                    .type(SourceConfig.SourceType.BUILTIN_JSON)
                    .build();
                break;
            case 3:
                // Custom Excel File (uses default path)
                config = new SourceConfig.Builder()
                    .type(SourceConfig.SourceType.CUSTOM_EXCEL)
                    .build();
                break;
            case 4:
                // Custom CSV File (uses default path)
                config = new SourceConfig.Builder()
                    .type(SourceConfig.SourceType.CUSTOM_CSV)
                    .build();
                break;
            case 5:
                // Gemini API
                config = new SourceConfig.Builder()
                    .type(SourceConfig.SourceType.GEMINI_API)
                    .build();
                break;
            default:
                ConsoleUI.displayInvalidChoice();
                config = new SourceConfig.Builder()
                    .type(SourceConfig.SourceType.BUILTIN_HARDCODED)
                    .build();
                break;
        }

        // Set the source config in the session manager
        sessionManager.setSourceConfig(config);
    }

    private void showDifficultyMenuAndStartRound(String topic) {
        navigationManager.push(MenuId.DIFFICULTY_MENU, topic);
        ConsoleUI.displayDifficultyMenu();
        ConsoleUI.displayNavigationDebug(MenuId.DIFFICULTY_MENU, MenuId.TOPIC_MENU);
        Integer difficultyChoice = InputHandler.getMenuChoice(1, 3, true);

        if (difficultyChoice == null) {
            // BACK -> return to topic menu
            navigationManager.goBack();
            return;
        }

        String difficulty = "";
        switch (difficultyChoice) {
            case 1:
                difficulty = "Easy";
                break;
            case 2:
                difficulty = "Medium";
                break;
            case 3:
                difficulty = "Hard";
                break;
        }

        gameService.startNewRound(topic, difficulty);
        
        // check if any questions were loaded before entering the game loop
        if (sessionManager.getCurrentRoundQuestionCount() == 0) {
            ConsoleUI.displayMessage("\nNo questions available for " + topic + " - " + difficulty + ".");
            ConsoleUI.displayMessage("Please ensure the selected source has questions for this topic/difficulty.");
            try { Thread.sleep(2500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return;
        }
        
        playRound();
    }

    private void playRound() {
        navigationManager.push(MenuId.PLAY_ROUND, null);
        while (gameService.hasMoreQuestions() && player.getHp() > 0 && !abortRound) {
            Question currentQuestion = gameService.getCurrentQuestion();
            if (currentQuestion == null) {
                break;
            }
            currentQuestion.shuffleChoices();
            handleQuestion(currentQuestion, false);
            if (abortRound) break;
            gameService.moveToNextQuestion();
        }

    
        if (abortRound) {
            ConsoleUI.displayMessage("\nRound aborted. Returning to main menu...");
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
           
            gameService.rollbackRound();
           
            abortRound = false;
            
            navigationManager.clearToRoot();
            return;
        }

   
        abortRound = false;

        
        if (sessionManager.getCurrentRoundQuestionCount() == 0) {
            ConsoleUI.displayMessage("\nNo questions were loaded for the selected topic/source/difficulty. Returning to main menu...");
            try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            navigationManager.clearToRoot();
            return;
        }

        if (player.getHp() > 0) {
            RoundSummary summary = gameService.completeRoundAndSummarize();
            ConsoleUI.displayRoundVictory(summary.getRoundScore(), summary.getNewGlobalPointsTotal());
        } else {
            handleFinalChance();
        }
        
        
        navigationManager.clearToRoot();
    }

    private void handleQuestion(Question question, boolean isFinalChance) {
        boolean hintUsed = false;
        boolean validAnswer = false;

        while (!validAnswer) {
            ConsoleUI.displayQuestion(player, question, hintUsed, sessionManager.getSourceConfig());
            String input = InputHandler.getUserInput();

            if (input.equalsIgnoreCase("HINT")) {
                if (isFinalChance) {
                    ConsoleUI.displayHintsUnavailableDuringFinalChance();
                    continue;
                }
                if (gameService.useHint()) {
                    ConsoleUI.displayHintConfirmation();
                    String confirmation = InputHandler.getUserInput();
                    if (confirmation.equalsIgnoreCase("Y") || confirmation.equalsIgnoreCase("YES")) {
                        hintUsed = true;
                        continue;
                    } else {
                        gameService.restoreHint();
                        continue;
                    }
                } else {
                    ConsoleUI.displayNoHintsMessage();
                    continue;
                }
            }

            
            if (input.equalsIgnoreCase("EXIT")) {
                ConsoleUI.displayMessage("\nXP gained in this round will be lost. Are you sure you want to end this round? (Y/N): ");
                String confirmation = InputHandler.getUserInput();
                if (confirmation.equalsIgnoreCase("Y") || confirmation.equalsIgnoreCase("YES")) {
                    abortRound = true;
                    return; 
                } else {
                    
                    continue;
                }
            }

            try {
                int answerIndex = Integer.parseInt(input) - 1; // 0-based
                AnswerResult result = gameService.evaluateAnswer(question, answerIndex, isFinalChance);
                if (result.isCorrect()) {
                    ConsoleUI.displayCorrectAnswerFeedback();
                    if (isFinalChance) {
                        ConsoleUI.displayMessage("\nYou restored 30 HP! You can continue.");
                        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    }
                } else {
                    ConsoleUI.displayIncorrectAnswerFeedback(result.getDamageTaken());
                    String correctAnswer = question.getChoices().get(question.getCorrectIndex());
                    ConsoleUI.displayCorrectAnswer(correctAnswer);
                    if (isFinalChance) {
                        ConsoleUI.displayGameOver();
                    }
                }
                validAnswer = true;
            } catch (NumberFormatException e) {
                ConsoleUI.displayMessage("\nInvalid input. Please enter a number or 'HINT'.");
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    private void handleFinalChance() {
        ConsoleUI.displayFinalChancePrompt();
        String topic = sessionManager.getCurrentTopic();
        Question finalChanceQuestion = gameService.getFinalChanceQuestion(topic);

        if (finalChanceQuestion != null) {
            finalChanceQuestion.shuffleChoices();
            handleQuestion(finalChanceQuestion, true);
            if (player.getHp() > 0) {
                ConsoleUI.displayMessage("\nYou survived! Returning to main menu...");
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            } else {
                ConsoleUI.displayGameOver();
            }
        } else {
            ConsoleUI.displayGameOver();
        }
    }

    private void exitGame() {
        ConsoleUI.displayExitConfirmation();
        String confirmation = InputHandler.getUserInput();
        if (confirmation.equalsIgnoreCase("Y") || confirmation.equalsIgnoreCase("YES")) {
            gameRunning = false;
        }
    }
}