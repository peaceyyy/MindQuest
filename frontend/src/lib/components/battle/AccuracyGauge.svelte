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
		if (currentAccuracy >= threshold) return '#22c55e'; // Bright green - Above threshold
		if (currentAccuracy >= threshold - 10) return '#eab308'; // Yellow - Close to threshold
		return '#ef4444'; // Red - Below threshold
	});
	
	// Status text based on performance
	let statusText = $derived(() => {
		if (currentAccuracy >= threshold) return 'ON TARGET';
		if (currentAccuracy >= threshold - 10) return 'CLOSE';
		return 'DANGER';
	});
	
	// Animation class for critical states
	let isInDanger = $derived(currentAccuracy < threshold - 10 && correctAnswers + incorrectAnswers > 0);
</script>

<div class="accuracy-gauge" data-accuracy-anchor>
	<!-- RPG-styled panel with beveled edges -->
	<div class="gauge-panel" class:danger-pulse={isInDanger}>
		<!-- Panel header with icon -->
		<div class="panel-header">
			<span class="header-title">ACCURACY</span>
		</div>
		
		<!-- Large circular percentage display -->
		<div class="percentage-ring">
			<svg viewBox="0 0 120 120" class="ring-svg">
				<!-- Background ring -->
				<circle 
					cx="60" cy="60" r="50" 
					fill="none" 
					stroke="rgba(30, 41, 59, 0.8)" 
					stroke-width="8"
				/>
				<!-- Progress ring -->
				<circle 
					cx="60" cy="60" r="50" 
					fill="none" 
					stroke={gaugeColor()}
					stroke-width="8"
					stroke-linecap="round"
					stroke-dasharray="314.159"
					stroke-dashoffset={314.159 - (currentAccuracy / 100) * 314.159}
					transform="rotate(-90 60 60)"
					class="progress-ring"
				/>
				<!-- Threshold marker -->
				<circle 
					cx="60" cy="60" r="50"
					fill="none"
					stroke="#f59e0b"
					stroke-width="3"
					stroke-dasharray="4 310"
					stroke-dashoffset={-((threshold / 100) * 314.159 - 2)}
					transform="rotate(-90 60 60)"
					class="threshold-ring"
				/>
			</svg>
			
			<!-- Center content -->
			<div class="ring-center">
				<span class="percentage-value" style="color: {gaugeColor()}">{currentAccuracy.toFixed(0)}%</span>
				<span class="percentage-sub" style="color: {gaugeColor()}">{statusText()}</span>
			</div>
		</div>
		
		<!-- Threshold target display -->
		<div class="threshold-display">
			<span class="threshold-label">TARGET:</span>
			<span class="threshold-value">{threshold}%</span>
		</div>
		
		<!-- Stats counters -->
		<div class="stats-container">
			<div class="stat-box stat-correct">
				<div class="stat-icon">✓</div>
				<div class="stat-count">{correctAnswers}</div>
			</div>
			<div class="stat-separator">|</div>
			<div class="stat-box stat-incorrect">
				<div class="stat-icon">✗</div>
				<div class="stat-count">{incorrectAnswers}</div>
			</div>
		</div>
	</div>
</div>

