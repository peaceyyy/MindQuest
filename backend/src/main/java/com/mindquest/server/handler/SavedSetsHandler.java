package com.mindquest.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindquest.server.dto.InlineQuestion;
import com.mindquest.server.dto.SavedQuestionSet;
import com.mindquest.server.dto.SavedSetsFile;
import com.mindquest.server.dto.SaveQuestionSetRequest;
import io.javalin.http.Context;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for saved AI question sets operations.
 * Manages CRUD operations for persisted question sets.
 */
public class SavedSetsHandler {

    private static final String SAVED_SETS_FILE = "data/saved_question_sets.json";
    private final ObjectMapper objectMapper;

    public SavedSetsHandler() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * GET /api/saved-sets - List all saved AI question sets.
     */
    public void listSavedSets(Context ctx) {
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
     * POST /api/saved-sets - Save a new question set.
     */
    public void saveQuestionSet(Context ctx) {
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
            
            // Add to list (most recent first)
            data.sets.add(0, newSet);
            
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
     * GET /api/saved-sets/{id}/questions - Get questions for a specific saved set.
     */
    public void getSavedSetQuestions(Context ctx) {
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

    /**
     * DELETE /api/saved-sets/{id} - Delete a saved question set.
     */
    public void deleteSavedSet(Context ctx) {
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
}
