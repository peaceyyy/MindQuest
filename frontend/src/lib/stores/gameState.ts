import { writable, derived } from 'svelte/store';

/**
 * Core game state store for battle mechanics
 * Centralizes all game-related state to avoid prop drilling and enable cross-component access
 */

// Session and question state
export const sessionId = writable<string>('');
export const currentQuestion = writable<any>(null);
export const questionStartTime = writable<number | null>(null);

// HP state
export const playerHP = writable<number>(100);
export const playerMaxHP = writable<number>(100);
export const enemyHP = writable<number>(100);
export const enemyMaxHP = writable<number>(100);

// Progress tracking
export const totalPoints = writable<number>(0);
export const questionsAnswered = writable<number>(0);

// Accuracy tracking for gauge widget
export const correctAnswers = writable<number>(0);
export const incorrectAnswers = writable<number>(0);
export const currentAccuracy = derived(
	[correctAnswers, incorrectAnswers],
	([$correct, $incorrect]) => {
		const total = $correct + $incorrect;
		return total === 0 ? 0 : Math.round(($correct / total) * 100);
	}
);

// Streak tracking for bonuses
export const correctStreak = writable<number>(0);
export const wrongStreak = writable<number>(0);
export const isHotStreak = writable<boolean>(false);

// Hint system
export const hints = writable<number>(0);
export const maxHints = writable<number>(0);
export const eliminatedChoices = writable<number[]>([]);
export const hintUsedThisQuestion = writable<boolean>(false);

// Answer history for review
export const answerHistory = writable<Array<{
	correct: boolean;
	questionText: string;
	choices: string[];
	correctIndex: number;
	userAnswerIndex: number;
}>>([]);

// Feedback from backend
export const feedback = writable<any>(null);
export const roundSummary = writable<any>(null);

/**
 * Reset game state for a new battle session
 */
export function resetGameState() {
	sessionId.set('');
	currentQuestion.set(null);
	questionStartTime.set(null);
	
	playerHP.set(100);
	playerMaxHP.set(100);
	enemyHP.set(100);
	enemyMaxHP.set(100);
	
	totalPoints.set(0);
	questionsAnswered.set(0);
	
	correctAnswers.set(0);
	incorrectAnswers.set(0);
	
	correctStreak.set(0);
	wrongStreak.set(0);
	isHotStreak.set(false);
	
	hints.set(0);
	maxHints.set(0);
	eliminatedChoices.set([]);
	hintUsedThisQuestion.set(false);
	
	answerHistory.set([]);
	feedback.set(null);
	roundSummary.set(null);
}

/**
 * Initialize a new game session with config
 */
export function initializeSession(newSessionId: string, initialHints: number = 0) {
	resetGameState();
	sessionId.set(newSessionId);
	hints.set(initialHints);
	maxHints.set(initialHints);
}
