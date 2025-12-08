package com.mindquest.server.handler;

import com.mindquest.loader.TopicScanner;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.model.question.Question;
import com.mindquest.server.SessionRegistry;
import com.mindquest.server.dto.AnswerRequest;
import com.mindquest.server.dto.InlineQuestion;
import com.mindquest.server.dto.StartRequest;
import com.mindquest.server.util.AnswerMapper;
import com.mindquest.server.util.QuestionFactory;
import com.mindquest.server.util.TopicNormalizer;
import com.mindquest.service.GameService;
import com.mindquest.service.dto.AnswerResult;
import com.mindquest.service.dto.RoundSummary;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Handler for core gameplay operations.
 * Manages round start, questions, answers, and hints.
 */
public class GameplayHandler {
    
    private static final String INLINE_QUESTION_ID_PREFIX = "gemini-";

    private final SessionRegistry sessionRegistry;

    public GameplayHandler(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * POST /api/sessions/{id}/start - Start a new round.
     */
    public void startRound(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessionRegistry.getSession(sessionId);
        
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
        String normalizedTopic = TopicNormalizer.normalizeTopic(req.topic);
        String normalizedDifficulty = TopicNormalizer.normalizeDifficulty(req.difficulty);
        
        // Check if inline questions are provided (e.g., from Gemini AI)
        if (req.questions != null && !req.questions.isEmpty()) {
            // Convert inline questions to concrete Question subclasses based on difficulty
            List<Question> inlineQuestions = new ArrayList<>();
            for (int i = 0; i < req.questions.size(); i++) {
                InlineQuestion iq = req.questions.get(i);
                Question q = QuestionFactory.createForDifficulty(
                    INLINE_QUESTION_ID_PREFIX + i,
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
            System.out.println("[GameplayHandler] Started round with " + inlineQuestions.size() + " inline questions for topic: " + normalizedTopic);
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
            System.out.println("[GameplayHandler] Using custom source: " + config.getType() + " for topic " + normalizedTopic);
        } else {
            gameService.setSourceConfig(null);
        }

        gameService.startNewRound(normalizedTopic, normalizedDifficulty);
        ctx.json(Map.of("message", "Round started", "topic", normalizedTopic, "difficulty", normalizedDifficulty));
    }

    /**
     * GET /api/sessions/{id}/question - Get the current question.
     */
    public void getCurrentQuestion(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessionRegistry.getSession(sessionId);
        
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
        
        // Diagnostic logging
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

    /**
     * POST /api/sessions/{id}/answer - Submit an answer to the current question.
     */
    public void submitAnswer(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessionRegistry.getSession(sessionId);
        
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
            answerIndex = AnswerMapper.letterToIndex(req.answer);
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

    /**
     * GET /api/sessions/{id}/hints - Get current hint count.
     */
    public void getHints(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessionRegistry.getSession(sessionId);
        
        if (gameService == null) {
            ctx.status(404).result("Session not found");
            return;
        }
        
        // Diagnostic logging
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

    /**
     * POST /api/sessions/{id}/use-hint - Use a hint (50/50 elimination).
     */
    public void useHint(Context ctx) {
        String sessionId = ctx.pathParam("id");
        GameService gameService = sessionRegistry.getSession(sessionId);
        
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
        List<Integer> wrongIndices = new ArrayList<>();
        for (int i = 0; i < q.getChoices().size(); i++) {
            if (i != correctIndex) {
                wrongIndices.add(i);
            }
        }
        
        // Randomly select TWO wrong answers to eliminate
        Collections.shuffle(wrongIndices);
        List<Integer> eliminatedIndices = new ArrayList<>();
        eliminatedIndices.add(wrongIndices.get(0));
        if (wrongIndices.size() > 1) {
            eliminatedIndices.add(wrongIndices.get(1));
        }
        
        ctx.json(Map.of(
            "success", true,
            "hints", gameService.getHints(),
            "maxHints", gameService.getMaxHints(),
            "eliminatedIndices", eliminatedIndices
        ));
    }
}
