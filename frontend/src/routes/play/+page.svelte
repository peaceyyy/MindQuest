

<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import HealthBar from '$lib/components/battle/HealthBar.svelte';
	import Sprite from '$lib/components/battle/Sprite.svelte';
	import DialogueBox from '$lib/components/battle/DialogueBox.svelte';
	import ActionMenu from '$lib/components/battle/ActionMenu.svelte';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';
	
	// Game state
	let sessionId = $state('');
	let currentQuestion = $state<any>(null);
	let feedback = $state<any>(null);
	let loading = $state(false);
	let error = $state('');
	let roundComplete = $state(false);
	
	// Battle State
	let playerHP = $state(100);
	let enemyHP = $state(100);
	
	// Config from URL params
	let topic = $state('');
	let difficulty = $state('');
	
	// Stats
	let totalPoints = $state(0);
	let questionsAnswered = $state(0);
	
	// Dialog state
	let showFleeConfirm = $state(false);
	let fleeLoading = $state(false);
	
	// Sprite paths based on difficulty
	let playerSprite = $derived(`/sprites/player/player-lv${getDifficultyLevel(difficulty)}.png`);
	let enemySprite = $derived(`/sprites/enemies/${topic}/${topic}-lv${getDifficultyLevel(difficulty)}.png`);
	
	function getDifficultyLevel(diff: string): number {
		switch (diff.toLowerCase()) {
			case 'easy': return 1;
			case 'medium': return 2;
			case 'hard': return 3;
			default: return 1;
		}
	}
	
	onMount(async () => {
		// Get topic & difficulty from URL params
		topic = page.url.searchParams.get('topic') || 'ai';
		difficulty = page.url.searchParams.get('difficulty') || 'easy';
		
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
			// Persist last session id so home can query it later
			try {
				localStorage.setItem('mindquest:lastSessionId', sessionId);
			} catch (e) {
				console.warn('LocalStorage not available:', e);
			}
			
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
			
			const res = await fetch(`/api/sessions/${sessionId}/question`);
			
			if (res.status === 204) {
				// Round complete
				roundComplete = true;
				await loadFinalStats();
				// Persist career points
				try {
					const prev = parseInt(localStorage.getItem('mindquest:careerPoints') || '0');
					const updated = prev + (totalPoints || 0);
					localStorage.setItem('mindquest:careerPoints', String(updated));
				} catch (e) {
					console.warn('Failed to persist career points:', e);
				}
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
	
	async function handleAnswer(index: number) {
		const selectedAnswer = ['A', 'B', 'C', 'D'][index];
		
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
			
			// Battle Logic
			if (result.correct) {
				enemyHP = Math.max(0, enemyHP - 20); // Assume 5 questions to kill
			} else {
				playerHP = Math.max(0, playerHP - 20); // 5 mistakes allowed
			}
			
			// Accumulate points
			totalPoints += (result.pointsAwarded || 0);
			
			// Check if round is complete
			if (result.roundComplete || playerHP === 0 || enemyHP === 0) {
				if (result.roundComplete) {
					roundComplete = true;
					try {
						const prev = parseInt(localStorage.getItem('mindquest:careerPoints') || '0');
						const updated = prev + (totalPoints || 0);
						localStorage.setItem('mindquest:careerPoints', String(updated));
					} catch (e) {
						console.warn('Failed to persist career points:', e);
					}
				}
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
	
	function showFleeDialog() {
		showFleeConfirm = true;
	}
	
	async function confirmFlee() {
		try {
			fleeLoading = true;
			error = '';
			
			// Call backend to rollback the round (no points awarded)
			const res = await fetch(`/api/sessions/${sessionId}/abandon`, {
				method: 'POST'
			});
			
			if (!res.ok) throw new Error('Failed to abandon round');
			
			const result = await res.json();
			console.log('Round abandoned:', result);
			
			// Navigate home without adding points
			goto('/');
		} catch (err: any) {
			error = err.message || 'Failed to flee';
			fleeLoading = false;
		}
	}
	
	function backToHome() {
		try {
			const prev = parseInt(localStorage.getItem('mindquest:careerPoints') || '0');
			const updated = prev + (totalPoints || 0);
			localStorage.setItem('mindquest:careerPoints', String(updated));
		} catch (e) {
			console.warn('Failed to persist career points on quit:', e);
		}
		goto('/');
	}
</script>

<div class="max-w-4xl mx-auto p-4 min-h-screen flex flex-col font-sans">
	{#if loading && !currentQuestion}
		<div class="flex-1 flex items-center justify-center">
			<div class="text-2xl font-bold animate-pulse text-blue-600">Loading Battle...</div>
		</div>
	{:else if error && !currentQuestion}
		<div class="flex-1 flex flex-col items-center justify-center text-center">
			<p class="text-red-600 mb-4 text-xl">{error}</p>
			<button class="px-6 py-3 bg-blue-500 text-white rounded hover:bg-blue-600" onclick={backToHome}>Retreat</button>
		</div>
	{:else if roundComplete}
		<div class="flex-1 flex flex-col items-center justify-center text-center space-y-8">
			<h2 class="text-4xl font-bold text-gray-800">Battle Finished!</h2>
			<div class="text-2xl space-y-2">
				<p>Total Points: <span class="font-bold text-blue-600">{totalPoints}</span></p>
				<p>Questions Answered: <span class="font-bold text-gray-600">{questionsAnswered}</span></p>
			</div>
			<div class="flex gap-4">
				<button class="px-8 py-4 bg-green-500 text-white rounded-lg font-bold hover:bg-green-600 shadow-lg transform hover:-translate-y-1 transition-all" onclick={goToResults}>Victory Screen</button>
				<button class="px-8 py-4 bg-gray-500 text-white rounded-lg font-bold hover:bg-gray-600 shadow-lg transform hover:-translate-y-1 transition-all" onclick={backToHome}>Return to Base</button>
			</div>
		</div>
	{:else if currentQuestion}
		<!-- Battle Scene -->
		<div class="flex-1 flex flex-col gap-4 md:gap-8 relative">
			<!-- Header / Stats -->
			<div class="absolute top-0 left-0 right-0 flex justify-between p-2 text-xs md:text-sm opacity-50 hover:opacity-100 transition-opacity z-10">
				<span>TOPIC: {topic.toUpperCase()}</span>
				<button class="text-red-500 hover:underline" onclick={showFleeDialog}>FLEE</button>
			</div>

			<!-- Enemy Zone (Top Right) -->
			<div class="flex justify-end items-center gap-4 p-4 mt-8">
				<div class="text-right">
					<h3 class="font-bold text-lg md:text-xl text-red-600 tracking-widest">{topic.toUpperCase()} BOSS</h3>
					<HealthBar current={enemyHP} max={100} label="ENEMY" color="bg-red-500" />
				</div>
				<Sprite src={enemySprite} alt="{topic} Boss" isEnemy={true} />
			</div>

			<!-- Player Zone (Bottom Left) -->
			<div class="flex justify-start items-center gap-4 p-4 mt-auto mb-4">
				<Sprite src={playerSprite} alt="Player" />
				<div>
					<h3 class="font-bold text-lg md:text-xl text-blue-600 tracking-widest">YOU</h3>
					<HealthBar current={playerHP} max={100} label="HP" color="bg-green-500" />
				</div>
			</div>
		</div>

		<!-- UI Zone -->
		<div class="mt-4 space-y-4 pb-8">
			<DialogueBox text={feedback ? (feedback.correct ? "Critical Hit! You dealt damage!" : `Missed! The answer was ${['A', 'B', 'C', 'D'][feedback.correctIndex]}.`) : currentQuestion.questionText} />
			
			{#if !feedback}
				<ActionMenu 
					choices={currentQuestion.choices} 
					onSelect={handleAnswer} 
					disabled={loading} 
				/>
			{:else}
				<button 
					class="w-full py-4 bg-blue-600 text-white font-bold rounded-lg shadow-lg hover:bg-blue-700 animate-bounce"
					onclick={nextQuestion}
				>
					CONTINUE BATTLE
				</button>
			{/if}
		</div>
	{/if}
</div>

<!-- Flee Confirmation Dialog -->
<ConfirmDialog 
	open={showFleeConfirm}
	title="Abandon Battle?"
	message="If you flee now, you won't receive any points for this round. Are you sure?"
	confirmText="Flee"
	cancelText="Stay and Fight"
	isDangerous={true}
	onConfirm={confirmFlee}
	onCancel={() => showFleeConfirm = false}
	isLoading={fleeLoading}
/>


