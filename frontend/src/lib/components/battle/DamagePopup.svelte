<script lang="ts">
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

	let popupEl: HTMLDivElement | undefined = $state();
	let visible = $state(true);

	$effect(() => {
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
		font-family: var(--font-pixel);
		font-size: 2rem;
		font-weight: bold;
		color: var(--color-danger);
		text-shadow: 
			3px 3px 0 #000,
			-2px -2px 0 #000,
			2px -2px 0 #000,
			-2px 2px 0 #000,
			0 0 20px rgba(239, 68, 68, 0.8);
		z-index: 100;
		white-space: nowrap;
		filter: drop-shadow(0 0 10px rgba(239, 68, 68, 0.6));
		animation: damage-impact 0.1s ease-out;
	}

	@keyframes damage-impact {
		0% { transform: scale(0.5); opacity: 0; }
		50% { transform: scale(1.3); }
		100% { transform: scale(1); opacity: 1; }
	}

	.damage-popup.crit {
		color: #fbbf24;
		font-size: 3rem;
		text-shadow: 
			4px 4px 0 #000,
			-3px -3px 0 #000,
			3px -3px 0 #000,
			-3px 3px 0 #000,
			0 0 30px rgba(251, 191, 36, 1),
			0 0 60px rgba(245, 158, 11, 0.8);
		filter: drop-shadow(0 0 20px rgba(251, 191, 36, 1));
		animation: crit-explosion 0.15s cubic-bezier(0.34, 1.56, 0.64, 1);
	}

	@keyframes crit-explosion {
		0% { 
			transform: scale(0.3) rotate(-5deg); 
			opacity: 0;
		}
		50% { 
			transform: scale(1.5) rotate(3deg);
		}
		100% { 
			transform: scale(1) rotate(0deg); 
			opacity: 1;
		}
	}

	.damage-popup.heal {
		color: var(--color-success);
		text-shadow: 
			3px 3px 0 #000,
			-2px -2px 0 #000,
			2px -2px 0 #000,
			-2px 2px 0 #000,
			0 0 20px rgba(34, 197, 94, 0.8);
	}

	.crit-label {
		display: block;
		font-size: 1rem;
		color: #fef08a;
		text-align: center;
		margin-top: 4px;
		animation: crit-label-flash 0.2s ease-out 0.1s;
		text-shadow: 
			2px 2px 0 #000,
			0 0 15px rgba(254, 240, 138, 1);
	}

	@keyframes crit-label-flash {
		0%, 100% { opacity: 1; transform: scale(1); }
		50% { opacity: 0.5; transform: scale(1.2); }
	}
</style>
