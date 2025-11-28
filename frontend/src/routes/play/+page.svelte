

<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import HealthBar from '$lib/components/battle/HealthBar.svelte';
	import Sprite from '$lib/components/battle/Sprite.svelte';
	import DialogueBox from '$lib/components/battle/DialogueBox.svelte';
	import ActionMenu from '$lib/components/battle/ActionMenu.svelte';
	import ConfirmDialog from '$lib/components/ConfirmDialog.svelte';
	import DamagePopup from '$lib/components/battle/DamagePopup.svelte';
	import BattleSidebar from '$lib/components/battle/BattleSidebar.svelte';
	import ReviewModal from '$lib/components/ReviewModal.svelte';
	import { screenShake, knockback, flashElement, attackLunge, victoryPose, defeatAnimation, hpBarDamageFlash } from '$lib/animations/battleEffects';
	import { sounds } from '$lib/audio/SoundManager';
	
	// Game state
	let sessionId = $state('');
	let currentQuestion = $state<any>(null);
	let feedback = $state<any>(null);
	let loading = $state(false);
	let error = $state('');
	let roundComplete = $state(false);
	let isVictory = $state(false);
	let defeatReason = $state<string>(''); // 'hp_depleted', 'accuracy_low', 'counterattack'
	let answerHistory = $state<Array<{
		correct: boolean;
		questionText: string;
		choices: string[];
		correctIndex: number;
		userAnswerIndex: number;
	}>>([]);
	let showReviewModal = $state(false);
	
	// Battle State - now synced with backend
	let playerHP = $state(100);
	let playerMaxHP = $state(100);
	let enemyHP = $state(100);
	let enemyMaxHP = $state(100);
	
	// Config from URL params
	let topic = $state('');
	let difficulty = $state('');
	
	// Stats
	let totalPoints = $state(0);
	let questionsAnswered = $state(0);
	let roundSummary = $state<any>(null);
	
	// Live accuracy tracking for gauge widget
	let correctAnswers = $state(0);
	let incorrectAnswers = $state(0);
	let currentAccuracy = $state(0); // Percentage (0-100)
	
	// Streak tracking for bonuses
	let correctStreak = $state(0);
	let wrongStreak = $state(0);
	let isHotStreak = $state(false);
	
	// Hint system
	let hints = $state(0);
	let maxHints = $state(0);
	let eliminatedChoices = $state<number[]>([]);
	let hintUsedThisQuestion = $state(false); // Track if hint was used for current question
	
	// Timing for critical hits
	let questionStartTime = $state<number | null>(null);
	
	// Dialog state
	let showFleeConfirm = $state(false);
	let fleeLoading = $state(false);

	
	
	// Animation refs
	let battleContainerRef: HTMLDivElement | null = $state(null);
	let playerSpriteRef: HTMLDivElement | null = $state(null);
	let enemySpriteRef: HTMLDivElement | null = $state(null);
	let playerHpBarRef: HTMLDivElement | null = $state(null);
	let enemyHpBarRef: HTMLDivElement | null = $state(null);
	
	// Damage popup state
	let damagePopups = $state<Array<{
		id: number;
		damage: number;
		x: number;
		y: number;
		isCrit: boolean;
		isHeal: boolean;
	}>>([]);
	let popupIdCounter = $state(0);
	
	let playerSprite = $derived(`/sprites/player/player-lv1.png`);
	// Enemys Sprite paths based on difficulty
	let enemySprite = $derived(`/sprites/enemies/${topic}/${topic}-lv${getDifficultyLevel(difficulty)}.png`);
	
	// Accuracy threshold for victory (inverted model)
	// Easy: 70% (you should know this!)
	// Medium: 60% (balanced)
	// Hard: 50% (forgiving - questions are hard)
	let accuracyThreshold = $derived(() => {
		switch (difficulty.toLowerCase()) {
			case 'easy': return 70;
			case 'medium': return 60;
			case 'hard': return 50;
			default: return 60;
		}
	});
	
	// Enemy damage per correct answer (INVERTED MODEL - scales with difficulty)
	// Hard enemies have MORE HP, requiring more hits to defeat
	let enemyDamagePerHit = $derived(() => {
		switch (difficulty.toLowerCase()) {
			case 'easy': return 25;   // 4 correct answers to win (strict - you should know this!)
			case 'medium': return 20; // 5 correct answers to win (balanced)
			case 'hard': return 16.67; // 6 correct answers to win (forgiving - questions are hard)
			default: return 20;
		}
	});
	
	// Play victory/defeat sound when round completes
	$effect(() => {
		if (roundComplete) {
			if (isVictory) {
				// Victory - play celebration sound
				sounds.play('victory');
			} else {
				// Defeat - play defeat sound (only if not already played)
				if (playerHP > 0) {
					// Player has HP but still lost (enemy survived)
					sounds.play('defeat');
				}
				// If playerHP === 0, defeat sound already played in handleAnswer
			}
		}
	});
	
	function getDifficultyLevel(diff: string): number {
		switch (diff.toLowerCase()) {
			case 'easy': return 1;
			case 'medium': return 2;
			case 'hard': return 3;
			default: return 1;
		}
	}
	
	function getCritMessage(diff: string): string {
		switch (diff.toLowerCase()) {
			case 'easy':
				return "⚡ CRITICAL HIT! Quick reflexes! +5% damage & XP!";
			case 'medium':
				return "⚡ CRITICAL HIT! Excellent timing! +15% damage & XP!";
			case 'hard':
				return "⚡ CRITICAL HIT! Masterful precision! +25% damage & XP!";
			default:
				return "⚡ CRITICAL HIT! +15% damage & XP!";
		}
	}
	
	function getDefeatReasonMessage(reason: string): string {
		switch (reason) {
			case 'hp_depleted':
				return "💔 HP Depleted - Too many mistakes!";
			case 'counterattack':
				return "💥 Boss Counterattack - Three wrong in a row!";
			case 'accuracy_low':
				return `Accuracy Too Low - Needed ${accuracyThreshold()}% to pass`;
			case 'enemy_survived':
				return "⏱️ Round Complete - Enemy survived!";
			default:
				return "Defeat";
		}
	}
	
	// Add damage popup at sprite position
	function showDamagePopup(targetRef: HTMLDivElement | null, damage: number, isCrit = false, isHeal = false) {
		if (!targetRef) return;
		
		const rect = targetRef.getBoundingClientRect();
		const containerRect = battleContainerRef?.getBoundingClientRect();
		
		// Position relative to battle container
		const x = rect.left - (containerRect?.left || 0) + rect.width / 2;
		const y = rect.top - (containerRect?.top || 0);
		
		const popup = {
			id: popupIdCounter++,
			damage,
			x,
			y,
			isCrit,
			isHeal
		};
		
		damagePopups = [...damagePopups, popup];
	}
	
	function removeDamagePopup(id: number) {
		damagePopups = damagePopups.filter(p => p.id !== id);
	}
	
	// Play attack animation sequence
	async function playPlayerAttackAnimation(damage: number, isCritical: boolean = false) {
		// Player lunges forward
		attackLunge(playerSpriteRef, 'right', 40);
		
		// Short delay, then enemy gets hit
		await new Promise(r => setTimeout(r, 150));
		
		// Enemy flash and knockback
		flashElement(enemySpriteRef, 'white', 0.1, 2);
		knockback(enemySpriteRef, 'right', 25);
		hpBarDamageFlash(enemyHpBarRef);
		
		// Show damage popup on enemy
		showDamagePopup(enemySpriteRef, damage, isCritical);
		
		// Play hit sound
		sounds.play('hit');
	}
	
	// Play damage received animation
	async function playPlayerDamageAnimation(damage: number) {
		// Screen shake
		screenShake(battleContainerRef, 10, 0.4);
		
		// Player flash and knockback
		flashElement(playerSpriteRef, 'red', 0.12, 3);
		knockback(playerSpriteRef, 'left', 20);
		hpBarDamageFlash(playerHpBarRef);
		
		// Show damage popup on player
		showDamagePopup(playerSpriteRef, damage);
		
		// Play damage sound
		sounds.play('wrong');
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
			eliminatedChoices = []; // Reset eliminated choices for new question
			hintUsedThisQuestion = false; // Reset hint usage for new question
			
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
			
			// Fetch current hints
			await fetchHints();
			
			// Start timing for critical hit detection
			questionStartTime = Date.now();
			
		} catch (err: any) {
			error = err.message || 'Failed to load question';
		} finally {
			loading = false;
		}
	}
	
	async function fetchHints() {
		try {
			const res = await fetch(`/api/sessions/${sessionId}/hints`);
			if (res.ok) {
				const data = await res.json();
				hints = data.hints;
				maxHints = data.maxHints;
			}
		} catch (err) {
			console.error('Failed to fetch hints:', err);
		}
	}
	
	async function useHint() {
		if (hints === 0 || feedback || hintUsedThisQuestion) {
			console.log('[useHint] Blocked - hints:', hints, 'feedback:', feedback, 'hintUsedThisQuestion:', hintUsedThisQuestion);
			return;
		}
		
		try {
			console.log('[useHint] Calling /use-hint endpoint...');
			const res = await fetch(`/api/sessions/${sessionId}/use-hint`, {
				method: 'POST'
			});
			
			if (!res.ok) {
				const errorData = await res.json();
				console.error('Failed to use hint:', errorData.error);
				return;
			}
			
			const data = await res.json();
			console.log('[DEBUG] Full hint response:', JSON.stringify(data, null, 2));
			console.log('[DEBUG] eliminatedIndices type:', typeof data.eliminatedIndices);
			console.log('[DEBUG] eliminatedIndices value:', data.eliminatedIndices);
			console.log('[DEBUG] Is array?:', Array.isArray(data.eliminatedIndices));
			
			hints = data.hints;
			// Backend now returns eliminatedIndices (array of 2 indices)
			const newEliminatedChoices = data.eliminatedIndices || [];
			console.log('[DEBUG] About to set eliminatedChoices to:', newEliminatedChoices);
			eliminatedChoices = newEliminatedChoices;
			hintUsedThisQuestion = true; // Mark hint as used for this question
			
			console.log('[DEBUG] eliminatedChoices after assignment:', eliminatedChoices);
			console.log('[DEBUG] eliminatedChoices is array?:', Array.isArray(eliminatedChoices));
			
			// Play a subtle sound effect
			sounds.play('correct'); // Reuse existing sound or add a hint sound later
			
		} catch (err) {
			console.error('Failed to use hint:', err);
		}
	}
	
	async function handleAnswer(index: number) {
		const selectedAnswer = ['A', 'B', 'C', 'D'][index];
		
		// Calculate answer time
		const answerTimeMs = questionStartTime ? Date.now() - questionStartTime : null;
		
		try {
			loading = true;
			error = '';
			
			const res = await fetch(`/api/sessions/${sessionId}/answer`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ 
					answer: selectedAnswer,
					answerTimeMs: answerTimeMs
				})
			});
			
			if (!res.ok) throw new Error('Failed to submit answer');
			
			const result = await res.json();
			feedback = result;
			questionsAnswered++;
			
			// Track answer history for defeat screen breakdown
			answerHistory.push({
				correct: result.correct,
				questionText: currentQuestion?.text || 'Question',
				choices: currentQuestion?.choices || [],
				correctIndex: result.correctIndex,
				userAnswerIndex: index
			});
			
			// Update live accuracy tracking from backend
			if (result.correctAnswers !== undefined) {
				correctAnswers = result.correctAnswers;
			}
			if (result.incorrectAnswers !== undefined) {
				incorrectAnswers = result.incorrectAnswers;
			}
			if (result.currentAccuracy !== undefined) {
				currentAccuracy = result.currentAccuracy;
			}
			
			// Update streak tracking from backend
			if (result.correctStreak !== undefined) {
				correctStreak = result.correctStreak;
			}
			if (result.wrongStreak !== undefined) {
				wrongStreak = result.wrongStreak;
			}
			if (result.isHotStreak !== undefined) {
				isHotStreak = result.isHotStreak;
			}
			
			// Use backend HP for player damage (authoritative)
			if (result.correct) {
				// Player attacks enemy - frontend-managed enemy HP
				let damageToEnemy = enemyDamagePerHit();
				
				// Apply difficulty-specific critical hit bonus
				// Easy: 5%, Medium: 15%, Hard: 25%
				if (result.isCritical) {
					const critMultiplier = difficulty.toLowerCase() === 'easy' ? 1.05 
						: difficulty.toLowerCase() === 'hard' ? 1.25 
						: 1.15;
					damageToEnemy = Math.round(damageToEnemy * critMultiplier);
				}
				
				enemyHP = Math.max(0, enemyHP - damageToEnemy);
				
				// Play attack animation (includes 'hit' sound)
				await playPlayerAttackAnimation(damageToEnemy, result.isCritical);
				
				// Play appropriate sound
				if (result.isCritical) {
					sounds.play('crit');
				}
				
				// Check for enemy defeat
				if (enemyHP === 0) {
					await new Promise(r => setTimeout(r, 300));
					victoryPose(playerSpriteRef);
					defeatAnimation(enemySpriteRef);
				}
			} else {
				// Player takes damage - use backend's calculated damage
				const damageTaken = result.damageTaken || 20;
				playerHP = result.currentHp; // Sync with backend HP
				
				// Play damage animation
				await playPlayerDamageAnimation(damageTaken);
				
				// Check for player defeat
				if (playerHP === 0) {
					await new Promise(r => setTimeout(r, 300));
					defeatAnimation(playerSpriteRef);
					sounds.play('defeat');
					
					// Determine defeat reason
					if (result.isCounterattack) {
						defeatReason = 'counterattack';
					} else {
						defeatReason = 'hp_depleted';
					}
				}
			}
			
			// Accumulate points
			totalPoints += (result.pointsAwarded || 0);
			
			// Check if round is complete
			if (result.roundComplete || playerHP === 0 || enemyHP === 0) {
				roundComplete = true;
				
				// Determine if it's a victory or defeat
				// Victory: Enemy defeated (HP = 0) AND player still alive (HP > 0)
				// Defeat: Player HP = 0 OR (round complete but enemy still alive)
				if (enemyHP === 0 && playerHP > 0) {
					// VICTORY!
					isVictory = true;
					// Capture round summary if provided
					if (result.summary && result.summary !== 'null') {
						roundSummary = result.summary;
					}
					try {
						const prev = parseInt(localStorage.getItem('mindquest:careerPoints') || '0');
						const updated = prev + (totalPoints || 0);
						localStorage.setItem('mindquest:careerPoints', String(updated));
					} catch (e) {
						console.warn('Failed to persist career points:', e);
					}
				} else {
					// DEFEAT - Player died OR round ended with enemy still alive
					isVictory = false;
					
					// Determine defeat reason if not already set
					if (!defeatReason) {
						if (playerHP === 0) {
							defeatReason = 'hp_depleted';
						} else if (enemyHP > 0 && currentAccuracy < accuracyThreshold()) {
							defeatReason = 'accuracy_low';
						} else {
							defeatReason = 'enemy_survived';
						}
					}
					
					// Capture summary for stats display even on defeat
					if (result.summary && result.summary !== 'null') {
						roundSummary = result.summary;
					}
					// DO NOT save points on defeat - stats shown for learning purposes only
					console.log('Defeated - no points awarded. Enemy HP:', enemyHP, 'Player HP:', playerHP, 'Reason:', defeatReason);
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
			
		} catch (err: any) {
			console.error('Failed to flee:', err);
			error = err.message || 'Failed to flee';
			fleeLoading = false;
			showFleeConfirm = false;
			return; // Don't navigate if there was an error
		} finally {
			// Always clean up state and navigate on success
			fleeLoading = false;
			showFleeConfirm = false;
		}
		
		// Navigate after dialog is closed and state is reset
		goto('/');
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

	async function restartRound() {
		// Reset all game state variables to their initial values
		sessionId = '';
		currentQuestion = null;
		feedback = null;
		loading = true; // Set loading to true immediately
		error = '';
		roundComplete = false;
		isVictory = false;
		defeatReason = '';
		answerHistory = [];
		showReviewModal = false;
		playerHP = 100;
		enemyHP = 100;
		totalPoints = 0;
		questionsAnswered = 0;
		roundSummary = null;
		correctAnswers = 0;
		incorrectAnswers = 0;
		currentAccuracy = 0;
		correctStreak = 0;
		wrongStreak = 0;
		isHotStreak = false;
		hints = 0;
		maxHints = 0;
		eliminatedChoices = [];
		questionStartTime = null;
		damagePopups = [];
		
		// Re-initialize the game
		await initializeGame();
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
			{#if isVictory}
				<h2 class="text-4xl font-bold text-green-600">Victory!</h2>
				<p class="text-gray-600">You defeated the {topic.toUpperCase()} Boss!</p>
			{:else}
				<h2 class="text-4xl font-bold text-red-600">Defeated...</h2>
				<p class="text-gray-600">The {topic.toUpperCase()} Boss was too strong this time.</p>
				
				<!-- Defeat Reason Badge -->
				{#if defeatReason}
					<div class="bg-red-100 border-2 border-red-400 rounded-lg px-4 py-3 text-red-800 font-semibold">
						{getDefeatReasonMessage(defeatReason)}
					</div>
				{/if}
				
				<!-- Answer Breakdown Visual -->
				
			{/if}
			
			{#if roundSummary}
				<!-- Enhanced Statistics Display -->
				<div class="bg-white/80 backdrop-blur-sm rounded-xl p-6 shadow-lg max-w-md w-full space-y-4">
					<h3 class="text-2xl font-bold text-gray-800 mb-4">Round Statistics</h3>
					
					<div class="grid grid-cols-2 gap-4 text-left">
						<div class="space-y-1">
							<p class="text-sm text-gray-500 uppercase tracking-wide">Total Questions</p>
							<p class="text-3xl font-bold text-blue-600">{roundSummary.totalQuestions}</p>
						</div>
						<div class="space-y-1">
							<p class="text-sm text-gray-500 uppercase tracking-wide">Accuracy</p>
							<p class="text-3xl font-bold text-green-600">{roundSummary.accuracyPercentage.toFixed(1)}%</p>
						</div>
						<div class="space-y-1">
							<p class="text-sm text-gray-500 uppercase tracking-wide">Correct</p>
							<p class="text-3xl font-bold text-emerald-600">{roundSummary.correctAnswers}</p>
						</div>
						<div class="space-y-1">
							<p class="text-sm text-gray-500 uppercase tracking-wide">Misses</p>
							<p class="text-3xl font-bold text-red-600">{roundSummary.incorrectAnswers}</p>
						</div>
					</div>
					
					{#if roundSummary.averageAnswerTimeMs > 0}
						<div class="border-t pt-4 mt-4">
							<p class="text-sm text-gray-500 uppercase tracking-wide mb-1">Average Time</p>
							<p class="text-2xl font-bold text-purple-600">{(roundSummary.averageAnswerTimeMs / 1000).toFixed(1)}s</p>
						</div>
					{/if}
					
					<div class="border-t pt-4 mt-4">
						<p class="text-sm text-gray-500 uppercase tracking-wide mb-1">Total Points Earned</p>
						<p class="text-3xl font-bold text-yellow-600">{totalPoints}</p>
					</div>
				</div>
			{:else}
				<!-- Fallback to basic stats if summary not available -->
				<div class="text-2xl space-y-2">
					<p>Total Points: <span class="font-bold text-blue-600">{totalPoints}</span></p>
					<p>Questions Answered: <span class="font-bold text-gray-600">{questionsAnswered}</span></p>
				</div>
			{/if}
			
			<div class="flex gap-4 flex-wrap justify-center">
				<!-- Review Answers Button (only on defeat) -->
				{#if !isVictory && answerHistory.length > 0}
					<button 
						class="px-8 py-4 bg-purple-500 text-white rounded-lg font-bold hover:bg-purple-600 shadow-lg transform hover:-translate-y-1 transition-all flex items-center gap-2"
						onclick={() => showReviewModal = true}
					>
						<span class="text-xl">📚</span>
						Review Answers
					</button>
				{/if}
				<button class="px-8 py-4 bg-blue-500 text-white rounded-lg font-bold hover:bg-blue-600 shadow-lg transform hover:-translate-y-1 transition-all" onclick={restartRound}>Play Again</button>
				<button class="px-8 py-4 bg-gray-500 text-white rounded-lg font-bold hover:bg-gray-600 shadow-lg transform hover:-translate-y-1 transition-all" onclick={backToHome}>Return to Base</button>
			</div>
		</div>
	{:else if currentQuestion}
		<!-- Battle Scene -->
		<div class="flex-1 flex flex-col gap-4 md:gap-8 relative" bind:this={battleContainerRef}>
			<!-- Damage Popups Layer -->
			{#each damagePopups as popup (popup.id)}
				<DamagePopup 
					damage={popup.damage}
					x={popup.x}
					y={popup.y}
					isCrit={popup.isCrit}
					isHeal={popup.isHeal}
					onComplete={() => removeDamagePopup(popup.id)}
				/>
			{/each}
			
		<!-- Header / Stats -->
		<div class="absolute top-0 left-0 right-0 flex justify-between items-center p-2 text-xs md:text-sm opacity-50 hover:opacity-100 transition-opacity z-10">
			<span></span>
			<button class="text-red-500 hover:underline" onclick={showFleeDialog}>FLEE</button>
		</div>

		

		<!-- Enemy Zone (Top Right) -->
		<div class="flex justify-end items-center gap-4 p-4 mt-8">
				<div class="text-right">
					<h3 class="font-bold text-lg md:text-xl text-red-600 tracking-widest">{topic.toUpperCase()} BOSS</h3>
					<HealthBar current={enemyHP} max={enemyMaxHP} label="ENEMY" color="bg-red-500" bind:barRef={enemyHpBarRef} />
				</div>
				<Sprite src={enemySprite} alt="{topic} Boss" isEnemy={true} bind:spriteRef={enemySpriteRef} />
			</div>

			<!-- Player Zone (Bottom Left) -->
			<div class="flex justify-start items-center gap-4 p-4 mt-auto mb-4">
				<Sprite src={playerSprite} alt="Player" bind:spriteRef={playerSpriteRef} />
				<div>
					<h3 class="font-bold text-lg md:text-xl text-blue-600 tracking-widest">YOU</h3>
					<HealthBar current={playerHP} max={playerMaxHP} label="HP" color="bg-green-500" bind:barRef={playerHpBarRef} />
				</div>
				
				<!-- Live Accuracy Gauge -->
				{#if questionsAnswered > 0}
					<!-- Streak indicators remain near the player but meter moved to UI panel -->
					<div class="flex flex-col gap-2">
						{#if correctStreak >= 3}
							<div class="bg-gradient-to-r from-yellow-500/20 to-orange-500/20 border-2 border-yellow-500 rounded-lg px-3 py-2 text-center animate-pulse">
								<div class="text-yellow-400 font-bold text-sm">🔥 HOT STREAK!</div>
								<div class="text-slate-100 text-xs">⛓️ {correctStreak} correct</div>
								<div class="text-yellow-300 text-xs">+10% XP bonus!</div>
							</div>
						{:else if correctStreak >= 1}
							<div class="bg-green-900/30 border border-green-600 rounded-lg px-3 py-2 text-center">
								<div class="text-green-400 font-semibold text-sm">⛓️ {correctStreak}</div>
							</div>
						{/if}

						{#if wrongStreak >= 2}
							<div class="bg-gradient-to-r from-red-500/20 to-orange-500/20 border-2 border-red-500 rounded-lg px-3 py-2 text-center animate-pulse">
								<div class="text-red-400 font-bold text-sm">⚠️ DANGER ZONE!</div>
								<div class="text-slate-100 text-xs">{wrongStreak} wrong</div>
								<div class="text-red-300 text-xs">Next: 1.5× damage!</div>
							</div>
						{:else if wrongStreak >= 1}
							<div class="bg-red-900/30 border border-red-600 rounded-lg px-3 py-2 text-center">
								<div class="text-red-400 font-semibold text-sm">⚠️ {wrongStreak}</div>
							</div>
						{/if}
					</div>
				{/if}
			</div>
		</div>

	<!-- UI Zone -->
	<div class="mt-2 pb-4">
		<div class="grid grid-cols-1 lg:grid-cols-[1fr_12rem] items-start gap-3">
			<!-- Main Content: Dialogue + Actions (Left, fixed grid column so width stays stable) -->
			<div class="w-full space-y-3">
				<DialogueBox text={
					feedback 
						? (feedback.correct 
							? (feedback.isCritical 
								? getCritMessage(difficulty) 
								: "Hit! You dealt damage!") 
							: feedback.isCounterattack
								? `💥 BOSS COUNTERATTACK! Three mistakes in a row! The enemy strikes back with devastating force! (1.5× damage)`
								: `Missed! The correct answer was:\n\n${['A', 'B', 'C', 'D'][feedback.correctIndex]}. ${currentQuestion.choices[feedback.correctIndex]}`) 
						: currentQuestion.questionText
				} />

				{#if !feedback}
					<ActionMenu
						choices={currentQuestion.choices}
						onSelect={handleAnswer}
						disabled={loading}
						eliminatedChoices={eliminatedChoices}
					/>
				{:else}
					<button 
						class="continue-button"
						onclick={nextQuestion}
					>
						<span class="continue-bg"></span>
						<span class="continue-content">
							<span class="continue-arrow">▶</span>
							<span>CONTINUE BATTLE</span>
							<span class="continue-arrow">◀</span>
						</span>
					</button>
				{/if}
			</div>

			<!-- Right Sidebar: Combined Stats & Items Panel  -->
			<div class="w-full lg:w-48 flex-shrink-0">
				<BattleSidebar
					currentAccuracy={currentAccuracy}
					threshold={accuracyThreshold()}
					difficulty={difficulty}
					correctAnswers={correctAnswers}
					incorrectAnswers={incorrectAnswers}
					hints={hints}
					maxHints={maxHints}
					onUseHint={useHint}
					hintDisabled={!!feedback}
					hintUsedThisQuestion={hintUsedThisQuestion}
				/>
			</div>
		</div>
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

<!-- Review Modal -->
{#if showReviewModal}
	<ReviewModal 
		questions={answerHistory}
		onClose={() => showReviewModal = false}
	/>
{/if}

<style>
	/* Continue Battle button - RPG styled */
	.continue-button {
		position: relative;
		width: 100%;
		padding: 12px 20px;
		background: linear-gradient(180deg, #3b82f6 0%, #2563eb 50%, #1d4ed8 100%);
		border: 4px solid transparent;
		border-radius: 12px;
		cursor: pointer;
		overflow: hidden;
		transition: all 0.2s ease;
		animation: continue-pulse 1.5s ease-in-out infinite;
		
		box-shadow: 
			0 0 0 2px rgba(96, 165, 250, 0.5),
			0 0 20px rgba(59, 130, 246, 0.3),
			0 6px 20px rgba(0, 0, 0, 0.3),
			inset 0 1px 0 rgba(255, 255, 255, 0.2);
	}
	
	.continue-button:hover {
		transform: translateY(-2px);
		box-shadow: 
			0 0 0 2px rgba(96, 165, 250, 0.7),
			0 0 30px rgba(59, 130, 246, 0.5),
			0 8px 24px rgba(0, 0, 0, 0.3),
			inset 0 1px 0 rgba(255, 255, 255, 0.3);
	}
	
	.continue-button:active {
		transform: translateY(1px);
	}
	
	@keyframes continue-pulse {
		0%, 100% { 
			box-shadow: 
				0 0 0 2px rgba(96, 165, 250, 0.5),
				0 0 20px rgba(59, 130, 246, 0.3),
				0 6px 20px rgba(0, 0, 0, 0.3),
				inset 0 1px 0 rgba(255, 255, 255, 0.2);
		}
		50% { 
			box-shadow: 
				0 0 0 2px rgba(96, 165, 250, 0.8),
				0 0 35px rgba(59, 130, 246, 0.5),
				0 6px 20px rgba(0, 0, 0, 0.3),
				inset 0 1px 0 rgba(255, 255, 255, 0.2);
		}
	}
	
	.continue-bg {
		position: absolute;
		inset: 0;
		background: linear-gradient(180deg, 
			rgba(255, 255, 255, 0.15) 0%, 
			transparent 50%
		);
	}
	
	.continue-content {
		position: relative;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 10px;
		font-size: 0.875rem;
		font-weight: 800;
		color: white;
		text-transform: uppercase;
		letter-spacing: 0.08em;
		text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
		z-index: 1;
		font-family: 'Press Start 2P', system-ui, monospace;
	}
	
	.continue-arrow {
		font-size: 0.875rem;
		opacity: 0.8;
		animation: arrow-bounce 0.8s ease infinite;
	}
	
	.continue-arrow:last-child {
		animation-delay: 0.4s;
	}
	
	@keyframes arrow-bounce {
		0%, 100% { transform: translateX(0); opacity: 0.8; }
		50% { transform: translateX(3px); opacity: 1; }
	}
</style>