<style>
	.accuracy-gauge {
		width: 100%;
	}
	
	.gauge-panel {
		position: relative;
		background: linear-gradient(180deg, 
			rgba(15, 23, 42, 0.98) 0%, 
			rgba(30, 41, 59, 0.95) 50%,
			rgba(15, 23, 42, 0.98) 100%
		);
		border: 3px solid transparent;
		border-radius: 12px;
		padding: 12px;
		
		/* Multi-layer border effect */
		box-shadow: 
			0 0 0 2px rgba(71, 85, 105, 0.6),
			0 0 0 4px rgba(30, 41, 59, 0.8),
			0 8px 24px rgba(0, 0, 0, 0.4),
			inset 0 1px 0 rgba(255, 255, 255, 0.05),
			inset 0 -1px 0 rgba(0, 0, 0, 0.3);
	}
	
	.gauge-panel::before {
		content: '';
		position: absolute;
		inset: -4px;
		border-radius: 18px;
		background: linear-gradient(135deg, 
			rgba(148, 163, 184, 0.4) 0%, 
			rgba(51, 65, 85, 0.2) 50%, 
			rgba(148, 163, 184, 0.4) 100%
		);
		z-index: -1;
	}
	
	/* Danger pulse animation */
	.danger-pulse {
		animation: danger-glow 1.5s ease-in-out infinite;
	}
	
	@keyframes danger-glow {
		0%, 100% { 
			box-shadow: 
				0 0 0 2px rgba(71, 85, 105, 0.6),
				0 0 0 4px rgba(30, 41, 59, 0.8),
				0 8px 24px rgba(0, 0, 0, 0.4),
				inset 0 1px 0 rgba(255, 255, 255, 0.05);
		}
		50% { 
			box-shadow: 
				0 0 0 2px rgba(239, 68, 68, 0.6),
				0 0 0 4px rgba(30, 41, 59, 0.8),
				0 8px 24px rgba(239, 68, 68, 0.2),
				0 0 30px rgba(239, 68, 68, 0.15),
				inset 0 1px 0 rgba(255, 255, 255, 0.05);
		}
	}
	
	/* Panel header */
	.panel-header {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 6px;
		padding-bottom: 8px;
		border-bottom: 2px solid rgba(71, 85, 105, 0.5);
		margin-bottom: 10px;
	}
	
	.header-title {
		font-size: 0.625rem;
		font-weight: 800;
		letter-spacing: 0.15em;
		color: #94a3b8;
		text-transform: uppercase;
		font-family: 'Press Start 2P', system-ui, monospace;
	}
	
	/* Circular progress ring */
	.percentage-ring {
		position: relative;
		width: 90px;
		height: 90px;
		margin: 0 auto 8px;
	}
	
	.ring-svg {
		width: 100%;
		height: 100%;
		filter: drop-shadow(0 0 8px rgba(0, 0, 0, 0.3));
	}
	
	.progress-ring {
		transition: stroke-dashoffset 0.6s ease, stroke 0.3s ease;
		filter: drop-shadow(0 0 6px currentColor);
	}
	
	.threshold-ring {
		filter: drop-shadow(0 0 4px #f59e0b);
	}
	
	.ring-center {
		position: absolute;
		inset: 0;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
	}
	
	.percentage-value {
		font-size: 1.5rem;
		font-weight: 900;
		font-family: 'Press Start 2P', system-ui, monospace;
		line-height: 1;
		text-shadow: 
			0 0 12px currentColor,
			0 2px 4px rgba(0, 0, 0, 0.5);
	}
	
	.percentage-sub {
		font-size: 0.45rem;
		font-weight: 700;
		letter-spacing: 0.05em;
		margin-top: 2px;
		opacity: 0.9;
		font-family: 'Press Start 2P', system-ui, monospace;
	}
	
	/* Threshold display */
	.threshold-display {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 6px;
		padding: 6px 10px;
		background: rgba(0, 0, 0, 0.3);
		border-radius: 6px;
		margin-bottom: 8px;
	}
	
	.threshold-label {
		font-size: 0.65rem;
		font-weight: 600;
		color: #64748b;
		letter-spacing: 0.05em;
	}
	
	.threshold-value {
		font-size: 0.875rem;
		font-weight: 800;
		color: #f59e0b;
		text-shadow: 0 0 6px rgba(245, 158, 11, 0.5);
	}
	
	/* Stats counters */
	.stats-container {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 10px;
		padding: 8px;
		background: linear-gradient(180deg, 
			rgba(0, 0, 0, 0.2) 0%, 
			rgba(0, 0, 0, 0.3) 100%
		);
		border-radius: 8px;
		border: 1px solid rgba(71, 85, 105, 0.3);
	}
	
	.stat-box {
		display: flex;
		align-items: center;
		gap: 6px;
		padding: 4px 8px;
	}
	
	.stat-icon {
		font-size: 1rem;
		font-weight: 700;
	}
	
	.stat-count {
		font-size: 1.125rem;
		font-weight: 800;
		font-family: 'Press Start 2P', system-ui, monospace;
	}
	
	.stat-correct {
		color: #22c55e;
		text-shadow: 0 0 8px rgba(34, 197, 94, 0.5);
	}
	
	.stat-incorrect {
		color: #ef4444;
		text-shadow: 0 0 8px rgba(239, 68, 68, 0.5);
	}
	
	.stat-separator {
		color: #475569;
		font-size: 1.25rem;
		font-weight: 300;
	}
</style>
