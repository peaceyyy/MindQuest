package com.mindquest;

import com.mindquest.controller.GameController;

public class Main {
    public static void main(String[] args) {
        GameController gameController = new GameController();
        try {
            gameController.startGame();
        } catch (IllegalStateException e) {
          
            System.out.println("Input stream closed. Exiting.");
        }
    }
}
