package com.mindquest.loader.factory;

import com.mindquest.loader.QuestionSource;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.loader.source.*;
import com.mindquest.model.question.Question;
import com.mindquest.model.QuestionBank;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;


public class QuestionBankFactory {

    // Default mode - can be overridden during run
    private static SourceConfig.SourceType DEFAULT_MODE = SourceConfig.SourceType.BUILTIN_HARDCODED;
    
    private static final AtomicInteger loaderThreadCounter = new AtomicInteger(1);
    
    // Background thread pool for async operations
    private static final ExecutorService executorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors(),
        r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("QuestionLoader-" + loaderThreadCounter.getAndIncrement());
            return t;
        }
    );
    
    static {
        // Register shutdown hook to clean up thread pool
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown();
        }));
    }
    

    public static List<Question> getQuestions(SourceConfig config) {
        try {
            QuestionSource loader = createLoader(config.getType());
            return loader.loadQuestions(config);
        } catch (Exception e) {
            System.err.println("Error loading questions from " + config.getType() + ": " + e.getMessage());
            System.out.println("Falling back to hardcoded questions.");
            return getQuestionsFromHardcoded(config.getTopic(), config.getDifficulty());
        }
    }
    
    /**
     * Uses default mode (BUILTIN_HARDCODED)
     */
    public static List<Question> getQuestions(String topic, String difficulty) {
        SourceConfig config = new SourceConfig.Builder()
            .type(DEFAULT_MODE)
            .topic(topic)
            .difficulty(difficulty)
            .build();
        return getQuestions(config);
    }
    
    /**
     * Creates the appropriate question loader based on source type.
     */
    private static QuestionSource createLoader(SourceConfig.SourceType type) {
        switch (type) {
            case BUILTIN_HARDCODED:
                return new HardcodedQuestionSource();
            
            case BUILTIN_JSON:
                return new JsonQuestionLoader();
            
            case CUSTOM_EXCEL:
                return new ExcelQuestionLoader();
            
            case CUSTOM_CSV:
                return new CsvQuestionLoader();
            
            case GEMINI_API:
                return new GeminiQuestionSource();
            
            default:
                return new HardcodedQuestionSource();
        }
    }

  
    private static List<Question> getQuestionsFromHardcoded(String topic, String difficulty) {
        QuestionBank bank = new QuestionBank();
        return bank.getQuestionsByTopicAndDifficulty(topic, difficulty);
    }

   
    public static String getCurrentMode() {
        return "Question Loading Mode: " + DEFAULT_MODE.toString();
    }
 
    public static void setDefaultMode(SourceConfig.SourceType mode) {
        DEFAULT_MODE = mode;
    }

    /**
     * Async variant of getQuestions using an internal executor.
     * Times out after 30 seconds to prevent indefinite hangs on file I/O.
     */
    public static CompletableFuture<List<Question>> getQuestionsAsync(SourceConfig config) {
        return CompletableFuture.supplyAsync(() -> getQuestions(config), executorService)
            .orTimeout(30, TimeUnit.SECONDS)
            .exceptionally(e -> {
                if (e.getCause() instanceof TimeoutException) {
                    System.err.println("[QuestionBankFactory] Question loading timed out after 30s, falling back to hardcoded.");
                }
                // Fallback to hardcoded questions on any error
                return getQuestionsFromHardcoded(config.getTopic(), config.getDifficulty());
            });
    }

    /**
     * Shutdown the internal executor service used for async loading.
     * Called from JVM shutdown hook and can be invoked manually during tests.
     */
    public static void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
