package com.mindquest.server;

import com.mindquest.controller.SessionManager;
import com.mindquest.model.QuestionBank;
import com.mindquest.model.game.Player;
import com.mindquest.model.question.Question;
import com.mindquest.service.GameService;
import com.mindquest.service.dto.AnswerResult;
import com.mindquest.loader.TopicScanner;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.service.dto.RoundSummary;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
            
            // Question Upload
            app.post("/api/upload/questions", GameServer::uploadQuestions);
            
            // Dev: Quick test file loading
            app.post("/api/test/load-file", GameServer::loadTestFile);
            // Debug: list external topics/files seen by the TopicScanner
            app.get("/api/debug/list-external", GameServer::listExternal);

            // Game flow
            app.post("/api/sessions/{id}/start", GameServer::startRound);
            app.get("/api/sessions/{id}/question", GameServer::getCurrentQuestion);
            app.post("/api/sessions/{id}/answer", GameServer::submitAnswer);
            app.get("/api/sessions/{id}/state", GameServer::getSessionState);
            app.get("/api/sessions/{id}/hints", GameServer::getHints);
            app.post("/api/sessions/{id}/use-hint", GameServer::useHint);
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
        
        // Check for custom sources
        // We check against the "loader-friendly" filename format (lowercase, underscores)
        String checkTopic = normalizedTopic.toLowerCase().replace(" ", "_");
        SourceConfig config = null;
        
        if (TopicScanner.topicExists(checkTopic, SourceConfig.SourceType.CUSTOM_CSV)) {
            config = new SourceConfig.Builder()
                .type(SourceConfig.SourceType.CUSTOM_CSV)
                .topic(normalizedTopic)
                .difficulty(normalizedDifficulty)
                .build();
        } else if (TopicScanner.topicExists(checkTopic, SourceConfig.SourceType.CUSTOM_EXCEL)) {
            config = new SourceConfig.Builder()
                .type(SourceConfig.SourceType.CUSTOM_EXCEL)
                .topic(normalizedTopic)
                .difficulty(normalizedDifficulty)
                .build();
        } else if (TopicScanner.topicExists(checkTopic, SourceConfig.SourceType.CUSTOM_JSON)) {
            config = new SourceConfig.Builder()
                .type(SourceConfig.SourceType.CUSTOM_JSON)
                .topic(normalizedTopic)
                .difficulty(normalizedDifficulty)
                .build();
        }
        
        if (config != null) {
            gameService.setSourceConfig(config);
            System.out.println("[GameServer] Using custom source: " + config.getType() + " for topic " + normalizedTopic);
        } else {
            gameService.setSourceConfig(null);
        }

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
        
        System.out.println("[DEBUG] Received answer: " + (req.answer != null ? req.answer : req.index) + " -> index: " + answerIndex + ", time: " + req.answerTimeMs + "ms");
        
        AnswerResult result = gameService.evaluateAnswer(q, answerIndex, false, req.answerTimeMs);
        gameService.moveToNextQuestion();
        
        // Check if round ended
        boolean roundComplete = !gameService.hasMoreQuestions();
        RoundSummary summary = null;
        if (roundComplete) {
             summary = gameService.completeRoundAndSummarize();
        }

        ctx.json(Map.ofEntries(
            Map.entry("correct", result.isCorrect()),
            Map.entry("pointsAwarded", result.getPointsAwarded()),
            Map.entry("damageTaken", result.getDamageTaken()),
            Map.entry("currentHp", result.getPlayerHpAfter()),
            Map.entry("correctIndex", q.getCorrectIndex()),
            Map.entry("roundComplete", roundComplete),
            Map.entry("isCritical", result.isCritical()),
            Map.entry("isCounterattack", result.isCounterattack()),
            Map.entry("correctAnswers", result.getCorrectAnswers()),
            Map.entry("incorrectAnswers", result.getIncorrectAnswers()),
            Map.entry("currentAccuracy", result.getCurrentAccuracy()),
            Map.entry("correctStreak", result.getCorrectStreak()),
            Map.entry("wrongStreak", result.getWrongStreak()),
            Map.entry("isHotStreak", result.isHotStreak()),
            Map.entry("summary", summary != null ? summary : "null")
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
        
        // Call backend to rollback the round (no points awarded)
        gameService.rollbackRound();
        
        ctx.json(Map.of(
            "message", "Round abandoned successfully",
            "globalPoints", gameService.getGlobalPoints()
        ));
    }

    /**
     * Debug endpoint to list external topics/files that TopicScanner detects.
     * Query parameter: type=csv|xlsx|json
     */
    private static void listExternal(Context ctx) {
        String type = ctx.queryParam("type");
        if (type == null) type = "csv";

        SourceConfig.SourceType sourceType;
        switch (type.toLowerCase()) {
            case "xlsx": sourceType = SourceConfig.SourceType.CUSTOM_EXCEL; break;
            case "json": sourceType = SourceConfig.SourceType.CUSTOM_JSON; break;
            default: sourceType = SourceConfig.SourceType.CUSTOM_CSV; break;
        }

        java.util.List<String> topics = TopicScanner.getAvailableTopics(sourceType);

        // For convenience include an example file path for the first topic if available
        String examplePath = "";
        if (!topics.isEmpty()) {
            examplePath = TopicScanner.getTopicFilePath(topics.get(0), sourceType);
        }

        ctx.json(Map.of(
            "type", type.toLowerCase(),
            "topics", topics,
            "examplePath", examplePath
        ));
    }
    
    private static void getHints(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessions.get(sessionId);
        if (gameService == null) {
            ctx.status(404).result("Session not found");
            return;
        }
        
        // Diagnostic logging to help debug client showing 0/0 hints
        try {
            System.out.println("[DEBUG] getHints called for session: " + sessionId + " -> hints=" + gameService.getHints() + ", maxHints=" + gameService.getMaxHints());
        } catch (Exception e) {
            System.err.println("[DEBUG] Failed to log hints for session " + sessionId + ": " + e.getMessage());
        }

        ctx.json(Map.of(
            "hints", gameService.getHints(),
            "maxHints", gameService.getMaxHints()
        ));
    }
    
    private static void useHint(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessions.get(sessionId);
        if (gameService == null) {
            ctx.status(404).result("Session not found");
            return;
        }
        
        Question q = gameService.getCurrentQuestion();
        if (q == null) {
            ctx.status(400).result("No active question");
            return;
        }
        
        boolean success = gameService.useHint();
        if (!success) {
            ctx.status(400).json(Map.of(
                "error", "No hints remaining",
                "hints", 0
            ));
            return;
        }
        
        // Eliminate TWO wrong answers (50/50 style)
        int correctIndex = q.getCorrectIndex();
        java.util.List<Integer> wrongIndices = new java.util.ArrayList<>();
        for (int i = 0; i < q.getChoices().size(); i++) {
            if (i != correctIndex) {
                wrongIndices.add(i);
            }
        }
        
        // Randomly select TWO wrong answers to eliminate
        java.util.Collections.shuffle(wrongIndices);
        java.util.List<Integer> eliminatedIndices = new java.util.ArrayList<>();
        eliminatedIndices.add(wrongIndices.get(0));
        if (wrongIndices.size() > 1) {
            eliminatedIndices.add(wrongIndices.get(1));
        }
        
        ctx.json(Map.of(
            "success", true,
            "hints", gameService.getHints(),
            "maxHints", gameService.getMaxHints(),
            "eliminatedIndices", eliminatedIndices  // Return array of 2 eliminated indices
        ));
    }

    private static void uploadQuestions(Context ctx) {
        UploadedFile uploadedFile = ctx.uploadedFile("questions");
        if (uploadedFile == null) {
            ctx.status(400).json(Map.of("message", "No file uploaded"));
            return;
        }

        String originalFilename = uploadedFile.filename();
        String extension = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            extension = originalFilename.substring(i).toLowerCase();
        }
        
        // Sanitize filename to match loader expectations (lowercase, underscores)
        String namePart = originalFilename.substring(0, i).toLowerCase().replace(" ", "_");
        String filename = namePart + extension;

        String targetDir;
        // Determine directory based on extension
        // Note: In a real JAR deployment, we should use the external data directory.
        // For now, we use the src path for dev, but we should ideally use TopicScanner's logic or a shared config.
        // Since TopicScanner has private constants, we'll replicate the logic or hardcode for dev.
        // Let's assume dev environment for now as per instructions.
        
        switch (extension) {
            case ".csv":
                targetDir = "src/questions/external_source/csv/";
                break;
            case ".xlsx":
                targetDir = "src/questions/external_source/xlsx/";
                break;
            case ".json":
                targetDir = "src/questions/external_source/json/";
                break;
            default:
                ctx.status(400).json(Map.of("message", "Unsupported file type: " + extension));
                return;
        }

        try {
            // Ensure directory exists
            Files.createDirectories(Paths.get(targetDir));

            Path targetPath = Paths.get(targetDir + filename);
            Path tmpPath = Paths.get(targetDir + filename + ".tmp");

            // Write to temp file first
            try (java.io.InputStream is = uploadedFile.content()) {
                Files.copy(is, tmpPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // On Windows, delete target first if it exists (avoids AccessDeniedException)
            if (Files.exists(targetPath)) {
                try {
                    Files.delete(targetPath);
                } catch (Exception deleteEx) {
                    System.err.println("[Upload] Could not delete existing file, retrying: " + deleteEx.getMessage());
                    // Brief pause to let any file handles release
                    Thread.sleep(100);
                    Files.delete(targetPath);
                }
            }

            // Now move temp to target
            Files.move(tmpPath, targetPath);

            String topicName = filename.substring(0, filename.lastIndexOf('.'));

            System.out.println("[Upload] Saved " + filename + " to " + targetDir);

            ctx.json(Map.of(
                "customTopicName", topicName,
                "message", "File uploaded successfully"
            ));

        } catch (Exception e) {
            System.err.println("[Upload] Error saving file: " + e.getMessage());
            e.printStackTrace();
            ctx.status(500).json(Map.of("message", "Failed to save file: " + e.getMessage()));
        }
    }

    /**
     * Dev endpoint: Load a test file from the questions folder (csv/ or xlsx/).
     * This copies the file to the external_source directory so it can be used by the loaders.
     */
    private static void loadTestFile(Context ctx) {
        String filename = ctx.queryParam("filename");
        if (filename == null || filename.isEmpty()) {
            ctx.status(400).json(Map.of("message", "Missing filename parameter"));
            return;
        }

        // Determine extension
        String extension = "";
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = filename.substring(dotIdx).toLowerCase();
        }

        // Determine source and target directories based on extension
        String sourceDir;
        String targetDir;
        switch (extension) {
            case ".csv":
                sourceDir = "../questions/csv/";
                targetDir = "src/questions/external_source/csv/";
                break;
            case ".xlsx":
                sourceDir = "../questions/xlsx/";
                targetDir = "src/questions/external_source/xlsx/";
                break;
            case ".json":
                sourceDir = "../questions/json/";
                targetDir = "src/questions/external_source/json/";
                break;
            default:
                ctx.status(400).json(Map.of("message", "Unsupported file type: " + extension));
                return;
        }

        // Try a few possible locations for the source test file. Depending on
        // how the developer started the backend the working directory may vary.
        java.util.List<Path> candidates = java.util.List.of(
            Paths.get(sourceDir + filename),                                 // ../questions/{type}/file
            Paths.get("questions/" + sourceDir.replace("../", "") + filename), // projectRoot/questions/{type}/file
            Paths.get("src/questions/external_source/" + (extension.equals(".csv") ? "csv/" : extension.equals(".xlsx") ? "xlsx/" : "json/") + filename) // backend copy location
        );

        Path sourcePath = null;
        java.util.List<String> tried = new java.util.ArrayList<>();
        for (Path p : candidates) {
            tried.add(p.toString());
            if (Files.exists(p)) {
                sourcePath = p;
                break;
            }
        }

        if (sourcePath == null) {
            ctx.status(404).json(Map.of("message", "Test file not found. Tried: " + String.join(", ", tried)));
            return;
        }

        try {
            // Ensure target directory exists
            Files.createDirectories(Paths.get(targetDir));
            
            // Copy file to external_source
            Path targetPath = Paths.get(targetDir + filename);
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            String topicName = filename.substring(0, dotIdx);
            
            // Use the actual loader to count questions (reuse existing logic)
            int questionsLoaded = 0;
            try {
                SourceConfig.SourceType sourceType = extension.equals(".csv") 
                    ? SourceConfig.SourceType.CUSTOM_CSV 
                    : extension.equals(".xlsx") 
                        ? SourceConfig.SourceType.CUSTOM_EXCEL 
                        : SourceConfig.SourceType.CUSTOM_JSON;
                
                SourceConfig config = new SourceConfig.Builder()
                    .type(sourceType)
                    .topic(topicName)
                    .difficulty("") // Load all difficulties to count
                    .build();
                
                java.util.List<Question> questions = com.mindquest.loader.factory.QuestionBankFactory.getQuestions(config);
                questionsLoaded = questions.size();
            } catch (Exception e) {
                System.err.println("[TestLoad] Could not count questions: " + e.getMessage());
            }
            
            System.out.println("[TestLoad] Copied " + filename + " from " + sourcePath + " to " + targetDir + " (" + questionsLoaded + " questions)");
            
            ctx.json(Map.of(
                "topicName", topicName,
                "questionsLoaded", questionsLoaded,
                "message", "Test file loaded successfully"
            ));
            
        } catch (Exception e) {
            System.err.println("[TestLoad] Error loading test file: " + e.getMessage());
            e.printStackTrace();
            ctx.status(500).json(Map.of("message", "Failed to load test file: " + e.getMessage()));
        }
    }

    // DTOs
    public static class StartRequest {
        public String topic;
        public String difficulty;
    }

    public static class AnswerRequest {
        public int index;  // Legacy numeric index (0-3)
        public String answer; // Letter answer (A/B/C/D)
        public Long answerTimeMs; // Time taken to answer in milliseconds
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
