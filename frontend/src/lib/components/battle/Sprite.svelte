<script lang="ts">
	let { src, alt, isEnemy = false } = $props();
</script>

<div class="relative w-32 h-32 md:w-48 md:h-48 flex items-center justify-center">
	<!-- Shadow -->
	<div class="absolute bottom-0 w-24 h-4 bg-black/20 rounded-[50%] blur-sm"></div>
	
	<!-- Sprite Image -->
	<img 
		{src} 
		{alt}
		class="w-full h-full object-contain pixelated {isEnemy ? 'animate-float-slow' : 'animate-bounce-subtle'}"
		style="image-rendering: pixelated;"
	/>
</div>

<style>
	.pixelated {
		image-rendering: pixelated;
	}
	
	@keyframes float {
		0%, 100% { transform: translateY(0); }
		50% { transform: translateY(-10px); }
	}
	
	.animate-float-slow {
		animation: float 3s ease-in-out infinite;
	}
    
    @keyframes bounce-subtle {
        0%, 100% { transform: translateY(0); }
        50% { transform: translateY(-4px); }
    }
    
    .animate-bounce-subtle {
        animation: bounce-subtle 2s ease-in-out infinite;
    }
</style>
