package com.mindquest.server;

import com.mindquest.model.QuestionBank;
import com.mindquest.server.handler.GameplayHandler;
import com.mindquest.server.handler.GeminiHandler;
import com.mindquest.server.handler.LocalLlmHandler;
import com.mindquest.server.handler.SavedSetsHandler;
import com.mindquest.server.handler.SessionHandler;
import com.mindquest.server.handler.UploadHandler;
import io.javalin.Javalin;

import java.util.Map;

/**
 * Main entry point for the MindQuest Game Server..
 */
public class GameServer {

    private final SessionRegistry sessionRegistry;
    private final SessionHandler sessionHandler;
    private final GameplayHandler gameplayHandler;
    private final UploadHandler uploadHandler;
    private final GeminiHandler geminiHandler;
    private final LocalLlmHandler localLlmHandler;
    private final SavedSetsHandler savedSetsHandler;

    public GameServer() {
        // Initialize shared dependencies
        QuestionBank globalQuestionBank = new QuestionBank();
        this.sessionRegistry = new SessionRegistry(globalQuestionBank);

        // Initialize handlers with dependencies
        this.sessionHandler = new SessionHandler(sessionRegistry);
        this.gameplayHandler = new GameplayHandler(sessionRegistry);
        this.uploadHandler = new UploadHandler();
        this.geminiHandler = new GeminiHandler();
        this.localLlmHandler = new LocalLlmHandler();
        this.savedSetsHandler = new SavedSetsHandler();
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }

    public void start() {
        try {
            int port = getPort();
            
            Javalin app = Javalin.create(config -> {
                config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
            }).start(port);

            // Request/Response logging for debugging
            app.before(ctx -> {
                System.out.println("[REQ] " + ctx.method() + " " + ctx.path() + " from " + ctx.ip());
            });
            
            app.after(ctx -> {
                System.out.println("[RES] " + ctx.status() + " " + ctx.method() + " " + ctx.path());
            });

            // Global error handler
            app.exception(Exception.class, (e, ctx) -> {
                System.err.println("[ERROR] Unhandled exception: " + e.getMessage());
                e.printStackTrace();
                ctx.status(500).json(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage()
                ));
            });

            // Register routes
            registerRoutes(app, port);
            
            System.out.println("===================================");
            System.out.println("Game Server started on port " + port);
            System.out.println("Health: http://localhost:" + port + "/health");
            System.out.println("===================================");
            
            // Graceful shutdown hook for cloud platforms
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[SHUTDOWN] Stopping server gracefully...");
                app.stop();
                sessionRegistry.shutdownAll();
                System.out.println("[SHUTDOWN] Server stopped.");
            }));
            
        } catch (Exception e) {
            System.err.println("[FATAL] Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void registerRoutes(Javalin app, int port) {
        // Health check endpoints
        app.get("/", ctx -> ctx.result("MindQuest Game Server is running!"));
        
        app.get("/health", ctx -> ctx.json(Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis(),
            "activeSessions", sessionRegistry.getActiveSessionCount(),
            "port", port
        )));

        // Session management
        app.post("/api/sessions", sessionHandler::createSession);
        app.get("/api/sessions/{id}/state", sessionHandler::getSessionState);
        app.post("/api/sessions/{id}/abandon", sessionHandler::abandonRound);

        // Gameplay
        app.post("/api/sessions/{id}/start", gameplayHandler::startRound);
        app.get("/api/sessions/{id}/question", gameplayHandler::getCurrentQuestion);
        app.post("/api/sessions/{id}/answer", gameplayHandler::submitAnswer);
        app.get("/api/sessions/{id}/hints", gameplayHandler::getHints);
        app.post("/api/sessions/{id}/use-hint", gameplayHandler::useHint);

        // Question Upload & Debug
        app.post("/api/upload/questions", uploadHandler::uploadQuestions);
        app.post("/api/test/load-file", uploadHandler::loadTestFile);
        app.get("/api/debug/list-external", uploadHandler::listExternal);

        // Gemini AI Question Generation
        app.get("/api/gemini/status", geminiHandler::getStatus);
        app.get("/api/gemini/network-test", geminiHandler::testNetwork);
        app.post("/api/gemini/generate", geminiHandler::generateQuestions);

        // Local LLM (LM Studio) Integration
        app.get("/api/llm/providers", localLlmHandler::getProviders);
        app.get("/api/llm/local/status", localLlmHandler::getLocalStatus);
        app.post("/api/llm/local/test", localLlmHandler::testLocalLlm);
        app.post("/api/llm/local/generate", localLlmHandler::generateQuestions);

        // Saved AI Question Sets
        app.get("/api/saved-sets", savedSetsHandler::listSavedSets);
        app.post("/api/saved-sets", savedSetsHandler::saveQuestionSet);
        app.get("/api/saved-sets/{id}/questions", savedSetsHandler::getSavedSetQuestions);
        app.delete("/api/saved-sets/{id}", savedSetsHandler::deleteSavedSet);
    }

    /**
     * Get port from environment variable (for cloud platforms) or fallback to 7070 for local dev.
     */
    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("[WARN] Invalid PORT env var '" + portEnv + "', using default 7070");
            }
        }
        return 7070;
    }
}
