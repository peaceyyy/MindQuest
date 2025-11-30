<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	
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
	let portalContainer: HTMLDivElement | null = null;
	
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
	
	function getChoiceLetter(index: number): string {
		return ['A', 'B', 'C', 'D'][index] || '?';
	}
	
	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Escape') onClose();
		if (e.key === 'ArrowLeft') prevQuestion();
		if (e.key === 'ArrowRight') nextQuestion();
	}
	
	// Portal pattern to escape transform context
	onMount(() => {
		portalContainer = document.createElement('div');
		portalContainer.id = 'review-modal-portal';
		document.body.appendChild(portalContainer);
		window.addEventListener('keydown', handleKeydown);
	});
	
	onDestroy(() => {
		if (portalContainer && document.body.contains(portalContainer)) {
			document.body.removeChild(portalContainer);
		}
		window.removeEventListener('keydown', handleKeydown);
	});
	
	// Render modal content into portal
	$effect(() => {
		if (!portalContainer) return;
		
		const isCorrect = currentQuestion?.correct;
		const isWrongAnswer = (i: number) => i === currentQuestion?.userAnswerIndex && !currentQuestion?.correct;
		const isCorrectChoice = (i: number) => i === currentQuestion?.correctIndex;
		
		portalContainer.innerHTML = `
			<div class="review-backdrop"></div>
			<div class="review-modal" role="dialog" tabindex="0">
				<div class="review-content">
					<!-- Header -->
					<div class="review-header">
						<div class="review-header-left">
							<span class="review-header-icon">REVIEW</span>
							<div>
								<h2 class="review-title">Question Review</h2>
								<p class="review-subtitle">Study your answers and learn from mistakes</p>
							</div>
						</div>
						<button class="review-close-btn" id="review-close">X</button>
					</div>
					
					<!-- Progress Bar -->
					<div class="review-progress">
						<span class="review-progress-text">Question ${currentIndex + 1} of ${questions.length}</span>
						<div class="review-progress-dots">
							${questions.map((q, i) => `
								<button class="review-dot ${q.correct ? 'dot-correct' : 'dot-wrong'} ${i === currentIndex ? 'dot-active' : ''}" data-index="${i}">
									${i + 1}
								</button>
							`).join('')}
						</div>
					</div>
					
					<!-- Question Content -->
					<div class="review-body">
						${currentQuestion ? `
							<!-- Question Text -->
							<div class="review-question-box">
								<p class="review-question-text">${currentQuestion.questionText}</p>
							</div>
							
							<!-- Choices -->
							<div class="review-choices">
								${currentQuestion.choices.map((choice, i) => {
									const correct = i === currentQuestion.correctIndex;
									const userPick = i === currentQuestion.userAnswerIndex;
									const wrongPick = userPick && !currentQuestion.correct;
									
									let stateClass = '';
									if (correct) stateClass = 'choice-correct';
									else if (wrongPick) stateClass = 'choice-wrong';
									
									return `
										<div class="review-choice ${stateClass}">
											<div class="choice-letter ${stateClass}">${getChoiceLetter(i)}</div>
											<div class="choice-content">
												<p class="choice-text">${choice}</p>
												${correct ? '<span class="choice-label label-correct">Correct Answer</span>' : ''}
												${wrongPick ? '<span class="choice-label label-wrong">Your Answer</span>' : ''}
											</div>
										</div>
									`;
								}).join('')}
							</div>
						` : ''}
					</div>
					
					<!-- Navigation -->
					<div class="review-nav">
						<button class="review-nav-btn" id="review-prev" ${currentIndex === 0 ? 'disabled' : ''}>
							<span class="nav-arrow">&#9664;</span> Previous
						</button>
						<span class="review-nav-hint">Arrow keys to navigate</span>
						<button class="review-nav-btn" id="review-next" ${currentIndex === questions.length - 1 ? 'disabled' : ''}>
							Next <span class="nav-arrow">&#9654;</span>
						</button>
					</div>
				</div>
			</div>
		`;
		
		// Attach event listeners
		portalContainer.querySelector('#review-close')?.addEventListener('click', onClose);
		portalContainer.querySelector('.review-backdrop')?.addEventListener('click', onClose);
		portalContainer.querySelector('#review-prev')?.addEventListener('click', prevQuestion);
		portalContainer.querySelector('#review-next')?.addEventListener('click', nextQuestion);
		
		// Dot navigation
		portalContainer.querySelectorAll('.review-dot').forEach(dot => {
			dot.addEventListener('click', (e) => {
				const idx = parseInt((e.target as HTMLElement).dataset.index || '0');
				currentIndex = idx;
			});
		});
	});
