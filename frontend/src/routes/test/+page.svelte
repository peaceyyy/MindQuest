<script lang="ts">
	// Backend API Tester — exercises all GameServer endpoints
	let sessionId = $state('');
	let topic = $state('ai');
	let difficulty = $state('easy');
	let answer = $state('');
	let output = $state('Click a button to test backend endpoints');
	let loading = $state(false);
	
	// Local LLM test state
	let llmProviders = $state<any[]>([]);
	let localLlmStatus = $state<any>(null);
	let localTestResult = $state<any>(null);
	let localLlmLoading = $state(false);
	
	// Gemini test state
	let geminiLoading = $state(false);
	let geminiTestResult = $state<any>(null);

	async function testHealth() {
		loading = true;
		try {
			const res = await fetch('/health');
			const data = await res.json();
			output = JSON.stringify(data, null, 2);
		} catch (err) {
			output = `Error: ${err}`;
		} finally {
			loading = false;
		}
	}

	async function createSession() {
		loading = true;
		try {
			const res = await fetch('/api/sessions', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({})
			});
			const data = await res.json();
			sessionId = data.sessionId || '';
			output = JSON.stringify(data, null, 2);
		} catch (err) {
			output = `Error: ${err}`;
		} finally {
			loading = false;
		}
	}

	async function startRound() {
		if (!sessionId) {
			output = 'Error: Create a session first';
			return;
		}
		loading = true;
		try {
			const res = await fetch(`/api/sessions/${sessionId}/start`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ topic, difficulty })
			});
			const data = await res.json();
			output = JSON.stringify(data, null, 2);
		} catch (err) {
			output = `Error: ${err}`;
		} finally {
			loading = false;
		}
	}

	async function getQuestion() {
		if (!sessionId) {
			output = 'Error: Create a session first';
			return;
		}
		loading = true;
		try {
			const res = await fetch(`/api/sessions/${sessionId}/question`);
			
			// DIAGNOSTIC: Log raw response text before parsing
			const rawText = await res.text();
			console.log('[DEBUG] Raw /question response:', rawText);
			
			if (!res.ok) {
				output = `Error ${res.status}: ${rawText}`;
				return;
			}
			
			try {
				const data = JSON.parse(rawText);
				output = JSON.stringify(data, null, 2);
			} catch (parseErr) {
				console.error('[ERROR] JSON parse failed:', parseErr);
				output = `JSON Parse Error: ${parseErr}\n\nRaw response:\n${rawText}`;
			}
		} catch (err) {
			console.error('[ERROR] Fetch failed:', err);
			output = `Network Error: ${err}`;
		} finally {
			loading = false;
		}
	}

	async function submitAnswer() {
		if (!sessionId) {
			output = 'Error: Create a session first';
			return;
		}
		loading = true;
		try {
			const res = await fetch(`/api/sessions/${sessionId}/answer`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ answer: answer.trim().toUpperCase() }) // Send letter (A/B/C/D)
			});
			
			if (!res.ok) {
				const errorText = await res.text();
				output = `Error ${res.status}: ${errorText}`;
				return;
			}
			
			const data = await res.json();
			output = JSON.stringify(data, null, 2);
		} catch (err) {
			console.error('[ERROR] Submit answer failed:', err);
			output = `Error: ${err}`;
		} finally {
			loading = false;
		}
	}

	async function getSessionState() {
		if (!sessionId) {
			output = 'Error: Create a session first';
			return;
		}
		loading = true;
		try {
			const res = await fetch(`/api/sessions/${sessionId}/state`);
			const data = await res.json();
			output = JSON.stringify(data, null, 2);
		} catch (err) {
			output = `Error: ${err}`;
		} finally {
			loading = false;
		}
	}
	
	// ============ Local LLM Test Functions ============
	
	async function getLlmProviders() {
		localLlmLoading = true;
		try {
			const res = await fetch('/api/llm/providers');
			const data = await res.json();
			llmProviders = data.providers || [];
			output = JSON.stringify(data, null, 2);
		} catch (err) {
			output = `Error: ${err}`;
		} finally {
			localLlmLoading = false;
		}
	}
	
	async function getLocalLlmStatus() {
		localLlmLoading = true;
		try {
			const res = await fetch('/api/llm/local/status');
			const data = await res.json();
			localLlmStatus = data;
			output = JSON.stringify(data, null, 2);
		} catch (err) {
			output = `Error: ${err}`;
		} finally {
			localLlmLoading = false;
		}
	}
	
	async function testLocalLlm() {
		localLlmLoading = true;
		localTestResult = null;
		output = '⏳ Sending test prompt to Local LLM... (this may take 5-30 seconds depending on your hardware)';
		try {
			const startTime = Date.now();
			const res = await fetch('/api/llm/local/test', { method: 'POST' });
			const data = await res.json();
			const clientElapsed = Date.now() - startTime;
			
			localTestResult = {
				...data,
				clientElapsedMs: clientElapsed
			};
			output = JSON.stringify(localTestResult, null, 2);
		} catch (err) {
			output = `Error: ${err}`;
			localTestResult = { success: false, error: String(err) };
		} finally {
			localLlmLoading = false;
		}
	}
	
	// Direct LM Studio test (bypasses Java backend)
	// NOTE: This will likely fail due to CORS - browsers block cross-origin requests
	// Use the /api/llm/* endpoints instead which go through the Java backend
	async function testLmStudioDirect() {
		localLlmLoading = true;
		output = '⏳ Testing LM Studio directly at localhost:1234...\n⚠️ Note: This may fail due to CORS. Use the Java backend endpoints instead.';
		try {
			// First test /v1/models
			const modelsRes = await fetch('http://localhost:1234/v1/models');
			if (!modelsRes.ok) {
				output = `❌ LM Studio /v1/models failed with status ${modelsRes.status}`;
				return;
			}
			const modelsData = await modelsRes.json();
			
			// Now test a simple chat completion
			const chatRes = await fetch('http://localhost:1234/v1/chat/completions', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({
					model: modelsData.data?.[0]?.id || 'local-model',
					messages: [
						{ role: 'user', content: 'Say "Hello from LM Studio!" in one short sentence.' }
					],
					temperature: 0.7,
					max_tokens: 50,
					stream: false
				})
			});
			
			const chatData = await chatRes.json();
			
			output = JSON.stringify({
				modelsEndpoint: '✅ Working',
				availableModels: modelsData.data?.map((m: any) => m.id) || [],
				chatCompletion: chatRes.ok ? '✅ Working' : `❌ Status ${chatRes.status}`,
				response: chatData.choices?.[0]?.message?.content || chatData,
				rawChatResponse: chatData
			}, null, 2);
			
		} catch (err) {
			output = `❌ Direct LM Studio test failed (CORS blocked):\n${err}\n\n` +
				`This is EXPECTED behavior - browsers block cross-origin requests.\n` +
				`Use the Java backend endpoints instead:\n` +
				`  • GET /api/llm/providers\n` +
				`  • GET /api/llm/local/status\n` +
				`  • POST /api/llm/local/test`;
		} finally {
			localLlmLoading = false;
		}
	}
	
	// ============ Gemini Test Functions ============
	
	async function getGeminiStatus() {
		geminiLoading = true;
		try {
			const res = await fetch('/api/gemini/status');
			const data = await res.json();
			output = JSON.stringify(data, null, 2);
		} catch (err) {
			output = `Error: ${err}`;
		} finally {
			geminiLoading = false;
		}
	}
	
	async function testGeminiNetwork() {
		geminiLoading = true;
		output = '⏳ Testing network connectivity to Gemini API...';
		try {
			const res = await fetch('/api/gemini/network-test');
			const data = await res.json();
			output = JSON.stringify(data, null, 2);
		} catch (err) {
			output = `Error: ${err}`;
		} finally {
			geminiLoading = false;
		}
	}
	
	async function testGeminiGenerate() {
		geminiLoading = true;
		geminiTestResult = null;
		output = '⏳ Generating test questions with Gemini... (this may take 5-15 seconds)';
		try {
			const startTime = Date.now();
			const res = await fetch('/api/gemini/generate', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({
					topic: 'general knowledge',
					difficulty: 'easy',
					count: 2
				})
			});
			const data = await res.json();
			const clientElapsed = Date.now() - startTime;
			
			geminiTestResult = {
				success: res.ok,
				...data,
				clientElapsedMs: clientElapsed
			};
			output = JSON.stringify(geminiTestResult, null, 2);
		} catch (err) {
			output = `Error: ${err}`;
			geminiTestResult = { success: false, error: String(err) };
		} finally {
			geminiLoading = false;
		}
	}
