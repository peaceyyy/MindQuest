<script lang="ts">
	let { choices, onSelect, disabled = false } = $props();
</script>

<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
	{#each choices as choice, index}
		<button
			class="group relative p-4 bg-white border-4 border-gray-300 rounded-lg text-left hover:border-blue-500 hover:bg-blue-50 transition-all disabled:opacity-50 disabled:cursor-not-allowed active:translate-y-1"
			onclick={() => onSelect(index)}
			{disabled}
		>
            <!-- Selection Indicator -->
            <div class="absolute left-2 top-1/2 -translate-y-1/2 w-0 h-0 border-t-[6px] border-t-transparent border-l-[10px] border-l-gray-800 border-b-[6px] border-b-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
            
			<span class="pl-6 block font-bold text-gray-700 group-hover:text-blue-700">
				{choice}
			</span>
		</button>
	{/each}
</div>
