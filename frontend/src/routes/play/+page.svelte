

<script lang="ts">
	import '$lib/styles/play-page.css';
	
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
	import BattleIntro from '$lib/components/battle/BattleIntro.svelte';
	import GameOverScreen from '$lib/components/battle/GameOverScreen.svelte';
	import ReviewModal from '$lib/components/ReviewModal.svelte';
	import { screenShake, knockback, flashElement, attackLunge, victoryPose, defeatAnimation, hpBarDamageFlash, enemyAttack } from '$lib/animations/battleEffects';
	import { sounds, bgm } from '$lib/audio/SoundManager';
	
	// Import stores
	import * as gameState from '$lib/stores/gameState';
	import * as uiState from '$lib/stores/uiState';
	
	// Import services
	import * as gameApi from '$lib/services/gameApi';
	import * as battleService from '$lib/services/battleService';
	
	// Subscribe to stores
	let sessionId = $state('');
	let currentQuestion = $state<any>(null);
	let questionStartTime = $state<number | null>(null);
	let playerHP = $state(100);
	let playerMaxHP = $state(100);
	let enemyHP = $state(100);
	let enemyMaxHP = $state(100);
	let totalPoints = $state(0);
	let questionsAnswered = $state(0);
	let correctAnswers = $state(0);
	let incorrectAnswers = $state(0);
	let currentAccuracy = $state(0);
	let correctStreak = $state(0);
	let wrongStreak = $state(0);
	let isHotStreak = $state(false);
	let hints = $state(0);
	let maxHints = $state(0);
	let eliminatedChoices = $state<number[]>([]);
	let hintUsedThisQuestion = $state(false);
	let answerHistory = $state<Array<{
		correct: boolean;
		questionText: string;
		choices: string[];
		correctIndex: number;
		userAnswerIndex: number;
	}>>([]);
	let feedback = $state<any>(null);
	let roundSummary = $state<any>(null);
	
	// UI state
	let showBattleIntro = $state(false);
	let introPhase = $state(0);
	let roundComplete = $state(false);
	let isVictory = $state(false);
	let defeatReason = $state<string>('');
	let showReviewModal = $state(false);
	let showFleeConfirm = $state(false);
	let loading = $state(false);
	let fleeLoading = $state(false);
	let error = $state('');
	let pendingRoundEnd = $state<(() => void) | null>(null);
	
	// Config from URL params
	let topic = $state('');
	let difficulty = $state('');

	
	
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
	
	// Derived values using battleService
	let playerSprite = $derived(battleService.getPlayerSpritePath());
	let enemySprite = $derived(() => battleService.getEnemySpritePath(topic, difficulty));
	let currentBgIndex = $state(1);
	let backgroundImage = $derived(battleService.getBackgroundPath(topic, currentBgIndex));
	let accuracyThreshold = $derived(() => battleService.getAccuracyThreshold(difficulty));
	let enemyDamagePerHit = $derived(() => battleService.getEnemyDamagePerHit(difficulty));
	
	// Stop BGM and play victory/defeat sound when round completes
	$effect(() => {
		if (roundComplete) {
			// Stop background music immediately so game over sounds are clear
			bgm.stop();
			
			if (isVictory) {
				// Victory - play celebration sound
				sounds.play('victory');
			} else {
				// Defeat - play defeat sound when game over screen shows
				sounds.play('defeat');
			}
		}
	});
	

	
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
		// Player lunges forward (more forceful)
		attackLunge(playerSpriteRef, 'right', isCritical ? 60 : 50);
		
		// Short delay, then enemy gets hit
		await new Promise(r => setTimeout(r, 150));
		
		// Enemy flash and knockback (much stronger impact)
		flashElement(enemySpriteRef, isCritical ? 'yellow' : 'white', 0.08, isCritical ? 3 : 2);
		knockback(enemySpriteRef, 'right', isCritical ? 50 : 38);
		hpBarDamageFlash(enemyHpBarRef);
		
		// Add extra screen shake for critical hits
		if (isCritical) {
			screenShake(battleContainerRef, 8, 0.3);
		}
		
		// Show damage popup on enemy
		showDamagePopup(enemySpriteRef, damage, isCritical);
		
		// Play hit sound
		sounds.play('hit');
	}
	
	// Play damage received animation
	async function playPlayerDamageAnimation(damage: number) {
		// Enemy attacks first with topic-specific animation
		const attackType = battleService.getEnemyAttackType(topic);
		enemyAttack(enemySpriteRef, attackType);
		
		// Short delay to show enemy attack
		await new Promise(r => setTimeout(r, 220));
		
		// Then player takes damage with intense effects
		// Stronger screen shake
		screenShake(battleContainerRef, 15, 0.5);
		
		// Player flash and knockback (more intense)
		flashElement(playerSpriteRef, 'red', 0.1, 4);
		knockback(playerSpriteRef, 'left', 35);
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
			
			// 3. Show battle intro sequence, then load first question
			await showIntroSequence();
			
		} catch (err: any) {
			error = err.message || 'Failed to initialize game';
		} finally {
			loading = false;
		}
	}
	
	/**
	 * Play the battle intro sequence (Pokemon-style encounter)
	 * Duration: ~4 seconds total
	 */
	async function showIntroSequence(): Promise<void> {
		showBattleIntro = true;
		introPhase = 0;
		
		// Play encounter sound
		sounds.play('encounter');
		
		// Phase 0: "A wild X appeared!" text (1.2s)
		await new Promise(r => setTimeout(r, 1200));
		
		// Phase 1: Enemy sprite slides in (1.5s)
		introPhase = 1;
		sounds.play('select');
		await new Promise(r => setTimeout(r, 1500));
		
		// Phase 2: "Get ready!" (1s)
		introPhase = 2;
		await new Promise(r => setTimeout(r, 1000));
		
		// End intro, load first question
		showBattleIntro = false;
		introPhase = 0;
		await loadQuestion();
	}
	
	async function loadQuestion() {
		try {
			loading = true;
			error = '';
			feedback = null;
			eliminatedChoices = []; // Reset eliminated choices for new question
			hintUsedThisQuestion = false; // Reset hint usage for new question
			
			// Use gameApi service
			const question = await gameApi.loadQuestion(sessionId);
			
			if (question === null) {
				// Round complete (204 status)
				console.log('[Play] Round complete');
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
			
			currentQuestion = question;
			
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
			const data = await gameApi.fetchHints(sessionId);
			hints = data.hints;
			maxHints = data.maxHints;
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
			// Use gameApi service
			const data = await gameApi.useHint(sessionId);
			
			hints = data.hints;
			eliminatedChoices = data.eliminatedIndices || [];
			hintUsedThisQuestion = true;
			
			console.log('[DEBUG] Hint used. Eliminated:', eliminatedChoices);
			
			// Play a subtle sound effect
			sounds.play('correct');
			
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
			
			// Use gameApi service
			const result = await gameApi.submitAnswer(sessionId, {
				answer: selectedAnswer,
				answerTimeMs: answerTimeMs
			});
			
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
				const baseDamage = enemyDamagePerHit();
				const damageToEnemy = battleService.calculateDamage(baseDamage, result.isCritical, difficulty);
				
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
				// Don't play defeat sound here - it will play when game over screen shows
				
				// Determine defeat reason
				if (result.isCounterattack) {
					defeatReason = 'counterattack';
				} else {
					defeatReason = 'hp_depleted';
				}
			}
		}
		
		// Accumulate points
		totalPoints += (result.pointsAwarded || 0);			// Check if round is complete
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
			const state = await gameApi.fetchSessionState(sessionId);
			totalPoints = state.globalPoints || 0;
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
			
			// Use gameApi service
			await gameApi.abandonSession(sessionId);
			console.log('Round abandoned');
			
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
		showBattleIntro = false;
		introPhase = 0;
		
		// Re-initialize the game
		await initializeGame();
	}
