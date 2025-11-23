<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	
	// Game state
	let sessionId = $state('');
	let currentQuestion = $state<any>(null);
	let selectedAnswer = $state('');
	let feedback = $state<any>(null);
	let loading = $state(false);
	let error = $state('');
	let roundComplete = $state(false);
	
	// Config from URL params
	let topic = $state('');
	let difficulty = $state('');
	
	// Stats
	let totalPoints = $state(0);
	let questionsAnswered = $state(0);
	
	onMount(async () => {
		// Get topic & difficulty from URL params
		topic = $page.url.searchParams.get('topic') || 'ai';
		difficulty = $page.url.searchParams.get('difficulty') || 'easy';
		
		// Initialize game flow
		await initializeGame();
	});
	
	async function initializeGame() {
		try {
			loading = true;
			error = '';
			
			// 1. Create session
			const sessionRes = await fetch('/api/sessions', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({})
			});
			
			if (!sessionRes.ok) throw new Error('Failed to create session');
			
			const sessionData = await sessionRes.json();
			sessionId = sessionData.sessionId;
			
			// 2. Start round
			const roundRes = await fetch(`/api/sessions/${sessionId}/start`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ topic, difficulty })
			});
			
			if (!roundRes.ok) throw new Error('Failed to start round');
			
			// 3. Load first question
			await loadQuestion();
			
		} catch (err: any) {
			error = err.message || 'Failed to initialize game';
		} finally {
			loading = false;
		}
	}
	
	async function loadQuestion() {
		try {
			loading = true;
			error = '';
			feedback = null;
			selectedAnswer = '';
			
			const res = await fetch(`/api/sessions/${sessionId}/question`);
			
			if (res.status === 204) {
				// Round complete
				roundComplete = true;
				await loadFinalStats();
				return;
			}
			
			if (!res.ok) {
				throw new Error('Failed to load question');
			}
			
			const rawText = await res.text();
			currentQuestion = JSON.parse(rawText);
			
		} catch (err: any) {
			error = err.message || 'Failed to load question';
		} finally {
			loading = false;
		}
	}
	
	async function submitAnswer() {
		if (!selectedAnswer) {
			error = 'Please select an answer';
			return;
		}
		
		try {
			loading = true;
			error = '';
			
			const res = await fetch(`/api/sessions/${sessionId}/answer`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ answer: selectedAnswer })
			});
			
			if (!res.ok) throw new Error('Failed to submit answer');
			
			const result = await res.json();
			feedback = result;
			questionsAnswered++;
			
			// FIXED: Accumulate points instead of replacing
			totalPoints += (result.pointsAwarded || 0);
			
			// Check if round is complete
			if (result.roundComplete) {
				roundComplete = true;
			}
			
		} catch (err: any) {
			error = err.message || 'Failed to submit answer';
		} finally {
			loading = false;
		}
	}
	
	async function nextQuestion() {
		feedback = null;
		await loadQuestion();
	}
	
	async function loadFinalStats() {
		try {
			const res = await fetch(`/api/sessions/${sessionId}/state`);
			if (res.ok) {
				const state = await res.json();
				totalPoints = state.globalPoints || 0;
			}
		} catch (err) {
			console.error('Failed to load final stats:', err);
		}
	}
	
	function goToResults() {
		goto(`/results?points=${totalPoints}&answered=${questionsAnswered}`);
	}
	
	function backToHome() {
		goto('/');
	}
</script>

