package com.mindquest.server.handler;

import com.mindquest.server.SessionRegistry;
import com.mindquest.service.GameService;
import io.javalin.http.Context;

import java.util.Map;

/**
 * Handler for session lifecycle operations.
 * Manages session creation, state retrieval, and abandonment.
 */
public class SessionHandler {

    private final SessionRegistry sessionRegistry;

    public SessionHandler(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * POST /api/sessions - Create a new game session.
     */
    public void createSession(Context ctx) {
        String sessionId = sessionRegistry.createSession();
        ctx.json(Map.of("sessionId", sessionId));
    }

    /**
     * GET /api/sessions/{id}/state - Get current session state.
     */
    public void getSessionState(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessionRegistry.getSession(sessionId);
        
        if (gameService == null) {
            ctx.status(404).result("Session not found");
            return;
        }
        
        ctx.json(Map.of(
            "globalPoints", gameService.getGlobalPoints(),
            "topic", gameService.getCurrentTopic() != null ? gameService.getCurrentTopic() : "None",
            "hasMore", gameService.hasMoreQuestions()
        ));
    }

    /**
     * POST /api/sessions/{id}/abandon - Abandon the current round.
     */
    public void abandonRound(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessionRegistry.getSession(sessionId);
        
        if (gameService == null) {
            ctx.status(404).result("Session not found");
            return;
        }
        
        // Call backend to rollback the round (no points awarded)
        gameService.rollbackRound();
        
        ctx.json(Map.of(
            "message", "Round abandoned successfully",
            "globalPoints", gameService.getGlobalPoints()
        ));
    }
}
