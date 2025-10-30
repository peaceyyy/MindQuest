package src.controller;

import src.model.Player;
import src.model.Question;
import src.model.QuestionBank;
import src.view.ConsoleUI;

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
        ConsoleUI.displayTopicMenu();
        int choice = InputHandler.getIntInput(1, 4);
        
        String topic = "";
        switch (choice) {
            case 1:
                topic = "Computer Science";
                showDifficultyMenuAndStartRound(topic);
                break;
            case 2:
                topic = "Artificial Intelligence";
                showDifficultyMenuAndStartRound(topic);
                break;
            case 3:
                topic = "Philosophy";
                showDifficultyMenuAndStartRound(topic);
                break;
            case 4:
                ConsoleUI.displayMessage("\nMixed Mode coming soon!");
                try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                break;
            default:
                ConsoleUI.displayInvalidChoice();
                break;
        }
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