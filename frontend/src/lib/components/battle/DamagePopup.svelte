<script lang="ts">
	import { onMount } from 'svelte';
	import gsap from 'gsap';

	interface Props {
		damage: number;
		x?: number;
		y?: number;
		isCrit?: boolean;
		isHeal?: boolean;
		onComplete?: () => void;
	}

	let {
		damage,
		x = 0,
		y = 0,
		isCrit = false,
		isHeal = false,
		onComplete
	}: Props = $props();

	let popupEl: HTMLDivElement;
	let visible = $state(true);

	onMount(() => {
		if (!popupEl) return;

		// Animate the damage number floating up and fading
		gsap.fromTo(
			popupEl,
			{
				y: 0,
				opacity: 1,
				scale: isCrit ? 1.5 : 1
			},
			{
				y: -60,
				opacity: 0,
				scale: isCrit ? 1.8 : 1.2,
				duration: 1,
				ease: 'power2.out',
				onComplete: () => {
					visible = false;
					onComplete?.();
				}
			}
		);
	});
</script>

{#if visible}
	<div
		bind:this={popupEl}
		class="damage-popup"
		class:crit={isCrit}
		class:heal={isHeal}
		style="left: {x}px; top: {y}px;"
	>
		{#if isHeal}
			+{damage}
		{:else}
			-{damage}
		{/if}
		{#if isCrit}
			<span class="crit-label">CRIT!</span>
		{/if}
	</div>
{/if}

<style>
	.damage-popup {
		position: absolute;
		pointer-events: none;
		font-family: 'Press Start 2P', 'Courier New', monospace;
		font-size: 1.5rem;
		font-weight: bold;
		color: #ef4444;
		text-shadow: 
			2px 2px 0 #000,
			-1px -1px 0 #000,
			1px -1px 0 #000,
			-1px 1px 0 #000;
		z-index: 100;
		white-space: nowrap;
	}

	.damage-popup.crit {
		color: #f59e0b;
		font-size: 2rem;
	}

	.damage-popup.heal {
		color: #22c55e;
	}

	.crit-label {
		display: block;
		font-size: 0.75rem;
		color: #fbbf24;
		text-align: center;
		margin-top: 2px;
	}
</style>
