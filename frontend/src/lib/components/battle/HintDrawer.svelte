<script lang="ts">
	import { onMount, onDestroy } from 'svelte';

	interface Props {
		hints: number;
		maxHints: number;
		onUseHint: () => void;
		disabled?: boolean;
		hintUsedThisQuestion?: boolean;
	}

	let { hints = $bindable(0), maxHints = $bindable(0), onUseHint, disabled = false, hintUsedThisQuestion = false }: Props = $props();

	let isOpen = $state(false);
	let canUseHint = $derived(hints > 0 && !disabled && !hintUsedThisQuestion);

	// Inline offsets computed at runtime to anchor above the AccuracyGauge
	let rightOffset = $state('20px');
	let bottomOffset = $state('0px');

	function computeAnchorOffsets() {
		try {
			const anchor = document.querySelector('[data-accuracy-anchor]') as HTMLElement | null;
			if (!anchor) {
				// fallback
				rightOffset = '20px';
				bottomOffset = '0px';
				return;
			}

			const rect = anchor.getBoundingClientRect();
			// Align right edge with anchor's right, and place drawer above anchor's top
			const rightPx = Math.max(8, Math.round(window.innerWidth - rect.right));
			const bottomPx = Math.max(8, Math.round(window.innerHeight - rect.top + 8));

			rightOffset = `${rightPx}px`;
			bottomOffset = `${bottomPx}px`;
		} catch (e) {
			rightOffset = '20px';
			bottomOffset = '0px';
		}
	}

	let resizeHandler: () => void;
	let scrollHandler: () => void;

	onMount(() => {
		// Compute initial position and update on resize/scroll
		computeAnchorOffsets();

		resizeHandler = () => computeAnchorOffsets();
		scrollHandler = () => computeAnchorOffsets();

		window.addEventListener('resize', resizeHandler, { passive: true });
		window.addEventListener('scroll', scrollHandler, { passive: true });
	});

	onDestroy(() => {
		if (resizeHandler) window.removeEventListener('resize', resizeHandler);
		if (scrollHandler) window.removeEventListener('scroll', scrollHandler);
	});

	function toggleDrawer() {
		isOpen = !isOpen;
	}
</script>

<!-- Hint Drawer Container -->
<div class="hint-drawer-container" style="right: {rightOffset}; bottom: {bottomOffset};">
	<!-- Collapsed Tab (Always visible) -->
	<button 
		class="hint-tab"
		class:has-hints={hints > 0}
		class:no-hints={hints === 0}
		onclick={toggleDrawer}
		aria-label="Toggle hints"
	>
		<!-- Arrow indicator -->
		<div class="arrow-indicator" class:open={isOpen}>
			{isOpen ? '▼' : '▲'}
		</div>
		
		<!-- Hint icon and label -->
		<div class="tab-content">
			<span class="tab-icon">💡</span>
			<span class="tab-label">HINTS</span>
			<span class="tab-counter" class:pulse={hints > 0 && !hintUsedThisQuestion}>{hints}/{maxHints}</span>
		</div>
	</button>
	
	<!-- Expandable Drawer Panel -->
	<div class="drawer-panel" class:open={isOpen}>
		<div class="drawer-inner">
			<!-- Header -->
			<div class="drawer-header">
				<div class="header-left">
					<span class="header-icon">💡</span>
					<h3 class="header-title">HINT INVENTORY</h3>
				</div>
				<div class="hint-count">{hints}/{maxHints}</div>
			</div>
			
			<!-- Hint Orbs Display -->
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
						<span>Use 50/50</span>
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
					Hint already used for this question
				{:else if hints > 0}
					50/50: Removes 2 wrong answers
				{:else}
					All hints used this round
				{/if}
			</p>
		</div>
	</div>
</div>

