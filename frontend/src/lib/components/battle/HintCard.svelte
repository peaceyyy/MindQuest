<script lang="ts">
	interface Props {
		hints: number;
		maxHints: number;
		onUseHint: () => void;
		disabled?: boolean;
		hintUsedThisQuestion?: boolean;
	}
	
	let { hints = $bindable(0), maxHints = $bindable(0), onUseHint, disabled = false, hintUsedThisQuestion = false }: Props = $props();
	
	let canUseHint = $derived(hints > 0 && !disabled);
</script>

<div class="hint-card">
	<!-- RPG-styled panel -->
	<div class="hint-panel" class:empty={hints === 0}>
		<!-- Header with icon -->
		<div class="panel-header">
			<div class="header-icon">💡</div>
			<span class="header-title">HINTS</span>
			<div class="hint-counter">{hints}/{maxHints}</div>
		</div>
		
		<!-- Hint orbs display -->
		<div class="orbs-container">
			{#each Array(maxHints) as _, i}
				<div class="orb-wrapper">
					<div 
						class="hint-orb"
						class:active={i < hints}
						class:empty={i >= hints}
					>
						{#if i < hints}
							<span class="orb-glow"></span>
							<span class="orb-icon">✦</span>
						{:else}
							<span class="orb-empty">○</span>
						{/if}
					</div>
				</div>
			{/each}
		</div>
		
		<!-- Use Hint Button -->
		<button
			class="hint-button"
			class:active={canUseHint}
			class:disabled={!canUseHint}
			disabled={!canUseHint}
			onclick={onUseHint}
		>
			<span class="button-bg"></span>
			<span class="button-content">
				{#if hintUsedThisQuestion}
					<span class="button-icon">✓</span>
					<span>Hint Used</span>
				{:else if hints > 0 && !disabled}
					<span class="button-icon">✨</span>
					<span>Use Hint</span>
				{:else if disabled}
					<span class="button-icon">⏳</span>
					<span>Wait...</span>
				{:else}
					<span class="button-icon">✕</span>
					<span>No hints left</span>
				{/if}
			</span>
		</button>
		
		<!-- Helper text -->
		<p class="helper-text">
			{#if hintUsedThisQuestion}
				Hint already used
			{:else if hints > 0}
				50/50: Removes 2 wrong answers
			{:else}
				All hints used this round
			{/if}
		</p>
	</div>
</div>

<style>
	.hint-card {
		width: 100%;
	}
	
	.hint-panel {
		position: relative;
		background: var(--bg-hint-panel);
		border: 3px solid transparent;
		border-radius: var(--radius-lg);
		padding: var(--spacing-lg);
		
		/* Multi-layer border effect */
		box-shadow: 
			0 0 0 2px rgba(139, 92, 246, 0.4),
			0 0 0 4px rgba(49, 46, 129, 0.6),
			var(--drop-shadow-lg),
			inset 0 1px 0 rgba(255, 255, 255, 0.05);
	}
	
	.hint-panel::before {
		content: '';
		position: absolute;
		inset: -4px;
		border-radius: 18px;
		background: linear-gradient(135deg, 
			rgba(167, 139, 250, 0.3) 0%, 
			rgba(88, 28, 135, 0.2) 50%, 
			rgba(167, 139, 250, 0.3) 100%
		);
		z-index: -1;
	}
	
	.hint-panel.empty {
		background: linear-gradient(180deg, 
			rgba(30, 30, 40, 0.95) 0%, 
			rgba(20, 20, 30, 0.95) 100%
		);
	}
	
	.hint-panel.empty::before {
		background: linear-gradient(135deg, 
			rgba(71, 85, 105, 0.3) 0%, 
			rgba(51, 65, 85, 0.2) 100%
		);
	}
	
	/* Panel header */
	.panel-header {
		display: flex;
		align-items: center;
		gap: 6px;
		padding-bottom: 8px;
		border-bottom: 2px solid rgba(139, 92, 246, 0.3);
		margin-bottom: 8px;
	}
	
	.header-icon {
		font-size: 1.125rem;
	}
	
	.header-title {
		font-size: 0.6rem;
		font-weight: 800;
		letter-spacing: 0.1em;
		color: var(--color-purple-400);
		text-transform: uppercase;
		flex: 1;
		font-family: var(--font-pixel);
	}
	
	.hint-counter {
		font-size: 0.875rem;
		font-weight: 800;
		color: var(--color-gold);
		text-shadow: var(--text-shadow-glow-gold-strong);
		font-family: var(--font-pixel);
	}
	
	/* Hint orbs */
	.orbs-container {
		display: flex;
		justify-content: center;
		gap: 6px;
		margin-bottom: 8px;
	}
	
	.orb-wrapper {
		position: relative;
	}
	
	.hint-orb {
		position: relative;
		width: 24px;
		height: 24px;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		transition: all 0.3s ease;
	}
	
	.hint-orb.active {
		background: var(--bg-button-gold);
		box-shadow: 
			var(--glow-gold-strong),
			var(--drop-shadow-md),
			inset 0 1px 0 rgba(255, 255, 255, 0.4);
		animation: orb-pulse 2s ease-in-out infinite;
	}
	
	.hint-orb.empty {
		background: rgba(30, 30, 40, 0.6);
		border: 2px solid var(--color-slate-600-alpha);
	}
	
	@keyframes orb-pulse {
		0%, 100% { 
			box-shadow: 
				0 0 12px rgba(251, 191, 36, 0.6),
				0 4px 8px rgba(0, 0, 0, 0.3);
		}
		50% { 
			box-shadow: 
				0 0 20px rgba(251, 191, 36, 0.8),
				0 4px 8px rgba(0, 0, 0, 0.3);
		}
	}
	
	.orb-glow {
		position: absolute;
		inset: -4px;
		border-radius: 50%;
		background: radial-gradient(circle, rgba(251, 191, 36, 0.3) 0%, transparent 70%);
	}
	
	.orb-icon {
		font-size: 0.75rem;
		color: #fff;
		text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
		z-index: 1;
	}
	
	.orb-empty {
		font-size: 0.875rem;
		color: #475569;
	}
	
	/* Hint button */
	.hint-button {
		position: relative;
		width: 100%;
		padding: 8px 12px;
		border-radius: 8px;
		border: none;
		cursor: pointer;
		overflow: hidden;
		transition: all 0.2s ease;
	}
	
	.hint-button.active {
		background: var(--bg-button-gold);
		box-shadow: 
			var(--glow-gold),
			inset 0 1px 0 rgba(255, 255, 255, 0.3);
	}
	
	.hint-button.active:hover {
		transform: translateY(-2px);
		box-shadow: 
			var(--glow-gold-strong),
			inset 0 1px 0 rgba(255, 255, 255, 0.3);
	}
	
	.hint-button.active:active {
		transform: translateY(1px);
	}
	
	.hint-button.disabled {
		background: rgba(51, 65, 85, 0.6);
		cursor: not-allowed;
		box-shadow: none;
	}
	
	.button-bg {
		position: absolute;
		inset: 0;
		background: linear-gradient(180deg, 
			rgba(255, 255, 255, 0.1) 0%, 
			transparent 50%
		);
	}
	
	.button-content {
		position: relative;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 6px;
		font-size: 0.7rem;
		font-weight: 700;
		text-transform: uppercase;
		letter-spacing: 0.05em;
		z-index: 1;
		font-family: 'Press Start 2P', system-ui, monospace;
	}
	
	.hint-button.active .button-content {
		color: #1e1b4b;
		text-shadow: 0 1px 0 rgba(255, 255, 255, 0.3);
	}
	
	.hint-button.disabled .button-content {
		color: #64748b;
	}
	
	.button-icon {
		font-size: 0.875rem;
	}
	
	/* Helper text */
	.helper-text {
		text-align: center;
		font-size: 0.55rem;
		color: rgba(167, 139, 250, 0.7);
		margin-top: 6px;
		font-weight: 500;
	}
	
	.hint-panel.empty .helper-text {
		color: rgba(100, 116, 139, 0.7);
	}
</style>
