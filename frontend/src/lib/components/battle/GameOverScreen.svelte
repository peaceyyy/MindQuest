<script lang="ts">
	import { getTopicDisplayName, getDefeatReasonMessage } from '$lib/services/battleService';
	
	interface Props {
		isVictory: boolean;
		defeatReason: string;
		topic: string;
		roundSummary: any;
		questionsAnswered: number;
		currentAccuracy: number;
		correctAnswers: number;
		incorrectAnswers: number;
		totalPoints: number;
		answerHistory: any[];
		onReview: () => void;
		onRetry: () => void;
		onHome: () => void;
	}
	
	let { 
		isVictory, 
		defeatReason, 
		topic,
		roundSummary, 
		questionsAnswered,
		currentAccuracy,
		correctAnswers,
		incorrectAnswers,
		totalPoints,
		answerHistory,
		onReview,
		onRetry,
		onHome
	}: Props = $props();
</script>

<div class="game-over-screen">
	
	<!-- Card 1: Defeat/Victory Banner -->
	<div class="game-over-card">
		<div class="game-over-banner" class:victory={isVictory} class:defeat={!isVictory}>
			{#if isVictory}
				<h2 class="game-over-title victory-text">VICTORY!</h2>
				<p class="game-over-subtitle">You defeated the {getTopicDisplayName(topic)} Boss!</p>
			{:else}
				<h2 class="game-over-title defeat-text">DEFEATED...</h2>
				<p class="game-over-subtitle">The {getTopicDisplayName(topic)} Boss was too strong this time.</p>
			{/if}
		</div>
		
		{#if !isVictory && defeatReason}
			<div class="defeat-reason-badge">
				<span class="defeat-reason-icon">!</span>
				<span class="defeat-reason-text">{getDefeatReasonMessage(defeatReason as any)}</span>
			</div>
		{/if}
	</div>

	<!-- Card 2: Stats and Actions -->
	<div class="game-over-card">
		<div class="stats-panel">
			<div class="stats-header">
				<h3 class="stats-header-text">ROUND STATISTICS</h3>
			</div>
			
			<div class="stats-grid">
				<div class="stat-item">
					<span class="stat-label">QUESTIONS</span>
					<span class="stat-value stat-blue">{roundSummary?.totalQuestions ?? questionsAnswered}</span>
				</div>
				<div class="stat-item">
					<span class="stat-label">ACCURACY</span>
					<span class="stat-value" class:stat-green={currentAccuracy >= 60} class:stat-red={currentAccuracy < 60}>
						{roundSummary ? roundSummary.accuracyPercentage.toFixed(1) : currentAccuracy.toFixed(1)}%
					</span>
				</div>
				<div class="stat-item">
					<span class="stat-label">CORRECT</span>
					<span class="stat-value stat-green">{roundSummary?.correctAnswers ?? correctAnswers}</span>
				</div>
				<div class="stat-item">
					<span class="stat-label">MISSES</span>
					<span class="stat-value stat-red">{roundSummary?.incorrectAnswers ?? incorrectAnswers}</span>
				</div>
			</div>
			
			{#if (roundSummary?.averageAnswerTimeMs ?? 0) > 0}
				<div class="stats-divider"></div>
				<div class="stat-row">
					<span class="stat-label">AVG TIME</span>
					<span class="stat-value stat-purple">{((roundSummary?.averageAnswerTimeMs ?? 0) / 1000).toFixed(1)}s</span>
				</div>
			{/if}
			
			<div class="stats-divider"></div>
			<div class="stat-row stat-highlight">
				<span class="stat-label">POINTS EARNED</span>
				<span class="stat-value stat-gold">{totalPoints}</span>
			</div>
		</div>
		
		<!-- Action Buttons -->
		<div class="game-over-actions">
			{#if !isVictory && answerHistory.length > 0}
				<button class="action-btn action-btn-review" onclick={onReview}>
					<span class="action-btn-text">REVIEW</span>
				</button>
			{/if}
			<button class="action-btn action-btn-retry" onclick={onRetry}>
				<span class="action-btn-text">RETRY</span>
			</button>
			<button class="action-btn action-btn-home" onclick={onHome}>
				<span class="action-btn-text">HOME</span>
			</button>
		</div>
	</div>
</div>