<style>
	.hint-drawer-container {
		position: fixed;
		bottom: 0;
		right: 20px;
		z-index: 100;
		pointer-events: none;
	}
	
	/* Collapsed Tab (Always visible at bottom) */
	.hint-tab {
		position: relative;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 4px;
		padding: 8px 16px 6px;
		background: linear-gradient(180deg, 
			rgba(49, 46, 129, 0.95) 0%, 
			rgba(30, 27, 75, 0.95) 100%
		);
		border: 3px solid transparent;
		border-bottom: none;
		border-radius: 12px 12px 0 0;
		cursor: pointer;
		pointer-events: auto;
		transition: all 0.3s ease;
		
		box-shadow: 
			0 0 0 2px rgba(139, 92, 246, 0.4),
			0 -4px 12px rgba(0, 0, 0, 0.3),
			inset 0 1px 0 rgba(255, 255, 255, 0.05);
	}
	
	.hint-tab::before {
		content: '';
		position: absolute;
		top: -3px;
		left: -3px;
		right: -3px;
		bottom: 0;
		border-radius: 12px 12px 0 0;
		background: linear-gradient(135deg, 
			rgba(167, 139, 250, 0.3) 0%, 
			rgba(88, 28, 135, 0.2) 50%, 
			rgba(167, 139, 250, 0.3) 100%
		);
		z-index: -1;
	}
	
	.hint-tab:hover {
		transform: translateY(-2px);
		box-shadow: 
			0 0 0 2px rgba(139, 92, 246, 0.6),
			0 -6px 16px rgba(0, 0, 0, 0.4),
			0 0 20px rgba(139, 92, 246, 0.2),
			inset 0 1px 0 rgba(255, 255, 255, 0.1);
	}
	
	.hint-tab.no-hints {
		background: linear-gradient(180deg, 
			rgba(30, 30, 40, 0.95) 0%, 
			rgba(20, 20, 30, 0.95) 100%
		);
	}
	
	.hint-tab.no-hints::before {
		background: linear-gradient(135deg, 
			rgba(71, 85, 105, 0.3) 0%, 
			rgba(51, 65, 85, 0.2) 100%
		);
	}
	
	.arrow-indicator {
		font-size: 0.75rem;
		color: #a78bfa;
		transition: transform 0.3s ease;
		animation: arrow-bob 2s ease-in-out infinite;
	}
	
	.arrow-indicator.open {
		animation: none;
	}
	
	@keyframes arrow-bob {
		0%, 100% { transform: translateY(0); }
		50% { transform: translateY(-3px); }
	}
	
	.tab-content {
		display: flex;
		align-items: center;
		gap: 6px;
	}
	
	.tab-icon {
		font-size: 1rem;
	}
	
	.tab-label {
		font-size: 0.6rem;
		font-weight: 800;
		letter-spacing: 0.1em;
		color: #a78bfa;
		text-transform: uppercase;
		font-family: 'Press Start 2P', system-ui, monospace;
	}
	
	.tab-counter {
		font-size: 0.875rem;
		font-weight: 800;
		color: #fbbf24;
		text-shadow: 0 0 8px rgba(251, 191, 36, 0.5);
		font-family: 'Press Start 2P', system-ui, monospace;
	}
	
	.tab-counter.pulse {
		animation: pulse-glow 2s ease-in-out infinite;
	}
	
	@keyframes pulse-glow {
		0%, 100% { 
			text-shadow: 0 0 8px rgba(251, 191, 36, 0.5);
		}
		50% { 
			text-shadow: 0 0 16px rgba(251, 191, 36, 0.8), 0 0 24px rgba(251, 191, 36, 0.4);
		}
	}
	
	/* Expandable Drawer Panel */
	.drawer-panel {
		position: absolute;
		bottom: 100%;
		right: 0;
		width: 280px;
		max-height: 0;
		overflow: hidden;
		opacity: 0;
		transform: translateY(20px);
		transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
		pointer-events: none;
	}
	
	.drawer-panel.open {
		max-height: 400px;
		opacity: 1;
		transform: translateY(0);
		pointer-events: auto;
	}
	
	.drawer-inner {
		background: linear-gradient(180deg, 
			rgba(49, 46, 129, 0.98) 0%, 
			rgba(30, 27, 75, 0.98) 50%,
			rgba(49, 46, 129, 0.98) 100%
		);
		border: 3px solid transparent;
		border-bottom: none;
		border-radius: 16px 16px 0 0;
		padding: 16px;
		
		box-shadow: 
			0 0 0 2px rgba(139, 92, 246, 0.4),
			0 -8px 24px rgba(0, 0, 0, 0.4),
			inset 0 1px 0 rgba(255, 255, 255, 0.05);
	}
	
	.drawer-inner::before {
		content: '';
		position: absolute;
		top: -3px;
		left: -3px;
		right: -3px;
		bottom: 0;
		border-radius: 18px 18px 0 0;
		background: linear-gradient(135deg, 
			rgba(167, 139, 250, 0.3) 0%, 
			rgba(88, 28, 135, 0.2) 50%, 
			rgba(167, 139, 250, 0.3) 100%
		);
		z-index: -1;
		pointer-events: none;
	}
	
	/* Drawer Header */
	.drawer-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding-bottom: 12px;
		border-bottom: 2px solid rgba(139, 92, 246, 0.3);
		margin-bottom: 12px;
	}
	
	.header-left {
		display: flex;
		align-items: center;
		gap: 8px;
	}
	
	.header-icon {
		font-size: 1.25rem;
	}
	
	.header-title {
		font-size: 0.65rem;
		font-weight: 800;
		letter-spacing: 0.1em;
		color: #a78bfa;
		text-transform: uppercase;
		font-family: 'Press Start 2P', system-ui, monospace;
		margin: 0;
	}
	
	.hint-count {
		font-size: 1rem;
		font-weight: 800;
		color: #fbbf24;
		text-shadow: 0 0 8px rgba(251, 191, 36, 0.5);
		font-family: 'Press Start 2P', system-ui, monospace;
	}
	
	/* Orbs Container */
	.orbs-container {
		display: flex;
		justify-content: center;
		gap: 8px;
		margin-bottom: 12px;
	}
	
	.orb-wrapper {
		position: relative;
	}
	
	.hint-orb {
		position: relative;
		width: 32px;
		height: 32px;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		transition: all 0.3s ease;
	}
	
	.hint-orb.active {
		background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
		box-shadow: 
			0 0 16px rgba(251, 191, 36, 0.6),
			0 4px 8px rgba(0, 0, 0, 0.3),
			inset 0 1px 0 rgba(255, 255, 255, 0.4);
		animation: orb-pulse 2s ease-in-out infinite;
	}
	
	.hint-orb.empty {
		background: rgba(30, 30, 40, 0.6);
		border: 2px solid rgba(71, 85, 105, 0.5);
	}
	
	@keyframes orb-pulse {
		0%, 100% { 
			box-shadow: 
				0 0 16px rgba(251, 191, 36, 0.6),
				0 4px 8px rgba(0, 0, 0, 0.3);
		}
		50% { 
			box-shadow: 
				0 0 24px rgba(251, 191, 36, 0.8),
				0 4px 8px rgba(0, 0, 0, 0.3);
		}
	}
	
	.orb-glow {
		position: absolute;
		inset: -6px;
		border-radius: 50%;
		background: radial-gradient(circle, rgba(251, 191, 36, 0.3) 0%, transparent 70%);
	}
	
	.orb-icon {
		font-size: 0.875rem;
		color: #fff;
		text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
		z-index: 1;
	}
	
	.orb-empty {
		font-size: 1rem;
		color: #475569;
	}
	
	/* Use Hint Button */
	.hint-button {
		position: relative;
		width: 100%;
		padding: 10px 14px;
		border-radius: 10px;
		border: none;
		cursor: pointer;
		overflow: hidden;
		transition: all 0.2s ease;
		margin-bottom: 8px;
	}
	
	.hint-button.active {
		background: linear-gradient(180deg, #fbbf24 0%, #f59e0b 100%);
		box-shadow: 
			0 4px 12px rgba(251, 191, 36, 0.4),
			inset 0 1px 0 rgba(255, 255, 255, 0.3);
	}
	
	.hint-button.active:hover {
		transform: translateY(-2px);
		box-shadow: 
			0 6px 16px rgba(251, 191, 36, 0.5),
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
		font-size: 0.75rem;
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
		font-size: 0.6rem;
		color: rgba(167, 139, 250, 0.7);
		margin: 0;
		font-weight: 500;
		line-height: 1.4;
	}
	
	/* Responsive adjustments */
	@media (max-width: 768px) {
		.hint-drawer-container {
			right: 10px;
		}
		
		.drawer-panel {
			width: 260px;
		}
	}
</style>
