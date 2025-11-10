package com.mindquest.controller;

import java.util.Scanner;

public class InputHandler {
    private static Scanner scanner = new Scanner(System.in);

    public static String getUserInput() {
        return scanner.nextLine().trim();
    }

    // For "Press Enter to continue" prompts 
    public static void waitForEnter() {
        try {
            // Small delay to prevent accidental double-press from previous input
            Thread.sleep(100);
            scanner.nextLine();
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

    public static void closeScanner() {
        scanner.close();
    }
}