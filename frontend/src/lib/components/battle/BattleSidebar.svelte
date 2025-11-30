<script lang="ts">
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
		// Flee props
		onFlee?: () => void;
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
		hintUsedThisQuestion = false,
		onFlee
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
		<div class="drawer-content" class:expanded={hintsExpanded}>
			<!-- Hint orbs row -->
			<div class="hint-orbs">
				{#each Array(maxHints) as _, i}
					<div class="orb" class:active={i < hints} class:used={i >= hints}>
						{#if i < hints}
							<span class="orb-inner">✦</span>
						{:else}
							<span class="orb-empty">○</span>
						{/if}
					</div>
				{/each}
			</div>
			
			<!-- Use hint button -->
			<button
				class="use-hint-btn"
				class:available={canUseHint}
				class:used={hintUsedThisQuestion}
				disabled={!canUseHint}
				onclick={onUseHint}
			>
				{#if hintUsedThisQuestion}
					<span class="btn-icon">✓</span>
					<span>USED</span>
				{:else if hints > 0 && !hintDisabled}
					<span class="btn-icon">⚡</span>
					<span>50/50</span>
				{:else}
					<span class="btn-icon">✕</span>
					<span>EMPTY</span>
				{/if}
			</button>
			
			<p class="hint-helper">
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
		<button class="drawer-handle" onclick={toggleHints}>
			<span class="handle-icon">🎒</span>
			<span class="handle-title">HINTS</span>
			<span class="handle-count" class:has-hints={hints > 0}>{hints}/{maxHints}</span>
			<span class="handle-arrow" class:expanded={hintsExpanded}>▲</span>
		</button>
	</div>

	<!-- ACCURACY PANEL (Fixed position, never moves) -->
	<div class="accuracy-panel" class:danger-pulse={isInDanger}>
		<div class="panel-header">
			<span class="panel-icon">🎯</span>
			<span class="panel-title">ACCURACY</span>
			<span class="panel-badge" style="color: {gaugeColor()}">{statusText()}</span>
		</div>
		
		<!-- Compact circular gauge -->
		<div class="accuracy-ring">
			<svg viewBox="0 0 80 80" class="ring-svg">
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
					class="progress-arc"
				/>
			</svg>
			<div class="ring-value" style="color: {gaugeColor()}">{currentAccuracy.toFixed(0)}%</div>
		</div>
		
		<!-- Target row -->
		<div class="target-row">
			<span class="target-label">TARGET</span>
			<span class="target-value">{threshold}%</span>
		</div>
		
		<!-- Stats row -->
		<div class="stats-row">
			<div class="stat correct">
				<span class="stat-icon">✓</span>
				<span class="stat-num">{correctAnswers}</span>
			</div>
			<div class="stat-divider">│</div>
			<div class="stat incorrect">
				<span class="stat-icon">✕</span>
				<span class="stat-num">{incorrectAnswers}</span>
			</div>
		</div>
	</div>
	
	<!-- FLEE BUTTON -->
	{#if onFlee}
		<button class="flee-btn" onclick={onFlee}>
			<span class="flee-icon">🚪</span>
			<span class="flee-text">FLEE</span>
		</button>
	{/if}
</div>

<style>
	.battle-sidebar {
		width: 100%;
		display: flex;
		flex-direction: column;
		gap: 8px;
	}
	
	/* ========== HINTS DRAWER WRAPPER ========== */
	.hints-drawer-wrapper {
		position: relative;
		z-index: 10;
	}
	
	/* Floating content - absolutely positioned ABOVE the handle */
	.drawer-content {
		position: absolute;
		bottom: 100%;
		left: 0;
		right: 0;
		background: linear-gradient(180deg, 
			rgba(15, 23, 42, 0.98) 0%, 
			rgba(30, 41, 59, 0.98) 100%
		);
		border: 2px solid rgba(71, 85, 105, 0.5);
		border-bottom: none;
		border-radius: 10px 10px 0 0;
		padding: 10px;
		box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.3);
		
		/* Hidden by default */
		opacity: 0;
		visibility: hidden;
		transform: translateY(10px);
		transition: all 0.25s ease;
	}
	
	.drawer-content.expanded {
		opacity: 1;
		visibility: visible;
		transform: translateY(0);
	}
	
	/* Handle - always visible, fixed height */
	.drawer-handle {
		display: flex;
		align-items: center;
		width: 100%;
		padding: 8px 10px;
		background: linear-gradient(180deg, 
			rgba(15, 23, 42, 0.98) 0%, 
			rgba(30, 41, 59, 0.95) 100%
		);
		border: 2px solid rgba(71, 85, 105, 0.5);
		border-radius: 10px;
		cursor: pointer;
		transition: all 0.2s;
		box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
	}
	
	.drawer-handle:hover {
		background: linear-gradient(180deg, 
			rgba(25, 35, 55, 0.98) 0%, 
			rgba(40, 50, 70, 0.95) 100%
		);
	}
	
	.handle-icon {
		font-size: 0.875rem;
	}
	
	.handle-title {
		font-size: 0.5rem;
		font-weight: 800;
		letter-spacing: 0.1em;
		color: #94a3b8;
		font-family: 'Press Start 2P', system-ui, monospace;
		flex: 1;
		margin-left: 6px;
		text-align: left;
	}
	
	.handle-count {
		font-size: 0.6rem;
		font-weight: 800;
		color: #64748b;
		font-family: 'Press Start 2P', system-ui, monospace;
		margin-right: 6px;
	}
	
	.handle-count.has-hints {
		color: #fbbf24;
		text-shadow: 0 0 6px rgba(251, 191, 36, 0.5);
	}
	
	.handle-arrow {
		font-size: 0.625rem;
		color: #64748b;
		transition: transform 0.3s ease;
	}
	
	.handle-arrow.expanded {
		transform: rotate(180deg);
	}
	
	/* Hint orbs */
	.hint-orbs {
		display: flex;
		justify-content: center;
		gap: 6px;
		margin-bottom: 8px;
	}
	
	.orb {
		width: 22px;
		height: 22px;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		transition: all 0.3s;
	}
	
	.orb.active {
		background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
		box-shadow: 0 0 8px rgba(251, 191, 36, 0.5);
	}
	
	.orb.used {
		background: rgba(30, 30, 40, 0.6);
		border: 1px solid rgba(71, 85, 105, 0.4);
	}
	
	.orb-inner {
		font-size: 0.625rem;
		color: #fff;
		text-shadow: 0 1px 2px rgba(0,0,0,0.3);
	}
	
	.orb-empty {
		font-size: 0.625rem;
		color: #475569;
	}
	
	/* Use hint button */
	.use-hint-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 6px;
		width: 100%;
		padding: 6px;
		border: none;
		border-radius: 6px;
		font-size: 0.55rem;
		font-weight: 800;
		font-family: 'Press Start 2P', system-ui, monospace;
		text-transform: uppercase;
		cursor: pointer;
		transition: all 0.2s;
		background: rgba(51, 65, 85, 0.5);
		color: #64748b;
	}
	
	.use-hint-btn.available {
		background: linear-gradient(180deg, #fbbf24 0%, #f59e0b 100%);
		color: #1e1b4b;
		box-shadow: 0 2px 8px rgba(251, 191, 36, 0.4);
	}
	
	.use-hint-btn.available:hover {
		transform: translateY(-1px);
		box-shadow: 0 4px 12px rgba(251, 191, 36, 0.5);
	}
	
	.use-hint-btn.used {
		background: rgba(34, 197, 94, 0.2);
		color: #22c55e;
		border: 1px solid rgba(34, 197, 94, 0.3);
	}
	
	.use-hint-btn:disabled:not(.used) {
		cursor: not-allowed;
		opacity: 0.6;
	}
	
	.btn-icon {
		font-size: 0.65rem;
	}
	
	.hint-helper {
		text-align: center;
		font-size: 0.45rem;
		color: #64748b;
		margin: 4px 0 0;
	}
	
	/* ========== ACCURACY PANEL ========== */
	.accuracy-panel {
		background: linear-gradient(180deg, 
			rgba(15, 23, 42, 0.98) 0%, 
			rgba(30, 41, 59, 0.95) 100%
		);
		border: 2px solid rgba(71, 85, 105, 0.5);
		border-radius: 10px;
		padding: 10px;
		box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
	}
	
	/* Danger pulse */
	.danger-pulse {
		animation: danger-glow 1.5s ease-in-out infinite;
	}
	
	@keyframes danger-glow {
		0%, 100% { border-color: rgba(71, 85, 105, 0.5); }
		50% { border-color: rgba(239, 68, 68, 0.6); box-shadow: 0 0 20px rgba(239, 68, 68, 0.15); }
	}
	
	.panel-header {
		display: flex;
		align-items: center;
		gap: 6px;
		padding: 6px 8px;
		background: rgba(0, 0, 0, 0.2);
		border-radius: 6px;
		margin-bottom: 8px;
	}
	
	.panel-icon {
		font-size: 0.875rem;
	}
	
	.panel-title {
		font-size: 0.5rem;
		font-weight: 800;
		letter-spacing: 0.1em;
		color: #94a3b8;
		font-family: 'Press Start 2P', system-ui, monospace;
		flex: 1;
	}
	
	.panel-badge {
		font-size: 0.5rem;
		font-weight: 800;
		font-family: 'Press Start 2P', system-ui, monospace;
		text-shadow: 0 0 6px currentColor;
	}
	
	/* Accuracy ring */
	.accuracy-ring {
		position: relative;
		width: 70px;
		height: 70px;
		margin: 0 auto 8px;
	}
	
	.ring-svg {
		width: 100%;
		height: 100%;
	}
	
	.progress-arc {
		transition: stroke-dashoffset 0.5s ease, stroke 0.3s ease;
		filter: drop-shadow(0 0 4px currentColor);
	}
	
	.ring-value {
		position: absolute;
		inset: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 0.875rem;
		font-weight: 900;
		font-family: 'Press Start 2P', system-ui, monospace;
		text-shadow: 0 0 8px currentColor;
	}
	
	.target-row {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 6px;
		padding: 4px 8px;
		background: rgba(0, 0, 0, 0.25);
		border-radius: 4px;
		margin-bottom: 6px;
	}
	
	.target-label {
		font-size: 0.45rem;
		font-weight: 600;
		color: #64748b;
		letter-spacing: 0.05em;
	}
	
	.target-value {
		font-size: 0.65rem;
		font-weight: 800;
		color: #f59e0b;
		font-family: 'Press Start 2P', system-ui, monospace;
		text-shadow: 0 0 4px rgba(245, 158, 11, 0.5);
	}
	
	/* Stats row */
	.stats-row {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 8px;
		padding: 6px;
		background: rgba(0, 0, 0, 0.2);
		border-radius: 6px;
	}
	
	.stat {
		display: flex;
		align-items: center;
		gap: 4px;
	}
	
	.stat-icon {
		font-size: 0.7rem;
	}
	
	.stat-num {
		font-size: 0.7rem;
		font-weight: 800;
		font-family: 'Press Start 2P', system-ui, monospace;
	}
	
	.stat.correct {
		color: #22c55e;
		text-shadow: 0 0 6px rgba(34, 197, 94, 0.4);
	}
	
	.stat.incorrect {
		color: #ef4444;
		text-shadow: 0 0 6px rgba(239, 68, 68, 0.4);
	}
	
	.stat-divider {
		color: #475569;
		font-size: 0.75rem;
	}
	
	/* ========== FLEE BUTTON ========== */
	.flee-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 6px;
		width: 100%;
		padding: 8px 10px;
		background: linear-gradient(180deg, 
			rgba(127, 29, 29, 0.6) 0%, 
			rgba(153, 27, 27, 0.8) 100%
		);
		border: 2px solid rgba(239, 68, 68, 0.4);
		border-radius: 10px;
		cursor: pointer;
		transition: all 0.2s;
		box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
	}
	
	.flee-btn:hover {
		background: linear-gradient(180deg, 
			rgba(153, 27, 27, 0.8) 0%, 
			rgba(185, 28, 28, 0.9) 100%
		);
		border-color: rgba(239, 68, 68, 0.6);
		box-shadow: 
			0 4px 12px rgba(0, 0, 0, 0.3),
			0 0 15px rgba(239, 68, 68, 0.2);
	}
	
	.flee-icon {
		font-size: 0.875rem;
	}
	
	.flee-text {
		font-size: 0.5rem;
		font-weight: 800;
		letter-spacing: 0.1em;
		color: #fca5a5;
		font-family: 'Press Start 2P', system-ui, monospace;
	}
</style>
