package com.mindquest.llm.util;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Resolves secrets (API keys, endpoints) from .env file or system environment.
 * 
 * Priority order:
 * 1. System environment variables
 * 2. .env file in project root
 * 3. User home directory .mindquest/credentials.properties
 * 
 * This class does NOT persist secrets - it only reads them.
 */
public class SecretResolver {
    
    private static final String ENV_FILE = ".env";
    private static final String HOME_CREDENTIALS = ".mindquest/credentials.properties";
    
    private final Map<String, String> secrets;
    
    public SecretResolver() {
        this.secrets = new HashMap<>();
        loadSecrets();
    }
    
    /**
     * Loads secrets from all available sources (env, .env, home).
     */
    private void loadSecrets() {
        System.out.println("\n========== [SecretResolver] DEBUG START ==========");
        
        // 1. Load from .env file in project root
        loadFromEnvFile();
        System.out.println("[DEBUG] After .env: LOCAL_LLM_ENDPOINT = " + secrets.get("LOCAL_LLM_ENDPOINT"));
        
        // 2. Load from user home credentials file
        loadFromHomeCredentials();
        System.out.println("[DEBUG] After home credentials: LOCAL_LLM_ENDPOINT = " + secrets.get("LOCAL_LLM_ENDPOINT"));
        
        // 3. System environment variables override everything
        loadFromSystemEnv();
        System.out.println("[DEBUG] After system env: LOCAL_LLM_ENDPOINT = " + secrets.get("LOCAL_LLM_ENDPOINT"));
        
        System.out.println("[DEBUG] System.getenv('LOCAL_LLM_ENDPOINT') = " + System.getenv("LOCAL_LLM_ENDPOINT"));
        System.out.println("========== [SecretResolver] DEBUG END ==========\n");
    }
    
    /**
     * Loads from .env file in the current directory or project root.
     */
    private void loadFromEnvFile() {
        // Try multiple locations to find .env file
        Path[] possiblePaths = {
            Paths.get(ENV_FILE),                    // Current directory
            Paths.get("..", ENV_FILE),              // Parent directory
            Paths.get(System.getProperty("user.dir"), ENV_FILE),  // Explicit current dir
            Paths.get(System.getProperty("user.dir"), "..", ENV_FILE)  // Explicit parent
        };
        
        Path envPath = null;
        for (Path p : possiblePaths) {
            System.out.println("[DEBUG] Checking for .env at: " + p.toAbsolutePath().normalize());
            if (Files.exists(p)) {
                envPath = p;
                break;
            }
        }
        
        if (envPath == null) {
            System.out.println("[SecretResolver] No .env file found in any location");
            System.out.println("[DEBUG] Current working directory: " + System.getProperty("user.dir"));
            return;
        }
        
        System.out.println("[DEBUG] Found .env at: " + envPath.toAbsolutePath().normalize());
        
        try (BufferedReader reader = Files.newBufferedReader(envPath)) {
            Properties props = new Properties();
            props.load(reader);
            
            System.out.println("[DEBUG] .env file contains LOCAL_LLM_ENDPOINT = " + props.getProperty("LOCAL_LLM_ENDPOINT"));
            
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key).trim();
                if (!value.isEmpty() && !value.contains("your_") && !value.contains("_here")) {
                    secrets.put(key, value);
                }
            }
            
            System.out.println("[SecretResolver] Loaded secrets from .env file");
        } catch (IOException e) {
            System.err.println("[SecretResolver] Error reading .env file: " + e.getMessage());
        }
    }
    
    /**
     * Loads from ~/.mindquest/credentials.properties
     */
    private void loadFromHomeCredentials() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) return;
        
        Path credPath = Paths.get(userHome, HOME_CREDENTIALS);
        
        if (!Files.exists(credPath)) {
            return;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(credPath)) {
            Properties props = new Properties();
            props.load(reader);
            
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key).trim();
                if (!value.isEmpty()) {
                    secrets.putIfAbsent(key, value); // Don't override .env values
                }
            }
            
            System.out.println("[SecretResolver] Loaded secrets from home credentials");
        } catch (IOException e) {
            System.err.println("[SecretResolver] Error reading home credentials: " + e.getMessage());
        }
    }
    
    /**
     * Loads from system environment variables (highest priority).
     */
    private void loadFromSystemEnv() {
        Map<String, String> env = System.getenv();
        
        // Only load known LLM-related keys
        String[] llmKeys = {
            "GOOGLE_API_KEY",      // Official SDK standard (recommended)
            "GEMINI_API_KEY",      // Legacy support
            "OPENAI_API_KEY",
            "LOCAL_LLM_ENDPOINT",
            "LOCAL_LLM_MODEL",
            "DEFAULT_LLM_PROVIDER"
        };
        
        for (String key : llmKeys) {
            String value = env.get(key);
            if (value != null && !value.isEmpty()) {
                System.out.println("[DEBUG] System env variable " + key + " = " + value + " (will OVERRIDE file values)");
                secrets.put(key, value); // Override file-based values
            }
        }
    }
    
    /**
     * Gets a secret by key.
     * @return secret value or null if not found
     */
    public String getSecret(String key) {
        return secrets.get(key);
    }
    
    /**
     * Gets a secret with a default value if not found.
     */
    public String getSecret(String key, String defaultValue) {
        return secrets.getOrDefault(key, defaultValue);
    }
    
    /**
     * Checks if a secret exists and is not empty.
     */
    public boolean hasSecret(String key) {
        String value = secrets.get(key);
        return value != null && !value.isEmpty();
    }
    
    /**
     * Lists all available secret keys (for debugging - does NOT expose values).
     */
    public Set<String> getAvailableKeys() {
        return new HashSet<>(secrets.keySet());
    }
    
    /**
     * Gets the Gemini API key.
     * Tries GOOGLE_API_KEY first (recommended), then falls back to legacy GEMINI_API_KEY.
     */
    public String getGeminiApiKey() {
        String key = getSecret("GOOGLE_API_KEY");
        if (key != null) {
            return key;
        }
        // Fallback to legacy key name
        return getSecret("GEMINI_API_KEY");
    }
    
    /**
     * Gets the local LLM endpoint.
     * Default: LM Studio's default port (1234)
     */
    public String getLocalLlmEndpoint() {
        String endpoint = getSecret("LOCAL_LLM_ENDPOINT", "http://localhost:1234/v1");
        System.out.println("[DEBUG] getLocalLlmEndpoint() returning: " + endpoint);
        return endpoint;
    }
    
    /**
     * Gets the local LLM model name.
     * LM Studio ignores this if only one model is loaded.
     */
    public String getLocalLlmModel() {
        return getSecret("LOCAL_LLM_MODEL", "local-model");
    }
    
    /**
     * Gets the default provider ID.
     */
    public String getDefaultProvider() {
        return getSecret("DEFAULT_LLM_PROVIDER", "gemini");
    }
}
