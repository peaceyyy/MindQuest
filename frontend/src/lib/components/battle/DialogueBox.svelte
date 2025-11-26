<script lang="ts">
	let { text } = $props();
</script>

<div class="bg-white border-4 border-gray-800 rounded-lg p-6 shadow-lg min-h-[120px] max-h-[200px] overflow-y-auto relative custom-scrollbar">
    <!-- Retro corner decoration -->
    <div class="absolute top-2 left-2 w-2 h-2 bg-gray-800"></div>
    <div class="absolute top-2 right-2 w-2 h-2 bg-gray-800"></div>
    <div class="absolute bottom-2 left-2 w-2 h-2 bg-gray-800"></div>
    <div class="absolute bottom-2 right-2 w-2 h-2 bg-gray-800"></div>
    
	<p class="text-lg md:text-xl font-medium leading-relaxed font-mono text-gray-800 whitespace-pre-line">
		{text}
	</p>
</div>

<style>
	/* Custom scrollbar styling */
	.custom-scrollbar::-webkit-scrollbar {
		width: 10px;
	}
	
	.custom-scrollbar::-webkit-scrollbar-track {
		background: #f3f4f6;
		border-radius: 4px;
		margin: 8px;
	}
	
	.custom-scrollbar::-webkit-scrollbar-thumb {
		background: #6b7280;
		border-radius: 4px;
		border: 2px solid #f3f4f6;
	}
	
	.custom-scrollbar::-webkit-scrollbar-thumb:hover {
		background: #4b5563;
	}
</style>
