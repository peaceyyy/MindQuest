/**
 * Battle Service
 * Pure calculation functions for game mechanics and battle logic
 */

// ===== Type Definitions =====

export type Difficulty = 'easy' | 'medium' | 'hard';
export type DefeatReason = 'hp_depleted' | 'accuracy_low' | 'counterattack' | 'enemy_survived';

// ===== Difficulty Calculations =====

/**
 * Get numeric difficulty level (1-3) for sprite selection
 */
export function getDifficultyLevel(difficulty: string): number {
	switch (difficulty.toLowerCase()) {
		case 'easy': return 1;
		case 'medium': return 2;
		case 'hard': return 3;
		default: return 1;
	}
}

/**
 * Get accuracy threshold required for victory
 * Inverted model: easier difficulties require higher accuracy
 */
export function getAccuracyThreshold(difficulty: string): number {
	switch (difficulty.toLowerCase()) {
		case 'easy': return 70;   // You should know this!
		case 'medium': return 60; // Balanced
		case 'hard': return 50;   // Forgiving - questions are hard
		default: return 60;
	}
}

/**
 * Get base damage per correct answer
 * Inverted model: harder difficulties deal less damage (enemy has more HP)
 */
export function getEnemyDamagePerHit(difficulty: string): number {
	switch (difficulty.toLowerCase()) {
		case 'easy': return 25;    // 4 correct answers to win
		case 'medium': return 20;  // 5 correct answers to win
		case 'hard': return 16.67; // 6 correct answers to win
		default: return 20;
	}
}

// ===== Damage Calculations =====

/**
 * Calculate damage with critical hit bonus applied
 */
export function calculateDamage(baseDamage: number, isCritical: boolean, difficulty: string): number {
	if (!isCritical) {
		return Math.round(baseDamage);
	}

	const critMultiplier = getCriticalHitMultiplier(difficulty);
	return Math.round(baseDamage * critMultiplier);
}

/**
 * Get critical hit multiplier based on difficulty
 */
export function getCriticalHitMultiplier(difficulty: string): number {
	switch (difficulty.toLowerCase()) {
		case 'easy': return 1.05;   // +5% damage
		case 'medium': return 1.15; // +15% damage
		case 'hard': return 1.25;   // +25% damage
		default: return 1.15;
	}
}

/**
 * Get critical hit bonus percentage for display
 */
export function getCriticalHitBonus(difficulty: string): number {
	switch (difficulty.toLowerCase()) {
		case 'easy': return 5;
		case 'medium': return 15;
		case 'hard': return 25;
		default: return 15;
	}
}

// ===== Message Generators =====

/**
 * Get critical hit message based on difficulty
 */
export function getCritMessage(difficulty: string): string {
	const bonus = getCriticalHitBonus(difficulty);
	
	switch (difficulty.toLowerCase()) {
		case 'easy':
			return `CRITICAL HIT! Quick reflexes! +${bonus}% damage!`;
		case 'medium':
			return `CRITICAL HIT! Excellent timing! +${bonus}% damage!`;
		case 'hard':
			return `CRITICAL HIT! Masterful precision! +${bonus}% damage!`;
		default:
			return `CRITICAL HIT! +${bonus}% damage!`;
	}
}

/**
 * Get defeat reason message for display
 */
export function getDefeatReasonMessage(reason: DefeatReason): string {
	switch (reason) {
		case 'hp_depleted':
			return 'Your HP reached zero!';
		case 'accuracy_low':
			return 'Your accuracy fell below the required threshold!';
		case 'counterattack':
			return 'The boss landed a devastating counterattack!';
		case 'enemy_survived':
			return 'You ran out of questions before defeating the boss!';
		default:
			return 'You were defeated!';
	}
}

/**
 * Get display-friendly topic name with proper capitalization
 */
export function getTopicDisplayName(topic: string): string {
	const lower = topic.toLowerCase();
	
	// Built-in topics get special formatting
	switch (lower) {
		case 'ai': return 'A.I.';
		case 'cs': return 'Computer Science';
		case 'philosophy': return 'Philosophy';
		default:
			// Custom topics: capitalize first letter of each word
			return topic.split(/[\s_-]+/)
				.map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
				.join(' ');
	}
}

// ===== Asset Path Helpers =====

/**
 * List of built-in topics with dedicated assets
 */
export const BUILT_IN_TOPICS = ['ai', 'cs', 'philosophy'] as const;

/**
 * Check if a topic is built-in
 */
export function isBuiltInTopic(topic: string): boolean {
	return BUILT_IN_TOPICS.includes(topic.toLowerCase() as any);
}

/**
 * Get enemy sprite path
 */
export function getEnemySpritePath(topic: string, difficulty: string): string {
	const normalizedTopic = topic.toLowerCase();
	const folder = isBuiltInTopic(normalizedTopic) ? normalizedTopic : 'default';
	const prefix = isBuiltInTopic(normalizedTopic) ? normalizedTopic : 'default';
	const level = getDifficultyLevel(difficulty);
	
	return `/sprites/enemies/${folder}/${prefix}-lv${level}.png`;
}

/**
 * Get player sprite path
 */
export function getPlayerSpritePath(): string {
	return '/sprites/player/player-lv1.png';
}

/**
 * Get background image path
 */
export function getBackgroundPath(topic: string, index: number = 1): string {
	const normalizedTopic = topic.toLowerCase();
	const folder = isBuiltInTopic(normalizedTopic) ? normalizedTopic : 'default';
	
	return `/backgrounds/${folder}/${folder}-bg_${index}.png`;
}

// ===== Enemy Attack Patterns =====

export type EnemyAttackType = 'lunge' | 'pulse' | 'spin';

/**
 * Get enemy attack animation type based on topic
 */
export function getEnemyAttackType(topic: string): EnemyAttackType {
	const normalizedTopic = topic.toLowerCase();
	
	switch (normalizedTopic) {
		case 'ai':
			return 'pulse'; // Digital/energy-based attack
		case 'philosophy':
			return 'spin'; // Mysterious/mind-bending attack
		case 'cs':
		default:
			return 'lunge'; // Physical/direct attack
	}
}
