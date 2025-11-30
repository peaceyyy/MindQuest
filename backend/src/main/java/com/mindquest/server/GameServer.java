package com.mindquest.server;

import com.mindquest.controller.SessionManager;
import com.mindquest.model.QuestionBank;
import com.mindquest.model.game.Player;
import com.mindquest.model.question.Question;
import com.mindquest.model.question.EasyQuestion;
import com.mindquest.model.question.MediumQuestion;
import com.mindquest.model.question.HardQuestion;
import com.mindquest.service.GameService;
import com.mindquest.service.dto.AnswerResult;
import com.mindquest.loader.TopicScanner;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.loader.factory.QuestionBankFactory;
import com.mindquest.llm.util.SecretResolver;
import com.mindquest.service.dto.RoundSummary;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URL;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;

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

            // Gemini AI Question Generation
            app.get("/api/gemini/status", GameServer::getGeminiStatus);
            app.get("/api/gemini/network-test", GameServer::testGeminiNetwork);
            app.post("/api/gemini/generate", GameServer::generateGeminiQuestions);
            
            // Saved AI Question Sets
            app.get("/api/saved-sets", GameServer::listSavedSets);
            app.post("/api/saved-sets", GameServer::saveQuestionSet);
            app.get("/api/saved-sets/{id}/questions", GameServer::getSavedSetQuestions);
            app.delete("/api/saved-sets/{id}", GameServer::deleteSavedSet);

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
        
        // Check if inline questions are provided (e.g., from Gemini AI)
        if (req.questions != null && !req.questions.isEmpty()) {
            // Convert inline questions to concrete Question subclasses based on difficulty
            List<Question> inlineQuestions = new ArrayList<>();
            for (int i = 0; i < req.questions.size(); i++) {
                InlineQuestion iq = req.questions.get(i);
                Question q = createQuestionForDifficulty(
                    "gemini-" + i,
                    iq.questionText,
                    iq.choices,
                    iq.correctIndex,
                    normalizedDifficulty,
                    normalizedTopic
                );
                inlineQuestions.add(q);
            }
            
            // Start round with inline questions
            gameService.startNewRoundWithQuestions(normalizedTopic, normalizedDifficulty, inlineQuestions);
            System.out.println("[GameServer] Started round with " + inlineQuestions.size() + " inline questions for topic: " + normalizedTopic);
            ctx.json(Map.of(
                "message", "Round started with AI-generated questions",
                "topic", normalizedTopic,
                "difficulty", normalizedDifficulty,
                "questionCount", inlineQuestions.size()
            ));
            return;
        }
        
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

    /**
     * GET /api/gemini/status - Check if Gemini API is available
     */
    private static void getGeminiStatus(Context ctx) {
        SecretResolver secrets = new SecretResolver();
        boolean hasKey = secrets.hasSecret("GOOGLE_API_KEY") || secrets.hasSecret("GEMINI_API_KEY");
        String maskedKey = "";
        
        if (hasKey) {
            String key = secrets.getGeminiApiKey();
            if (key != null && key.length() > 8) {
                maskedKey = key.substring(0, 4) + "..." + key.substring(key.length() - 4);
            }
        }
        
        ctx.json(Map.of(
            "available", hasKey,
            "keyConfigured", hasKey,
            "maskedKey", maskedKey,
            "message", hasKey ? "Gemini API is configured" : "No API key found. Set GOOGLE_API_KEY in .env file."
        ));
    }

    /**
     * GET /api/gemini/network-test - Test network connectivity to Google APIs
     */
    private static void testGeminiNetwork(Context ctx) {
        Map<String, Object> results = new HashMap<>();
        
        // Test 1: Basic Google connectivity
        results.put("test1_google", testHttpConnection("https://www.google.com", 5000));
        
        // Test 2: Generative AI endpoint (just connectivity, not auth)
        results.put("test2_generativelanguage", testHttpConnection("https://generativelanguage.googleapis.com", 5000));
        
        // Test 3: Check SSL/TLS version
        results.put("java_version", System.getProperty("java.version"));
        results.put("java_vendor", System.getProperty("java.vendor"));
        
        // Test 4: Try to see which TLS versions are available
        try {
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getDefault();
            results.put("ssl_protocol", sslContext.getProtocol());
            results.put("ssl_providers", java.util.Arrays.toString(
                java.security.Security.getProviders()));
        } catch (Exception e) {
            results.put("ssl_error", e.getMessage());
        }
        
        ctx.json(results);
    }
    
    private static Map<String, Object> testHttpConnection(String urlString, int timeoutMs) {
        Map<String, Object> result = new HashMap<>();
        result.put("url", urlString);
        
        try {
            long start = System.currentTimeMillis();
            URL url = new URL(urlString);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestMethod("GET");
            
            int responseCode = conn.getResponseCode();
            long elapsed = System.currentTimeMillis() - start;
            
            result.put("success", true);
            result.put("responseCode", responseCode);
            result.put("elapsedMs", elapsed);
            result.put("cipher", conn.getCipherSuite());
            
            conn.disconnect();
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            
            // Get root cause
            Throwable cause = e.getCause();
            if (cause != null) {
                result.put("cause", cause.getClass().getSimpleName() + ": " + cause.getMessage());
            }
        }
        
        return result;
    }

    /**
     * POST /api/gemini/generate - Generate questions using Gemini AI
     * Request body: { topic: string, difficulty: string, count: number }
     */
    private static void generateGeminiQuestions(Context ctx) {
        GeminiGenerateRequest req;
        try {
            req = ctx.bodyAsClass(GeminiGenerateRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "Invalid request format", "message", e.getMessage()));
            return;
        }
        
        // Validate inputs
        if (req.topic == null || req.topic.trim().isEmpty()) {
            ctx.status(400).json(Map.of("error", "Topic is required"));
            return;
        }
        
        if (req.difficulty == null || req.difficulty.trim().isEmpty()) {
            ctx.status(400).json(Map.of("error", "Difficulty is required"));
            return;
        }
        
        // Enforce topic length limit (max 100 chars to prevent prompt injection)
        String topic = req.topic.trim();
        if (topic.length() > 100) {
            topic = topic.substring(0, 100);
        }
        
        // Sanitize topic - remove special characters that could affect prompt
        topic = topic.replaceAll("[^a-zA-Z0-9\\s\\-]", "");
        
        // Normalize difficulty
        String difficulty = normalizeDifficulty(req.difficulty);
        
        // Clamp question count (5-10)
        int count = req.count;
        if (count < 5) count = 5;
        if (count > 10) count = 10;
        
        System.out.println("[Gemini] Generating " + count + " questions for topic '" + topic + "' at " + difficulty + " difficulty");
        
        try {
            // Build config with question count as extra param
            SourceConfig config = new SourceConfig.Builder()
                .type(SourceConfig.SourceType.GEMINI_API)
                .topic(topic)
                .difficulty(difficulty)
                .addExtraParam("questionCount", String.valueOf(count))
                .build();
            
            // Generate questions
            long startTime = System.currentTimeMillis();
            List<Question> questions = QuestionBankFactory.getQuestions(config);
            long elapsed = System.currentTimeMillis() - startTime;
            
            if (questions == null || questions.isEmpty()) {
                ctx.status(500).json(Map.of(
                    "error", "No questions generated",
                    "message", "Gemini returned no valid questions. Try a different topic."
                ));
                return;
            }
            
            // Convert questions to JSON-friendly format
            List<Map<String, Object>> questionList = new ArrayList<>();
            for (Question q : questions) {
                Map<String, Object> qMap = new HashMap<>();
                qMap.put("id", q.getId());
                qMap.put("questionText", q.getQuestionText());
                qMap.put("choices", q.getChoices());
                qMap.put("correctIndex", q.getCorrectIndex());
                qMap.put("difficulty", q.getDifficulty());
                qMap.put("topic", q.getTopic());
                questionList.add(qMap);
            }
            
            System.out.println("[Gemini] Generated " + questions.size() + " questions in " + elapsed + "ms");
            
            ctx.json(Map.of(
                "success", true,
                "topic", topic,
                "difficulty", difficulty,
                "count", questions.size(),
                "generationTimeMs", elapsed,
                "questions", questionList
            ));
            
        } catch (Exception e) {
            System.err.println("[Gemini] Generation failed: " + e.getMessage());
            e.printStackTrace();
            ctx.status(500).json(Map.of(
                "error", "Generation failed",
                "message", e.getMessage()
            ));
        }
    }

    // ========== SAVED AI QUESTION SETS ==========
    
    private static final String SAVED_SETS_FILE = "data/saved_question_sets.json";
    private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    
    /**
     * GET /api/saved-sets - List all saved AI question sets
     */
    private static void listSavedSets(Context ctx) {
        try {
            Path filePath = Paths.get(SAVED_SETS_FILE);
            if (!Files.exists(filePath)) {
                ctx.json(Map.of("sets", new ArrayList<>()));
                return;
            }
            
            String json = Files.readString(filePath);
            SavedSetsFile data = objectMapper.readValue(json, SavedSetsFile.class);
            
            // Return sets without the full questions array (just metadata)
            List<Map<String, Object>> setsList = new ArrayList<>();
            for (SavedQuestionSet set : data.sets) {
                Map<String, Object> setInfo = new HashMap<>();
                setInfo.put("id", set.id);
                setInfo.put("name", set.name);
                setInfo.put("topic", set.topic);
                setInfo.put("difficulty", set.difficulty);
                setInfo.put("questionCount", set.questions.size());
                setInfo.put("provider", set.provider);
                setInfo.put("createdAt", set.createdAt);
                setsList.add(setInfo);
            }
            
            ctx.json(Map.of("sets", setsList));
            
        } catch (Exception e) {
            System.err.println("[SavedSets] Failed to list: " + e.getMessage());
            ctx.status(500).json(Map.of("error", "Failed to load saved sets"));
        }
    }
    
    /**
     * POST /api/saved-sets - Save a new question set
     * Body: { name: string, topic: string, difficulty: string, provider: string, questions: [...] }
     */
    private static void saveQuestionSet(Context ctx) {
        try {
            SaveQuestionSetRequest req = ctx.bodyAsClass(SaveQuestionSetRequest.class);
            
            if (req.name == null || req.name.trim().isEmpty()) {
                ctx.status(400).json(Map.of("error", "Name is required"));
                return;
            }
            if (req.questions == null || req.questions.isEmpty()) {
                ctx.status(400).json(Map.of("error", "Questions array is required"));
                return;
            }
            
            // Load existing sets
            Path filePath = Paths.get(SAVED_SETS_FILE);
            SavedSetsFile data;
            if (Files.exists(filePath)) {
                String json = Files.readString(filePath);
                data = objectMapper.readValue(json, SavedSetsFile.class);
            } else {
                data = new SavedSetsFile();
                data.sets = new ArrayList<>();
            }
            
            // Create new set
            SavedQuestionSet newSet = new SavedQuestionSet();
            newSet.id = UUID.randomUUID().toString();
            newSet.name = req.name.trim();
            newSet.topic = req.topic;
            newSet.difficulty = req.difficulty;
            newSet.provider = req.provider != null ? req.provider : "gemini";
            newSet.createdAt = System.currentTimeMillis();
            newSet.questions = req.questions;
            
            // Add to list
            data.sets.add(0, newSet); // Add at beginning (most recent first)
            
            // Save to file
            Files.createDirectories(filePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), data);
            
            System.out.println("[SavedSets] Saved set '" + newSet.name + "' with " + newSet.questions.size() + " questions");
            
            ctx.json(Map.of(
                "success", true,
                "id", newSet.id,
                "message", "Question set saved successfully"
            ));
            
        } catch (Exception e) {
            System.err.println("[SavedSets] Failed to save: " + e.getMessage());
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "Failed to save question set: " + e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/saved-sets/{id} - Delete a saved question set
     */
    private static void deleteSavedSet(Context ctx) {
        String setId = ctx.pathParam("id");
        
        try {
            Path filePath = Paths.get(SAVED_SETS_FILE);
            if (!Files.exists(filePath)) {
                ctx.status(404).json(Map.of("error", "No saved sets found"));
                return;
            }
            
            String json = Files.readString(filePath);
            SavedSetsFile data = objectMapper.readValue(json, SavedSetsFile.class);
            
            // Find and remove the set
            boolean removed = data.sets.removeIf(set -> set.id.equals(setId));
            
            if (!removed) {
                ctx.status(404).json(Map.of("error", "Set not found"));
                return;
            }
            
            // Save updated file
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), data);
            
            System.out.println("[SavedSets] Deleted set: " + setId);
            
            ctx.json(Map.of("success", true, "message", "Question set deleted"));
            
        } catch (Exception e) {
            System.err.println("[SavedSets] Failed to delete: " + e.getMessage());
            ctx.status(500).json(Map.of("error", "Failed to delete question set"));
        }
    }
    
    /**
     * GET /api/saved-sets/{id}/questions - Get questions for a specific saved set
     * (Used when playing a saved set)
     */
    private static void getSavedSetQuestions(Context ctx) {
        String setId = ctx.pathParam("id");
        
        try {
            Path filePath = Paths.get(SAVED_SETS_FILE);
            if (!Files.exists(filePath)) {
                ctx.status(404).json(Map.of("error", "No saved sets found"));
                return;
            }
            
            String json = Files.readString(filePath);
            SavedSetsFile data = objectMapper.readValue(json, SavedSetsFile.class);
            
            // Find the set
            SavedQuestionSet found = null;
            for (SavedQuestionSet set : data.sets) {
                if (set.id.equals(setId)) {
                    found = set;
                    break;
                }
            }
            
            if (found == null) {
                ctx.status(404).json(Map.of("error", "Set not found"));
                return;
            }
            
            ctx.json(Map.of(
                "id", found.id,
                "name", found.name,
                "topic", found.topic,
                "difficulty", found.difficulty,
                "questions", found.questions
            ));
            
        } catch (Exception e) {
            System.err.println("[SavedSets] Failed to get questions: " + e.getMessage());
            ctx.status(500).json(Map.of("error", "Failed to load questions"));
        }
    }

    // DTOs for saved sets
    public static class SavedSetsFile {
        public List<SavedQuestionSet> sets;
    }
    
    public static class SavedQuestionSet {
        public String id;
        public String name;
        public String topic;
        public String difficulty;
        public String provider;
        public long createdAt;
        public List<InlineQuestion> questions;
    }
    
    public static class SaveQuestionSetRequest {
        public String name;
        public String topic;
        public String difficulty;
        public String provider;
        public List<InlineQuestion> questions;
    }

    // DTOs
    public static class StartRequest {
        public String topic;
        public String difficulty;
        public java.util.List<InlineQuestion> questions; // Optional: For Gemini/LLM generated questions
    }
    
    public static class InlineQuestion {
        public String questionText;
        public java.util.List<String> choices;
        public int correctIndex;
        public String id;
        public String difficulty;
        public String topic;
    }

    public static class GeminiGenerateRequest {
        public String topic;
        public String difficulty;
        public int count = 5;  // Default to 5 questions
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
    
    /**
     * Factory method to create the appropriate Question subclass based on difficulty.
     * Since Question is abstract, we must use EasyQuestion, MediumQuestion, or HardQuestion.
     */
    private static Question createQuestionForDifficulty(
            String id, 
            String questionText, 
            List<String> choices, 
            int correctIndex, 
            String difficulty,
            String topic) {
        
        String lowerDiff = difficulty.toLowerCase();
        switch (lowerDiff) {
            case "easy":
                return new EasyQuestion(id, questionText, choices, correctIndex, topic);
            case "medium":
                return new MediumQuestion(id, questionText, choices, correctIndex, topic);
            case "hard":
                return new HardQuestion(id, questionText, choices, correctIndex, topic);
            default:
                // Default to Medium if unknown difficulty
                return new MediumQuestion(id, questionText, choices, correctIndex, topic);
        }
    }
}
