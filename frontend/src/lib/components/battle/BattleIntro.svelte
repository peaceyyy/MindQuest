<script lang="ts">
	import { getTopicDisplayName } from '$lib/services/battleService';
	
	interface Props {
		topic: string;
		enemySprite: string;
		backgroundImage: string;
		introPhase: number;
	}
	
	let { topic, enemySprite, backgroundImage, introPhase }: Props = $props();
</script>

<div class="battle-intro" style="background-image: url('{backgroundImage}');">
	<!-- Scan line overlay for retro effect -->
	<div class="intro-scanlines"></div>
	
	<!-- Flash effect on encounter -->
	<div class="intro-flash" class:active={introPhase === 0}></div>
	
	<!-- Phase 0 & 1: Encounter text -->
	{#if introPhase >= 0}
		<div class="intro-text-container" class:fade-out={introPhase >= 2} style="text-align: center;">
			<p class="intro-text intro-text-wild" class:visible={introPhase >= 0}>
				A wild
			</p>
			<h1 class="intro-text intro-text-boss" class:visible={introPhase >= 0}>
				{getTopicDisplayName(topic)} BOSS
			</h1>
			<p class="intro-text intro-text-appeared" class:visible={introPhase >= 0}>
				appeared!
			</p>
		</div>
	{/if}
	

	{#if introPhase >= 1}
		<div class="intro-sprite-container" class:visible={introPhase >= 1}>
			<img 
				src={enemySprite} 
				alt="{topic} Boss" 
				class="intro-enemy-sprite"
				class:bounce={introPhase >= 1}
			/>
		</div>
	{/if}
	

	{#if introPhase >= 2}
		<div class="intro-ready" class:visible={introPhase >= 2}>
			<span class="ready-text">GET READY!</span>
		</div>
	{/if}
</div>
