<script lang="ts">
	// Props from parent
	interface Props {
		currentAccuracy: number; // 0-100
		threshold: number; // Target accuracy percentage (70/60/50 based on difficulty)
		difficulty: string; // 'easy', 'medium', 'hard'
		correctAnswers: number;
		incorrectAnswers: number;
	}
	
	let { currentAccuracy, threshold, difficulty, correctAnswers, incorrectAnswers }: Props = $props();
	
	// Color based on performance relative to threshold
	let gaugeColor = $derived(() => {
		if (currentAccuracy >= threshold) return '#10b981'; // Green - Above threshold
		if (currentAccuracy >= threshold - 10) return '#f59e0b'; // Yellow - Close to threshold
		return '#ef4444'; // Red - Below threshold
	});
	
	// Stroke dasharray for circular progress (circumference = 2 * PI * radius)
	const radius = 45;
	const circumference = 2 * Math.PI * radius;
	let strokeDashoffset = $derived(circumference - (currentAccuracy / 100) * circumference);
	
	// Threshold marker position (degrees)
	let thresholdAngle = $derived((threshold / 100) * 360 - 90); // -90 to start from top
</script>

<div class="accuracy-gauge">
	<!-- SVG Circular Gauge -->
	<svg width="120" height="120" viewBox="0 0 120 120" class="gauge-svg">
		<!-- Background circle -->
		<circle
			cx="60"
			cy="60"
			r={radius}
			fill="none"
			stroke="#1f2937"
			stroke-width="10"
			opacity="0.3"
		/>
		
		<!-- Threshold marker line -->
		<line
			x1="60"
			y1="60"
			x2={60 + radius * Math.cos((thresholdAngle * Math.PI) / 180)}
			y2={60 + radius * Math.sin((thresholdAngle * Math.PI) / 180)}
			stroke="#6b7280"
			stroke-width="2"
			stroke-dasharray="4 2"
			opacity="0.6"
		/>
		
		<!-- Progress circle -->
		<circle
			cx="60"
			cy="60"
			r={radius}
			fill="none"
			stroke={gaugeColor()}
			stroke-width="10"
			stroke-linecap="round"
			stroke-dasharray={circumference}
			stroke-dashoffset={strokeDashoffset}
			transform="rotate(-90 60 60)"
			class="gauge-progress"
		/>
		
		<!-- Center text -->
		<text x="60" y="55" text-anchor="middle" class="gauge-percentage">
			{currentAccuracy.toFixed(0)}%
		</text>
		<text x="60" y="72" text-anchor="middle" class="gauge-label">
			Accuracy
		</text>
	</svg>
	
	<!-- Stats below gauge -->
	<div class="gauge-stats">
		<div class="stat-row">
			<span class="stat-correct">✓ {correctAnswers}</span>
			<span class="stat-incorrect">✗ {incorrectAnswers}</span>
		</div>
		<div class="threshold-text">
			Target: {threshold}%
		</div>
	</div>
</div>

<style>
	.accuracy-gauge {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
	}
	
	.gauge-svg {
		filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
	}
	
	.gauge-progress {
		transition: stroke-dashoffset 0.5s ease, stroke 0.3s ease;
	}
	
	.gauge-percentage {
		font-size: 1.75rem;
		font-weight: bold;
		fill: #f3f4f6;
	}
	
	.gauge-label {
		font-size: 0.75rem;
		fill: #9ca3af;
		text-transform: uppercase;
		letter-spacing: 0.05em;
	}
	
	.gauge-stats {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.25rem;
	}
	
	.stat-row {
		display: flex;
		gap: 1rem;
		font-size: 0.875rem;
		font-weight: 600;
	}
	
	.stat-correct {
		color: #10b981;
	}
	
	.stat-incorrect {
		color: #ef4444;
	}
	
	.threshold-text {
		font-size: 0.75rem;
		color: #6b7280;
	}
</style>
