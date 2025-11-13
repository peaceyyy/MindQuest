# LLM Loader — Design & Interface Contract

Status: Draft

This document defines the design and minimal API contract for an LLM-based question loader (the "LLM Loader"). The goal is to add a pluggable LLM provider system to MindQuest with a default provider of Gemini. This document intentionally focuses on interfaces, DTO shapes, security considerations, and the in-game UX for providing API keys (no implementation yet).

## Goals

- Provide a clean Java interface for LLM providers so new providers (Gemini, OpenAI, others) can be plugged in via SPI or programmatic registration.
- Provide sync and streaming response modes.
- Keep secrets off the repo. Provide clear, minimal guidance to players on where to place their API keys (environment or local secure file) and a simple in-game instruction to check key presence / test connection.
- Provide a single `LlmLoader` service that the game can call to request question generation or to request raw LLM output that will later be parsed into `Question` objects.
- Add testability via a `MockProvider` and an SPI-based `ProviderRegistry`.

---

## High-level components

- `com.mindquest.llm` package (new)
  - Interfaces and DTOs
  - `ProviderRegistry` — discovers providers (ServiceLoader) and instantiates them
  - `LlmLoader` — application-facing service using providers to send prompts
  - `SecretStore` abstraction — abstracts where API keys are stored
  - Built-in providers (initial): `GeminiProvider` (skeleton), `MockProvider` (for tests)

---

## Where users should put their API key (in-game UX decision)

You decided to ask users to place their API keys somewhere and show an instruction in the game. Recommended options (document and support all; default to simplest to implement first):

1. Environment variable (recommended for power users)

   - Variable name: `GEMINI_API_KEY`
   - Pros: simple, platform-native, not stored in repo
   - Cons: less discoverable for non-technical users
2. Local credentials file in the user's home directory (recommended fallback)

   - Path: `%USERPROFILE%\\.mindquest\\credentials.properties` on Windows, `~/.mindquest/credentials.properties` on UNIX
   - File format (properties):
     - `gemini.api_key=YOUR_KEY_HERE`
   - File MUST be created with restrictive permissions. Document this requirement in-game and in README.
   - Pros: discoverable and persistent across runs
   - Cons: user must manage file permissions; still stored on disk
3. OS keychain / secure storage (future / recommended long-term)

   - Use native keychain (Windows Credential Manager, macOS Keychain, Linux secret service) via an existing Java library if available.
   - Pros: most secure; OS-managed
   - Cons: extra dependencies & cross-platform testing overhead

In-game instruction: show a short message under the source menu: "To use Gemini, set the GEMINI_API_KEY environment variable or create a file at ~/.mindquest/credentials.properties with `gemini.api_key=...`. Do NOT check keys into git."

---

## Environment & secret discovery priority (order of resolution)

When LlmLoader needs an API key, resolve in this order:

1. Explicit provider token passed programmatically (not persisted) — highest priority (useful for tests/advanced automation)
2. Environment variable `GEMINI_API_KEY`
3. Home credentials file (`~/.mindquest/credentials.properties`) — read-only
4. SecretStore implementation (if later added for OS keychain)

If none found, `LlmLoader` returns a clear error (AuthMissing) and the UI should prompt the user to follow the displayed instructions.

---

## [DTOs and Contracts (Java interfaces and POJOs)]()

These shapes are minimal and enough for initial integration. Later we can add richer metadata and streaming events.

### Prompt

- Purpose: container for a prompt request (metadata + instructions).
- Fields:
  - String id (optional): client-generated request id
  - String instruction: top-level instruction for the LLM
  - String context: optional context or seed text
  - int maxTokens
  - double temperature
  - boolean stream — whether provider should stream results
  - Map<String,String> hints — optional provider-specific options

Java sketch:

```java
public final class Prompt {
  private final String id;
  private final String instruction;
  private final String context;
  private final int maxTokens;
  private final double temperature;
  private final boolean stream;
  private final Map<String,String> hints;
  // getters, constructor, builder
}
```

### CompletionResult

- Purpose: the final aggregated response for non-streaming calls.
- Fields:
  - String id — request id
  - String text — full returned text
  - Map<String,Object> metadata — provider returned metadata (tokens used, model, raw)
  - ProviderInfo providerInfo — provider id, model name, latency, other metrics

```java
public final class CompletionResult {
  private final String id;
  private final String text;
  private final Map<String,Object> metadata;
}
```

### StreamEvent

- Purpose: an event emitted from streaming providers (partial text, done flag, error)
- Fields:
  - String requestId
  - String partialText
  - boolean done
  - Optional`<Throwable>` error

```java
public final class StreamEvent {
  private final String requestId;
  private final String partialText;
  private final boolean done;
  private final Throwable error;
}
```

### ProviderMetadata / ProviderInfo

- Provider id (e.g., "gemini"), model name, supported features (streaming: true/false), endpoint override, rate-limit info.

---

## Java interface contract

### LlmProvider (primary interface)

Purpose: unify provider implementations. Implementations MUST be thread-safe or document their safety.

Essential methods (sketch):

```java
public interface LlmProvider extends AutoCloseable {
    ProviderMetadata getMetadata();

    CompletionResult complete(Prompt prompt) throws LlmException;

    // Streaming API: consumer receives StreamEvent objects (blocking until complete)
    // Implementation choice: return a java.util.concurrent.Flow.Publisher<StreamEvent>
    Flow.Publisher<StreamEvent> stream(Prompt prompt) throws LlmException;

    void close();
}
```

