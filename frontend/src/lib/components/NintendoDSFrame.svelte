<script lang="ts">
	/**
	 * NintendoDSFrame.svelte
	 * A CSS-based handheld gaming device frame (inspired by DS top screen).
	 * The content is placed INSIDE the screen area - the frame hugs around it.
	 * Designed for vertical/portrait orientation like a phone or tablet.
	 */
	
	let { children } = $props();
</script>

<div class="device-wrapper">
	<!-- The Device Shell -->
	<div class="device-shell">
		<!-- Top bezel with speaker grilles -->
		<div class="bezel-top">
			<div class="speaker-grille"></div>
			<div class="speaker-grille"></div>
		</div>
		
		<!-- Left side rail with D-Pad -->
		<div class="side-rail side-rail-left">
			<div class="ds-dpad">
				<div class="dpad-vertical"></div>
				<div class="dpad-horizontal"></div>
				<div class="dpad-center"></div>
			</div>
		</div>
		
		<!-- THE SCREEN - Content goes here -->
		<div class="screen-viewport">
			{@render children()}
		</div>
		
		<!-- Right side rail with face buttons -->
		<div class="side-rail side-rail-right">
			<div class="ds-face-buttons">
				<div class="ds-btn ds-btn-x">X</div>
				<div class="ds-btn ds-btn-y">Y</div>
				<div class="ds-btn ds-btn-a">A</div>
				<div class="ds-btn ds-btn-b">B</div>
			</div>
		</div>
		
		<!-- Bottom bezel -->
		<div class="bezel-bottom"></div>
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
		align-items: stretch;
		padding: 12px;
		box-sizing: border-box;
		overflow: hidden;
		/* Dark background visible around the device */
		background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f0f23 100%);
	}
	
	/* ===== The Device Shell - Uses CSS Grid ===== */
	.device-shell {
		display: grid;
		grid-template-areas:
			"top    top    top"
			"left   screen right"
			"bottom bottom bottom";
		/* Fixed column widths: rails are fixed, screen is fixed */
		grid-template-columns: 50px minmax(0, 800px) 50px;
		grid-template-rows: 16px 1fr 16px;
		
		/* Fixed width for the whole device */
		width: 900px;
		max-width: calc(100vw - 24px);
		height: 100%;
		max-height: calc(100vh - 24px);
		
		/* The red plastic shell */
		background: linear-gradient(180deg, #dc2626 0%, #b91c1c 50%, #991b1b 100%);
		border-radius: 24px;
		
		/* Outer shadow for depth */
		box-shadow: 
			0 8px 32px rgba(0, 0, 0, 0.4),
			0 2px 8px rgba(0, 0, 0, 0.2),
			inset 0 1px 0 rgba(255, 255, 255, 0.15);
	}
	
	/* ===== Top Bezel ===== */
	.bezel-top {
		grid-area: top;
		display: flex;
		justify-content: center;
		align-items: center;
		gap: 60px;
		padding: 0 20px;
	}
	
	.speaker-grille {
		width: 40px;
		height: 6px;
		background: repeating-linear-gradient(
			90deg,
			#1f2937 0px,
			#1f2937 2px,
			#991b1b 2px,
			#991b1b 4px
		);
		border-radius: 3px;
		opacity: 0.6;
	}
	
	/* ===== Bottom Bezel ===== */
	.bezel-bottom {
		grid-area: bottom;
		background: linear-gradient(180deg, #991b1b 0%, #7f1d1d 100%);
		border-radius: 0 0 20px 20px;
	}
	
	/* ===== Side Rails ===== */
	.side-rail {
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 20px 8px;
	}
	
	.side-rail-left {
		grid-area: left;
		border-radius: 20px 0 0 20px;
	}
	
	.side-rail-right {
		grid-area: right;
		border-radius: 0 20px 20px 0;
	}
	
	/* ===== D-Pad ===== */
	.ds-dpad {
		position: relative;
		width: 36px;
		height: 36px;
	}
	
	.dpad-vertical,
	.dpad-horizontal {
		position: absolute;
		background: linear-gradient(180deg, #374151 0%, #1f2937 100%);
		border-radius: 3px;
		box-shadow: 
			inset 0 1px 0 rgba(255, 255, 255, 0.1),
			0 2px 3px rgba(0, 0, 0, 0.4);
	}
	
	.dpad-vertical {
		width: 12px;
		height: 36px;
		left: 50%;
		transform: translateX(-50%);
	}
	
	.dpad-horizontal {
		width: 36px;
		height: 12px;
		top: 50%;
		transform: translateY(-50%);
	}
	
	.dpad-center {
		position: absolute;
		width: 10px;
		height: 10px;
		background: #1f2937;
		border-radius: 50%;
		top: 50%;
		left: 50%;
		transform: translate(-50%, -50%);
		box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.6);
	}
	
	/* ===== Face Buttons ===== */
	.ds-face-buttons {
		position: relative;
		width: 36px;
		height: 36px;
	}
	
	.ds-btn {
		position: absolute;
		width: 14px;
		height: 14px;
		border-radius: 50%;
		font-size: 7px;
		font-weight: bold;
		color: #9ca3af;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 
			0 2px 3px rgba(0, 0, 0, 0.4),
			inset 0 1px 0 rgba(255, 255, 255, 0.15);
		user-select: none;
	}
	
	.ds-btn-x {
		top: 0;
		left: 50%;
		transform: translateX(-50%);
		background: linear-gradient(180deg, #6b7280 0%, #4b5563 100%);
	}
	
	.ds-btn-y {
		top: 50%;
		left: 0;
		transform: translateY(-50%);
		background: linear-gradient(180deg, #6b7280 0%, #4b5563 100%);
	}
	
	.ds-btn-a {
		top: 50%;
		right: 0;
		transform: translateY(-50%);
		background: linear-gradient(180deg, #dc2626 0%, #b91c1c 100%);
		color: #fecaca;
	}
	
	.ds-btn-b {
		bottom: 0;
		left: 50%;
		transform: translateX(-50%);
		background: linear-gradient(180deg, #6b7280 0%, #4b5563 100%);
	}
	
	/* ===== THE SCREEN - Where content lives ===== */
	.screen-viewport {
		grid-area: screen;
		background: #1a1a2e;
		border-radius: 8px;
		overflow: auto;
		
		/* Fixed width for consistent layout - prevents content from causing width shifts */
		width: 100%;
		min-width: 0; /* Allow shrinking within grid */
		
		/* Flex container to let content fill properly */
		display: flex;
		flex-direction: column;
		
		/* Inner shadow to look recessed */
		box-shadow: 
			inset 0 2px 8px rgba(0, 0, 0, 0.5),
			inset 0 0 2px rgba(0, 0, 0, 0.3);
		
		/* Subtle screen bezel */
		border: 3px solid #111827;
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
	
	/* ===== Responsive: Hide frame on small screens ===== */
	@media (max-width: 700px) {
		.device-wrapper {
			padding: 0;
			background: #1a1a2e;
			max-height: none;
			height: auto;
			min-height: 100vh;
			overflow: auto;
		}
		
		.device-shell {
			display: block;
			background: transparent;
			box-shadow: none;
			border-radius: 0;
			max-width: 100%;
			max-height: none;
			height: auto;
		}
		
		.bezel-top,
		.bezel-bottom,
		.side-rail {
			display: none;
		}
		
		.screen-viewport {
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
