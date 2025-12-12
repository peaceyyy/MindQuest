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
		filter: drop-shadow(var(--drop-shadow-md));
	}
	
	.choice-button {
		position: relative;
		display: flex;
		align-items: center;
		gap: var(--spacing-md);
		padding: 1.25rem 1.25rem;
		min-height: 90px;
		background: var(--bg-card-primary);
		border: 3px solid var(--color-slate-600);
		border-radius: var(--radius-lg);
		text-align: left;
		cursor: pointer;
		transition: all var(--transition-normal);
		overflow: hidden;
		box-shadow: 
			0 0 0 1px var(--color-slate-400)20,
			inset 0 1px 0 rgba(255, 255, 255, 0.05);
	}
	
	.choice-button:hover:not(:disabled) {
		border-color: var(--color-player-primary);
		transform: translateX(4px) translateY(-2px);
		box-shadow: 
			0 0 0 1px var(--color-player-primary)66,
			0 0 30px rgba(59, 130, 246, 0.6),
			0 0 60px rgba(59, 130, 246, 0.3),
			0 8px 20px rgba(0, 0, 0, 0.4),
			inset 0 1px 0 rgba(255, 255, 255, 0.15);
		animation: choice-energy 0.6s ease-in-out infinite;
	}
	
	@keyframes choice-energy {
		0%, 100% {
			filter: brightness(1) contrast(1);
		}
		50% {
			filter: brightness(1.15) contrast(1.1);
		}
	}
	
	.choice-button:active:not(:disabled) {
		transform: translateX(4px) translateY(2px);
	}
	
	.choice-button.eliminated {
		opacity: 0.5;
		cursor: not-allowed;
		border-color: var(--color-danger)66;
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
		background: linear-gradient(135deg, var(--color-player-primary) 0%, var(--color-player-secondary) 100%);
		border-radius: var(--radius-sm);
		font-weight: 800;
		font-size: var(--text-base);
		color: white;
		text-shadow: var(--text-shadow-standard);
		box-shadow: 
			var(--drop-shadow-sm),
			inset 0 1px 0 rgba(255, 255, 255, 0.2);
		flex-shrink: 0;
	}
	
	.choice-label.eliminated {
		background: linear-gradient(135deg, var(--color-slate-500) 0%, var(--color-slate-600) 100%);
	}
	
	/* Selection arrow */
	.selection-arrow {
		position: absolute;
		left: 8px;
		opacity: 0;
		color: var(--color-player-primary);
		font-size: var(--text-sm);
		transition: opacity var(--transition-fast);
		text-shadow: var(--glow-blue);
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
		font-size: var(--text-base);
		color: var(--color-slate-100);
		line-height: 1.3;
		transition: color var(--transition-normal);
	}
	
	.choice-button:hover:not(:disabled) .choice-text {
		color: #ffffff;
	}
	
	.choice-text.eliminated {
		text-decoration: line-through;
		color: var(--color-slate-500);
	}
	
	.eliminated-mark {
		color: var(--color-danger);
		margin-right: var(--spacing-sm);
		font-weight: 700;
	}
	
	/* Hover glow effect */
	.hover-glow {
		position: absolute;
		inset: 0;
		background: linear-gradient(90deg, 
			transparent 0%, 
			var(--color-player-primary)1a 50%, 
			transparent 100%
		);
		opacity: 0;
		transition: opacity var(--transition-normal);
		pointer-events: none;
	}
	
	.choice-button:hover:not(:disabled) .hover-glow {
		opacity: 1;
	}
</style>
