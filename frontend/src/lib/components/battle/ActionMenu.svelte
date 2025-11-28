<script lang="ts">
	let { choices, onSelect, disabled = false, eliminatedChoices = [] }: {
		choices: string[];
		onSelect: (index: number) => void;
		disabled?: boolean;
		eliminatedChoices?: number[];
	} = $props();
	
	const choiceLabels = ['A', 'B', 'C', 'D'];
	
	// Debug effect to log eliminated choices
	$effect(() => {
		console.log('[ActionMenu] Current eliminatedChoices:', eliminatedChoices);
		console.log('[ActionMenu] eliminatedChoices length:', eliminatedChoices.length);
	});
</script>

<div class="action-menu">
	<div class="grid grid-cols-1 md:grid-cols-2 gap-3">
		{#each choices as choice, index}
			{@const isEliminated = eliminatedChoices.includes(index)}
			{#if isEliminated}
				{@const debugLog = console.log(`[ActionMenu] Choice ${index} is eliminated`)}
			{/if}
			<button
				class="choice-button"
				class:eliminated={isEliminated}
				class:disabled={disabled && !isEliminated}
				onclick={() => onSelect(index)}
				disabled={disabled || isEliminated}
			>
				<!-- Choice label badge -->
				<div class="choice-label" class:eliminated={isEliminated}>
					{choiceLabels[index]}
				</div>
				
				<!-- Selection arrow indicator -->
				<div class="selection-arrow">▶</div>
				
				<!-- Choice text -->
				<span class="choice-text" class:eliminated={isEliminated}>
					{#if isEliminated}
						<span class="eliminated-mark">✕</span>
					{/if}
					{choice}
				</span>
				
				<!-- Hover glow effect -->
				<div class="hover-glow"></div>
			</button>
		{/each}
	</div>
</div>

<style>
	.action-menu {
		filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.3));
	}
	
	.choice-button {
		position: relative;
		display: flex;
		align-items: center;
		gap: 10px;
		padding: 12px 16px;
		background: linear-gradient(180deg, 
			rgba(15, 23, 42, 0.95) 0%, 
			rgba(30, 41, 59, 0.95) 100%
		);
		border: 3px solid rgba(71, 85, 105, 0.8);
		border-radius: 10px;
		text-align: left;
		cursor: pointer;
		transition: all 0.2s ease;
		overflow: hidden;
		
		box-shadow: 
			0 0 0 1px rgba(148, 163, 184, 0.2),
			inset 0 1px 0 rgba(255, 255, 255, 0.05);
	}
	
	.choice-button:hover:not(:disabled) {
		border-color: #60a5fa;
		transform: translateX(4px);
		box-shadow: 
			0 0 0 1px rgba(96, 165, 250, 0.4),
			0 0 20px rgba(59, 130, 246, 0.3),
			inset 0 1px 0 rgba(255, 255, 255, 0.1);
	}
	
	.choice-button:active:not(:disabled) {
		transform: translateX(4px) translateY(2px);
	}
	
	.choice-button.eliminated {
		opacity: 0.5;
		cursor: not-allowed;
		border-color: rgba(239, 68, 68, 0.4);
		background: linear-gradient(180deg, 
			rgba(30, 30, 30, 0.9) 0%, 
			rgba(40, 40, 40, 0.9) 100%
		);
	}
	
	.choice-button.disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}
	
	/* Choice label (A, B, C, D) */
	.choice-label {
		display: flex;
		align-items: center;
		justify-content: center;
		min-width: 32px;
		height: 32px;
		background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
		border-radius: 6px;
		font-weight: 800;
		font-size: 0.875rem;
		color: white;
		text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
		box-shadow: 
			0 2px 4px rgba(0, 0, 0, 0.3),
			inset 0 1px 0 rgba(255, 255, 255, 0.2);
		flex-shrink: 0;
	}
	
	.choice-label.eliminated {
		background: linear-gradient(135deg, #64748b 0%, #475569 100%);
	}
	
	/* Selection arrow */
	.selection-arrow {
		position: absolute;
		left: 8px;
		opacity: 0;
		color: #60a5fa;
		font-size: 0.75rem;
		transition: opacity 0.15s ease;
		text-shadow: 0 0 8px rgba(96, 165, 250, 0.8);
	}
	
	.choice-button:hover:not(:disabled) .selection-arrow {
		opacity: 1;
		animation: arrow-bounce 0.6s ease infinite;
	}
	
	@keyframes arrow-bounce {
		0%, 100% { transform: translateX(0); }
		50% { transform: translateX(3px); }
	}
	
	/* Choice text */
	.choice-text {
		flex: 1;
		font-weight: 600;
		font-size: 0.875rem;
		color: #e2e8f0;
		line-height: 1.3;
		transition: color 0.2s ease;
	}
	
	.choice-button:hover:not(:disabled) .choice-text {
		color: #ffffff;
	}
	
	.choice-text.eliminated {
		text-decoration: line-through;
		color: #64748b;
	}
	
	.eliminated-mark {
		color: #ef4444;
		margin-right: 8px;
		font-weight: 700;
	}
	
	/* Hover glow effect */
	.hover-glow {
		position: absolute;
		inset: 0;
		background: linear-gradient(90deg, 
			transparent 0%, 
			rgba(59, 130, 246, 0.1) 50%, 
			transparent 100%
		);
		opacity: 0;
		transition: opacity 0.2s ease;
		pointer-events: none;
	}
	
	.choice-button:hover:not(:disabled) .hover-glow {
		opacity: 1;
	}
</style>
