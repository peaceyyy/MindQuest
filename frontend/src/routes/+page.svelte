<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	
	let topic = $state('ai');
	let difficulty = $state('easy');
	let globalPoints = $state(0);
	let loading = $state(false);
	
	onMount(async () => {
		// Try to get global points from a test session
		// (In a real app, you'd track this in localStorage or a persistent session)
		await loadGlobalPoints();
	});
	
	async function loadGlobalPoints() {
		try {
			// First check if we have a persisted career points value in localStorage
			const persisted = localStorage.getItem('mindquest:careerPoints');
			console.log('[DEBUG] Persisted career points:', persisted);
			if (persisted) {
				globalPoints = parseInt(persisted) || 0;
				console.log('[DEBUG] Loaded career points from localStorage:', globalPoints);
				return;
			}
			// Next, check for a lastSessionId we can query (useful when resuming)
			const lastSessionId = localStorage.getItem('mindquest:lastSessionId');
			console.log('[DEBUG] Last session ID:', lastSessionId);
			if (lastSessionId) {
				const stateRes = await fetch(`/api/sessions/${lastSessionId}/state`);
				if (stateRes.ok) {
					const state = await stateRes.json();
					globalPoints = state.globalPoints || 0;
					console.log('[DEBUG] Loaded from session state:', globalPoints);
					return;
				}
			}
			// Fallback: zero
			globalPoints = 0;
			console.log('[DEBUG] No saved points, defaulting to 0');
		} catch (err) {
			console.log('Could not load global points:', err);
			globalPoints = 0;
		}
	}
	
	function startGame() {
		// Navigate to play page with topic & difficulty as URL params
		goto(`/play?topic=${topic}&difficulty=${difficulty}`);
	}
</script>

<div class="container">
	<header>
		<h1>MindQuest</h1>
		<p>Test your knowledge across multiple domains</p>
		<div class="global-points">Career Points: {globalPoints}</div>
	</header>

	<main>
		<section class="config">
			<div class="field">
				<label for="topic">Topic</label>
				<select id="topic" bind:value={topic}>
					<option value="ai">Artificial Intelligence</option>
					<option value="cs">Computer Science</option>
					<option value="philosophy">Philosophy</option>
				</select>
			</div>

			<div class="field">
				<label for="difficulty">Difficulty</label>
				<select id="difficulty" bind:value={difficulty}>
					<option value="easy">Easy</option>
					<option value="medium">Medium</option>
					<option value="hard">Hard</option>
				</select>
			</div>

			<button class="start-btn" onclick={startGame}>Start Game</button>
		</section>

		<footer>
			<a href="/test">API Tester</a>
		</footer>
	</main>
</div>

<style>
	.container {
		max-width: 600px;
		margin: 2rem auto;
		padding: 1rem;
	}

	header {
		text-align: center;
		margin-bottom: 3rem;
	}

	h1 {
		font-size: 2.5rem;
		margin-bottom: 0.5rem;
	}

	.global-points {
		margin-top: 1rem;
		padding: 0.75rem 1.5rem;
		background: #dbeafe;
		color: #1e40af;
		border-radius: 6px;
		font-weight: 600;
		font-size: 1.125rem;
		display: inline-block;
	}

	.config {
		display: flex;
		flex-direction: column;
		gap: 1.5rem;
	}

	.field {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	label {
		font-weight: 600;
	}

	select {
		padding: 0.75rem;
		font-size: 1rem;
		border: 1px solid #ccc;
		border-radius: 4px;
	}

	.start-btn {
		padding: 1rem 2rem;
		font-size: 1.125rem;
		font-weight: 600;
		background: #3b82f6;
		color: white;
		border: none;
		border-radius: 4px;
		cursor: pointer;
		margin-top: 1rem;
	}

	.start-btn:hover {
		background: #2563eb;
	}

	footer {
		margin-top: 3rem;
		text-align: center;
		font-size: 0.875rem;
	}

	footer a {
		color: #6b7280;
		text-decoration: underline;
	}
</style>
