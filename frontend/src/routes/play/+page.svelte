

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
	import { sounds, bgm } from '$lib/audio/SoundManager';
	
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
	
	// Pending round end (when player needs to see feedback before end screen)
	let pendingRoundEnd = $state<(() => void) | null>(null);
	
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
	
	// Built-in topics that have dedicated backgrounds
	const BUILT_IN_TOPICS = ['ai', 'cs', 'philosophy'];
	
	// Background image path based on topic
	// Built-in topics use their dedicated folder, custom topics use 'default'
	let backgroundFolder = $derived(() => {
		const normalizedTopic = topic.toLowerCase();
		return BUILT_IN_TOPICS.includes(normalizedTopic) ? normalizedTopic : 'default';
	});
	
	// For now, we use bg_1. In the future, this can cycle through multiple backgrounds
	let currentBgIndex = $state(1);
	let backgroundImage = $derived(`/backgrounds/${backgroundFolder()}/${backgroundFolder()}-bg_${currentBgIndex}.png`);
	
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
		const source = page.url.searchParams.get('source');
		
		console.log('[Play] Mounted with source:', source, 'topic:', topic, 'difficulty:', difficulty);
		
		// Start battle BGM for this topic
		bgm.playForTopic(topic);
		
		// Check if this is an inline questions session (from Gemini or saved sets)
		let inlineQuestions: any[] | null = null;
		if (source === 'inline' || source === 'gemini') {
			try {
				// Try new key first, fall back to old key for compatibility
				const stored = sessionStorage.getItem('mindquest:inlineQuestions') 
					|| sessionStorage.getItem('mindquest:geminiQuestions');
				if (stored) {
					inlineQuestions = JSON.parse(stored);
					console.log('[Play] Loaded', inlineQuestions?.length, 'inline questions from sessionStorage');
					// Clean up both keys
					sessionStorage.removeItem('mindquest:inlineQuestions');
					sessionStorage.removeItem('mindquest:geminiQuestions');
				} else {
					console.warn('[Play] No inline questions found in sessionStorage!');
				}
			} catch (e) {
				console.error('Failed to load inline questions from sessionStorage:', e);
			}
		}
		
		// Initialize game flow
		await initializeGame(inlineQuestions);
	});
	
	async function initializeGame(inlineQuestions: any[] | null = null) {
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
			
			// 2. Start round (with optional inline questions from Gemini or saved sets)
			const startBody: any = { topic, difficulty };
			if (inlineQuestions && inlineQuestions.length > 0) {
				startBody.questions = inlineQuestions;
				console.log(`[Play] Starting round with ${inlineQuestions.length} inline questions`);
				console.log('[Play] First question sample:', JSON.stringify(inlineQuestions[0]).slice(0, 200));
			}
			
			const roundRes = await fetch(`/api/sessions/${sessionId}/start`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(startBody)
			});
			
			console.log('[Play] Start round response status:', roundRes.status);
			
			if (!roundRes.ok) {
				const errorText = await roundRes.text();
				console.error('[Play] Start round failed:', errorText);
				throw new Error('Failed to start round');
			}
			
			const startData = await roundRes.json();
			console.log('[Play] Start round response:', startData);
			
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
			
			console.log('[Play] Loading question for session:', sessionId);
			const res = await fetch(`/api/sessions/${sessionId}/question`);
			console.log('[Play] Question response status:', res.status);
			
			if (res.status === 204) {
				// Round complete
				console.log('[Play] Round complete (204 status)');
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
				const errorText = await res.text();
				console.error('[Play] Failed to load question:', errorText);
				throw new Error('Failed to load question');
			}
			
			const rawText = await res.text();
			console.log('[Play] Question response:', rawText.slice(0, 200));
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
				questionText: currentQuestion?.questionText || currentQuestion?.question || 'Question',
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
				// Determine if it's a victory or defeat BEFORE setting roundComplete
				// Victory: Enemy defeated (HP = 0) AND player still alive (HP > 0)
				// Defeat: Player HP = 0 OR (round complete but enemy still alive)
				const willBeVictory = enemyHP === 0 && playerHP > 0;
				
				// If the player got the last question WRONG, delay the end screen
				// so they can see the correct answer first
				const shouldDelayEndScreen = !result.correct;
				
				// Prepare end state but don't transition yet if we need to show feedback
				const finalizeRound = () => {
					roundComplete = true;
					
					if (willBeVictory) {
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
				};
				
				if (shouldDelayEndScreen) {
					// Mark that round will end, but let the player see the feedback first
					// They'll need to click "Continue" to see the end screen
					// Store the finalize function to be called when they proceed
					pendingRoundEnd = finalizeRound;
				} else {
					// Correct answer on last question or victory - can transition immediately
					// (though we still give a brief moment for the victory animation)
					if (willBeVictory) {
						setTimeout(finalizeRound, 800); // Let victory pose play
					} else {
						finalizeRound();
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
		// Check if there's a pending round end (player saw feedback for last wrong answer)
		if (pendingRoundEnd) {
			pendingRoundEnd();
			pendingRoundEnd = null;
			return;
		}
		
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
		// BGM will transition when main menu mounts
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
		hintUsedThisQuestion = false;
		pendingRoundEnd = null;
		currentBgIndex = 1; // Reset background index
		questionStartTime = null;
		damagePopups = [];
		
		// Re-initialize the game
		await initializeGame();
	}
</script>

<div class="game-container">
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
		<!-- Game Over Screen - RPG Styled -->
		<div class="game-over-screen">
			<!-- Title Banner -->
			<div class="game-over-banner" class:victory={isVictory} class:defeat={!isVictory}>
				{#if isVictory}
					<h2 class="game-over-title victory-text">VICTORY!</h2>
					<p class="game-over-subtitle">You defeated the {topic.toUpperCase()} Boss!</p>
				{:else}
					<h2 class="game-over-title defeat-text">DEFEATED...</h2>
					<p class="game-over-subtitle">The {topic.toUpperCase()} Boss was too strong this time.</p>
				{/if}
			</div>
			
			<!-- Defeat Reason Badge -->
			{#if !isVictory && defeatReason}
				<div class="defeat-reason-badge">
					<span class="defeat-reason-icon">⚠</span>
					<span class="defeat-reason-text">{getDefeatReasonMessage(defeatReason)}</span>
				</div>
			{/if}
			
			<!-- Stats Panel -->
			{#if roundSummary}
				<div class="stats-panel">
					<div class="stats-header">
						<span class="stats-header-icon">📊</span>
						<h3 class="stats-header-text">ROUND STATISTICS</h3>
					</div>
					
					<div class="stats-grid">
						<div class="stat-item">
							<span class="stat-label">QUESTIONS</span>
							<span class="stat-value stat-blue">{roundSummary.totalQuestions}</span>
						</div>
						<div class="stat-item">
							<span class="stat-label">ACCURACY</span>
							<span class="stat-value" class:stat-green={roundSummary.accuracyPercentage >= 60} class:stat-red={roundSummary.accuracyPercentage < 60}>
								{roundSummary.accuracyPercentage.toFixed(1)}%
							</span>
						</div>
						<div class="stat-item">
							<span class="stat-label">CORRECT</span>
							<span class="stat-value stat-green">{roundSummary.correctAnswers}</span>
						</div>
						<div class="stat-item">
							<span class="stat-label">MISSES</span>
							<span class="stat-value stat-red">{roundSummary.incorrectAnswers}</span>
						</div>
					</div>
					
					{#if roundSummary.averageAnswerTimeMs > 0}
						<div class="stats-divider"></div>
						<div class="stat-row">
							<span class="stat-label">AVG TIME</span>
							<span class="stat-value stat-purple">{(roundSummary.averageAnswerTimeMs / 1000).toFixed(1)}s</span>
						</div>
					{/if}
					
					<div class="stats-divider"></div>
					<div class="stat-row stat-highlight">
						<span class="stat-label">POINTS EARNED</span>
						<span class="stat-value stat-gold">{totalPoints}</span>
					</div>
				</div>
			{:else}
				<!-- Fallback Stats -->
				<div class="stats-panel">
					<div class="stat-row">
						<span class="stat-label">TOTAL POINTS</span>
						<span class="stat-value stat-blue">{totalPoints}</span>
					</div>
					<div class="stat-row">
						<span class="stat-label">ANSWERED</span>
						<span class="stat-value stat-gray">{questionsAnswered}</span>
					</div>
				</div>
			{/if}
			
			<!-- Action Buttons -->
			<div class="game-over-actions">
				{#if !isVictory && answerHistory.length > 0}
					<button class="action-btn action-btn-review" onclick={() => showReviewModal = true}>
						<span class="action-btn-icon">📚</span>
						<span>REVIEW</span>
					</button>
				{/if}
				<button class="action-btn action-btn-retry" onclick={restartRound}>
					<span class="action-btn-icon">🔄</span>
					<span>RETRY</span>
				</button>
				<button class="action-btn action-btn-home" onclick={backToHome}>
					<span class="action-btn-icon">🏠</span>
					<span>HOME</span>
				</button>
			</div>
		</div>
	{:else if currentQuestion}
		<!-- Battle Scene -->
		<div class="flex-1 flex flex-col gap-4 md:gap-8 relative overflow-hidden rounded-xl" bind:this={battleContainerRef}>
			<!-- Dynamic Background Layer -->
			<div 
				class="battle-background"
				style="background-image: url('{backgroundImage}');"
			></div>
			
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
					onFlee={showFleeDialog}
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
	/* ===== Fixed Game Container ===== */
	.game-container {
		width: 100%;
		height: 100%;
		min-height: 0;
		padding: 1rem;
		display: flex;
		flex-direction: column;
		font-family: system-ui, sans-serif;
		box-sizing: border-box;
		overflow: auto;
	}
	
	/* Battle Background - Dynamic per topic */
	.battle-background {
		position: absolute;
		inset: 0;
		z-index: 0;
		background-size: cover;
		background-position: center;
		background-repeat: no-repeat;
		/* Fallback gradient if image not loaded */
		background-color: #1a1a2e;
		/* Slight darkening overlay for better sprite visibility */
		filter: brightness(0.85);
		transition: background-image 0.3s ease-in-out;
	}
	
	/* Ensure all battle content sits above the background */
	.battle-background ~ * {
		position: relative;
		z-index: 1;
	}
	
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
	
	/* ===== Game Over Screen - RPG Styled ===== */
	.game-over-screen {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 1.25rem;
		padding: 1rem;
		text-align: center;
		font-family: 'Press Start 2P', system-ui, monospace;
	}
	
	/* Title Banner */
	.game-over-banner {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
	}
	
	.game-over-title {
		font-size: 1.75rem;
		font-weight: 800;
		letter-spacing: 0.05em;
		text-shadow: 
			3px 3px 0 rgba(0, 0, 0, 0.4),
			0 0 20px currentColor;
		animation: title-pulse 2s ease-in-out infinite;
	}
	
	.victory-text {
		color: #22c55e;
		text-shadow: 
			3px 3px 0 rgba(0, 0, 0, 0.4),
			0 0 30px rgba(34, 197, 94, 0.6);
	}
	
	.defeat-text {
		color: #ef4444;
		text-shadow: 
			3px 3px 0 rgba(0, 0, 0, 0.4),
			0 0 30px rgba(239, 68, 68, 0.6);
	}
	
	@keyframes title-pulse {
		0%, 100% { opacity: 1; transform: scale(1); }
		50% { opacity: 0.9; transform: scale(1.02); }
	}
	
	.game-over-subtitle {
		font-size: 0.625rem;
		color: #94a3b8;
		letter-spacing: 0.1em;
	}
	
	/* Defeat Reason Badge */
	.defeat-reason-badge {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		padding: 0.625rem 1rem;
		background: linear-gradient(180deg, rgba(239, 68, 68, 0.2) 0%, rgba(185, 28, 28, 0.3) 100%);
		border: 2px solid #ef4444;
		border-radius: 8px;
		box-shadow: 
			0 0 15px rgba(239, 68, 68, 0.3),
			inset 0 1px 0 rgba(255, 255, 255, 0.1);
	}
	
	.defeat-reason-icon {
		font-size: 1rem;
		color: #fca5a5;
	}
	
	.defeat-reason-text {
		font-size: 0.5rem;
		color: #fca5a5;
		letter-spacing: 0.05em;
	}
	
	/* Stats Panel */
	.stats-panel {
		width: 100%;
		max-width: 320px;
		background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
		border: 3px solid #334155;
		border-radius: 12px;
		padding: 1rem;
		box-shadow: 
			0 8px 24px rgba(0, 0, 0, 0.4),
			inset 0 1px 0 rgba(255, 255, 255, 0.05);
	}
	
	.stats-header {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 0.5rem;
		margin-bottom: 1rem;
		padding-bottom: 0.75rem;
		border-bottom: 2px solid #334155;
	}
	
	.stats-header-icon {
		font-size: 1rem;
	}
	
	.stats-header-text {
		font-size: 0.625rem;
		color: #e2e8f0;
		letter-spacing: 0.1em;
	}
	
	.stats-grid {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 0.75rem;
	}
	
	.stat-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.25rem;
		padding: 0.5rem;
		background: rgba(51, 65, 85, 0.3);
		border-radius: 8px;
	}
	
	.stat-label {
		font-size: 0.5rem;
		color: #64748b;
		letter-spacing: 0.1em;
	}
	
	.stat-value {
		font-size: 1.25rem;
		font-weight: 800;
	}
	
	.stat-blue { color: #3b82f6; }
	.stat-green { color: #22c55e; }
	.stat-red { color: #ef4444; }
	.stat-purple { color: #a855f7; }
	.stat-gold { color: #eab308; text-shadow: 0 0 10px rgba(234, 179, 8, 0.5); }
	.stat-gray { color: #94a3b8; }
	
	.stats-divider {
		height: 2px;
		background: linear-gradient(90deg, transparent 0%, #334155 50%, transparent 100%);
		margin: 0.75rem 0;
	}
	
	.stat-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 0.5rem;
	}
	
	.stat-highlight {
		background: linear-gradient(90deg, rgba(234, 179, 8, 0.1) 0%, rgba(234, 179, 8, 0.05) 100%);
		border-radius: 8px;
		border: 1px solid rgba(234, 179, 8, 0.3);
	}
	
	/* Action Buttons */
	.game-over-actions {
		display: flex;
		gap: 0.75rem;
		flex-wrap: wrap;
		justify-content: center;
		margin-top: 0.5rem;
	}
	
	.action-btn {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.25rem;
		padding: 0.75rem 1.25rem;
		border: 3px solid transparent;
		border-radius: 10px;
		font-family: 'Press Start 2P', system-ui, monospace;
		font-size: 0.5rem;
		color: white;
		cursor: pointer;
		transition: all 0.2s ease;
		box-shadow: 
			0 4px 12px rgba(0, 0, 0, 0.3),
			inset 0 1px 0 rgba(255, 255, 255, 0.2);
	}
	
	.action-btn:hover {
		transform: translateY(-3px);
		box-shadow: 
			0 6px 20px rgba(0, 0, 0, 0.4),
			inset 0 1px 0 rgba(255, 255, 255, 0.3);
	}
	
	.action-btn:active {
		transform: translateY(1px);
	}
	
	.action-btn-icon {
		font-size: 1.25rem;
	}
	
	.action-btn-review {
		background: linear-gradient(180deg, #a855f7 0%, #7c3aed 50%, #6d28d9 100%);
		border-color: #c084fc;
	}
	
	.action-btn-review:hover {
		box-shadow: 
			0 6px 20px rgba(168, 85, 247, 0.4),
			0 0 20px rgba(168, 85, 247, 0.3),
			inset 0 1px 0 rgba(255, 255, 255, 0.3);
	}
	
	.action-btn-retry {
		background: linear-gradient(180deg, #3b82f6 0%, #2563eb 50%, #1d4ed8 100%);
		border-color: #60a5fa;
	}
	
	.action-btn-retry:hover {
		box-shadow: 
			0 6px 20px rgba(59, 130, 246, 0.4),
			0 0 20px rgba(59, 130, 246, 0.3),
			inset 0 1px 0 rgba(255, 255, 255, 0.3);
	}
	
	.action-btn-home {
		background: linear-gradient(180deg, #64748b 0%, #475569 50%, #334155 100%);
		border-color: #94a3b8;
	}
	
	.action-btn-home:hover {
		box-shadow: 
			0 6px 20px rgba(100, 116, 139, 0.4),
			0 0 20px rgba(100, 116, 139, 0.3),
			inset 0 1px 0 rgba(255, 255, 255, 0.3);
	}
</style>




