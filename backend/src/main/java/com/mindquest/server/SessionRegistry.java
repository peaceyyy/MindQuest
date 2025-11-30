package com.mindquest.server;

import com.mindquest.controller.SessionManager;
import com.mindquest.model.QuestionBank;
import com.mindquest.model.game.Player;
import com.mindquest.service.GameService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized registry for managing game sessions.
 * Thread-safe singleton that stores session ID to GameService mappings.
 */
public class SessionRegistry {

    private final Map<String, GameService> sessions = new ConcurrentHashMap<>();
    private final QuestionBank globalQuestionBank;

    /**
     * Create a new SessionRegistry with the given QuestionBank.
     * 
     * @param globalQuestionBank The shared question bank for all sessions
     */
    public SessionRegistry(QuestionBank globalQuestionBank) {
        this.globalQuestionBank = globalQuestionBank;
    }

    /**
     * Create a new game session.
     * 
     * @return The session ID of the newly created session
     */
    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        Player player = new Player();
        SessionManager sessionManager = new SessionManager(player, globalQuestionBank);
        GameService gameService = new GameService(sessionManager, player, globalQuestionBank);
        
        sessions.put(sessionId, gameService);
        return sessionId;
    }

    /**
     * Get a GameService by session ID.
     * 
     * @param sessionId The session ID
     * @return The GameService, or null if not found
     */
    public GameService getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Remove a session from the registry.
     * 
     * @param sessionId The session ID to remove
     * @return The removed GameService, or null if not found
     */
    public GameService removeSession(String sessionId) {
        return sessions.remove(sessionId);
    }

    /**
     * Get the count of active sessions.
     * 
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Shutdown all sessions gracefully.
     * Should be called during server shutdown.
     */
    public void shutdownAll() {
        sessions.values().forEach(service -> {
            try {
                service.shutdown();
            } catch (Exception e) {
                System.err.println("[SessionRegistry] Error closing service: " + e.getMessage());
            }
        });
        sessions.clear();
    }

    /**
     * Get the global question bank.
     * 
     * @return The shared QuestionBank instance
     */
    public QuestionBank getQuestionBank() {
        return globalQuestionBank;
    }
}
