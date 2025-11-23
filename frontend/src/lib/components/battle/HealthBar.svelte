<script lang="ts">
	let { current = 100, max = 100, label = "HP", color = "bg-green-500" } = $props();
	
	let percentage = $derived(Math.max(0, Math.min(100, (current / max) * 100)));
</script>

<div class="w-full max-w-[200px]">
	<div class="flex justify-between text-xs font-bold mb-1 uppercase tracking-wider text-gray-600">
		<span>{label}</span>
		<span>{current}/{max}</span>
	</div>
	<div class="h-4 bg-gray-200 rounded-full overflow-hidden border-2 border-gray-300 shadow-inner">
		<div 
			class="h-full {color} transition-all duration-500 ease-out"
			style="width: {percentage}%"
		></div>
	</div>
</div>
