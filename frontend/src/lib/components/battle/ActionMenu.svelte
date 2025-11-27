<script lang="ts">
	let { choices, onSelect, disabled = false, eliminatedChoices = [] }: {
		choices: string[];
		onSelect: (index: number) => void;
		disabled?: boolean;
		eliminatedChoices?: number[];
	} = $props();
</script>

<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
	{#each choices as choice, index}
		{@const isEliminated = eliminatedChoices.includes(index)}
		<button
			class="group relative p-4 border-4 rounded-lg text-left transition-all disabled:cursor-not-allowed active:translate-y-1"
			class:bg-white={!isEliminated}
			class:border-gray-300={!isEliminated}
			class:hover:border-blue-500={!isEliminated && !disabled}
			class:hover:bg-blue-50={!isEliminated && !disabled}
			class:border-gray-400={isEliminated}
			class:opacity-40={isEliminated}
			class:opacity-50={disabled && !isEliminated}
			style={isEliminated ? 'background-color: rgba(229, 231, 235, 0.5);' : ''}
			onclick={() => onSelect(index)}
			disabled={disabled || isEliminated}
		>
            <!-- Selection Indicator -->
            {#if !isEliminated}
				<div class="absolute left-2 top-1/2 -translate-y-1/2 w-0 h-0 border-t-[6px] border-t-transparent border-l-[10px] border-l-gray-800 border-b-[6px] border-b-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
			{/if}
            
			<span 
				class="pl-6 block font-bold transition-colors"
				class:text-gray-700={!isEliminated}
				class:group-hover:text-blue-700={!isEliminated && !disabled}
				class:text-gray-500={isEliminated}
				class:line-through={isEliminated}
			>
				{#if isEliminated}
					<span class="text-red-500 mr-2">❌</span>
				{/if}
				{choice}
			</span>
		</button>
	{/each}
</div>
