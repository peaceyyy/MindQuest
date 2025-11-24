package com.mindquest.tests;

import com.mindquest.loader.source.JsonQuestionLoader;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.model.question.Question;

import java.util.List;

public class JsonLoaderQuickTest {
    public static void main(String[] args) {
        System.out.println("=== JSON Loader Quick Test ===\n");
        
        // Test Philosophy Medium (the one you mentioned failing)
        testLoad("Philosophy", "Medium");
        testLoad("cs", "easy");
        testLoad("ai", "hard");
    }
    
    private static void testLoad(String topic, String difficulty) {
        System.out.println("Testing: " + topic + " / " + difficulty);
        
        try {
            JsonQuestionLoader loader = new JsonQuestionLoader();
            SourceConfig config = new SourceConfig.Builder()
                .type(SourceConfig.SourceType.BUILTIN_JSON)
                .topic(topic)
                .difficulty(difficulty)
                .build();
            
            List<Question> questions = loader.loadQuestions(config);
            
            System.out.println("  ✓ Loaded " + questions.size() + " questions");
            if (!questions.isEmpty()) {
                Question first = questions.get(0);
                System.out.println("  Sample ID: " + first.getId());
                System.out.println("  Sample Q: " + first.getQuestionText().substring(0, Math.min(50, first.getQuestionText().length())) + "...");
            }
            System.out.println();
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
        }
    }
}
