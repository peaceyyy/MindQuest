<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	
	let topic = $state('ai');
	let difficulty = $state('easy');
	let globalPoints = $state(0);
	let loading = $state(false);
	
	onMount(async () => {
		await loadGlobalPoints();
	});
	
	async function loadGlobalPoints() {
		try {
			const persisted = localStorage.getItem('mindquest:careerPoints');
			if (persisted) {
				globalPoints = parseInt(persisted) || 0;
				return;
			}
			const lastSessionId = localStorage.getItem('mindquest:lastSessionId');
			if (lastSessionId) {
				const stateRes = await fetch(`/api/sessions/${lastSessionId}/state`);
				if (stateRes.ok) {
					const state = await stateRes.json();
					globalPoints = state.globalPoints || 0;
					return;
				}
			}
			globalPoints = 0;
		} catch (err) {
			console.log('Could not load global points:', err);
			globalPoints = 0;
		}
	}
	
	function startGame() {
		goto(`/play?topic=${topic}&difficulty=${difficulty}`);
	}
	
	// Map topics to display info
	const topicInfo: Record<string, {name: string, desc: string}> = {
		ai: { name: 'ARTIFICIAL INTELLIGENCE', desc: 'Battle the Neural Network Beast' },
		cs: { name: 'COMPUTER SCIENCE', desc: 'Face the Binary Code Phantom' },
		philosophy: { name: 'PHILOSOPHY', desc: 'Challenge the Ancient Thinker' }
	};
</script>

<div class="min-h-screen bg-gradient-to-b from-indigo-950 via-purple-900 to-slate-900 text-white flex items-center justify-center p-4">
	<div class="max-w-2xl w-full">
		<!-- Title with Pixel Art Feel -->
		<div class="text-center mb-12">
			<h1 class="text-6xl md:text-7xl font-black tracking-wider mb-4 pixel-text animate-pulse-slow" style="text-shadow: 4px 4px 0px rgba(0,0,0,0.5), -2px -2px 0px rgba(255,255,255,0.1);">
				MINDQUEST
			</h1>
			<p class="text-lg md:text-xl text-purple-200 font-mono tracking-wide">
				⚔️ Battle Ignorance Through Knowledge ⚔️
			</p>
		</div>

		<!-- Stats Display -->
		<div class="mb-8 text-center">
			<div class="inline-block bg-black/40 border-4 border-yellow-400 rounded-lg px-8 py-4 shadow-lg shadow-yellow-400/50">
				<div class="text-xs text-yellow-300 font-bold tracking-widest mb-1">CAREER POINTS</div>
				<div class="text-4xl font-black text-yellow-400 font-mono">{globalPoints}</div>
			</div>
		</div>

		<!-- Battle Config Card -->
		<div class="bg-black/50 border-4 border-white/30 rounded-xl p-8 shadow-2xl backdrop-blur-sm">
			<!-- Topic Selection -->
			<div class="mb-6">
				<div class="block text-sm font-bold tracking-widest text-purple-200 mb-3 uppercase">Select Enemy</div>
				<div class="grid grid-cols-1 gap-3">
					{#each Object.entries(topicInfo) as [key, info]}
						<button
							class="text-left p-4 border-4 rounded-lg transition-all transform hover:scale-105 active:scale-95 {topic === key ? 'border-cyan-400 bg-cyan-400/20 shadow-lg shadow-cyan-400/50' : 'border-gray-600 bg-gray-800/50 hover:border-gray-400'}"
							onclick={() => topic = key}
						>
							<div class="font-bold text-lg tracking-wide">{info.name}</div>
							<div class="text-sm text-gray-300 mt-1">{info.desc}</div>
						</button>
					{/each}
				</div>
			</div>

			<!-- Difficulty Selection -->
			<div class="mb-8">
				<div class="block text-sm font-bold tracking-widest text-purple-200 mb-3 uppercase">Battle Difficulty</div>
				<div class="grid grid-cols-3 gap-3">
					<button
						class="py-3 px-4 border-4 rounded-lg font-bold uppercase text-sm transition-all transform hover:scale-105 active:scale-95 {difficulty === 'easy' ? 'border-green-400 bg-green-400/20 text-green-300 shadow-lg shadow-green-400/50' : 'border-gray-600 bg-gray-800/50 text-gray-300 hover:border-gray-400'}"
						onclick={() => difficulty = 'easy'}
					>
						Easy
					</button>
					<button
						class="py-3 px-4 border-4 rounded-lg font-bold uppercase text-sm transition-all transform hover:scale-105 active:scale-95 {difficulty === 'medium' ? 'border-yellow-400 bg-yellow-400/20 text-yellow-300 shadow-lg shadow-yellow-400/50' : 'border-gray-600 bg-gray-800/50 text-gray-300 hover:border-gray-400'}"
						onclick={() => difficulty = 'medium'}
					>
						Medium
					</button>
					<button
						class="py-3 px-4 border-4 rounded-lg font-bold uppercase text-sm transition-all transform hover:scale-105 active:scale-95 {difficulty === 'hard' ? 'border-red-400 bg-red-400/20 text-red-300 shadow-lg shadow-red-400/50' : 'border-gray-600 bg-gray-800/50 text-gray-300 hover:border-gray-400'}"
						onclick={() => difficulty = 'hard'}
					>
						Hard
					</button>
				</div>
			</div>

			<!-- Start Button -->
			<button
				onclick={startGame}
				disabled={loading}
				class="w-full py-5 bg-gradient-to-r from-purple-600 to-pink-600 border-4 border-white/50 rounded-xl font-black text-xl uppercase tracking-widest shadow-lg hover:shadow-2xl hover:from-purple-500 hover:to-pink-500 transition-all transform hover:scale-105 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
				style="text-shadow: 2px 2px 4px rgba(0,0,0,0.8);"
			>
				⚔️ Begin Battle ⚔️
			</button>
		</div>

		<!-- Footer Hint -->
		<div class="mt-8 text-center text-sm text-purple-300/60 font-mono">
			<p>Answer questions correctly to defeat enemies and earn points!</p>
		</div>
	</div>
</div>

<style>
	.pixel-text {
		font-family: 'Impact', 'Arial Black', sans-serif;
		letter-spacing: 0.05em;
	}
	
	@keyframes pulse-slow {
		0%, 100% { opacity: 1; }
		50% { opacity: 0.8; }
	}
	
	.animate-pulse-slow {
		animation: pulse-slow 3s ease-in-out infinite;
	}
</style>
