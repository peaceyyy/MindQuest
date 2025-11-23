<script lang="ts">
	// Backend API Tester — exercises all GameServer endpoints
	let sessionId = $state('');
	let topic = $state('ai');
	let difficulty = $state('easy');
	let answer = $state('');
	let output = $state('Click a button to test backend endpoints');
	let loading = $state(false);

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

		<!-- Output -->
		<section class="border border-zinc-800 rounded-lg p-4 bg-zinc-900">
			<h2 class="text-xl font-semibold mb-3">Output</h2>
			<pre class="bg-black p-4 rounded overflow-auto text-xs text-zinc-300 max-h-96">{output}</pre>
		</section>

		<!-- Instructions -->
		<section class="text-sm text-zinc-500 border-t border-zinc-800 pt-4">
			<p class="font-semibold mb-2">Quick Test Flow:</p>
			<ol class="list-decimal list-inside space-y-1">
				<li>Check backend health</li>
				<li>Create a session (saves sessionId automatically)</li>
				<li>Start a round (select topic & difficulty)</li>
				<li>Get the current question</li>
				<li>Submit an answer (A, B, C, or D)</li>
				<li>Check session state to verify points/progress</li>
			</ol>
		</section>
	</div>
</div>