</script>

<div class="min-h-screen bg-zinc-950 text-zinc-50 p-8">
	<div class="max-w-4xl mx-auto space-y-6">
		<h1 class="text-3xl font-bold mb-8">MindQuest Backend Tester</h1>

		<!-- Health Check -->
		<section class="border border-zinc-800 rounded-lg p-4 bg-zinc-900">
			<h2 class="text-xl font-semibold mb-3">1. Health Check</h2>
			<button
				onclick={testHealth}
				disabled={loading}
				class="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-zinc-700 rounded text-sm font-medium transition"
			>
				GET /health
			</button>
		</section>

		<!-- Session Management -->
		<section class="border border-zinc-800 rounded-lg p-4 bg-zinc-900">
			<h2 class="text-xl font-semibold mb-3">2. Create Session</h2>
			<button
				onclick={createSession}
				disabled={loading}
				class="px-4 py-2 bg-green-600 hover:bg-green-700 disabled:bg-zinc-700 rounded text-sm font-medium transition"
			>
				POST /api/sessions
			</button>
			{#if sessionId}
				<p class="mt-2 text-sm text-zinc-400">Session ID: <code class="text-blue-400">{sessionId}</code></p>
			{/if}
		</section>

		<!-- Start Round -->
		<section class="border border-zinc-800 rounded-lg p-4 bg-zinc-900">
			<h2 class="text-xl font-semibold mb-3">3. Start Round</h2>
			<div class="flex gap-3 items-end flex-wrap">
				<label class="flex flex-col gap-1">
					<span class="text-sm text-zinc-400">Topic</span>
					<select
						bind:value={topic}
						class="px-3 py-2 bg-zinc-800 border border-zinc-700 rounded text-sm"
					>
						<option value="ai">AI</option>
						<option value="cs">CS</option>
						<option value="philosophy">Philosophy</option>
					</select>
				</label>
				<label class="flex flex-col gap-1">
					<span class="text-sm text-zinc-400">Difficulty</span>
					<select
						bind:value={difficulty}
						class="px-3 py-2 bg-zinc-800 border border-zinc-700 rounded text-sm"
					>
						<option value="easy">Easy</option>
						<option value="medium">Medium</option>
						<option value="hard">Hard</option>
					</select>
				</label>
				<button
					onclick={startRound}
					disabled={loading || !sessionId}
					class="px-4 py-2 bg-purple-600 hover:bg-purple-700 disabled:bg-zinc-700 rounded text-sm font-medium transition"
				>
					POST /api/sessions/:id/start
				</button>
			</div>
		</section>

		<!-- Get Question -->
		<section class="border border-zinc-800 rounded-lg p-4 bg-zinc-900">
			<h2 class="text-xl font-semibold mb-3">4. Get Current Question</h2>
			<button
				onclick={getQuestion}
				disabled={loading || !sessionId}
				class="px-4 py-2 bg-orange-600 hover:bg-orange-700 disabled:bg-zinc-700 rounded text-sm font-medium transition"
			>
				GET /api/sessions/:id/question
			</button>
		</section>

		<!-- Submit Answer -->
		<section class="border border-zinc-800 rounded-lg p-4 bg-zinc-900">
			<h2 class="text-xl font-semibold mb-3">5. Submit Answer</h2>
			<div class="flex gap-3 items-end flex-wrap">
				<label class="flex flex-col gap-1 flex-1 min-w-[200px]">
					<span class="text-sm text-zinc-400">Answer</span>
					<input
						type="text"
						bind:value={answer}
						placeholder="Enter answer (A, B, C, D)"
						class="px-3 py-2 bg-zinc-800 border border-zinc-700 rounded text-sm"
					/>
				</label>
				<button
					onclick={submitAnswer}
					disabled={loading || !sessionId}
					class="px-4 py-2 bg-red-600 hover:bg-red-700 disabled:bg-zinc-700 rounded text-sm font-medium transition"
				>
					POST /api/sessions/:id/answer
				</button>
			</div>
		</section>

		<!-- Get Session State -->
		<section class="border border-zinc-800 rounded-lg p-4 bg-zinc-900">
			<h2 class="text-xl font-semibold mb-3">6. Get Session State</h2>
			<button
				onclick={getSessionState}
				disabled={loading || !sessionId}
				class="px-4 py-2 bg-cyan-600 hover:bg-cyan-700 disabled:bg-zinc-700 rounded text-sm font-medium transition"
			>
				GET /api/sessions/:id/state
			</button>
		</section>

		<!-- Local LLM Testing Section -->
		<section class="border-2 border-yellow-600 rounded-lg p-4 bg-zinc-900">
			<h2 class="text-xl font-semibold mb-3 text-yellow-400">🧪 Local LLM (LM Studio) Testing</h2>
			<p class="text-sm text-zinc-400 mb-4">Test your local LM Studio integration</p>
			
			<div class="flex gap-3 flex-wrap mb-4">
				<button
					onclick={getLlmProviders}
					disabled={localLlmLoading}
					class="px-4 py-2 bg-yellow-600 hover:bg-yellow-700 disabled:bg-zinc-700 rounded text-sm font-medium transition"
				>
					GET /api/llm/providers
				</button>
				
				<button
					onclick={getLocalLlmStatus}
					disabled={localLlmLoading}
					class="px-4 py-2 bg-yellow-600 hover:bg-yellow-700 disabled:bg-zinc-700 rounded text-sm font-medium transition"
				>
					GET /api/llm/local/status
				</button>
				
				<button
					onclick={testLocalLlm}
					disabled={localLlmLoading}
					class="px-4 py-2 bg-green-600 hover:bg-green-700 disabled:bg-zinc-700 rounded text-sm font-medium transition"
				>
					POST /api/llm/local/test
				</button>
			</div>
			
			<div class="border-t border-zinc-700 pt-4 mt-4">
				<p class="text-sm text-zinc-500 mb-2">🔧 Direct LM Studio Test (bypasses Java backend):</p>
				<button
					onclick={testLmStudioDirect}
					disabled={localLlmLoading}
					class="px-4 py-2 bg-purple-600 hover:bg-purple-700 disabled:bg-zinc-700 rounded text-sm font-medium transition"
				>
					Test LM Studio Directly (localhost:1234)
				</button>
			</div>
			
			{#if localLlmLoading}
				<div class="mt-4 flex items-center gap-2 text-yellow-400">
					<span class="animate-spin">⏳</span>
					<span>Processing... (local inference can take 5-30 seconds)</span>
				</div>
			{/if}
			
			{#if localTestResult}
				<div class="mt-4 p-3 rounded {localTestResult.success ? 'bg-green-900/30 border border-green-700' : 'bg-red-900/30 border border-red-700'}">
					<p class="font-semibold {localTestResult.success ? 'text-green-400' : 'text-red-400'}">
						{localTestResult.success ? '✅ Local LLM is working!' : '❌ Local LLM test failed'}
					</p>
					{#if localTestResult.response}
						<p class="text-sm text-zinc-300 mt-2">Response: "{localTestResult.response}"</p>
					{/if}
					{#if localTestResult.elapsedMs}
						<p class="text-xs text-zinc-500 mt-1">Server time: {localTestResult.elapsedMs}ms | Client time: {localTestResult.clientElapsedMs}ms</p>
					{/if}
				</div>
			{/if}
		</section>

		<!-- Gemini AI Testing Section -->
		<section class="border-2 border-blue-600 rounded-lg p-4 bg-zinc-900">
			<h2 class="text-xl font-semibold mb-3 text-blue-400">🤖 Gemini AI Testing</h2>
			<p class="text-sm text-zinc-400 mb-4">Test Google Gemini API integration (requires GEMINI_API_KEY in .env)</p>
			
			<div class="flex gap-3 flex-wrap mb-4">
				<button
					onclick={getGeminiStatus}
					disabled={geminiLoading}
					class="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-zinc-700 rounded text-sm font-medium transition"
				>
					GET /api/gemini/status
				</button>
				
				<button
					onclick={testGeminiNetwork}
					disabled={geminiLoading}
					class="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-zinc-700 rounded text-sm font-medium transition"
				>
					GET /api/gemini/network-test
				</button>
				
				<button
					onclick={testGeminiGenerate}
					disabled={geminiLoading}
					class="px-4 py-2 bg-green-600 hover:bg-green-700 disabled:bg-zinc-700 rounded text-sm font-medium transition"
				>
					POST /api/gemini/generate (2 questions)
				</button>
			</div>
			
			{#if geminiLoading}
				<div class="mt-4 flex items-center gap-2 text-blue-400">
					<span class="animate-spin">⏳</span>
					<span>Calling Gemini API...</span>
				</div>
			{/if}
			
			{#if geminiTestResult}
				<div class="mt-4 p-3 rounded {geminiTestResult.success ? 'bg-green-900/30 border border-green-700' : 'bg-red-900/30 border border-red-700'}">
					<p class="font-semibold {geminiTestResult.success ? 'text-green-400' : 'text-red-400'}">
						{geminiTestResult.success ? '✅ Gemini is working!' : '❌ Gemini test failed'}
					</p>
					{#if geminiTestResult.questions}
						<p class="text-sm text-zinc-300 mt-2">Generated {geminiTestResult.questions.length} questions</p>
					{/if}
					{#if geminiTestResult.clientElapsedMs}
						<p class="text-xs text-zinc-500 mt-1">Response time: {geminiTestResult.clientElapsedMs}ms</p>
					{/if}
				</div>
			{/if}
		</section>

		<!-- Output -->
		<section class="border border-zinc-800 rounded-lg p-4 bg-zinc-900">
			<h2 class="text-xl font-semibold mb-3">Output</h2>
			<pre class="bg-black p-4 rounded overflow-auto text-xs text-zinc-300 max-h-96">{output}</pre>
		</section>

		

		<!-- Instructions -->
		<section class="text-sm text-zinc-500 border-t border-zinc-800 pt-4 space-y-6">
			<div>
				<p class="font-semibold mb-2 text-zinc-300">📋 Quick Test Flow - Game Session:</p>
				<ol class="list-decimal list-inside space-y-1">
					<li>Check backend health</li>
					<li>Create a session (saves sessionId automatically)</li>
					<li>Start a round (select topic & difficulty)</li>
					<li>Get the current question</li>
					<li>Submit an answer (A, B, C, or D)</li>
					<li>Check session state to verify points/progress</li>
				</ol>
			</div>
			
			<div>
				<p class="font-semibold mb-2 text-yellow-400">🧪 Quick Test Flow - Local LLM (LM Studio):</p>
				<ol class="list-decimal list-inside space-y-1">
					<li>Start LM Studio and load a model</li>
					<li>Enable local server: Developer → Start Server (port 1234)</li>
					<li>Click "GET /api/llm/providers" - should show "local" as available</li>
					<li>Click "GET /api/llm/local/status" - should show "online" with model list</li>
					<li>Click "POST /api/llm/local/test" - should return a response from your local model</li>
				</ol>
				<p class="text-xs text-zinc-600 mt-2">⚠️ The "Test LM Studio Directly" button will fail due to CORS - this is expected.</p>
			</div>
			
			<div>
				<p class="font-semibold mb-2 text-blue-400">🤖 Quick Test Flow - Gemini AI:</p>
				<ol class="list-decimal list-inside space-y-1">
					<li>Ensure GEMINI_API_KEY is set in your .env file</li>
					<li>Click "GET /api/gemini/status" - should show API key configured</li>
					<li>Click "GET /api/gemini/network-test" - tests connectivity to Google</li>
					<li>Click "POST /api/gemini/generate" - generates 2 test questions</li>
				</ol>
				<p class="text-xs text-zinc-600 mt-2">💡 Get your API key at: <a href="https://aistudio.google.com/apikey" target="_blank" class="text-blue-500 hover:underline">aistudio.google.com/apikey</a></p>
			</div>
			
			<div>
				<p class="font-semibold mb-2 text-zinc-300">🔧 Troubleshooting:</p>
				<ul class="list-disc list-inside space-y-1">
					<li><span class="text-red-400">503 Service Unavailable</span> - Java backend not running or LM Studio server not started</li>
					<li><span class="text-red-400">CORS error</span> - Use Java backend endpoints, not direct LM Studio calls</li>
					<li><span class="text-red-400">Timeout</span> - Local models can take 30+ seconds on CPU, ensure GPU acceleration</li>
					<li><span class="text-red-400">API key error</span> - Check .env file has correct GEMINI_API_KEY</li>
				</ul>
			</div>
		</section>
	</div>
</div>
