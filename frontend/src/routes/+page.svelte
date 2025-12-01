<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import CustomQuestionsModal from '$lib/components/CustomQuestionsModal.svelte';
	import { sounds, bgm } from '$lib/audio/SoundManager';
	
	let topic = $state('ai');
	let difficulty = $state('easy');
	let globalPoints = $state(0);
	let loading = $state(false);
	let showCustomModal = $state(false);
	let devMode = $state(import.meta.env.DEV); // Only show dev tools in dev mode
	let selectedCustomTopic = $state<string | null>(null); // Track selected custom topic
	let pendingGeminiQuestions = $state<any[] | null>(null); // Store Gemini questions until game starts
	
	// Quick test files available in questions folder
	const testFiles = [
		{ name: 'philosophy.csv', type: 'csv' },
		{ name: 'philosophy.xlsx', type: 'xlsx' }
	];
	
	onMount(async () => {
		await loadGlobalPoints();
		// Start main menu BGM
		bgm.play('main_menu');
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
		playSelect();
		console.log('[Home] startGame - pendingGeminiQuestions:', pendingGeminiQuestions?.length);
		
		// If inline questions are pending (from Gemini or saved sets), store them in sessionStorage for the play page
		if (pendingGeminiQuestions && pendingGeminiQuestions.length > 0) {
			sessionStorage.setItem('mindquest:inlineQuestions', JSON.stringify(pendingGeminiQuestions));
			console.log('[Home] Stored questions in sessionStorage, navigating to play');
			// Use special marker for inline questions
			goto(`/play?topic=${encodeURIComponent(selectedCustomTopic || 'Custom')}&difficulty=${difficulty}&source=inline`);
			return;
		}
		
		// If custom topic is selected, use the actual custom topic name
		const actualTopic = topic === 'custom' && selectedCustomTopic ? selectedCustomTopic : topic;
		console.log('[Home] No inline questions, navigating with topic:', actualTopic);
		goto(`/play?topic=${actualTopic}&difficulty=${difficulty}`);
	}

	function handleCustomTopicSelect(topicName: string, source?: 'file' | 'gemini' | 'saved', geminiQuestions?: any[]) {
		playSelect();
		// User selected a custom topic from the modal
		selectedCustomTopic = topicName;
		topic = 'custom'; // Mark that custom is selected
		
		console.log('[Home] handleCustomTopicSelect:', { topicName, source, questionCount: geminiQuestions?.length });
		
		// Both 'gemini' and 'saved' sources pass inline questions
		if ((source === 'gemini' || source === 'saved') && geminiQuestions && geminiQuestions.length > 0) {
			// Store questions for when game starts
			pendingGeminiQuestions = geminiQuestions;
			console.log('[Home] Stored', geminiQuestions.length, 'pending questions');
		} else {
			pendingGeminiQuestions = null;
			console.log('[Home] No pending questions (source:', source, ')');
		}
		
		showCustomModal = false;
	}

	async function quickLoadTestFile(filename: string) {
		playSelect();
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
		selectedCustomTopic = customTopic;
		topic = 'custom';
		alert(`Loaded ${data.questionsLoaded ?? data.questionsLoaded} questions from ${filename}`);
		} catch (err: any) {
			alert(`Error: ${err.message}`);
		} finally {
			loading = false;
		}
	}
	
	// Map topics to display info (static topics)
	const staticTopics = {
		ai: { name: 'ARTIFICIAL INTELLIGENCE', desc: 'Battle the Neural Network Beast' },
		cs: { name: 'COMPUTER SCIENCE', desc: 'Face the Binary Code Phantom' },
		philosophy: { name: 'PHILOSOPHY', desc: 'Challenge the Ancient Thinker' }
	};

	function playSelect() {
		sounds.play('select');
	}
	
	// Reactive topicInfo that includes custom topic with dynamic description
	let topicInfo = $derived({
		...staticTopics,
		custom: { 
			name: 'CUSTOM QUESTIONS', 
			desc: selectedCustomTopic || 'Select your custom question set', 
			color: 'orange' 
		}
	});
</script>

