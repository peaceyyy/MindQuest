<script lang="ts">
	interface Props {
		current?: number;
		max?: number;
		label?: string;
		color?: string;
		barRef?: HTMLDivElement | null;
	}
	
	let { current = 100, max = 100, label = "HP", color = "bg-green-500", barRef = $bindable(null) }: Props = $props();
	
	let percentage = $derived(Math.max(0, Math.min(100, (current / max) * 100)));
	
	// Dynamic color based on HP percentage
	let dynamicColor = $derived(() => {
		if (percentage <= 25) return 'bg-red-500';
		if (percentage <= 50) return 'bg-yellow-500';
		return color;
	});
</script>

<div class="w-full max-w-[200px]" bind:this={barRef}>
	<div class="flex justify-between text-xs font-bold mb-1 uppercase tracking-wider text-white hp-label">
		<span>{label}</span>
		<span>{current}/{max}</span>
	</div>
	<div class="hp-bar-container">
		<div 
			class="h-full {dynamicColor()} hp-bar-fill"
			style="width: {percentage}%"
		></div>
	</div>
</div>

<style>
	.hp-label {
		text-shadow: 2px 2px 0 rgba(0, 0, 0, 0.5), var(--text-shadow-glow-gold);
	}
	
	.hp-bar-container {
		height: 1rem;
		background-color: var(--color-slate-300);
		border-radius: 9999px;
		overflow: hidden;
		border: 2px solid var(--color-slate-400);
		box-shadow: 
			inset 0 2px 4px rgba(0, 0, 0, 0.1),
			0 0 10px rgba(251, 191, 36, 0.3);
		position: relative;
	}
	
	.hp-bar-container::before {
		content: '';
		position: absolute;
		top: 0;
		left: -100%;
		width: 100%;
		height: 100%;
		background: linear-gradient(
			90deg,
			transparent,
			rgba(255, 255, 255, 0.4),
			transparent
		);
		animation: hp-shimmer 2s infinite;
		z-index: 2;
	}
	
	@keyframes hp-shimmer {
		0% { left: -100%; }
		100% { left: 200%; }
	}
	
	.hp-bar-fill {
		transition: all var(--transition-slow) ease-out;
		position: relative;
		box-shadow: inset 0 -2px 8px rgba(0, 0, 0, 0.3);
	}
	
	.hp-bar-fill::after {
		content: '';
		position: absolute;
		top: 0;
		left: 0;
		width: 100%;
		height: 50%;
		background: linear-gradient(180deg, rgba(255, 255, 255, 0.4), transparent);
	}
</style>
