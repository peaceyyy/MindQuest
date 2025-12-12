<script lang="ts">
	interface Props {
		src: string;
		alt: string;
		isEnemy?: boolean;
		spriteRef?: HTMLDivElement | null;
	}
	
	let { src, alt, isEnemy = false, spriteRef = $bindable(null) }: Props = $props();
</script>

<div class="sprite-container" bind:this={spriteRef}>
	<!-- Shadow -->
	<div class="sprite-shadow"></div>
	
	<!-- Sprite Image -->
	<img 
		{src} 
		{alt}
		class="sprite-image {isEnemy ? 'animate-float-slow' : 'animate-bounce-subtle'}"
	/>
</div>

<style>
	.sprite-container {
		position: relative;
		width: 8rem; /* 128px */
		height: 8rem;
		display: flex;
		align-items: center;
		justify-content: center;
		/* Promote to own layer for smoother compositing */
		transform: translateZ(0);
		backface-visibility: hidden;
		perspective: 1000px;
	}
	
	/* Energy aura effect */
	.sprite-container::before {
		content: '';
		position: absolute;
		width: 120%;
		height: 120%;
		background: radial-gradient(
			circle,
			rgba(59, 130, 246, 0.3) 0%,
			rgba(59, 130, 246, 0.1) 40%,
			transparent 70%
		);
		border-radius: 50%;
		animation: aura-pulse 2s ease-in-out infinite;
		filter: blur(8px);
		z-index: -1;
	}
	
	@keyframes aura-pulse {
		0%, 100% {
			opacity: 0.4;
			transform: scale(0.9);
		}
		50% {
			opacity: 0.8;
			transform: scale(1.1);
		}
	}
	
	@media (min-width: 768px) {
		.sprite-container {
			width: 12rem; /* 192px */
			height: 12rem;
		}
	}
	
	.sprite-shadow {
		position: absolute;
		bottom: 0;
		width: 6rem;
		height: 1rem;
		background-color: rgba(0, 0, 0, 0.2);
		border-radius: 50%;
		filter: blur(4px);
		/* Keep shadow on its own layer */
		transform: translateZ(0);
	}
	
	.sprite-image {
		width: 100%;
		height: 100%;
		object-fit: contain;
		object-position: center;
		image-rendering: pixelated;
		/* Critical for smooth animation */
		transform: translateZ(0);
		backface-visibility: hidden;
		filter: drop-shadow(0 0 15px rgba(96, 165, 250, 0.5));
		animation: sprite-glow 3s ease-in-out infinite;
	}
	
	@keyframes sprite-glow {
		0%, 100% {
			filter: drop-shadow(0 0 10px rgba(96, 165, 250, 0.4));
		}
		50% {
			filter: drop-shadow(0 0 25px rgba(96, 165, 250, 0.8));
		}
	}
	
	/* Optimized keyframes - only transform property */
	@keyframes float {
		0%, 100% { 
			transform: translate3d(0, 0, 0); 
		}
		50% { 
			transform: translate3d(0, -10px, 0); 
		}
	}

	.animate-float-slow {
		animation: float 3s ease-in-out infinite;
		will-change: transform;
		/* Additional GPU optimization */
		transform: translate3d(0, 0, 0);
	}
    
	@keyframes bounce-subtle {
		0%, 100% { 
			transform: translate3d(0, 0, 0); 
		}
		50% { 
			transform: translate3d(0, -4px, 0); 
		}
	}
    
	.animate-bounce-subtle {
		animation: bounce-subtle 2s ease-in-out infinite;
		will-change: transform;
		/* Additional GPU optimization */
		transform: translate3d(0, 0, 0);
	}
</style>
