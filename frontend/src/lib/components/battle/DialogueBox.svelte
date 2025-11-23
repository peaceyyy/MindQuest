<script lang="ts">
	let { text } = $props();
</script>

<div class="bg-white border-4 border-gray-800 rounded-lg p-6 shadow-lg min-h-[120px] relative">
    <!-- Retro corner decoration -->
    <div class="absolute top-2 left-2 w-2 h-2 bg-gray-800"></div>
    <div class="absolute top-2 right-2 w-2 h-2 bg-gray-800"></div>
    <div class="absolute bottom-2 left-2 w-2 h-2 bg-gray-800"></div>
    <div class="absolute bottom-2 right-2 w-2 h-2 bg-gray-800"></div>
    
	<p class="text-lg md:text-xl font-medium leading-relaxed font-mono text-gray-800">
		{text}
	</p>
</div>
