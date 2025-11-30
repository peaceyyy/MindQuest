package com.mindquest.server.handler;

import com.mindquest.loader.TopicScanner;
import com.mindquest.loader.config.SourceConfig;
import com.mindquest.loader.factory.QuestionBankFactory;
import com.mindquest.model.question.Question;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handler for file upload and debug operations.
 * Manages question file uploads and external source listing.
 */
public class UploadHandler {

    public UploadHandler() {
        // No dependencies needed
    }

    /**
     * POST /api/upload/questions - Upload a question file (CSV, XLSX, or JSON).
     */
    public void uploadQuestions(Context ctx) {
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
            try (InputStream is = uploadedFile.content()) {
                Files.copy(is, tmpPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // On Windows, delete target first if it exists (avoids AccessDeniedException)
            if (Files.exists(targetPath)) {
                try {
                    Files.delete(targetPath);
                } catch (Exception deleteEx) {
                    System.err.println("[Upload] Could not delete existing file, retrying: " + deleteEx.getMessage());
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
     * POST /api/test/load-file - Dev endpoint to load a test file.
     */
    public void loadTestFile(Context ctx) {
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

        // Try a few possible locations for the source test file
        List<Path> candidates = List.of(
            Paths.get(sourceDir + filename),
            Paths.get("questions/" + sourceDir.replace("../", "") + filename),
            Paths.get("src/questions/external_source/" + getSubdir(extension) + filename)
        );

        Path sourcePath = null;
        List<String> tried = new ArrayList<>();
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
            
            // Use the actual loader to count questions
            int questionsLoaded = 0;
            try {
                SourceConfig.SourceType sourceType = getSourceType(extension);
                
                SourceConfig config = new SourceConfig.Builder()
                    .type(sourceType)
                    .topic(topicName)
                    .difficulty("")
                    .build();
                
                List<Question> questions = QuestionBankFactory.getQuestions(config);
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
     * GET /api/debug/list-external - List external topics/files detected by TopicScanner.
     */
    public void listExternal(Context ctx) {
        String type = ctx.queryParam("type");
        if (type == null) type = "csv";

        SourceConfig.SourceType sourceType;
        switch (type.toLowerCase()) {
            case "xlsx": sourceType = SourceConfig.SourceType.CUSTOM_EXCEL; break;
            case "json": sourceType = SourceConfig.SourceType.CUSTOM_JSON; break;
            default: sourceType = SourceConfig.SourceType.CUSTOM_CSV; break;
        }

        List<String> topics = TopicScanner.getAvailableTopics(sourceType);

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

    private String getSubdir(String extension) {
        switch (extension) {
            case ".csv": return "csv/";
            case ".xlsx": return "xlsx/";
            case ".json": return "json/";
            default: return "";
        }
    }

    private SourceConfig.SourceType getSourceType(String extension) {
        switch (extension) {
            case ".csv": return SourceConfig.SourceType.CUSTOM_CSV;
            case ".xlsx": return SourceConfig.SourceType.CUSTOM_EXCEL;
            case ".json": return SourceConfig.SourceType.CUSTOM_JSON;
            default: return SourceConfig.SourceType.CUSTOM_CSV;
        }
    }
}
