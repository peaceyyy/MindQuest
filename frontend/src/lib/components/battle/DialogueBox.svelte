<script lang="ts">
	let { text } = $props();
</script>

<div class="dialogue-box">
	<!-- Outer glow frame -->
	<div class="dialogue-frame">
		<!-- Corner decorations (pixel art style) -->
		<div class="corner corner-tl"></div>
		<div class="corner corner-tr"></div>
		<div class="corner corner-bl"></div>
		<div class="corner corner-br"></div>
		
		<!-- Inner content area -->
		<div class="dialogue-content custom-scrollbar">
			<p class="dialogue-text">
				{text}
			</p>
		</div>
	</div>
</div>

<style>
	.dialogue-box {
		position: relative;
		filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.4));
		margin-bottom: 16px;
	}
	
	.dialogue-frame {
		position: relative;
		background: linear-gradient(180deg, 
			rgba(15, 23, 42, 0.98) 0%, 
			rgba(30, 41, 59, 0.98) 100%
		);
		border: 4px solid transparent;
		border-radius: 12px;
		padding: 4px;
		
		/* Gradient border effect */
		background-clip: padding-box;
		
		/* Outer border glow */
		box-shadow: 
			0 0 0 4px rgba(71, 85, 105, 0.8),
			0 0 20px rgba(59, 130, 246, 0.15),
			inset 0 1px 0 rgba(255, 255, 255, 0.1);
	}
	
	.dialogue-frame::before {
		content: '';
		position: absolute;
		inset: -4px;
		border-radius: 14px;
		background: linear-gradient(135deg, 
			rgba(148, 163, 184, 0.6) 0%, 
			rgba(71, 85, 105, 0.4) 50%, 
			rgba(148, 163, 184, 0.6) 100%
		);
		z-index: -1;
	}
	
	/* Pixel-art corner decorations */
	.corner {
		position: absolute;
		width: 8px;
		height: 8px;
		background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
		border-radius: 2px;
		box-shadow: 0 0 6px rgba(59, 130, 246, 0.6);
		z-index: 10;
	}
	
	.corner-tl { top: 6px; left: 6px; }
	.corner-tr { top: 6px; right: 6px; }
	.corner-bl { bottom: 6px; left: 6px; }
	.corner-br { bottom: 6px; right: 6px; }
	
	.dialogue-content {
		background: linear-gradient(180deg, 
			rgba(241, 245, 249, 0.98) 0%, 
			rgba(226, 232, 240, 0.98) 100%
		);
		border-radius: 8px;
		padding: 20px 24px;
		min-height: 100px;
		max-height: 160px;
		overflow-y: auto;
		
		/* Inner shadow for depth */
		box-shadow: 
			inset 0 2px 4px rgba(0, 0, 0, 0.1),
			inset 0 -1px 0 rgba(255, 255, 255, 0.8);
	}
	
	.dialogue-text {
		font-size: 0.95rem;
		line-height: 1.6;
		font-weight: 500;
		color: #1e293b;
		white-space: pre-line;
		font-family: 'Segoe UI', system-ui, sans-serif;
	}
	
	@media (min-width: 768px) {
		.dialogue-text {
			font-size: 1.05rem;
		}
	}
	
	/* Custom scrollbar styling */
	.custom-scrollbar::-webkit-scrollbar {
		width: 10px;
	}
	
	.custom-scrollbar::-webkit-scrollbar-track {
		background: rgba(148, 163, 184, 0.3);
		border-radius: 5px;
		margin: 8px 0;
	}
	
	.custom-scrollbar::-webkit-scrollbar-thumb {
		background: linear-gradient(180deg, #64748b 0%, #475569 100%);
		border-radius: 5px;
		border: 2px solid rgba(241, 245, 249, 0.5);
	}
	
	.custom-scrollbar::-webkit-scrollbar-thumb:hover {
		background: linear-gradient(180deg, #475569 0%, #334155 100%);
	}
</style>