</script>

<div class="game-container">
	{#if loading && !currentQuestion && !showBattleIntro}
		<div class="flex-1 flex items-center justify-center">
			<div class="text-2xl font-bold animate-pulse text-blue-600">Loading Battle...</div>
		</div>
	{:else if error && !currentQuestion}
		<div class="flex-1 flex flex-col items-center justify-center text-center">
			<p class="text-red-600 mb-4 text-xl">{error}</p>
			<button class="px-6 py-3 bg-blue-500 text-white rounded hover:bg-blue-600" onclick={backToHome}>Retreat</button>
		</div>
	{:else if showBattleIntro}
		<BattleIntro 
			topic={topic}
			enemySprite={enemySprite()}
			backgroundImage={backgroundImage}
			introPhase={introPhase}
		/>
	{:else if roundComplete}
		<GameOverScreen 
			isVictory={isVictory}
			defeatReason={defeatReason}
			topic={topic}
			roundSummary={roundSummary}
			questionsAnswered={questionsAnswered}
			currentAccuracy={currentAccuracy}
			correctAnswers={correctAnswers}
			incorrectAnswers={incorrectAnswers}
			totalPoints={totalPoints}
			answerHistory={answerHistory}
			onReview={() => showReviewModal = true}
			onRetry={restartRound}
			onHome={backToHome}
		/>
	{:else if currentQuestion}
		<!-- Battle Scene -->
		<div class="battle-container" bind:this={battleContainerRef}>
			<!-- Dynamic Background Layer -->
			<div 
				class="battle-background"
				style="background-image: url('{backgroundImage}');"
			></div>
			
			<!-- Flee Button (Top-Left) -->
			<button class="battle-flee-button" onclick={showFleeDialog}>
				<span class="flee-icon">🚪</span>
				<span class="flee-text">FLEE</span>
			</button>
			
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
			


		

	
		<div class="battle-enemy-section">
				<div class="text-right">
					<h3 class="font-bold text-lg md:text-xl text-red-600 tracking-widest">{topic.toUpperCase()} BOSS</h3>
					<HealthBar current={enemyHP} max={enemyMaxHP} label="HP" color="bg-red-500" bind:barRef={enemyHpBarRef} />
				</div>
				<Sprite src={enemySprite()} alt="{topic} Boss" isEnemy={true} bind:spriteRef={enemySpriteRef} />
			</div>

			
			<div class="battle-player-section">
				<Sprite src={playerSprite} alt="Player" bind:spriteRef={playerSpriteRef} />
				<div>
					<h3 class="font-bold text-lg md:text-xl text-blue-600 tracking-widest">YOU</h3>
					<HealthBar current={playerHP} max={playerMaxHP} label="HP" color="bg-green-500" bind:barRef={playerHpBarRef} />
				</div>
				
		
				{#if questionsAnswered > 0}
					<!-- Streak indicators remain near the player but meter moved to UI panel -->
					<div class="flex flex-col gap-2 max-w-[180px]">
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
	<div class="ui-zone">
		<div class="ui-grid">
			
			<div>
				<DialogueBox text={
					feedback 
						? (feedback.correct 
							? (feedback.isCritical 
								? battleService.getCritMessage(difficulty) 
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

	
			<div>
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
