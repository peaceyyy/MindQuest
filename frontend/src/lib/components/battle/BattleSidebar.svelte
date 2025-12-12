<script lang="ts">
	import '$lib/styles/battle-sidebar.css';

	interface Props {
		// Accuracy props
		currentAccuracy: number;
		threshold: number;
		difficulty: string;
		correctAnswers: number;
		incorrectAnswers: number;
		// Hint props
		hints: number;
		maxHints: number;
		onUseHint: () => void;
		hintDisabled?: boolean;
		hintUsedThisQuestion?: boolean;
	}
	
	let { 
		currentAccuracy, 
		threshold, 
		difficulty, 
		correctAnswers, 
		incorrectAnswers,
		hints,
		maxHints,
		onUseHint,
		hintDisabled = false,
		hintUsedThisQuestion = false
	}: Props = $props();
	
	// Hint drawer state
	let hintsExpanded = $state(false);
	let canUseHint = $derived(hints > 0 && !hintDisabled && !hintUsedThisQuestion);
	
	// Accuracy color logic
	let gaugeColor = $derived(() => {
		if (currentAccuracy >= threshold) return '#22c55e';
		if (currentAccuracy >= threshold - 10) return '#eab308';
		return '#ef4444';
	});
	
	let statusText = $derived(() => {
		if (currentAccuracy >= threshold) return 'GOOD';
		if (currentAccuracy >= threshold - 10) return 'OK';
		return 'LOW';
	});
	
	let isInDanger = $derived(currentAccuracy < threshold - 10 && correctAnswers + incorrectAnswers > 0);
	
	function toggleHints() {
		hintsExpanded = !hintsExpanded;
	}
</script>

<div class="battle-sidebar">
	<!-- HINTS DRAWER (Floating above accuracy, expands upward) -->
	<div class="hints-drawer-wrapper">
		<!-- Floating content that expands UPWARD (absolutely positioned) -->
		<div class="hints-drawer-content" class:expanded={hintsExpanded}>
			<!-- Hint orbs row -->
			<div class="hint-orbs">
				{#each Array(maxHints) as _, i}
					<div class="hint-orb" class:active={i < hints} class:used={i >= hints}>
						{#if i < hints}
							<span class="hint-orb-inner">✦</span>
						{:else}
							<span class="hint-orb-empty">○</span>
						{/if}
					</div>
				{/each}
			</div>
			
			<!-- Use hint button -->
			<button
				class="hint-use-button"
				class:available={canUseHint}
				class:used={hintUsedThisQuestion}
				disabled={!canUseHint}
				onclick={onUseHint}
			>
				{#if hintUsedThisQuestion}
					<span class="hint-button-icon">✓</span>
					<span>USED</span>
				{:else if hints > 0 && !hintDisabled}
					<span class="hint-button-icon">⚡</span>
					<span>50/50</span>
				{:else}
					<span class="hint-button-icon">✕</span>
					<span>EMPTY</span>
				{/if}
			</button>
			
			<p class="hint-helper-text">
				{#if hintUsedThisQuestion}
					Used this turn
				{:else if hints > 0}
					Eliminate 2 wrong
				{:else}
					No items left
				{/if}
			</p>
		</div>
		
		<!-- Drawer handle/tab (fixed position, always at same spot) -->
		<button class="hints-drawer-handle" class:connected={hintsExpanded} onclick={toggleHints}>
			<span class="hints-handle-icon">🎒</span>
			<span class="hints-handle-title">HINTS</span>
			<span class="hints-handle-count" class:has-hints={hints > 0}>{hints}/{maxHints}</span>
			<span class="hints-handle-arrow" class:expanded={hintsExpanded}>▲</span>
		</button>
	</div>

	<!-- ACCURACY PANEL (Fixed position, never moves) -->
	<div class="accuracy-panel" class:danger-pulse={isInDanger}>
		<div class="accuracy-panel-header">
			<span class="accuracy-panel-icon">🎯</span>
			<span class="accuracy-panel-title">ACCURACY</span>
		</div>
		
		<!-- Compact circular gauge -->
		<div class="accuracy-ring">
			<svg viewBox="0 0 80 80" class="accuracy-ring-svg">
				<circle cx="40" cy="40" r="32" fill="none" stroke="rgba(30, 41, 59, 0.8)" stroke-width="6"/>
				<circle 
					cx="40" cy="40" r="32" 
					fill="none" 
					stroke={gaugeColor()}
					stroke-width="6"
					stroke-linecap="round"
					stroke-dasharray="201.06"
					stroke-dashoffset={201.06 - (currentAccuracy / 100) * 201.06}
					transform="rotate(-90 40 40)"
					class="accuracy-progress-arc"
				/>
			</svg>
			<div class="accuracy-ring-value" style="color: {gaugeColor()}">{currentAccuracy.toFixed(0)}%</div>
		</div>
		
		<!-- Status badge below gauge -->
		<div class="accuracy-status-badge" style="color: {gaugeColor()}; border-color: {gaugeColor()}">
			{statusText()}
		</div>
		
		<!-- Target row -->
		<div class="accuracy-target-row">
			<span class="accuracy-target-label">TARGET</span>
			<span class="accuracy-target-value">{threshold}%</span>
		</div>
		
		<!-- Stats row -->
		<div class="accuracy-stats-row">
			<div class="accuracy-stat correct">
				<span class="accuracy-stat-icon">✓</span>
				<span class="accuracy-stat-number">{correctAnswers}</span>
			</div>
			<div class="accuracy-stat-divider">│</div>
			<div class="accuracy-stat incorrect">
				<span class="accuracy-stat-icon">✕</span>
				<span class="accuracy-stat-number">{incorrectAnswers}</span>
			</div>
		</div>
	</div>
	

</div>
