package com.mindquest.loader.source;

import com.mindquest.loader.QuestionSource;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.model.question.Question;
import com.mindquest.model.QuestionBank;

import java.io.IOException;
import java.util.List;

/**
 * QuestionSource implementation that wraps the original hardcoded QuestionBank.
 * Provides backward compatibility with existing hardcoded questions.
 */
public class HardcodedQuestionSource implements QuestionSource {
    
    @Override
    public List<Question> loadQuestions(SourceConfig config) throws IOException {
        QuestionBank bank = new QuestionBank();
        String topic = config.getTopic();
        String difficulty = config.getDifficulty();
        
        return bank.getQuestionsByTopicAndDifficulty(topic, difficulty);
    }
    
    @Override
    public String getSourceName() {
        return "Hardcoded Question Bank";
    }
}
