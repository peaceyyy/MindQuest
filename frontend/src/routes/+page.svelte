<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import CustomQuestionsModal from '$lib/components/CustomQuestionsModal.svelte';
	
	let topic = $state('ai');
	let difficulty = $state('easy');
	let globalPoints = $state(0);
	let loading = $state(false);
	let showUploadModal = $state(false);
	let devMode = $state(import.meta.env.DEV); // Only show dev tools in dev mode
	
	// Quick test files available in questions folder
	const testFiles = [
		{ name: 'philosophy.csv', type: 'csv' },
		{ name: 'philosophy.xlsx', type: 'xlsx' }
	];
	
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

	function handleUploadSuccess(event: CustomEvent) {
		// Automatically select the uploaded topic
		// We might need to add it to topicInfo if we want it to show up nicely, 
		// or just handle it as a custom topic.
		// For now, let's just alert or log, and maybe set the topic variable.
		// But topicInfo is hardcoded. We might need to allow custom topics in the UI.
		// Since the backend returns the topic name, we can set `topic` to it.
		// But the UI expects `topic` to be a key in `topicInfo`.
		// We can add a dynamic entry to `topicInfo` or just handle it.
		
		const customTopic = event.detail.customTopicName;
		topicInfo[customTopic] = { name: customTopic.toUpperCase(), desc: 'Custom Question Set' };
		topic = customTopic;
		showUploadModal = false;
	}

	async function quickLoadTestFile(filename: string) {
		loading = true;
		try {
			const res = await fetch(`http://localhost:7070/api/test/load-file?filename=${encodeURIComponent(filename)}`, {
				method: 'POST'
			});

			if (!res.ok) {
				// Try to parse JSON error, otherwise fall back to plain text
				let message = `Failed to load ${filename}`;
				try {
					const err = await res.json();
					message = err.message || JSON.stringify(err);
				} catch (e) {
					try {
						const text = await res.text();
						message = text;
					} catch (e2) {
						// ignore
					}
				}
				alert(message);
				return;
			}

			// Parse JSON safely; if parsing fails, show text
			let data: any = null;
			try {
				data = await res.json();
			} catch (e) {
				const text = await res.text();
				alert(`Loaded (unexpected response): ${text}`);
				return;
			}

			const customTopic = data.topicName || data.customTopicName || filename.replace(/\..+$/, '');
			topicInfo[customTopic] = { name: customTopic.toUpperCase(), desc: `Test file: ${filename}` };
			topic = customTopic;
			alert(`Loaded ${data.questionsLoaded ?? data.questionsLoaded} questions from ${filename}`);
		} catch (err: any) {
			alert(`Error: ${err.message}`);
		} finally {
			loading = false;
		}
	}
	
	// Map topics to display info
	let topicInfo: Record<string, {name: string, desc: string}> = $state({
		ai: { name: 'ARTIFICIAL INTELLIGENCE', desc: 'Battle the Neural Network Beast' },
		cs: { name: 'COMPUTER SCIENCE', desc: 'Face the Binary Code Phantom' },
		philosophy: { name: 'PHILOSOPHY', desc: 'Challenge the Ancient Thinker' }
	});
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
			
			<!-- Upload Button -->
			<button
				onclick={() => showUploadModal = true}
				class="w-full mt-4 py-3 bg-gray-700 border-2 border-gray-500 rounded-lg font-bold text-sm uppercase tracking-widest hover:bg-gray-600 transition-all"
			>
				📂 Upload Custom Questions
			</button>

			<!-- Dev Quick Test Buttons -->
			{#if devMode}
				<div class="mt-6 p-4 border-2 border-dashed border-orange-500/50 rounded-lg bg-orange-500/10">
					<div class="text-xs text-orange-400 font-bold uppercase tracking-widest mb-3">Dev Quick Test</div>
					<div class="flex flex-wrap gap-2">
						{#each testFiles as file}
							<button
								onclick={() => quickLoadTestFile(file.name)}
								disabled={loading}
								class="px-3 py-2 bg-orange-600 hover:bg-orange-500 text-white text-xs font-bold rounded transition-all disabled:opacity-50"
							>
								{file.name}
							</button>
						{/each}
					</div>
				</div>
			{/if}
		</div>

		<!-- Footer Hint -->
		<div class="mt-8 text-center text-sm text-purple-300/60 font-mono">
			<p>Answer questions correctly to defeat enemies and earn points!</p>
		</div>
	</div>
</div>

{#if showUploadModal}
	<CustomQuestionsModal 
		on:close={() => showUploadModal = false}
		on:uploadSuccess={handleUploadSuccess}
	/>
{/if}

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
