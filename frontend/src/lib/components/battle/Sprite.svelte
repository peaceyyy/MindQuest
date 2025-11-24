<script lang="ts">
	let { src, alt, isEnemy = false } = $props();
</script>

<div class="sprite-container">
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
