package com.mindquest.view;

import com.mindquest.util.ColorUtils;
import com.mindquest.util.ConsoleUtils;
import com.mindquest.config.GameConfig;
import com.mindquest.controller.InputHandler;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.model.game.Player;
import com.mindquest.model.question.Question;

import java.util.List;

public class ConsoleUI {

    public static void clearScreen() {
        ConsoleUtils.clearScreen();
    }

    public static void displayWelcome() {
        clearScreen();
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.boldOrange("*") + ColorUtils.boldYellow("        Welcome to MindQuest!        ") + ColorUtils.boldOrange("*"));
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.yellow("\nPrepare to test your knowledge!"));
        System.out.println("\nPress Enter to continue...");
        InputHandler.waitForEnter(); // Use debounced input
    }

    public static void displayMainMenu(int globalPoints) {
        clearScreen();
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.boldOrange("*") + ColorUtils.boldYellow("           MAIN MENU                 ") + ColorUtils.boldOrange("*"));
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.boldYellow("Total Points: ") + ColorUtils.orange(String.valueOf(globalPoints)));
        System.out.println(ColorUtils.orange("-----------------------------------------"));
        System.out.println(ColorUtils.yellow("1.") + " Start New Game");
        System.out.println(ColorUtils.yellow("2.") + " Exit Game");
        System.out.print("\n" + ColorUtils.boldYellow("Enter your choice: "));
    }

    public static void displayTopicMenu() {
        clearScreen();
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.boldOrange("*") + ColorUtils.boldYellow("         SELECT TOPIC                ") + ColorUtils.boldOrange("*"));
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.yellow("1.") + " Computer Science");
        System.out.println(ColorUtils.yellow("2.") + " Artificial Intelligence");
        System.out.println(ColorUtils.yellow("3.") + " Philosophy");
        System.out.println(ColorUtils.yellow("4.") + " Mixed Mode " + ColorUtils.orange("(Coming Soon)"));
        System.out.print("\n" + ColorUtils.boldYellow("Enter your choice: "));
    }
    
    public static void displayDynamicTopicMenu(List<String> topics) {
        clearScreen();
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.boldOrange("*") + ColorUtils.boldYellow("         SELECT TOPIC                ") + ColorUtils.boldOrange("*"));
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        
        for (int i = 0; i < topics.size(); i++) {
            System.out.println(ColorUtils.yellow((i + 1) + ".") + " " + formatTopicName(topics.get(i)));
        }
        
        // Always show Mixed Mode option at the end
        System.out.println(ColorUtils.yellow((topics.size() + 1) + ".") + " Mixed Topics" + ColorUtils.orange("(Coming Soon)"));
        
        System.out.print("\n" + ColorUtils.boldYellow("Enter your choice: "));
    }
    
    /**
     * Formats topic names for display (converts lowercase filenames to Title Case).
     */
    private static String formatTopicName(String topic) {
        // Special case mappings
        if (topic.equals("cs")) return "Computer Science";
        if (topic.equals("ai")) return "Artificial Intelligence";
        
        // Convert to title case (e.g., "philosophy" -> "Philosophy")
        if (topic.isEmpty()) return topic;
        return topic.substring(0, 1).toUpperCase() + topic.substring(1).toLowerCase();
    }

    public static void displayDifficultyMenu() {
        clearScreen();
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.boldOrange("*") + ColorUtils.boldYellow("        SELECT DIFFICULTY            ") + ColorUtils.boldOrange("*"));
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.yellow("1.") + " Easy");
        System.out.println(ColorUtils.yellow("2.") + " Medium");
        System.out.println(ColorUtils.yellow("3.") + " Hard");
        System.out.print("\n" + ColorUtils.boldYellow("Enter your choice: "));
    }

    public static void displaySourceMenu() {
        clearScreen();
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.boldOrange("*") + ColorUtils.boldYellow("       SELECT QUESTION SOURCE        ") + ColorUtils.boldOrange("*"));
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.yellow("1.") + " Built-in Hardcoded Questions " + ColorUtils.orange("(Default)"));
        System.out.println(ColorUtils.yellow("2.") + " Built-in JSON Files");
        System.out.println(ColorUtils.yellow("3.") + " Custom Excel File (.xlsx)");
        System.out.println(ColorUtils.yellow("4.") + " Custom CSV File");
        System.out.println(ColorUtils.yellow("5.") + " Gemini AI Generated");
        System.out.println("\n" + ColorUtils.orange("Note: For custom files, place topic-based files in:"));
        System.out.println(ColorUtils.orange("  - CSV:   src/questions/external_source/csv/{topic}.csv"));
        System.out.println(ColorUtils.orange("           (e.g., ai.csv, cs.csv, philosophy.csv)"));
        System.out.println(ColorUtils.orange("  - Excel: src/questions/external_source/xlsx/{topic}.xlsx"));
        System.out.println(ColorUtils.orange("           (e.g., ai.xlsx, cs.xlsx, philosophy.xlsx)"));
        System.out.println(ColorUtils.orange("  - Gemini: Requires GEMINI_API_KEY in .env file"));
        System.out.print("\n" + ColorUtils.boldYellow("Enter your choice: "));
    }

    public static void displayQuestion(Player player, Question question, boolean hintUsed, SourceConfig sourceConfig) {
        clearScreen();
        displayHUD(player);
        System.out.println("\n" + question.getQuestionText());
        List<String> choicesToDisplay = hintUsed ? question.removeIncorrectOptions() : question.getChoices();

        for (int i = 0; i < choicesToDisplay.size(); i++) {
            System.out.println(String.format("%d. %s", (i + 1), choicesToDisplay.get(i)));
        }
        System.out.println("\nType HINT to remove half of the incorrect options (Remaining: " + player.getHints() + ")");
        System.out.println(ColorUtils.orange("Tip: Type 'EXIT' to abandon this round and return to the main menu."));
        System.out.print("Enter your answer (1-" + choicesToDisplay.size() + "): ");

        if (GameConfig.DEBUG) {
            System.out.println("\n[DEBUG] Correct Answer Index: " + (question.getCorrectIndex() + 1));
            System.out.println("[DEBUG] Question ID: " + question.getId());
            if (sourceConfig != null) {
                System.out.println("[DEBUG] Question Source: " + getSourceTypeName(sourceConfig.getType()));
            }
        }
    }

    public static void displayHUD(Player player) {
        System.out.println(ColorUtils.orange("-----------------------------------------"));
        System.out.println(ColorUtils.yellow("HP: ") + ColorUtils.boldYellow(String.valueOf(player.getHp())) + 
                          ColorUtils.yellow(" | Hints: ") + ColorUtils.boldYellow(String.valueOf(player.getHints())) + 
                          ColorUtils.yellow(" | Score: ") + ColorUtils.boldYellow(String.valueOf(player.getScore())));
        System.out.println(ColorUtils.orange("-----------------------------------------"));
    }

    public static void displayCorrectAnswerFeedback() {
        System.out.println("\nCorrect! Well done!");
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void displayIncorrectAnswerFeedback(int damage) {
        System.out.println("\n" + ColorUtils.orange("Incorrect!") + " You lose " + ColorUtils.boldYellow(String.valueOf(damage)) + " HP.");
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void displayCorrectAnswer(String correctAnswer) {
        System.out.println(ColorUtils.yellow("\nThe correct answer was: ") + ColorUtils.boldOrange(correctAnswer));
        try { Thread.sleep(2500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void displayNoHintsMessage() {
        System.out.println("\nNo hints remaining!");
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void displayHintConfirmation() {
        System.out.print("\nWould you like to use your HINT? (Y/N): ");
    }

    public static void displayHintsUnavailableDuringFinalChance() {
        System.out.println("\nHints unavailable during Final Chance.");
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void displayGameOver() {
        clearScreen();
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.boldOrange("*") + ColorUtils.boldYellow("            GAME OVER                ") + ColorUtils.boldOrange("*"));
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println("\nYour HP dropped to zero.");
        System.out.println("Press Enter to continue...");
        InputHandler.waitForEnter(); // Use debounced input
    }

    public static void displayRoundVictory(int roundScore, int globalPoints) {
        clearScreen();
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.boldOrange("*") + ColorUtils.boldYellow("          ROUND COMPLETE!            ") + ColorUtils.boldOrange("*"));
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.yellow("\nCongratulations! You cleared the round."));
        System.out.println(ColorUtils.boldYellow("Round Score: ") + ColorUtils.orange(String.valueOf(roundScore)));
        System.out.println(ColorUtils.boldYellow("Total Points: ") + ColorUtils.orange(String.valueOf(globalPoints)));
        System.out.println("Press Enter to continue...");
        InputHandler.waitForEnter(); // Use debounced input
    }

    public static void displayFinalChancePrompt() {
        clearScreen();
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.boldOrange("*") + ColorUtils.boldYellow("          FINAL CHANCE!              ") + ColorUtils.boldOrange("*"));
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.yellow("\nYour HP is at zero! One last question to regain some HP."));
        System.out.println("Press Enter to continue...");
        InputHandler.waitForEnter(); // Use debounced input
    }

    public static void displayExitConfirmation() {
        System.out.print("\nAre you sure you want to exit the game? (Y/N): ");
    }

    public static void displayGoodbye() {
        clearScreen();
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        System.out.println(ColorUtils.boldOrange("*") + ColorUtils.boldYellow("          Thanks for playing!        ") + ColorUtils.boldOrange("*"));
        System.out.println(ColorUtils.boldOrange("*****************************************"));
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void displayInvalidChoice() {
        System.out.println("\nInvalid choice. Please try again.");
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void displayMessage(String message) {
        System.out.println(message);
    }
    
    /**
     * Helper method to get a human-readable name for the source type.
     */
    private static String getSourceTypeName(SourceConfig.SourceType sourceType) {
        if (sourceType == null) return "Unknown";
        
        switch (sourceType) {
            case BUILTIN_HARDCODED:
                return "Built-in Hardcoded Questions";
            case BUILTIN_JSON:
                return "Built-in JSON Files";
            case CUSTOM_CSV:
                return "Custom CSV File";
            case CUSTOM_EXCEL:
                return "Custom Excel File";
            case GEMINI_API:
                return "Gemini AI Generated";
            default:
                return "Unknown Source";
        }
    }
}