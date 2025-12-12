/**
 * Formatting Service
 * View-model formatting for results, stats, and display values
 */

/**
 * Format points display with separators
 */
export function formatPoints(points: number): string {
	return points.toLocaleString();
}

/**
 * Calculate accuracy percentage from points and questions
 * Assumes 10 points per correct answer
 */
export function calculateAccuracy(points: number, questionsAnswered: number): number {
	if (questionsAnswered === 0) return 0;
	const possiblePoints = questionsAnswered * 10;
	return Math.round((points / possiblePoints) * 100);
}

/**
 * Get performance rating based on accuracy
 */
export function getPerformanceRating(accuracy: number): {
	label: string;
	color: string;
	emoji: string;
} {
	if (accuracy >= 90) {
		return { label: 'Excellent', color: '#22c55e', emoji: '🏆' };
	} else if (accuracy >= 75) {
		return { label: 'Great', color: '#3b82f6', emoji: '⭐' };
	} else if (accuracy >= 60) {
		return { label: 'Good', color: '#f59e0b', emoji: '👍' };
	} else if (accuracy >= 40) {
		return { label: 'Fair', color: '#f97316', emoji: '📚' };
	} else {
		return { label: 'Keep Practicing', color: '#ef4444', emoji: '💪' };
	}
}

/**
 * Format duration in milliseconds to readable format
 */
export function formatDuration(ms: number): string {
	const seconds = Math.floor(ms / 1000);
	if (seconds < 60) {
		return `${seconds}s`;
	}
	const minutes = Math.floor(seconds / 60);
	const remainingSeconds = seconds % 60;
	return `${minutes}m ${remainingSeconds}s`;
}

/**
 * Format answer time for critical hit display
 */
export function formatAnswerTime(ms: number | null): string {
	if (ms === null) return 'N/A';
	const seconds = (ms / 1000).toFixed(2);
	return `${seconds}s`;
}

/**
 * Get grade letter based on percentage
 */
export function getGradeLetter(percentage: number): string {
	if (percentage >= 90) return 'A';
	if (percentage >= 80) return 'B';
	if (percentage >= 70) return 'C';
	if (percentage >= 60) return 'D';
	return 'F';
}

/**
 * Sort answer history by correctness (wrong answers first for review)
 */
export function sortAnswersForReview<T extends { correct: boolean }>(
	answers: T[]
): T[] {
	return [...answers].sort((a, b) => {
		if (a.correct === b.correct) return 0;
		return a.correct ? 1 : -1; // Wrong answers first
	});
}

/**
 * Group answers by correctness
 */
export function groupAnswersByCorrectness<T extends { correct: boolean }>(
	answers: T[]
): { correct: T[]; incorrect: T[] } {
	return answers.reduce(
		(acc, answer) => {
			if (answer.correct) {
				acc.correct.push(answer);
			} else {
				acc.incorrect.push(answer);
			}
			return acc;
		},
		{ correct: [] as T[], incorrect: [] as T[] }
	);
}

/**
 * Calculate stats summary
 */
export function calculateStats(answers: { correct: boolean }[]): {
	total: number;
	correct: number;
	incorrect: number;
	accuracy: number;
} {
	const total = answers.length;
	const correct = answers.filter(a => a.correct).length;
	const incorrect = total - correct;
	const accuracy = total > 0 ? Math.round((correct / total) * 100) : 0;

	return { total, correct, incorrect, accuracy };
}

/**
 * Format large numbers with K/M suffixes
 */
export function formatCompactNumber(num: number): string {
	if (num >= 1000000) {
		return `${(num / 1000000).toFixed(1)}M`;
	}
	if (num >= 1000) {
		return `${(num / 1000).toFixed(1)}K`;
	}
	return num.toString();
}

/**
 * Get encouragement message based on performance
 */
export function getEncouragementMessage(accuracy: number, isVictory: boolean): string {
	if (isVictory) {
		if (accuracy >= 90) {
			return "Perfect execution! You're a true master! 🏆";
		} else if (accuracy >= 75) {
			return "Impressive performance! Keep up the great work! ⭐";
		} else {
			return "Victory achieved! Practice makes perfect! 🎉";
		}
	} else {
		if (accuracy >= 60) {
			return "You're so close! Review the mistakes and try again! 📚";
		} else {
			return "Don't give up! Every battle makes you stronger! 💪";
		}
	}
}
