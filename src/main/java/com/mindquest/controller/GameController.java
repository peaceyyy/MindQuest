package com.mindquest.controller;

import com.mindquest.loader.SourceConfig;
import com.mindquest.loader.TopicScanner;
import com.mindquest.view.ConsoleUI;
import com.mindquest.model.Player;
import com.mindquest.model.Question;
import com.mindquest.model.QuestionBank;

import java.util.List;

public class GameController {
    private SessionManager sessionManager;
    private Player player;
    private QuestionBank questionBank;
    private boolean gameRunning;

    public GameController() {
        this.player = new Player();
        this.questionBank = new QuestionBank();
        this.sessionManager = new SessionManager(player, questionBank);
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
                // Gemini API (not implemented yet)
                ConsoleUI.displayMessage("\nGemini API loader coming soon! Using default hardcoded questions.");
                try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                config = new SourceConfig.Builder()
                    .type(SourceConfig.SourceType.BUILTIN_HARDCODED)
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
        
        sessionManager.startNewRound(topic, difficulty);
        playRound();
    }

    private void playRound() {
        while (sessionManager.hasMoreQuestions() && player.getHp() > 0) {
            Question currentQuestion = sessionManager.getCurrentQuestion();
            if (currentQuestion == null) {
                break;
            }
            
            // Shuffle choices before displaying
            currentQuestion.shuffleChoices();
            
            handleQuestion(currentQuestion, false);
            sessionManager.moveToNextQuestion();
        }
        
        // Round complete or HP reached zero
        if (player.getHp() > 0) {
            // Round victory - calculate final score with HP bonus
            int hpBonus = (int) (player.getHp() * 0.5);
            int roundScore = player.getScore() + hpBonus;
            player.addScore(hpBonus); // Add HP bonus to player's round score
            
            // Add the round score to global points
            sessionManager.addToGlobalPoints(roundScore);
            
            ConsoleUI.displayRoundVictory(roundScore, sessionManager.getGlobalPoints());
        } else {
            // HP is zero - trigger Final Chance
            handleFinalChance();
        }
    }

    private void handleQuestion(Question question, boolean isFinalChance) {
        boolean hintUsed = false;
        boolean validAnswer = false;
        
        while (!validAnswer) {
            ConsoleUI.displayQuestion(player, question, hintUsed);
            String input = InputHandler.getUserInput();
            
            // Check if user wants to use a hint
            if (input.equalsIgnoreCase("HINT")) {
                if (isFinalChance) {
                    ConsoleUI.displayHintsUnavailableDuringFinalChance();
                    continue;
                }
                
                if (player.useHint()) {
                    ConsoleUI.displayHintConfirmation();
                    String confirmation = InputHandler.getUserInput();
                    if (confirmation.equalsIgnoreCase("Y") || confirmation.equalsIgnoreCase("YES")) {
                        hintUsed = true;
                        continue; // Re-display question with hint
                    } else {
                        player.restoreHint(); // Restore the hint (user declined)
                        continue;
                    }
                } else {
                    ConsoleUI.displayNoHintsMessage();
                    continue;
                }
            }
            
            // Try to parse the answer
            try {
                int answerIndex = Integer.parseInt(input) - 1; // Convert to 0-based index
                
                if (answerIndex == question.getCorrectIndex()) {
                    // Correct answer
                    int points = question.calculateScore();
                    player.addScore(points);
                    ConsoleUI.displayCorrectAnswerFeedback();
                    
                    if (isFinalChance) {
                        player.restoreHp(30); // Restore 30 HP on Final Chance success
                        ConsoleUI.displayMessage("\nYou restored 30 HP! You can continue.");
                        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    }
                    validAnswer = true;
                } else {
                    // Incorrect answer
                    int damage = question.calculateDamage();
                    player.takeDamage(damage);
                    ConsoleUI.displayIncorrectAnswerFeedback(damage);
                    
                    // Show the correct answer
                    String correctAnswer = question.getChoices().get(question.getCorrectIndex());
                    ConsoleUI.displayCorrectAnswer(correctAnswer);
                    
                    if (isFinalChance) {
                        ConsoleUI.displayGameOver();
                    }
                    validAnswer = true;
                }
            } catch (NumberFormatException e) {
                ConsoleUI.displayMessage("\nInvalid input. Please enter a number or 'HINT'.");
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    private void handleFinalChance() {
        ConsoleUI.displayFinalChancePrompt();
        
        // Get a random Hard question from current topic or any topic if Mixed Mode
        String topic = sessionManager.getCurrentTopic();
        Question finalChanceQuestion = getFinalChanceQuestion(topic);
        
        if (finalChanceQuestion != null) {
            finalChanceQuestion.shuffleChoices();
            handleQuestion(finalChanceQuestion, true);
            
            // Check if player survived Final Chance
            if (player.getHp() > 0) {
                // Player restored HP and can continue
                // For now, we'll just return to main menu
                ConsoleUI.displayMessage("\nYou survived! Returning to main menu...");
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            } else {
                // Player failed Final Chance
                ConsoleUI.displayGameOver();
            }
        } else {
            ConsoleUI.displayGameOver();
        }
    }

    private Question getFinalChanceQuestion(String topic) {
        // Get a random Hard question from the specified topic
        var hardQuestions = questionBank.getQuestionsByTopicAndDifficulty(topic, "Hard");
        if (hardQuestions != null && !hardQuestions.isEmpty()) {
            int randomIndex = (int) (Math.random() * hardQuestions.size());
            return hardQuestions.get(randomIndex);
        }
        return null;
    }

    private void exitGame() {
        ConsoleUI.displayExitConfirmation();
        String confirmation = InputHandler.getUserInput();
        if (confirmation.equalsIgnoreCase("Y") || confirmation.equalsIgnoreCase("YES")) {
            gameRunning = false;
        }
    }
}