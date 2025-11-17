package com.mindquest;

import com.mindquest.controller.GameController;

public class Main {
    public static void main(String[] args) {
        GameController gameController = new GameController();
        try {
            gameController.startGame();
        } catch (IllegalStateException e) {
            // Likely EOF on System.in (non-interactive run). Exit gracefully.
            System.out.println("Input stream closed. Exiting.");
        }
    }
}