<div class="container">
	{#if loading && !currentQuestion}
		<div class="loading">Loading game...</div>
	{:else if error && !currentQuestion}
		<div class="error-screen">
			<p class="error">{error}</p>
			<button onclick={backToHome}>Back to Home</button>
		</div>
	{:else if roundComplete}
		<div class="complete-screen">
			<h2>Round Complete!</h2>
			<p class="stats">Total Points: {totalPoints}</p>
			<p class="stats">Questions Answered: {questionsAnswered}</p>
			<button onclick={goToResults}>View Results</button>
			<button onclick={backToHome}>Back to Home</button>
		</div>
	{:else if currentQuestion}
		<div class="game">
			<header class="game-header">
				<div class="info">
					<span>Topic: {topic.toUpperCase()}</span>
					<span>Difficulty: {difficulty}</span>
				</div>
				<div class="stats-bar">
					<span>Points: {totalPoints}</span>
					<span>Answered: {questionsAnswered}</span>
					<button class="quit-btn" onclick={backToHome}>Quit Game</button>
				</div>
			</header>

			<div class="question-card">
				<p class="question-text">{currentQuestion.questionText}</p>

				<div class="choices">
					{#each currentQuestion.choices as choice, index}
						<label class="choice">
							<input
								type="radio"
								name="answer"
								value={['A', 'B', 'C', 'D'][index]}
								bind:group={selectedAnswer}
								disabled={feedback !== null}
							/>
							<span class="choice-label">
								{['A', 'B', 'C', 'D'][index]}. {choice}
							</span>
						</label>
					{/each}
				</div>

				{#if feedback}
					<div class="feedback {feedback.correct ? 'correct' : 'incorrect'}">
						<p>{feedback.correct ? '✓ Correct!' : '✗ Incorrect'}</p>
						<p>Points: {feedback.pointsAwarded}</p>
						{#if !feedback.correct}
							<p>Correct answer: {['A', 'B', 'C', 'D'][feedback.correctIndex]}</p>
						{/if}
					</div>
				{/if}

				{#if error}
					<p class="error">{error}</p>
				{/if}

				<div class="actions">
					{#if !feedback}
						<button onclick={submitAnswer} disabled={loading || !selectedAnswer}>
							{loading ? 'Submitting...' : 'Submit Answer'}
						</button>
					{:else}
						<button onclick={nextQuestion} disabled={loading}>
							{loading ? 'Loading...' : 'Next Question'}
						</button>
					{/if}
				</div>
			</div>
		</div>
	{/if}
</div>

<style>
	.container {
		max-width: 800px;
		margin: 2rem auto;
		padding: 1rem;
	}

	.loading,
	.error-screen,
	.complete-screen {
		text-align: center;
		padding: 3rem 1rem;
	}

	.error {
		color: #dc2626;
		margin-bottom: 1rem;
	}

	.game-header {
		display: flex;
		justify-content: space-between;
		margin-bottom: 2rem;
		padding-bottom: 1rem;
		border-bottom: 1px solid #e5e7eb;
	}

	.info,
	.stats-bar {
		display: flex;
		align-items: center;
		gap: 1.5rem;
		font-size: 0.875rem;
	}

	.quit-btn {
		padding: 0.5rem 1rem;
		font-size: 0.875rem;
		font-weight: 600;
		background: #ef4444;
		color: white;
		border: none;
		border-radius: 4px;
		cursor: pointer;
		transition: background 0.2s;
	}

	.quit-btn:hover {
		background: #dc2626;
	}

	.question-card {
		background: #f9fafb;
		border: 1px solid #e5e7eb;
		border-radius: 8px;
		padding: 2rem;
	}

	.question-text {
		font-size: 1.25rem;
		font-weight: 500;
		margin-bottom: 2rem;
		line-height: 1.6;
	}

	.choices {
		display: flex;
		flex-direction: column;
		gap: 1rem;
		margin-bottom: 2rem;
	}

	.choice {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		padding: 1rem;
		background: white;
		border: 2px solid #e5e7eb;
		border-radius: 6px;
		cursor: pointer;
		transition: all 0.2s;
	}

	.choice:hover {
		border-color: #3b82f6;
		background: #eff6ff;
	}

	.choice input[type="radio"] {
		width: 18px;
		height: 18px;
	}

	.choice-label {
		flex: 1;
		font-size: 1rem;
	}

	.feedback {
		padding: 1rem;
		border-radius: 6px;
		margin-bottom: 1.5rem;
	}

	.feedback.correct {
		background: #d1fae5;
		color: #065f46;
	}

	.feedback.incorrect {
		background: #fee2e2;
		color: #991b1b;
	}

	.actions {
		display: flex;
		gap: 1rem;
	}

	button {
		flex: 1;
		padding: 0.875rem 1.5rem;
		font-size: 1rem;
		font-weight: 600;
		background: #3b82f6;
		color: white;
		border: none;
		border-radius: 6px;
		cursor: pointer;
	}

	button:hover:not(:disabled) {
		background: #2563eb;
	}

	button:disabled {
		background: #9ca3af;
		cursor: not-allowed;
	}

	.stats {
		font-size: 1.25rem;
		margin: 0.5rem 0;
	}

	h2 {
		font-size: 2rem;
		margin-bottom: 1.5rem;
	}
</style>
