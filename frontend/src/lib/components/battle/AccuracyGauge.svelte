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
	<!-- Retro-styled container with border -->
	<div class="gauge-container">
		<!-- Title Header -->
		<div class="gauge-header">
			<span class="gauge-title">ACCURACY</span>
		</div>
		
		<!-- Large Percentage Display -->
		<div class="percentage-display">
			<span class="percentage-value" style="color: {gaugeColor()}">{currentAccuracy.toFixed(0)}%</span>
		</div>
		
		<!-- Progress Bar -->
		<div class="progress-bar-container">
			<div class="progress-bar-bg">
				<!-- Threshold Marker -->
				<div class="threshold-marker" style="left: {threshold}%">
					<div class="threshold-line"></div>
					<div class="threshold-label">{threshold}%</div>
				</div>
				
				<!-- Filled Progress -->
				<div 
					class="progress-bar-fill" 
					style="width: {currentAccuracy}%; background-color: {gaugeColor()}"
				></div>
			</div>
		</div>
		
		<!-- Stats Row -->
		<div class="stats-row">
			<div class="stat-item stat-correct">
				<span class="stat-icon">✓</span>
				<span class="stat-value">{correctAnswers}</span>
			</div>
			<div class="stat-divider"></div>
			<div class="stat-item stat-incorrect">
				<span class="stat-icon">✗</span>
				<span class="stat-value">{incorrectAnswers}</span>
			</div>
		</div>
		
		<!-- Target Label -->
		<div class="target-label">
			TARGET: {threshold}%
		</div>
	</div>
</div>

<style>
	.accuracy-gauge {
		display: flex;
		flex-direction: column;
		align-items: center;
	}
	
	.gauge-container {
		background: linear-gradient(135deg, rgba(17, 24, 39, 0.95) 0%, rgba(31, 41, 55, 0.95) 100%);
		border: 3px solid #374151;
		border-radius: 12px;
		padding: 12px 16px;
		min-width: 200px;
		box-shadow: 
			0 4px 6px -1px rgba(0, 0, 0, 0.3),
			0 2px 4px -1px rgba(0, 0, 0, 0.2),
			inset 0 1px 0 0 rgba(255, 255, 255, 0.1);
	}
	
	.gauge-header {
		text-align: center;
		margin-bottom: 8px;
		border-bottom: 2px solid #374151;
		padding-bottom: 6px;
	}
	
	.gauge-title {
		font-size: 0.75rem;
		font-weight: 700;
		letter-spacing: 0.1em;
		color: #9ca3af;
		text-transform: uppercase;
		font-family: monospace;
	}
	
	.percentage-display {
		text-align: center;
		margin: 12px 0;
	}
	
	.percentage-value {
		font-size: 2.5rem;
		font-weight: 900;
		font-family: monospace;
		text-shadow: 
			0 0 10px currentColor,
			0 2px 4px rgba(0, 0, 0, 0.5);
		line-height: 1;
	}
	
	.progress-bar-container {
		margin: 12px 0;
	}
	
	.progress-bar-bg {
		position: relative;
		height: 24px;
		background: linear-gradient(to bottom, #1f2937, #111827);
		border: 2px solid #374151;
		border-radius: 6px;
		overflow: visible;
		box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.4);
	}
	
	.progress-bar-fill {
		height: 100%;
		border-radius: 4px;
		transition: width 0.5s ease, background-color 0.3s ease;
		box-shadow: 
			0 0 8px currentColor,
			inset 0 1px 0 rgba(255, 255, 255, 0.3);
		position: relative;
	}
	
	.progress-bar-fill::after {
		content: '';
		position: absolute;
		top: 0;
		left: 0;
		right: 0;
		height: 50%;
		background: linear-gradient(to bottom, rgba(255, 255, 255, 0.3), transparent);
		border-radius: 4px 4px 0 0;
	}
	
	.threshold-marker {
		position: absolute;
		top: -8px;
		bottom: -8px;
		transform: translateX(-50%);
		z-index: 10;
		pointer-events: none;
	}
	
	.threshold-line {
		width: 2px;
		height: 100%;
		background: #f59e0b;
		box-shadow: 0 0 4px #f59e0b;
	}
	
	.threshold-label {
		position: absolute;
		top: -18px;
		left: 50%;
		transform: translateX(-50%);
		font-size: 0.65rem;
		font-weight: 700;
		color: #f59e0b;
		background: #1f2937;
		padding: 1px 4px;
		border-radius: 3px;
		white-space: nowrap;
		font-family: monospace;
	}
	
	.stats-row {
		display: flex;
		justify-content: center;
		align-items: center;
		gap: 8px;
		margin: 12px 0 8px 0;
		padding: 8px;
		background: rgba(0, 0, 0, 0.3);
		border-radius: 6px;
	}
	
	.stat-item {
		display: flex;
		align-items: center;
		gap: 6px;
		font-family: monospace;
	}
	
	.stat-icon {
		font-size: 1rem;
		font-weight: 700;
	}
	
	.stat-value {
		font-size: 1.125rem;
		font-weight: 700;
	}
	
	.stat-correct {
		color: #10b981;
		text-shadow: 0 0 4px rgba(16, 185, 129, 0.5);
	}
	
	.stat-incorrect {
		color: #ef4444;
		text-shadow: 0 0 4px rgba(239, 68, 68, 0.5);
	}
	
	.stat-divider {
		width: 2px;
		height: 20px;
		background: #374151;
	}
	
	.target-label {
		text-align: center;
		font-size: 0.7rem;
		font-weight: 600;
		color: #6b7280;
		text-transform: uppercase;
		letter-spacing: 0.05em;
		font-family: monospace;
		border-top: 1px solid #374151;
		padding-top: 8px;
	}
</style>
