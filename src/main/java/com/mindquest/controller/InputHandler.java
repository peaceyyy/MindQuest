package com.mindquest.controller;

import java.util.Scanner;

public class InputHandler {
    private static Scanner scanner = new Scanner(System.in);

    public static String getUserInput() {
        if (!scanner.hasNextLine()) {
            return null;
        }
        String line = scanner.nextLine();
        if (line == null) return null;
        return line.trim();
    }

    // For "Press Enter to continue" prompts 
    public static void waitForEnter() {
        try {
            // Small delay to prevent accidental double-press from previous input
            Thread.sleep(100);
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static int getIntInput(int min, int max) {
        int choice = -1;
        boolean validInput = false;
        while (!validInput) {
            try {
                String input = getUserInput();
                if (input == null) {
                    throw new IllegalStateException("No input available (EOF)");
                }
                choice = Integer.parseInt(input);
                if (choice >= min && choice <= max) {
                    validInput = true;
                } else {
                    System.out.println("Input out of range. Please enter a number between " + min + " and " + max + ".");
                    Thread.sleep(1000); 

                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                try {
                    Thread.sleep(1000); // Pause to let user read message
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return choice;
    }

    /**
     * Read a menu choice, allowing an optional 'BACK' command.
     * Returns null when the user typed BACK and allowBack is true.
     */
    public static Integer getMenuChoice(int min, int max, boolean allowBack) {
        while (true) {
            try {
                String input = getUserInput();
                if (input == null) {
                    throw new IllegalStateException("No input available (EOF)");
                }
                if (allowBack && input.equalsIgnoreCase("BACK")) {
                    return null;
                }
                int choice = Integer.parseInt(input);
                if (choice >= min && choice <= max) {
                    return choice;
                }
                System.out.println("Input out of range. Please enter a number between " + min + " and " + max + ".");
                Thread.sleep(1000);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void closeScanner() {
        scanner.close();
    }
}