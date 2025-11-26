<script lang="ts">
	interface Question {
		correct: boolean;
		questionText: string;
		choices: string[];
		correctIndex: number;
		userAnswerIndex: number;
	}
	
	interface Props {
		questions: Question[];
		onClose: () => void;
	}
	
	let { questions, onClose }: Props = $props();
	
	let currentIndex = $state(0);
	let currentQuestion = $derived(questions[currentIndex]);
	
	function nextQuestion() {
		if (currentIndex < questions.length - 1) {
			currentIndex++;
		}
	}
	
	function prevQuestion() {
		if (currentIndex > 0) {
			currentIndex--;
		}
	}
	
	// Get letter for choice index (A, B, C, D)
	function getChoiceLetter(index: number): string {
		return ['A', 'B', 'C', 'D'][index] || '?';
	}
</script>

<!-- Modal Overlay -->
<div class="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 p-4" onclick={onClose}>
	<!-- Modal Content -->
	<div class="bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl shadow-2xl max-w-3xl w-full max-h-[90vh] overflow-hidden" onclick={(e) => e.stopPropagation()}>
		<!-- Header -->
		<div class="bg-gradient-to-r from-blue-600 to-purple-600 p-6 flex justify-between items-center">
			<div>
				<h2 class="text-2xl font-bold text-white">📚 Question Review</h2>
				<p class="text-blue-100 text-sm mt-1">Study your answers and learn from mistakes</p>
			</div>
			<button 
				class="text-white hover:bg-white/20 rounded-full p-2 transition-colors"
				onclick={onClose}
				aria-label="Close"
			>
				<svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
				</svg>
			</button>
		</div>
		
		<!-- Question Progress -->
		<div class="px-6 py-3 bg-gray-700/50 flex items-center justify-between border-b border-gray-600">
			<div class="text-white text-sm font-semibold">
				Question {currentIndex + 1} of {questions.length}
			</div>
			<div class="flex gap-1">
				{#each questions as q, i}
					<button
						class="w-8 h-8 rounded-full text-xs font-bold transition-all"
						class:bg-green-500={q.correct}
						class:bg-red-500={!q.correct}
						class:ring-2={i === currentIndex}
						class:ring-white={i === currentIndex}
						class:ring-offset-2={i === currentIndex}
						class:ring-offset-gray-800={i === currentIndex}
						class:opacity-50={i !== currentIndex}
						onclick={() => currentIndex = i}
					>
						{i + 1}
					</button>
				{/each}
			</div>
		</div>
		
		<!-- Question Content -->
		<div class="p-6 overflow-y-auto max-h-[50vh]">
			{#if currentQuestion}
				<!-- Result Badge -->
				<div class="mb-4">
					{#if currentQuestion.correct}
						<div class="inline-flex items-center gap-2 bg-green-500/20 border-2 border-green-500 rounded-lg px-4 py-2">
							<span class="text-2xl">✓</span>
							<span class="text-green-400 font-bold">Correct Answer</span>
						</div>
					{:else}
						<div class="inline-flex items-center gap-2 bg-red-500/20 border-2 border-red-500 rounded-lg px-4 py-2">
							<span class="text-2xl">✗</span>
							<span class="text-red-400 font-bold">Incorrect Answer</span>
						</div>
					{/if}
				</div>
				
				<!-- Question Text -->
				<div class="bg-gray-700/50 rounded-lg p-4 mb-6">
					<p class="text-white text-lg leading-relaxed">{currentQuestion.questionText}</p>
				</div>
				
				<!-- Choices -->
				<div class="space-y-3">
					{#each currentQuestion.choices as choice, i}
						{@const isCorrect = i === currentQuestion.correctIndex}
						{@const isUserAnswer = i === currentQuestion.userAnswerIndex}
						{@const isWrongAnswer = isUserAnswer && !currentQuestion.correct}
						
						<div 
							class="rounded-lg p-4 border-2 transition-all"
							class:border-green-500={isCorrect}
							class:border-red-500={isWrongAnswer}
							class:border-gray-600={!isCorrect && !isUserAnswer}
							style={isCorrect ? 'background-color: rgba(34, 197, 94, 0.2);' : isWrongAnswer ? 'background-color: rgba(239, 68, 68, 0.2);' : 'background-color: rgba(55, 65, 81, 0.3);'}
						>
							<div class="flex items-start gap-3">
								<!-- Choice Letter -->
								<div 
									class="flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm"
									class:bg-green-500={isCorrect}
									class:bg-red-500={isWrongAnswer}
									class:bg-gray-600={!isCorrect && !isUserAnswer}
									class:text-white={isCorrect || isWrongAnswer}
									class:text-gray-300={!isCorrect && !isUserAnswer}
								>
									{getChoiceLetter(i)}
								</div>
								
								<!-- Choice Text -->
								<div class="flex-1">
									<p 
										class="text-base"
										class:text-green-300={isCorrect}
										class:font-semibold={isCorrect}
										class:text-red-300={isWrongAnswer}
										class:text-gray-300={!isCorrect && !isUserAnswer}
									>
										{choice}
									</p>
									
									<!-- Labels -->
									{#if isCorrect}
										<span class="inline-block mt-2 text-xs bg-green-600 text-white px-2 py-1 rounded-full font-semibold">
											✓ Correct Answer
										</span>
									{/if}
									{#if isWrongAnswer}
										<span class="inline-block mt-2 text-xs bg-red-600 text-white px-2 py-1 rounded-full font-semibold">
											Your Answer
										</span>
									{/if}
								</div>
							</div>
						</div>
					{/each}
				</div>
			{/if}
		</div>
		
		<!-- Navigation Footer -->
		<div class="p-6 bg-gray-700/50 border-t border-gray-600 flex justify-between items-center">
			<button
				class="px-6 py-3 bg-gray-600 text-white rounded-lg font-semibold hover:bg-gray-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
				onclick={prevQuestion}
				disabled={currentIndex === 0}
			>
				← Previous
			</button>
			
			<div class="text-gray-400 text-sm">
				Use number buttons to jump to any question
			</div>
			
			<button
				class="px-6 py-3 bg-gray-600 text-white rounded-lg font-semibold hover:bg-gray-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
				onclick={nextQuestion}
				disabled={currentIndex === questions.length - 1}
			>
				Next →
			</button>
		</div>
	</div>
</div>