</script>

<svelte:head>
	<style>
		.review-backdrop {
			position: fixed;
			inset: 0;
			background: rgba(0, 0, 0, 0.8);
			backdrop-filter: blur(4px);
			z-index: 99990;
		}
		
		.review-modal {
			position: fixed;
			top: 50%;
			left: 50%;
			transform: translate(-50%, -50%);
			z-index: 99991;
			width: 90%;
			max-width: 640px;
			max-height: 85vh;
			outline: none;
		}
		
		.review-content {
			background: linear-gradient(180deg, #1a1f2e 0%, #0d1117 100%);
			border: 3px solid #2d3748;
			border-radius: 12px;
			box-shadow: 
				0 0 0 1px rgba(255,255,255,0.05),
				0 20px 50px rgba(0,0,0,0.5),
				inset 0 1px 0 rgba(255,255,255,0.05);
			overflow: hidden;
			display: flex;
			flex-direction: column;
			max-height: 85vh;
		}
		
		/* Header */
		.review-header {
			display: flex;
			justify-content: space-between;
			align-items: center;
			padding: 16px 20px;
			background: linear-gradient(180deg, #252d3d 0%, #1a2233 100%);
			border-bottom: 2px solid #2d3748;
		}
		
		.review-header-left {
			display: flex;
			align-items: center;
			gap: 12px;
		}
		
		.review-header-icon {
			display: flex;
			align-items: center;
			justify-content: center;
			width: 48px;
			height: 48px;
			background: linear-gradient(180deg, #3b82f6 0%, #1d4ed8 100%);
			border: 2px solid #60a5fa;
			border-radius: 8px;
			font-family: 'Press Start 2P', monospace;
			font-size: 0.5rem;
			color: white;
			text-shadow: 1px 1px 0 rgba(0,0,0,0.3);
		}
		
		.review-title {
			font-family: 'Press Start 2P', monospace;
			font-size: 0.875rem;
			color: #e2e8f0;
			margin: 0 0 4px 0;
			letter-spacing: 0.05em;
		}
		
		.review-subtitle {
			font-size: 0.75rem;
			color: #64748b;
			margin: 0;
		}
		
		.review-close-btn {
			width: 36px;
			height: 36px;
			background: linear-gradient(180deg, #475569 0%, #334155 100%);
			border: 2px solid #64748b;
			border-radius: 6px;
			color: #e2e8f0;
			font-family: 'Press Start 2P', monospace;
			font-size: 0.625rem;
			cursor: pointer;
			transition: all 0.15s ease;
		}
		
		.review-close-btn:hover {
			background: linear-gradient(180deg, #64748b 0%, #475569 100%);
			border-color: #94a3b8;
		}
		
		/* Progress */
		.review-progress {
			display: flex;
			justify-content: space-between;
			align-items: center;
			padding: 12px 20px;
			background: rgba(30, 41, 59, 0.5);
			border-bottom: 1px solid #2d3748;
		}
		
		.review-progress-text {
			font-family: 'Press Start 2P', monospace;
			font-size: 0.5rem;
			color: #94a3b8;
			letter-spacing: 0.05em;
		}
		
		.review-progress-dots {
			display: flex;
			gap: 6px;
		}
		
		.review-dot {
			width: 28px;
			height: 28px;
			border-radius: 50%;
			border: 2px solid transparent;
			font-family: 'Press Start 2P', monospace;
			font-size: 0.5rem;
			color: white;
			cursor: pointer;
			transition: all 0.15s ease;
			opacity: 0.6;
		}
		
		.review-dot:hover {
			opacity: 1;
			transform: scale(1.1);
		}
		
		.dot-correct {
			background: linear-gradient(180deg, #22c55e 0%, #16a34a 100%);
			border-color: #4ade80;
		}
		
		.dot-wrong {
			background: linear-gradient(180deg, #ef4444 0%, #dc2626 100%);
			border-color: #f87171;
		}
		
		.dot-active {
			opacity: 1;
			box-shadow: 0 0 0 3px rgba(255,255,255,0.3);
			transform: scale(1.1);
		}
		
		/* Body */
		.review-body {
			flex: 1;
			overflow-y: auto;
			padding: 20px;
		}
		
		.review-question-box {
			background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
			border: 2px solid #334155;
			border-radius: 10px;
			padding: 16px;
			margin-bottom: 20px;
		}
		
		.review-question-text {
			font-size: 0.9rem;
			line-height: 1.6;
			color: #e2e8f0;
			margin: 0;
		}
		
		/* Choices */
		.review-choices {
			display: flex;
			flex-direction: column;
			gap: 10px;
		}
		
		.review-choice {
			display: flex;
			align-items: flex-start;
			gap: 12px;
			padding: 12px 14px;
			background: rgba(30, 41, 59, 0.4);
			border: 2px solid #334155;
			border-radius: 10px;
			transition: all 0.15s ease;
		}
		
		.review-choice.choice-correct {
			background: rgba(34, 197, 94, 0.15);
			border-color: #22c55e;
		}
		
		.review-choice.choice-wrong {
			background: rgba(239, 68, 68, 0.15);
			border-color: #ef4444;
		}
		
		.choice-letter {
			flex-shrink: 0;
			width: 32px;
			height: 32px;
			display: flex;
			align-items: center;
			justify-content: center;
			background: linear-gradient(180deg, #475569 0%, #334155 100%);
			border: 2px solid #64748b;
			border-radius: 50%;
			font-family: 'Press Start 2P', monospace;
			font-size: 0.625rem;
			color: #e2e8f0;
		}
		
		.choice-letter.choice-correct {
			background: linear-gradient(180deg, #22c55e 0%, #16a34a 100%);
			border-color: #4ade80;
		}
		
		.choice-letter.choice-wrong {
			background: linear-gradient(180deg, #ef4444 0%, #dc2626 100%);
			border-color: #f87171;
		}
		
		.choice-content {
			flex: 1;
			min-width: 0;
		}
		
		.choice-text {
			font-size: 0.85rem;
			line-height: 1.5;
			color: #cbd5e1;
			margin: 0 0 6px 0;
			word-wrap: break-word;
		}
		
		.choice-correct .choice-text {
			color: #86efac;
			font-weight: 600;
		}
		
		.choice-wrong .choice-text {
			color: #fca5a5;
		}
		
		.choice-label {
			display: inline-block;
			padding: 4px 10px;
			border-radius: 4px;
			font-family: 'Press Start 2P', monospace;
			font-size: 0.4rem;
			letter-spacing: 0.05em;
		}
		
		.label-correct {
			background: rgba(34, 197, 94, 0.3);
			border: 1px solid #22c55e;
			color: #86efac;
		}
		
		.label-wrong {
			background: rgba(239, 68, 68, 0.3);
			border: 1px solid #ef4444;
			color: #fca5a5;
		}
		
		/* Navigation */
		.review-nav {
			display: flex;
			justify-content: space-between;
			align-items: center;
			padding: 16px 20px;
			background: rgba(30, 41, 59, 0.5);
			border-top: 2px solid #2d3748;
		}
		
		.review-nav-btn {
			display: flex;
			align-items: center;
			gap: 8px;
			padding: 10px 16px;
			background: linear-gradient(180deg, #475569 0%, #334155 100%);
			border: 2px solid #64748b;
			border-radius: 8px;
			font-family: 'Press Start 2P', monospace;
			font-size: 0.5rem;
			color: #e2e8f0;
			cursor: pointer;
			transition: all 0.15s ease;
		}
		
		.review-nav-btn:hover:not(:disabled) {
			background: linear-gradient(180deg, #64748b 0%, #475569 100%);
			border-color: #94a3b8;
			transform: translateY(-2px);
		}
		
		.review-nav-btn:disabled {
			opacity: 0.4;
			cursor: not-allowed;
		}
		
		.nav-arrow {
			font-size: 0.625rem;
		}
		
		.review-nav-hint {
			font-size: 0.65rem;
			color: #64748b;
		}
	</style>
</svelte:head>
