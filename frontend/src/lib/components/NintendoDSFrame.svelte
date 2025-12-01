<script lang="ts">
	/**
	 * NintendoDSFrame.svelte
	 * A minimal DS-inspired frame showing just the top half of the device.
	 * As if looking down at a DS from above - clean and focused on the screen.
	 */
	
	let { children } = $props();
</script>

<div class="device-wrapper">
	<!-- The Device Shell - Top half only -->
	<div class="device-shell">
		<!-- Top bezel with speaker grilles and power LED -->
		<div class="bezel-top">
			<div class="power-led"></div>
			<div class="speaker-grille"></div>
			<div class="camera-dot"></div>
			<div class="speaker-grille"></div>
			<div class="wifi-led"></div>
		</div>
		
		<!-- THE SCREEN - Content goes here -->
		<div class="screen-viewport">
			{@render children()}
		</div>
		
		<!-- Hinge area - the "cut off" bottom edge -->
		<div class="hinge-edge">
			<div class="hinge-detail"></div>
			<div class="hinge-groove"></div>
			<div class="hinge-detail"></div>
		</div>
	</div>
</div>

<style>
	/* ===== Wrapper - Full viewport background ===== */
	.device-wrapper {
		min-height: 100vh;
		max-height: 100vh;
		height: 100vh;
		width: 100%;
		display: flex;
		justify-content: center;
		align-items: flex-start;
		padding: 0;
		box-sizing: border-box;
		overflow: hidden;
		/* Dark background visible around the device */
		background: linear-gradient(180deg, #0a0a12 0%, #12121f 50%, #0f0f1a 100%);
	}
	
	/* ===== The Device Shell - Simple vertical layout ===== */
	.device-shell {
		display: flex;
		flex-direction: column;
		
		/* Width and height */
		width: 100%;
		max-width: 900px;
		height: 100%;
		
		/* The red plastic shell */
		background: linear-gradient(180deg, #dc2626 0%, #c92020 30%, #b91c1c 100%);
		
		/* Rounded top corners only - bottom is "cut off" at hinge */
		border-radius: 20px 20px 0 0;
		
		/* Outer shadow for depth */
		box-shadow: 
			0 8px 32px rgba(0, 0, 0, 0.5),
			0 2px 8px rgba(0, 0, 0, 0.3),
			inset 0 1px 0 rgba(255, 255, 255, 0.12),
			inset -2px 0 0 rgba(0, 0, 0, 0.1),
			inset 2px 0 0 rgba(255, 255, 255, 0.05);
	}
	
	/* ===== Top Bezel ===== */
	.bezel-top {
		display: flex;
		justify-content: center;
		align-items: center;
		gap: 20px;
		padding: 10px 20px;
		flex-shrink: 0;
	}
	
	.speaker-grille {
		width: 50px;
		height: 6px;
		background: repeating-linear-gradient(
			90deg,
			#1f2937 0px,
			#1f2937 2px,
			#991b1b 2px,
			#991b1b 4px
		);
		border-radius: 3px;
		opacity: 0.7;
		box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.4);
	}
	
	.power-led {
		width: 6px;
		height: 6px;
		background: radial-gradient(circle, #22c55e 0%, #16a34a 60%, #15803d 100%);
		border-radius: 50%;
		box-shadow: 
			0 0 6px rgba(34, 197, 94, 0.6),
			0 0 2px rgba(34, 197, 94, 0.8);
		animation: led-pulse 3s ease-in-out infinite;
	}
	
	.wifi-led {
		width: 5px;
		height: 5px;
		background: radial-gradient(circle, #f59e0b 0%, #d97706 60%, #b45309 100%);
		border-radius: 50%;
		box-shadow: 0 0 4px rgba(245, 158, 11, 0.5);
		opacity: 0.8;
	}
	
	.camera-dot {
		width: 8px;
		height: 8px;
		background: radial-gradient(circle, #1f2937 0%, #111827 100%);
		border-radius: 50%;
		box-shadow: 
			inset 0 1px 2px rgba(0, 0, 0, 0.8),
			0 0 0 1px rgba(0, 0, 0, 0.3);
	}
	
	@keyframes led-pulse {
		0%, 100% { opacity: 1; }
		50% { opacity: 0.6; }
	}
	
	/* ===== THE SCREEN - Where content lives ===== */
	.screen-viewport {
		flex: 1;
		margin: 0 12px;
		background: #0a0a14;
		border-radius: 6px;
		overflow: auto;
		
		/* Fixed width for consistent layout */
		width: calc(100% - 24px);
		min-width: 0;
		min-height: 0;
		
		/* Flex container to let content fill properly */
		display: flex;
		flex-direction: column;
		
		/* Inner shadow to look recessed like a real screen */
		box-shadow: 
			inset 0 3px 12px rgba(0, 0, 0, 0.7),
			inset 0 0 4px rgba(0, 0, 0, 0.5),
			0 -1px 0 rgba(255, 255, 255, 0.05);
		
		/* Dark screen bezel */
		border: 3px solid #0f0f1a;
	}
	
	/* Ensure content inside the screen fills it properly and doesn't overflow */
	.screen-viewport > :global(*) {
		flex: 1;
		min-height: 0;
		min-width: 0;
		width: 100%;
		max-width: 100%;
		overflow: auto;
	}
	
	/* ===== Hinge Edge - The "cut off" bottom ===== */
	.hinge-edge {
		display: flex;
		justify-content: center;
		align-items: center;
		gap: 12px;
		padding: 8px 20px 10px;
		flex-shrink: 0;
		background: linear-gradient(180deg, #991b1b 0%, #7f1d1d 100%);
		border-top: 1px solid rgba(0, 0, 0, 0.2);
	}
	
	.hinge-detail {
		width: 60px;
		height: 4px;
		background: linear-gradient(180deg, #6b7280 0%, #4b5563 100%);
		border-radius: 2px;
		box-shadow: 
			inset 0 1px 0 rgba(255, 255, 255, 0.2),
			0 1px 2px rgba(0, 0, 0, 0.4);
	}
	
	.hinge-groove {
		width: 120px;
		height: 6px;
		background: linear-gradient(180deg, #1f2937 0%, #111827 100%);
		border-radius: 3px;
		box-shadow: 
			inset 0 2px 4px rgba(0, 0, 0, 0.6),
			0 1px 0 rgba(255, 255, 255, 0.1);
	}
	
	/* ===== Responsive: Simplify on small screens ===== */
	@media (max-width: 700px) {
		.device-wrapper {
			padding: 0;
			background: #0a0a14;
			max-height: none;
			height: auto;
			min-height: 100vh;
			overflow: auto;
		}
		
		.device-shell {
			background: transparent;
			box-shadow: none;
			border-radius: 0;
			max-width: 100%;
			max-height: none;
			height: auto;
		}
		
		.bezel-top,
		.hinge-edge {
			display: none;
		}
		
		.screen-viewport {
			margin: 0;
			width: 100%;
			border-radius: 0;
			border: none;
			box-shadow: none;
			min-height: 100vh;
			overflow: visible;
		}
		
		.screen-viewport > :global(*) {
			overflow: visible;
		}
	}
</style>
