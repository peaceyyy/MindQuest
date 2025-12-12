package com.mindquest.llm.diagnostic;

import com.mindquest.llm.exception.LlmException;
import com.mindquest.llm.util.SecretResolver;
import com.mindquest.llm.*;
import java.util.List;

/**
 * Simple diagnostic to test LLM provider loading and secret resolution.
 * Run this to verify the setup before integrating into the game.
 */
public class LlmDiagnostic {
    
    public static void main(String[] args) {
        System.out.println("=== MindQuest LLM Diagnostic ===\n");
        
        // Test 1: SecretResolver
        System.out.println("1. Testing SecretResolver:");
        SecretResolver secrets = new SecretResolver();
        System.out.println("   Available keys: " + secrets.getAvailableKeys());
        System.out.println("   Gemini API Key: " + maskSecret(secrets.getGeminiApiKey()));
        System.out.println("   Local LLM Endpoint: " + secrets.getLocalLlmEndpoint());
        System.out.println("   Default Provider: " + secrets.getDefaultProvider());
        System.out.println();
        
        // Test 2: ProviderRegistry
        System.out.println("2. Testing ProviderRegistry:");
        ProviderRegistry registry = new ProviderRegistry();
        List<String> providerIds = registry.listProviderIds();
        System.out.println("   Registered providers: " + providerIds);
        
        if (providerIds.isEmpty()) {
            System.err.println("   ERROR: No providers registered! Check ServiceLoader configuration.");
            return;
        }
        
        List<ProviderMetadata> providers = registry.listProviders();
        for (ProviderMetadata meta : providers) {
            System.out.println("   - " + meta);
        }
        System.out.println();
        
        // Test 3: Create and test MockProvider
        System.out.println("3. Testing MockProvider:");
        try {
            LlmProvider mockProvider = registry.createProvider("mock", null, null);
            System.out.println("   Created: " + mockProvider.getMetadata());
            System.out.println("   Connection test: " + mockProvider.testConnection());
            
            // Test completion
            Prompt testPrompt = new Prompt.Builder()
                .id("test-1")
                .instruction("Generate a test question about Java")
                .maxTokens(100)
                .temperature(0.7)
                .build();
            
            CompletionResult result = mockProvider.complete(testPrompt);
            System.out.println("   Response: " + result.getText());
            
            mockProvider.close();
            System.out.println("   Provider closed successfully");
        } catch (LlmException e) {
            System.err.println("   ERROR: " + e);
        }
        System.out.println();
        
        // Test 4: GeminiProvider (will fail without API key)
        System.out.println("4. Testing GeminiProvider:");
        try {
            String geminiKey = secrets.getGeminiApiKey();
            if (geminiKey == null || geminiKey.isEmpty() || geminiKey.contains("your_")) {
                System.out.println("   SKIP: No valid Gemini API key found (this is expected)");
                System.out.println("   To test Gemini: create .env file from .env.template and add your API key");
            } else {
                LlmProvider geminiProvider = registry.createProvider("gemini", geminiKey, null);
                System.out.println("   Created: " + geminiProvider.getMetadata());
                System.out.println("   Connection test: " + geminiProvider.testConnection());
                System.out.println("   Note: Actual API calls not yet implemented (skeleton only)");
                geminiProvider.close();
            }
        } catch (LlmException e) {
            System.out.println("   Expected error (skeleton implementation): " + e.getMessage());
        }
        System.out.println();
        
        System.out.println("=== Diagnostic Complete ===");
        System.out.println("\nNext steps:");
        System.out.println("1. Copy .env.template to .env and add your Gemini API key");
        System.out.println("2. Implement actual HTTP calls in GeminiProvider");
        System.out.println("3. Integrate LlmLoader into the game's question source system");
    }
    
    private static String maskSecret(String secret) {
        if (secret == null || secret.isEmpty()) {
            return "(not set)";
        }
        if (secret.contains("your_")) {
            return "(placeholder - not a real key)";
        }
        if (secret.length() <= 8) {
            return "***";
        }
        return secret.substring(0, 4) + "..." + secret.substring(secret.length() - 4);
    }
}
