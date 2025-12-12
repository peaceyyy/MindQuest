<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import * as formatting from '$lib/services/formatting';

	let points = $derived(parseInt(page.url.searchParams.get('points') || '0', 10));
	let answered = $derived(parseInt(page.url.searchParams.get('answered') || '0', 10));
	let accuracy = $derived(formatting.calculateAccuracy(points, answered));
	let performance = $derived(formatting.getPerformanceRating(accuracy));
	let grade = $derived(formatting.getGradeLetter(accuracy));
	
	function playAgain() {
		goto('/');
	}
</script>

<div class="container">
	<div class="results-card">
		<h1 class="pixel-text">Round Complete!</h1>
		
		<div class="performance-badge" style="background: {performance.color}20; border-color: {performance.color};">
			<span class="performance-emoji">{performance.emoji}</span>
			<span class="performance-label">{performance.label}</span>
			<span class="performance-grade">Grade: {grade}</span>
		</div>
		
		<div class="stats-grid">
			<div class="stat">
				<div class="stat-value" style="color: var(--color-gold);">{formatting.formatPoints(points)}</div>
				<div class="stat-label">Total Points</div>
			</div>
			
			<div class="stat">
				<div class="stat-value" style="color: var(--color-info);">{answered}</div>
				<div class="stat-label">Questions Answered</div>
			</div>
			
			<div class="stat">
				<div class="stat-value" style="color: {performance.color};">{accuracy}%</div>
				<div class="stat-label">Accuracy</div>
			</div>
		</div>
		
		<div class="actions">
			<button class="primary modal-btn modal-btn-primary" onclick={playAgain}>
				▶ Play Again ◀
			</button>
		</div>
	</div>
</div>

<style>
	.container {
		max-width: 600px;
		margin: 3rem auto;
		padding: var(--spacing-lg);
	}

	.results-card {
		background: var(--bg-card-primary);
		border-radius: var(--radius-xl);
		padding: 3rem 2rem;
		text-align: center;
		box-shadow: var(--card-shadow);
	}

	h1 {
		font-size: var(--text-3xl);
		margin-bottom: 2rem;
		color: var(--color-gold);
	}

	.performance-badge {
		display: inline-flex;
		flex-direction: column;
		align-items: center;
		gap: var(--spacing-sm);
		padding: var(--spacing-xl);
		border-radius: var(--radius-lg);
		border: 2px solid;
		margin-bottom: var(--spacing-2xl);
		animation: pulse var(--duration-very-slow) ease-in-out infinite;
	}

	.performance-emoji {
		font-size: 3rem;
	}

	.performance-label {
		font-family: var(--font-pixel);
		font-size: var(--text-base);
		font-weight: 800;
		text-transform: uppercase;
		letter-spacing: 0.1em;
	}

	.performance-grade {
		font-size: var(--text-sm);
		opacity: 0.8;
	}

	.stats-grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
		gap: var(--spacing-xl);
		margin-bottom: 3rem;
	}

	.stat {
		padding: var(--spacing-xl);
		background: var(--bg-card-secondary);
		border-radius: var(--radius-md);
		border: 1px solid var(--color-slate-700);
		transition: transform var(--transition-fast);
	}

	.stat:hover {
		transform: translateY(-4px);
	}

	.stat-value {
		font-size: var(--text-3xl);
		font-weight: 700;
		margin-bottom: var(--spacing-sm);
		font-family: var(--font-pixel);
	}

	.stat-label {
		font-size: var(--text-sm);
		color: var(--color-slate-400);
		font-weight: 500;
		text-transform: uppercase;
		letter-spacing: 0.05em;
	}

	.actions {
		display: flex;
		flex-direction: column;
		gap: var(--spacing-lg);
	}

	button {
		padding: var(--spacing-lg) var(--spacing-2xl);
		font-size: var(--text-sm);
		cursor: pointer;
		transition: all var(--transition-normal);
	}
</style>
