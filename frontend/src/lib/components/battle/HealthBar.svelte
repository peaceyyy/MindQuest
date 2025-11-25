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
	<div class="flex justify-between text-xs font-bold mb-1 uppercase tracking-wider text-gray-600">
		<span>{label}</span>
		<span>{current}/{max}</span>
	</div>
	<div class="h-4 bg-gray-200 rounded-full overflow-hidden border-2 border-gray-300 shadow-inner">
		<div 
			class="h-full {dynamicColor()} transition-all duration-500 ease-out"
			style="width: {percentage}%"
		></div>
	</div>
</div>
