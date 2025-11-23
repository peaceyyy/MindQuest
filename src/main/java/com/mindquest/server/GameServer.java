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
            Javalin app = Javalin.create(config -> {
                config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
            }).start(7070);

            // Health check
            app.get("/", ctx -> ctx.result("MindQuest Game Server is running!"));

            // Session management
            app.post("/api/sessions", GameServer::createSession);
            
            // Game flow
            app.post("/api/sessions/{id}/start", GameServer::startRound);
            app.get("/api/sessions/{id}/question", GameServer::getCurrentQuestion);
            app.post("/api/sessions/{id}/answer", GameServer::submitAnswer);
            app.get("/api/sessions/{id}/state", GameServer::getSessionState);
            
            System.out.println("Game Server started on port 7070");
            
            // Keep the main thread alive just in case
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
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

        gameService.startNewRound(req.topic, req.difficulty);
        ctx.json(Map.of("message", "Round started", "topic", req.topic, "difficulty", req.difficulty));
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
        
        ctx.json(q);
    }

    private static void submitAnswer(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessions.get(sessionId);
        if (gameService == null) {
            ctx.status(404).result("Session not found");
            return;
        }
        
        AnswerRequest req = ctx.bodyAsClass(AnswerRequest.class);
        
        Question q = gameService.getCurrentQuestion();
        if (q == null) {
            ctx.status(400).result("No active question");
            return;
        }
        
        AnswerResult result = gameService.evaluateAnswer(q, req.index, false); // Assuming not final chance for now
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

    // DTOs
    public static class StartRequest {
        public String topic;
        public String difficulty;
    }

    public static class AnswerRequest {
        public int index;
    }
}
