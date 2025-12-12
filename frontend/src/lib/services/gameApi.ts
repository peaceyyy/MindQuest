/**
 * Game API Service
 * Handles all HTTP communication with the backend game server
 */

// ===== Type Definitions =====

export interface QuestionResponse {
	questionText: string;
	question?: string; // Legacy field
	choices: string[];
}

export interface HintsResponse {
	hints: number;
	maxHints: number;
}

export interface UseHintResponse {
	hints: number;
	eliminatedIndices: number[];
}

export interface AnswerRequest {
	answer: string; // 'A', 'B', 'C', or 'D'
	answerTimeMs: number | null;
}

export interface AnswerResponse {
	correct: boolean;
	correctIndex: number;
	isCritical: boolean;
	isCounterattack: boolean;
	damageTaken?: number;
	currentHp: number;
	pointsAwarded: number;
	roundComplete: boolean;
	correctAnswers: number;
	incorrectAnswers: number;
	currentAccuracy: number;
	correctStreak: number;
	wrongStreak: number;
	isHotStreak: boolean;
	summary?: any;
}

export interface SessionStateResponse {
	globalPoints: number;
	questionsAnswered: number;
	// Add other state fields as needed
}

export interface GameSessionRequest {
	topic: string;
	difficulty: string;
	enableHints?: boolean;
}

export interface GameSessionResponse {
	sessionId: string;
	maxHints: number;
}

export interface AbandonResponse {
	message: string;
}

// ===== API Functions =====

/**
 * Create a new game session
 */
export async function createGameSession(
	topic: string,
	difficulty: string,
	enableHints: boolean = false
): Promise<GameSessionResponse> {
	const response = await fetch('/api/sessions/create', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ topic, difficulty, enableHints })
	});

	if (!response.ok) {
		throw new Error('Failed to create game session');
	}

	return response.json();
}

/**
 * Load the next question for a session
 * Returns null if the round is complete (HTTP 204)
 */
export async function loadQuestion(sessionId: string): Promise<QuestionResponse | null> {
	console.log('[gameApi] Loading question for session:', sessionId);
	const response = await fetch(`/api/sessions/${sessionId}/question`);
	console.log('[gameApi] Question response status:', response.status);

	if (response.status === 204) {
		// Round complete - no more questions
		console.log('[gameApi] Round complete (204 status)');
		return null;
	}

	if (!response.ok) {
		const errorText = await response.text();
		console.error('[gameApi] Failed to load question:', errorText);
		throw new Error('Failed to load question');
	}

	const rawText = await response.text();
	console.log('[gameApi] Question response:', rawText.slice(0, 200));
	return JSON.parse(rawText);
}

/**
 * Submit an answer for the current question
 */
export async function submitAnswer(
	sessionId: string,
	answerRequest: AnswerRequest
): Promise<AnswerResponse> {
	const response = await fetch(`/api/sessions/${sessionId}/answer`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(answerRequest)
	});

	if (!response.ok) {
		throw new Error('Failed to submit answer');
	}

	return response.json();
}

/**
 * Fetch current hints state
 */
export async function fetchHints(sessionId: string): Promise<HintsResponse> {
	const response = await fetch(`/api/sessions/${sessionId}/hints`);

	if (!response.ok) {
		throw new Error('Failed to fetch hints');
	}

	return response.json();
}

/**
 * Use a hint to eliminate wrong answers
 */
export async function useHint(sessionId: string): Promise<UseHintResponse> {
	console.log('[gameApi] Calling /use-hint endpoint...');
	const response = await fetch(`/api/sessions/${sessionId}/use-hint`, {
		method: 'POST'
	});

	if (!response.ok) {
		const errorData = await response.json();
		console.error('[gameApi] Failed to use hint:', errorData.error);
		throw new Error(errorData.error || 'Failed to use hint');
	}

	const data = await response.json();
	console.log('[gameApi] Full hint response:', JSON.stringify(data, null, 2));
	return data;
}

/**
 * Fetch final session state/stats
 */
export async function fetchSessionState(sessionId: string): Promise<SessionStateResponse> {
	const response = await fetch(`/api/sessions/${sessionId}/state`);

	if (!response.ok) {
		throw new Error('Failed to load session state');
	}

	return response.json();
}

/**
 * Abandon/flee the current round (no points awarded)
 */
export async function abandonSession(sessionId: string): Promise<AbandonResponse> {
	const response = await fetch(`/api/sessions/${sessionId}/abandon`, {
		method: 'POST'
	});

	if (!response.ok) {
		throw new Error('Failed to abandon round');
	}

	return response.json();
}
