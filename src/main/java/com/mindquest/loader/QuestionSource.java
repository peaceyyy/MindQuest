package com.mindquest.loader;

import com.mindquest.model.Question;
import java.io.IOException;
import java.util.List;

/**
 * Unified interface for loading questions from different sources.
 * Implementations: JSON files, CSV files, Excel files, Gemini API, hardcoded bank.
 */
public interface QuestionSource {
    
    /**
     * Loads questions based on the provided configuration.
     * 
     * @param config Configuration specifying source type, topic, difficulty, and path
     * @return List of Question objects loaded from the source
     * @throws IOException if the source cannot be read or parsed
     */
    List<Question> loadQuestions(SourceConfig config) throws IOException;
    
    /**
     * Returns a human-readable name for this question source.
     * Used for logging and user feedback.
     */
    String getSourceName();
}
