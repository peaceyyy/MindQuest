package com.mindquest.util;

import com.mindquest.loader.config.SourceConfig;
import com.mindquest.loader.factory.QuestionBankFactory;
import com.mindquest.loader.TopicScanner;
import com.mindquest.model.question.Question;

import java.io.File;
import java.util.List;

/**
 * Comprehensive resource loading test for JAR deployment.
 * 
 * Tests:
 * 1. Built-in JSON resources (bundled in JAR)
 * 2. External CSV files (filesystem)
 * 3. External Excel files (filesystem)
 * 4. Topic discovery via TopicScanner
 * 5. Graceful fallback on missing resources
 * 
 * Run this BOTH in IDE and from packaged JAR to verify deployment readiness.
 */
public class JarDeploymentTest {

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("    JAR DEPLOYMENT TEST");
        System.out.println("===========================================\n");

        detectEnvironment();
        testBuiltInJsonResources();
        testExternalCsvResources();
        testExternalExcelResources();
        testTopicDiscovery();
        testGracefulFallback();

        System.out.println("\n===========================================");
        System.out.println("    DEPLOYMENT TEST COMPLETE");
        System.out.println("===========================================");
    }

    private static void detectEnvironment() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("Environment Detection");
        System.out.println("─────────────────────────────────────────");

        try {
            String classPath = JarDeploymentTest.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath();

            if (classPath.endsWith(".jar")) {
                System.out.println("✓ Running from JAR: " + classPath);
                System.out.println("  External data should be in: ./questions/");
            } else {
                System.out.println("✓ Running from IDE/classes");
                System.out.println("  Using development paths: src/questions/...");
            }
        } catch (Exception e) {
            System.out.println("⚠ Could not detect environment");
        }

        System.out.println();
    }

    private static void testBuiltInJsonResources() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("Test 1: Built-in JSON Resources (Classpath)");
        System.out.println("─────────────────────────────────────────");

        String[][] tests = {
            {"philosophy", "Medium"},
            {"cs", "easy"},
            {"ai", "hard"}
        };

        for (String[] test : tests) {
            String topic = test[0];
            String difficulty = test[1];

            try {
                SourceConfig config = new SourceConfig.Builder()
                    .type(SourceConfig.SourceType.BUILTIN_JSON)
                    .topic(topic)
                    .difficulty(difficulty)
                    .build();

                List<Question> questions = QuestionBankFactory.getQuestions(config);

                if (questions != null && !questions.isEmpty()) {
                    System.out.println("✓ " + topic + "/" + difficulty + ": " + questions.size() + " questions");
                    System.out.println("  Sample ID: " + questions.get(0).getId());
                } else {
                    System.out.println("✗ " + topic + "/" + difficulty + ": No questions loaded");
                }

            } catch (Exception e) {
                System.out.println("✗ " + topic + "/" + difficulty + ": " + e.getMessage());
            }
        }

        System.out.println();
    }

    private static void testExternalCsvResources() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("Test 2: External CSV Resources (Filesystem)");
        System.out.println("─────────────────────────────────────────");

        // Check if external CSV directory exists
        String csvPath = isJarEnvironment() ? "./questions/csv/" : "src/questions/external_source/csv/";
        File csvDir = new File(csvPath);

        if (!csvDir.exists()) {
            System.out.println("⚠ CSV directory not found: " + csvPath);
            System.out.println("  Skipping CSV tests (this is OK if not using CSV source)");
            System.out.println();
            return;
        }

        System.out.println("✓ CSV directory found: " + csvPath);

        try {
            SourceConfig config = new SourceConfig.Builder()
                .type(SourceConfig.SourceType.CUSTOM_CSV)
                .topic("philosophy")
                .difficulty("Medium")
                .build();

            List<Question> questions = QuestionBankFactory.getQuestions(config);

            if (questions != null && !questions.isEmpty()) {
                System.out.println("✓ Loaded " + questions.size() + " questions from CSV");
                System.out.println("  Sample: " + questions.get(0).getQuestionText().substring(0, 50) + "...");
            } else {
                System.out.println("⚠ No questions loaded (CSV file might be empty or missing)");
            }

        } catch (Exception e) {
            System.out.println("✗ CSV loading failed: " + e.getMessage());
        }

        System.out.println();
    }

    private static void testExternalExcelResources() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("Test 3: External Excel Resources (Filesystem)");
        System.out.println("─────────────────────────────────────────");

        String xlsxPath = isJarEnvironment() ? "./questions/xlsx/" : "src/questions/external_source/xlsx/";
        File xlsxDir = new File(xlsxPath);

        if (!xlsxDir.exists()) {
            System.out.println("⚠ Excel directory not found: " + xlsxPath);
            System.out.println("  Skipping Excel tests (this is OK if not using Excel source)");
            System.out.println();
            return;
        }

        System.out.println("✓ Excel directory found: " + xlsxPath);

        try {
            SourceConfig config = new SourceConfig.Builder()
                .type(SourceConfig.SourceType.CUSTOM_EXCEL)
                .topic("philosophy")
                .difficulty("Medium")
                .build();

            List<Question> questions = QuestionBankFactory.getQuestions(config);

            if (questions != null && !questions.isEmpty()) {
                System.out.println("✓ Loaded " + questions.size() + " questions from Excel");
                System.out.println("  Sample: " + questions.get(0).getQuestionText().substring(0, 50) + "...");
            } else {
                System.out.println("⚠ No questions loaded (Excel file might be empty or missing)");
            }

        } catch (Exception e) {
            System.out.println("✗ Excel loading failed: " + e.getMessage());
        }

        System.out.println();
    }

    private static void testTopicDiscovery() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("Test 4: Topic Discovery");
        System.out.println("─────────────────────────────────────────");

        // Test JSON topics (always available from classpath)
        List<String> jsonTopics = TopicScanner.getAvailableTopics(SourceConfig.SourceType.BUILTIN_JSON);
        System.out.println("JSON topics: " + jsonTopics);

        // Test CSV topics (filesystem scan)
        List<String> csvTopics = TopicScanner.getAvailableTopics(SourceConfig.SourceType.CUSTOM_CSV);
        System.out.println("CSV topics: " + csvTopics);

        // Test Excel topics (filesystem scan)
        List<String> xlsxTopics = TopicScanner.getAvailableTopics(SourceConfig.SourceType.CUSTOM_EXCEL);
        System.out.println("Excel topics: " + xlsxTopics);

        System.out.println();
    }

    private static void testGracefulFallback() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("Test 5: Graceful Fallback (Missing Resources)");
        System.out.println("─────────────────────────────────────────");

        try {
            // Try loading non-existent topic
            SourceConfig config = new SourceConfig.Builder()
                .type(SourceConfig.SourceType.BUILTIN_JSON)
                .topic("nonexistent")
                .difficulty("Medium")
                .build();

            List<Question> questions = QuestionBankFactory.getQuestions(config);

            if (questions != null && !questions.isEmpty()) {
                System.out.println("✓ Fallback triggered: Got " + questions.size() + " hardcoded questions");
                System.out.println("  (Expected behavior when resource not found)");
            } else {
                System.out.println("✗ No fallback questions provided");
            }

        } catch (Exception e) {
            System.out.println("✗ Exception instead of graceful fallback: " + e.getMessage());
        }

        System.out.println();
    }

    private static boolean isJarEnvironment() {
        try {
            String classPath = JarDeploymentTest.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath();
            return classPath.endsWith(".jar");
        } catch (Exception e) {
            return false;
        }
    }
}