<div class="h-screen bg-gradient-to-b from-indigo-950 via-purple-900 to-slate-900 text-white flex items-center justify-center p-4 overflow-hidden">
	<!-- Centered Wrapper -->
	<div class="w-full max-w-4xl flex flex-col gap-4">
		<!-- Header: Title centered above the card -->
		<div class="text-center shrink-0">
			<h1 class="text-4xl md:text-5xl lg:text-6xl font-black tracking-wider pixel-text animate-pulse-slow" style="text-shadow: 4px 4px 0px rgba(0,0,0,0.5), -2px -2px 0px rgba(255,255,255,0.1);">
				MINDQUEST
			</h1>
			<p class="text-sm md:text-base text-purple-200 font-mono tracking-wide mt-1">
				⚔️ Battle Ignorance Through Knowledge ⚔️
			</p>
		</div>

		<!-- Main Content: Battle Config -->
		<div class="bg-black/50 border-4 border-white/30 rounded-xl p-4 md:p-6 shadow-2xl backdrop-blur-sm w-full">
			<!-- Two-column layout on larger screens -->
			<div class="grid grid-cols-1 lg:grid-cols-2 gap-4 lg:gap-6">
				<!-- Left: Topic Selection -->
				<div>
					<div class="block text-sm font-bold tracking-widest text-purple-200 mb-2 uppercase">Select Enemy</div>
					<div class="grid grid-cols-1 gap-2">
						{#each Object.entries(topicInfo) as [key, info]}
							<button
								class="text-left p-3 border-4 rounded-lg transition-all transform hover:scale-[1.02] active:scale-95 {topic === key ? 'border-cyan-400 bg-cyan-400/20 shadow-lg shadow-cyan-400/50' : 'border-gray-600 bg-gray-800/50 hover:border-gray-400'}"
								onclick={() => {
									playSelect();
									if (key === 'custom') {
										showCustomModal = true;
									} else {
										topic = key;
									}
								}}
							>
								<div class="font-bold text-base tracking-wide">{info.name}</div>
								<div class="text-xs text-gray-300 mt-0.5">{info.desc}</div>
							</button>
						{/each}
					</div>
				</div>

				<!-- Right: Difficulty + Start Button -->
				<div class="flex flex-col gap-4">
					<!-- Difficulty Selection -->
					<div>
						<div class="block text-sm font-bold tracking-widest text-purple-200 mb-2 uppercase">Battle Difficulty</div>
						<div class="grid grid-cols-3 gap-2">
							<button
								class="py-2 px-3 border-4 rounded-lg font-bold uppercase text-sm transition-all transform hover:scale-105 active:scale-95 {difficulty === 'easy' ? 'border-green-400 bg-green-400/20 text-green-300 shadow-lg shadow-green-400/50' : 'border-gray-600 bg-gray-800/50 text-gray-300 hover:border-gray-400'}"
								onclick={() => { playSelect(); difficulty = 'easy'; }}
							>
								Easy
							</button>
							<button
								class="py-2 px-3 border-4 rounded-lg font-bold uppercase text-sm transition-all transform hover:scale-105 active:scale-95 {difficulty === 'medium' ? 'border-yellow-400 bg-yellow-400/20 text-yellow-300 shadow-lg shadow-yellow-400/50' : 'border-gray-600 bg-gray-800/50 text-gray-300 hover:border-gray-400'}"
								onclick={() => { playSelect(); difficulty = 'medium'; }}
							>
								Medium
							</button>
							<button
								class="py-2 px-3 border-4 rounded-lg font-bold uppercase text-sm transition-all transform hover:scale-105 active:scale-95 {difficulty === 'hard' ? 'border-red-400 bg-red-400/20 text-red-300 shadow-lg shadow-red-400/50' : 'border-gray-600 bg-gray-800/50 text-gray-300 hover:border-gray-400'}"
								onclick={() => { playSelect(); difficulty = 'hard'; }}
							>
								Hard
							</button>
						</div>
					</div>

					<!-- Career points component (flex, modular) -->
					<div class="flex items-center justify-center">
						<div class="flex flex-col items-center justify-center w-40 h-20 bg-black/40 border-4 border-yellow-400 rounded-lg px-4 py-2 shadow-lg shadow-yellow-400/50">
							<div class="text-xs text-yellow-300 font-bold tracking-widest">CAREER POINTS</div>
							<div class="text-2xl font-black text-yellow-400 font-mono">{globalPoints}</div>
						</div>
					</div>

					<!-- Start Button -->
					<button
						onclick={startGame}
						disabled={loading}
						class="w-full py-4 bg-gradient-to-r from-purple-600 to-pink-600 border-4 border-white/50 rounded-xl font-black text-lg uppercase tracking-widest shadow-lg hover:shadow-2xl hover:from-purple-500 hover:to-pink-500 transition-all transform hover:scale-105 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed mt-auto"
						style="text-shadow: 2px 2px 4px rgba(0,0,0,0.8);"
					>
						⚔️ Begin Battle ⚔️
					</button>

					<!-- Dev Quick Test Buttons -->
					{#if devMode}
						<div class="p-3 border-2 border-dashed border-orange-500/50 rounded-lg bg-orange-500/10">
							<div class="text-xs text-orange-400 font-bold uppercase tracking-widest mb-2">Dev Quick Test</div>
							<div class="flex flex-wrap gap-2">
								{#each testFiles as file}
									<button
										onclick={() => quickLoadTestFile(file.name)}
										disabled={loading}
										class="px-3 py-1.5 bg-orange-600 hover:bg-orange-500 text-white text-xs font-bold rounded transition-all disabled:opacity-50"
									>
										{file.name}
									</button>
								{/each}
							</div>
						</div>
					{/if}
				</div>
			</div>
		</div>

		<!-- Footer Hint (compact) -->
		<div class="text-center text-xs text-purple-300/60 font-mono shrink-0">
			<p>Answer questions correctly to defeat enemies and earn points!</p>
		</div>
	</div>
</div>

{#if showCustomModal}
	<CustomQuestionsModal 
		onclose={() => showCustomModal = false}
		ontopicselect={handleCustomTopicSelect}
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
