<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';

	let points = $derived(parseInt(page.url.searchParams.get('points') || '0', 10));
	let answered = $derived(parseInt(page.url.searchParams.get('answered') || '0', 10));
	let accuracy = $derived(answered > 0 ? Math.round((points / (answered * 10)) * 100) : 0);
	
	function playAgain() {
		goto('/');
	}
</script>

<div class="container">
	<div class="results-card">
		<h1>Round Complete!</h1>
		
		<div class="stats-grid">
			<div class="stat">
				<div class="stat-value">{points}</div>
				<div class="stat-label">Total Points</div>
			</div>
			
			<div class="stat">
				<div class="stat-value">{answered}</div>
				<div class="stat-label">Questions Answered</div>
			</div>
			
			<div class="stat">
				<div class="stat-value">{accuracy}%</div>
				<div class="stat-label">Accuracy</div>
			</div>
		</div>
		
		<div class="actions">
			<button class="primary" onclick={playAgain}>Play Again</button>
		</div>
	</div>
</div>

<style>
	.container {
		max-width: 600px;
		margin: 3rem auto;
		padding: 1rem;
	}

	.results-card {
		background: #f9fafb;
		border: 1px solid #e5e7eb;
		border-radius: 12px;
		padding: 3rem 2rem;
		text-align: center;
	}

	h1 {
		font-size: 2.5rem;
		margin-bottom: 2.5rem;
	}

	.stats-grid {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
		gap: 2rem;
		margin-bottom: 3rem;
	}

	.stat {
		padding: 1.5rem;
		background: white;
		border-radius: 8px;
		border: 1px solid #e5e7eb;
	}

	.stat-value {
		font-size: 2.5rem;
		font-weight: 700;
		color: #3b82f6;
		margin-bottom: 0.5rem;
	}

	.stat-label {
		font-size: 0.875rem;
		color: #6b7280;
		font-weight: 500;
	}

	.actions {
		display: flex;
		flex-direction: column;
		gap: 1rem;
	}

	button {
		padding: 1rem 2rem;
		font-size: 1.125rem;
		font-weight: 600;
		background: #3b82f6;
		color: white;
		border: none;
		border-radius: 6px;
		cursor: pointer;
	}

	button:hover {
		background: #2563eb;
	}
</style>
