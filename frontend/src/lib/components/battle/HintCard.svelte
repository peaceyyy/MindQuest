<script lang="ts">
	interface Props {
		hints: number;
		maxHints: number;
		onUseHint: () => void;
		disabled?: boolean;
	}
	
	let { hints = $bindable(0), maxHints = $bindable(0), onUseHint, disabled = false }: Props = $props();
</script>

<div class="bg-gradient-to-br backdrop-blur-sm border-2 border-indigo-500 rounded-xl p-4 shadow-lg" style="background: linear-gradient(to bottom right, rgba(49, 46, 129, 0.4), rgba(88, 28, 135, 0.4)); border-color: rgba(99, 102, 241, 0.5);">
	<!-- Header -->
	<div class="flex items-center justify-between mb-3">
		<div class="flex items-center gap-2">
			<span class="text-2xl">💡</span>
			<h3 class="text-indigo-300 font-bold text-sm uppercase tracking-wide">Hints</h3>
		</div>
		<div class="text-white font-bold text-lg">
			{hints}/{maxHints}
		</div>
	</div>
	
	<!-- Hint Orbs Display -->
	<div class="flex gap-2 mb-3 justify-center">
		{#each Array(maxHints) as _, i}
			<div 
				class="w-8 h-8 rounded-full transition-all duration-300"
				class:bg-gradient-to-br={i < hints}
				class:from-yellow-400={i < hints}
				class:to-amber-500={i < hints}
				class:shadow-lg={i < hints}
				class:animate-pulse={i < hints}
				class:border={i >= hints}
				class:border-gray-600={i >= hints}
				style={i < hints ? 'box-shadow: 0 10px 15px -3px rgba(234, 179, 8, 0.5);' : i >= hints ? 'background-color: rgba(55, 65, 81, 0.5);' : ''}
			>
				{#if i < hints}
					<div class="w-full h-full flex items-center justify-center text-white text-xs font-bold">
						💡
					</div>
				{/if}
			</div>
		{/each}
	</div>
	
	<!-- Use Hint Button -->
	<button
		class="w-full py-2.5 rounded-lg font-bold text-sm transition-all duration-200 transform"
		class:bg-gradient-to-r={hints > 0 && !disabled}
		class:from-yellow-500={hints > 0 && !disabled}
		class:to-amber-600={hints > 0 && !disabled}
		class:text-white={hints > 0 && !disabled}
		class:hover:scale-105={hints > 0 && !disabled}
		class:hover:shadow-lg={hints > 0 && !disabled}
		class:active:scale-95={hints > 0 && !disabled}
		class:text-gray-500={hints === 0 || disabled}
		class:cursor-not-allowed={hints === 0 || disabled}
		style={(hints > 0 && !disabled) ? 'box-shadow: 0 10px 15px -3px rgba(234, 179, 8, 0.5);' : (hints === 0 || disabled) ? 'background-color: rgba(55, 65, 81, 0.5);' : ''}
		disabled={hints === 0 || disabled}
		onclick={onUseHint}
	>
		{#if hints > 0 && !disabled}
			✨ Use Hint
		{:else if disabled}
			⏳ Answer first
		{:else}
			❌ No hints left
		{/if}
	</button>
	
	<!-- Helper Text -->
	<p class="text-center text-xs mt-2" style="color: rgba(165, 180, 252, 0.7);">
		{#if hints > 0}
			Eliminates one wrong answer
		{:else}
			Out of hints for this round
		{/if}
	</p>
</div>
