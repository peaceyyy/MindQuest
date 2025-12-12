import { writable } from 'svelte/store';

/**
 * UI state store for battle interface management
 * Separates presentation concerns from game logic
 */

// Battle intro sequence
export const showBattleIntro = writable<boolean>(false);
export const introPhase = writable<number>(0); // 0: grass/encounter, 1: enemy appears, 2: ready to fight

// Round completion and victory/defeat
export const roundComplete = writable<boolean>(false);
export const isVictory = writable<boolean>(false);
export const defeatReason = writable<string>(''); // 'hp_depleted', 'accuracy_low', 'counterattack'

// Modals and dialogs
export const showReviewModal = writable<boolean>(false);
export const showFleeConfirm = writable<boolean>(false);

// Loading states
export const loading = writable<boolean>(false);
export const fleeLoading = writable<boolean>(false);

// Error messaging
export const error = writable<string>('');

// Pending actions (when player needs to see feedback before end screen)
export const pendingRoundEnd = writable<(() => void) | null>(null);

/**
 * Reset UI state for a new battle
 */
export function resetUIState() {
	showBattleIntro.set(false);
	introPhase.set(0);
	
	roundComplete.set(false);
	isVictory.set(false);
	defeatReason.set('');
	
	showReviewModal.set(false);
	showFleeConfirm.set(false);
	
	loading.set(false);
	fleeLoading.set(false);
	
	error.set('');
	pendingRoundEnd.set(null);
}

/**
 * Trigger battle intro sequence
 */
export function startBattleIntro() {
	showBattleIntro.set(true);
	introPhase.set(0);
}

/**
 * Progress to next phase of battle intro
 */
export function nextIntroPhase() {
	introPhase.update(phase => phase + 1);
}

/**
 * Complete battle intro and transition to gameplay
 */
export function completeBattleIntro() {
	showBattleIntro.set(false);
	introPhase.set(0);
}

/**
 * Trigger round end with victory
 */
export function triggerVictory() {
	roundComplete.set(true);
	isVictory.set(true);
}

/**
 * Trigger round end with defeat
 */
export function triggerDefeat(reason: string) {
	roundComplete.set(true);
	isVictory.set(false);
	defeatReason.set(reason);
}
