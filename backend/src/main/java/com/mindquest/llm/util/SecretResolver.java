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
        // 1. Load from .env file in project root
        loadFromEnvFile();
        
        // 2. Load from user home credentials file
        loadFromHomeCredentials();
        
        // 3. System environment variables override everything
        loadFromSystemEnv();
    }
    
    /**
     * Loads from .env file in the current directory or project root.
     */
    private void loadFromEnvFile() {
        Path envPath = Paths.get(ENV_FILE);
        
        if (!Files.exists(envPath)) {
            System.out.println("[SecretResolver] No .env file found in project root");
            return;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(envPath)) {
            Properties props = new Properties();
            props.load(reader);
            
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
     */
    public String getLocalLlmEndpoint() {
        return getSecret("LOCAL_LLM_ENDPOINT", "http://localhost:11434");
    }
    
    /**
     * Gets the local LLM model name.
     */
    public String getLocalLlmModel() {
        return getSecret("LOCAL_LLM_MODEL", "llama2");
    }
    
    /**
     * Gets the default provider ID.
     */
    public String getDefaultProvider() {
        return getSecret("DEFAULT_LLM_PROVIDER", "gemini");
    }
}
