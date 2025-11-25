package com.mindquest.server;

import com.mindquest.controller.SessionManager;
import com.mindquest.model.QuestionBank;
import com.mindquest.model.game.Player;
import com.mindquest.model.question.Question;
import com.mindquest.service.GameService;
import com.mindquest.service.dto.AnswerResult;
import com.mindquest.service.dto.RoundSummary;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {
    // Map sessionId -> GameService (which holds Player and SessionManager)
    private static final Map<String, GameService> sessions = new ConcurrentHashMap<>();
    private static final QuestionBank globalQuestionBank = new QuestionBank();

    public static void main(String[] args) {
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

            // Health check endpoints
            app.get("/", ctx -> ctx.result("MindQuest Game Server is running!"));
            
            app.get("/health", ctx -> ctx.json(Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis(),
                "activeSessions", sessions.size(),
                "port", port
            )));

            // Session management
            app.post("/api/sessions", GameServer::createSession);
            
            // Game flow
            app.post("/api/sessions/{id}/start", GameServer::startRound);
            app.get("/api/sessions/{id}/question", GameServer::getCurrentQuestion);
            app.post("/api/sessions/{id}/answer", GameServer::submitAnswer);
            app.get("/api/sessions/{id}/state", GameServer::getSessionState);
            app.post("/api/sessions/{id}/abandon", GameServer::abandonRound);
            
            System.out.println("===================================");
            System.out.println("Game Server started on port " + port);
            System.out.println("Health: http://localhost:" + port + "/health");
            System.out.println("===================================");
            
            // Graceful shutdown hook for cloud platforms
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[SHUTDOWN] Stopping server gracefully...");
                app.stop();
                // Clean up any background resources
                sessions.values().forEach(service -> {
                    try {
                        service.shutdown();
                    } catch (Exception e) {
                        System.err.println("[SHUTDOWN] Error closing service: " + e.getMessage());
                    }
                });
                System.out.println("[SHUTDOWN] Server stopped.");
            }));
            
            // Javalin's Jetty server runs in background threads, no need to block main thread
            
        } catch (Exception e) {
            System.err.println("[FATAL] Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void createSession(Context ctx) {
        String sessionId = UUID.randomUUID().toString();
        Player player = new Player();
        SessionManager sessionManager = new SessionManager(player, globalQuestionBank);
        GameService gameService = new GameService(sessionManager, player, globalQuestionBank);
        
        sessions.put(sessionId, gameService);
        ctx.json(Map.of("sessionId", sessionId));
    }

    private static void startRound(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessions.get(sessionId);
        if (gameService == null) {
            ctx.status(404).result("Session not found");
            return;
        }
        
        StartRequest req = ctx.bodyAsClass(StartRequest.class);
        
        if (req.topic == null || req.difficulty == null) {
            ctx.status(400).result("Missing topic or difficulty");
            return;
        }

        // Normalize topic and difficulty to match QuestionBank format
        String normalizedTopic = normalizeTopic(req.topic);
        String normalizedDifficulty = normalizeDifficulty(req.difficulty);

        gameService.startNewRound(normalizedTopic, normalizedDifficulty);
        ctx.json(Map.of("message", "Round started", "topic", normalizedTopic, "difficulty", normalizedDifficulty));
    }

    private static void getCurrentQuestion(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessions.get(sessionId);
        if (gameService == null) {
            ctx.status(404).result("Session not found");
            return;
        }
        
        Question q = gameService.getCurrentQuestion();
        if (q == null) {
            // Check if round is over or just not started
            if (!gameService.hasMoreQuestions()) {
                 ctx.status(204).result("Round complete");
            } else {
                 ctx.status(404).result("No question available");
            }
            return;
        }
        
        // DIAGNOSTIC LOGGING: Log question details before sending
        try {
            System.out.println("[DEBUG] Sending question ID: " + q.getId());
            System.out.println("[DEBUG] Question text: " + q.getQuestionText());
            System.out.println("[DEBUG] Choices count: " + q.getChoices().size());
            System.out.println("[DEBUG] Correct index: " + q.getCorrectIndex());
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to log question details: " + e.getMessage());
        }
        
        ctx.json(q);
    }

    private static void submitAnswer(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessions.get(sessionId);
        if (gameService == null) {
            ctx.status(404).result("Session not found");
            return;
        }
        
        AnswerRequest req;
        try {
            req = ctx.bodyAsClass(AnswerRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "Invalid request format", "message", e.getMessage()));
            return;
        }
        
        Question q = gameService.getCurrentQuestion();
        if (q == null) {
            ctx.status(400).result("No active question");
            return;
        }
        
        // Map letter answer (A/B/C/D) to index (0-3) if provided as string
        int answerIndex = req.index;
        if (req.answer != null && !req.answer.isEmpty()) {
            answerIndex = letterToIndex(req.answer);
            if (answerIndex == -1) {
                ctx.status(400).json(Map.of("error", "Invalid answer format", "message", "Answer must be A, B, C, D or index 0-3"));
                return;
            }
        }
        
        System.out.println("[DEBUG] Received answer: " + (req.answer != null ? req.answer : req.index) + " -> index: " + answerIndex);
        
        AnswerResult result = gameService.evaluateAnswer(q, answerIndex, false); // Assuming not final chance for now
        gameService.moveToNextQuestion();
        
        // Check if round ended
        boolean roundComplete = !gameService.hasMoreQuestions();
        RoundSummary summary = null;
        if (roundComplete) {
             summary = gameService.completeRoundAndSummarize();
        }

        ctx.json(Map.of(
            "correct", result.isCorrect(),
            "pointsAwarded", result.getPointsAwarded(),
            "damageTaken", result.getDamageTaken(),
            "currentHp", result.getPlayerHpAfter(),
            "correctIndex", q.getCorrectIndex(),
            "roundComplete", roundComplete,
            "summary", summary != null ? summary : "null"
        ));
    }

    private static void getSessionState(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessions.get(sessionId);
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

    private static void abandonRound(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessions.get(sessionId);
        if (gameService == null) {
            ctx.status(404).result("Session not found");
            return;
        }
        
        // Rollback the round (no points awarded)
        gameService.rollbackRound();
        
        ctx.json(Map.of(
            "message", "Round abandoned successfully",
            "globalPoints", gameService.getGlobalPoints()
        ));
    }

    // DTOs
    public static class StartRequest {
        public String topic;
        public String difficulty;
    }

    public static class AnswerRequest {
        public int index;  // Legacy numeric index (0-3)
        public String answer; // Letter answer (A/B/C/D)
    }

    /**
     * Convert letter answer (A/B/C/D) to zero-based index (0-3).
     * Returns -1 if invalid.
     */
    private static int letterToIndex(String letter) {
        if (letter == null || letter.isEmpty()) {
            return -1;
        }
        
        String upper = letter.trim().toUpperCase();
        switch (upper) {
            case "A": return 0;
            case "B": return 1;
            case "C": return 2;
            case "D": return 3;
            default: return -1;
        }
    }

    /**
     * Get port from environment variable (for GCP Cloud Run, Render, etc.) or fallback to 7070 for local dev.
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
        return 7070; // local development fallback
    }

    // Normalization helpers to convert user-friendly input to QuestionBank format
    private static String normalizeTopic(String topic) {
        if (topic == null) return null;
        
        String lower = topic.toLowerCase().trim();
        
        // Map short forms and variations to full names
        switch (lower) {
            case "ai":
            case "artificial intelligence":
                return "Artificial Intelligence";
            case "cs":
            case "computer science":
                return "Computer Science";
            case "philosophy":
            case "phil":
                return "Philosophy";
            default:
                // Capitalize first letter of each word for unknown topics
                return capitalizeWords(topic.trim());
        }
    }

    private static String normalizeDifficulty(String difficulty) {
        if (difficulty == null) return null;
        
        String lower = difficulty.toLowerCase().trim();
        
        // Capitalize first letter: easy -> Easy, medium -> Medium, hard -> Hard
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }

    private static String capitalizeWords(String input) {
        String[] words = input.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
}