Notes:

- `complete` is a blocking call that returns an aggregated result.
- `stream` returns a `Flow.Publisher<StreamEvent>` so callers can subscribe and receive partial updates. We can later adapt to other reactive libs if desired.
- `LlmException` is a checked/unchecked exception type carrying categories (AUTH, NETWORK, RATE_LIMIT, PARSE).

### LlmProviderFactory

Factory interface to create configured provider instances (used by SPI / ServiceLoader):

```java
public interface LlmProviderFactory {
    String getProviderId(); // e.g., "gemini"

    // Build provider given token and options (options may be null)
    LlmProvider create(String apiKey, ProviderOptions options);
}
```

This keeps provider instantiation outside the registry and allows providers to manage resource lifecycles.

### ProviderRegistry

Responsibilities:

- Discover `LlmProviderFactory` implementations via `ServiceLoader`.
- Expose `List<ProviderMetadata> listProviders()` and `LlmProvider createProvider(String id, String apiKey, ProviderOptions opts)`.

This component is lightweight and purely in-memory.

---

## LlmLoader service (application API)

Purpose: a single service class the game calls. It handles token resolution (env/home file), selects provider via ProviderRegistry, and calls provider APIs. It also centralizes retry/backoff and fallback behavior.

Public surface (sketch):

```java
public class LlmLoader {
  public LlmLoader(ProviderRegistry registry, SecretResolver secrets, LlmLoaderConfig config) { ... }

  public CompletionResult generate(Prompt prompt) throws LlmException { ... }

  public Flow.Publisher<StreamEvent> generateStream(Prompt prompt) throws LlmException { ... }

  public boolean testProviderConnection(String providerId, String apiKey); // performs a small test (e.g., ping)
}
```

Behavior:

- `generate` resolves API key via secrets resolver (unless caller supplied token explicitly in a `Prompt.hints` param) and instantiates provider through registry.
- For provider errors, LlmLoader should expose the error category to caller so the UI can react (AUTH error → show instructions; RATE_LIMIT → show wait suggestion; NETWORK → try again later).
- Retry/backoff is configurable on `LlmLoaderConfig` and applied to transient network failures.
- On final failure, callers may fallback to built-in hardcoded questions.

---

## Error handling and categories

Define `LlmException` with categories:

- AUTH (invalid/missing key)
- NETWORK
- RATE_LIMIT
- PARSE (unexpected response format)
- PROVIDER_ERROR (5xx)

Each exception includes the provider id and optionally the original HTTP response or status.

---

## Security & storage specifics

- Do NOT store API keys in project files or under `src/` or `resources/`.
- If using a home credentials file, document the required permissions and provide a small shell command to create it safely (for Windows `icacls`, for Unix `chmod 600`). Example instruction in README and in-game.

Example user instruction (to display in game under source menu):

"To use Gemini, set the environment variable GEMINI_API_KEY or create the file `~/.mindquest/credentials.properties` with `gemini.api_key=YOUR_KEY_HERE`. Make sure the file is only readable by you (chmod 600)."

---

## Prompting strategy (high level)

- For question generation: provide a stable prompt template that asks the LLM to output JSON following a schema (topic, difficulty, questionText, choices[], correctIndex). Example: request a list of N questions in strict JSON array format.
- Validate the JSON returned with a small parser and reject malformed items.
- If LLM returns free text, LlmLoader should not try to parse it into `Question` objects until a separate `LlmQuestionSource` implements the parsing rules.

---

## Testing strategy

- Implement `MockProvider` that returns deterministic responses for unit tests.
- Unit tests should not require a real API key.
- Integration tests with real providers are optional and must be guarded by environment variables and not be run in CI by default.

---

## Minimal Console UX flow (instructions only)

1. Player selects "Custom LLM / Gemini" source from source menu.
2. If no key is detected via env/home file, show a short message:
   - "No Gemini API key found. To use Gemini, set your key in the environment variable GEMINI_API_KEY or create a file at ~/.mindquest/credentials.properties with `gemini.api_key=...`. Press Enter to continue."
3. Provide an extra option under the source menu: "Test Gemini connection" — attempts a test request using resolved key and reports success/failure.

Note: The UI should never echo the API key. If the user wants to paste a key into the running game, the safer approach is to prompt and use it only for a single test call (do NOT persist unless the user chooses to create the file in their home directory manually).

---

## Next steps (implementation order)

1. Add `com.mindquest.llm` package and DTO + interface files (`Prompt`, `CompletionResult`, `StreamEvent`, `LlmProvider`, `LlmProviderFactory`, `LlmException`).
2. Implement `ProviderRegistry` using `ServiceLoader`.
3. Implement `MockProvider` and unit tests validating the registry and LlmLoader wiring.
4. Add `GeminiProvider` skeleton and `GeminiProviderFactory` (no network calls yet). Ensure it can be registered and instantiated.
5. Implement `LlmLoader` service and integrate error categories and retry logic (configurable).
6. Add console instructions and a "Test provider" flow that calls `LlmLoader.testProviderConnection`.

---

## Acceptance criteria (for initial phase)

- Design doc exists (this file) and reviewed.
- Interfaces and DTO signatures compiled cleanly (when created).
- `ProviderRegistry` can discover a `MockProvider` via SPI and `LlmLoader` can call the mock to receive canned responses.
- Console shows clear instruction on where to place API keys and a "Test provider" action.
