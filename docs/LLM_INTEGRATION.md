# LLM Integration Guide

This guide explains how to configure and use the LLM (Large Language Model) integration in MindQuest.

## Overview

MindQuest supports **pluggable LLM providers** for dynamic question generation. The system is designed with interfaces and ServiceLoader (SPI) so you can easily add new providers without modifying core code.

### Currently Supported Providers

- **Gemini** (`gemini`) - Google's Gemini API (default)
- **Mock** (`mock`) - Testing provider with canned responses
- **Local LLM** (planned) - For Ollama, LM Studio, etc.

---

## Quick Start

### 1. Set up your API key

Copy the template file and add your API key:

```powershell
# Copy template
copy .env.template .env

# Edit .env and replace placeholder with your actual key
# GEMINI_API_KEY=your_actual_api_key_here
```

**Important:** `.env` is gitignored. Never commit real API keys!

### 2. Get a Gemini API key

1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy the key and paste it into `.env`

### 3. Test the integration

Run the diagnostic tool to verify everything works:

```powershell
mvn compile
java -cp target/classes com.mindquest.llm.LlmDiagnostic
```

Expected output:

```
=== MindQuest LLM Diagnostic ===

1. Testing SecretResolver:
   Gemini API Key: AIza...xxxx (masked)
   Default Provider: gemini

2. Testing ProviderRegistry:
   Registered providers: [gemini, mock]

3. Testing MockProvider:
   Connection test: true
   Response: Mock LLM response for testing

=== Diagnostic Complete ===
```

---

### How It Works

1. **ServiceLoader Discovery**: `ProviderRegistry` uses Java's ServiceLoader to find all `LlmProviderFactory` implementations registered in `META-INF/services/com.mindquest.llm.LlmProviderFactory`.
2. **API Key Resolution**: `SecretResolver` reads from `.env` or environment variables.
3. **Provider Selection**: Game code requests a provider by ID (e.g., `"gemini"`), registry creates an instance with the resolved API key.
4. **Prompt Execution**: Call `provider.complete(prompt)` for sync or `provider.stream(prompt)` for streaming responses.

---

## Adding a New Provider

Want to add support for OpenAI, Anthropic, or a local LLM? Here's how:

### Step 1: Implement `LlmProvider`

```java
package com.mindquest.llm.providers;

import com.mindquest.llm.*;
import java.util.concurrent.Flow;

public class MyProvider implements LlmProvider {
  
    private final String apiKey;
  
    public MyProvider(String apiKey, ProviderOptions options) {
        this.apiKey = apiKey;
        // Initialize HTTP client, configure endpoint, etc.
    }
  
    @Override
    public ProviderMetadata getMetadata() {
        return new ProviderMetadata(
            "my-provider",
            "My LLM Provider",
            "model-name",
            true, // supports streaming
            "https://api.example.com"
        );
    }
  
    @Override
    public CompletionResult complete(Prompt prompt) throws LlmException {
        // Make HTTP request to your API
        // Parse response
        // Return CompletionResult
    }
  
    @Override
    public Flow.Publisher<StreamEvent> stream(Prompt prompt) throws LlmException {
        // Implement streaming if supported
    }
  
    @Override
    public boolean testConnection() {
        // Test API connectivity
    }
  
    @Override
    public void close() {
        // Cleanup resources
    }
}
```

### Step 2: Create Factory

```java
package com.mindquest.llm.providers;

import com.mindquest.llm.*;

public class MyProviderFactory implements LlmProviderFactory {
  
    @Override
    public String getProviderId() {
        return "my-provider";
    }
  
    @Override
    public String getDisplayName() {
        return "My LLM Provider";
    }
  
    @Override
    public LlmProvider create(String apiKey, ProviderOptions options) throws LlmException {
        return new MyProvider(apiKey, options);
    }
}
```

### Step 3: Register via ServiceLoader

Add your factory to `src/main/resources/META-INF/services/com.mindquest.llm.LlmProviderFactory`:

```
com.mindquest.llm.providers.MockProviderFactory
com.mindquest.llm.providers.GeminiProviderFactory
com.mindquest.llm.providers.MyProviderFactory
```

### Step 4: Test

```java
ProviderRegistry registry = new ProviderRegistry();
LlmProvider provider = registry.createProvider("my-provider", "api-key", null);
```

---

---

## Troubleshooting

### "No LLM providers found via ServiceLoader"

- Check that `META-INF/services/com.mindquest.llm.LlmProviderFactory` exists
- Verify factory classes are listed with fully-qualified names
- Run `mvn clean compile` to rebuild

### "Gemini API key is required"

- Ensure `.env` file exists and contains `GEMINI_API_KEY=...`
- Check that the key doesn't contain placeholder text (`your_gemini_api_key_here`)
- Try setting as environment variable: `$env:GEMINI_API_KEY="your-key"`

---

## Next Steps

1. **Implement Gemini HTTP calls** - See `GeminiProvider.java` TODOs
2. **Add LlmLoader service** - Wrap provider usage with retry/fallback
3. **Integrate into game** - Add LLM-based question source
4. **Add local LLM support** - For Ollama/LM Studio
5. **Write unit tests** - Test with MockProvider

---

## API Documentation

### Gemini API

- [Official Docs](https://ai.google.dev/api/rest)
- [Quickstart](https://ai.google.dev/tutorials/rest_quickstart)
- Endpoint: `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
- Authentication: `x-goog-api-key` header

### Request Format

```json
{
  "contents": [{
    "parts": [{
      "text": "Your prompt here"
    }]
  }],
  "generationConfig": {
    "temperature": 0.7,
    "maxOutputTokens": 1000
  }
}
```
