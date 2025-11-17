package com.mindquest.controller;

import com.mindquest.loader.config.SourceConfig;
import com.mindquest.loader.TopicScanner;
import com.mindquest.view.ConsoleUI;
import com.mindquest.model.game.Player;
import com.mindquest.model.question.Question;
import com.mindquest.model.QuestionBank;
import com.mindquest.service.GameService;
import com.mindquest.service.dto.AnswerResult;
import com.mindquest.service.dto.RoundSummary;

import java.util.List;

public class GameController {
    private SessionManager sessionManager;
    private Player player;
    private QuestionBank questionBank;
    private GameService gameService;
    private boolean gameRunning;
    private boolean abortRound = false;

    public GameController() {
        this.player = new Player();
        this.questionBank = new QuestionBank();
        this.sessionManager = new SessionManager(player, questionBank);
        this.gameService = new GameService(sessionManager, player, questionBank);
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
        ConsoleUI.displayMainMenu(sessionManager.getGlobalPoints());
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
        // First, let user select the question source
        showSourceMenu();

        // Get available topics based on selected source
        SourceConfig.SourceType selectedSource = sessionManager.getSourceConfig().getType();
        List<String> availableTopics = TopicScanner.getAvailableTopics(selectedSource);

        if (availableTopics.isEmpty()) {
            ConsoleUI.displayMessage("\nNo topics found for the selected source!");
            ConsoleUI.displayMessage("Please add files to the appropriate directory.");
            try { Thread.sleep(2500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return;
        }

        // Display dynamic topic menu
        ConsoleUI.displayDynamicTopicMenu(availableTopics);
        int choice = InputHandler.getIntInput(1, availableTopics.size() + 1);

        if (choice == availableTopics.size() + 1) {
            // Mixed Mode (coming soon)
            ConsoleUI.displayMessage("\nMixed Mode coming soon!");
            try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } else {
            // Valid topic selection
            String selectedTopic = availableTopics.get(choice - 1);
            showDifficultyMenuAndStartRound(selectedTopic);
        }
    }

    private void showSourceMenu() {
        ConsoleUI.displaySourceMenu();
        int choice = InputHandler.getIntInput(1, 5);

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
        ConsoleUI.displayDifficultyMenu();
        int difficultyChoice = InputHandler.getIntInput(1, 3);

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
        playRound();
    }

    private void playRound() {
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

        // If the round was aborted by the player, do not award round XP — just return to menu
        if (abortRound) {
            ConsoleUI.displayMessage("\nRound aborted. Returning to main menu...");
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            // Rollback any provisional changes made during this round
            gameService.rollbackRound();
            // Reset abort flag for subsequent rounds
            abortRound = false;
            return;
        }

        // Reset abort flag for subsequent rounds
        abortRound = false;

        if (player.getHp() > 0) {
            RoundSummary summary = gameService.completeRoundAndSummarize();
            ConsoleUI.displayRoundVictory(summary.getRoundScore(), summary.getNewGlobalPointsTotal());
        } else {
            handleFinalChance();
        }
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

            // Allow user to exit the current session and return to main menu
            if (input.equalsIgnoreCase("EXIT")) {
                ConsoleUI.displayMessage("\nXP gained in this round will be lost. Are you sure you want to end this round? (Y/N): ");
                String confirmation = InputHandler.getUserInput();
                if (confirmation.equalsIgnoreCase("Y") || confirmation.equalsIgnoreCase("YES")) {
                    abortRound = true;
                    return; // leave question handling and bubble up to playRound
                } else {
                    // Continue the question if user declined
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