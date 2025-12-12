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
		filter: var(--drop-shadow-lg);
		margin-bottom: var(--spacing-lg);
		animation: dialogue-entrance 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
	}
	
	@keyframes dialogue-entrance {
		from {
			opacity: 0;
			transform: translateY(20px) scale(0.95);
		}
		to {
			opacity: 1;
			transform: translateY(0) scale(1);
		}
	}
	
	.dialogue-frame {
		position: relative;
		background: var(--bg-card-primary);
		border: 4px solid transparent;
		border-radius: var(--radius-lg);
		padding: var(--spacing-xs);
		
		/* Gradient border effect */
		background-clip: padding-box;
		
		/* Outer border glow with pulse */
		box-shadow: 
			0 0 0 4px var(--color-slate-600),
			var(--glow-blue-subtle),
			inset 0 1px 0 rgba(255, 255, 255, 0.1);
		animation: border-pulse 2s ease-in-out infinite;
	}
	
	@keyframes border-pulse {
		0%, 100% {
			box-shadow: 
				0 0 0 4px var(--color-slate-600),
				0 0 15px rgba(59, 130, 246, 0.15),
				inset 0 1px 0 rgba(255, 255, 255, 0.1);
		}
		50% {
			box-shadow: 
				0 0 0 4px rgba(96, 165, 250, 0.8),
				0 0 25px rgba(59, 130, 246, 0.4),
				inset 0 1px 0 rgba(255, 255, 255, 0.15);
		}
	}
	
	.dialogue-frame::before {
		content: '';
		position: absolute;
		inset: -4px;
		border-radius: 14px;
		background: var(--bg-border-shimmer);
		z-index: -1;
	}
	
	/* Pixel-art corner decorations */
	.corner {
		position: absolute;
		width: 8px;
		height: 8px;
		background: var(--bg-button-primary);
		border-radius: var(--radius-sm);
		box-shadow: var(--glow-blue);
		z-index: 10;
		animation: corner-pulse 1.5s ease-in-out infinite;
	}
	
	@keyframes corner-pulse {
		0%, 100% {
			transform: scale(1);
			box-shadow: 0 0 6px rgba(59, 130, 246, 0.6);
		}
		50% {
			transform: scale(1.3);
			box-shadow: 0 0 15px rgba(59, 130, 246, 1), 0 0 30px rgba(96, 165, 250, 0.5);
		}
	}
	
	.corner-tl { top: 6px; left: 6px; animation-delay: 0s; }
	.corner-tr { top: 6px; right: 6px; animation-delay: 0.2s; }
	.corner-bl { bottom: 6px; left: 6px; animation-delay: 0.4s; }
	.corner-br { bottom: 6px; right: 6px; animation-delay: 0.6s; }
	
	.dialogue-content {
		background: var(--bg-light);
		border-radius: var(--radius-md);
		padding: var(--spacing-xl) var(--spacing-2xl);
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
		color: var(--color-slate-900);
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
		background: var(--color-slate-300-alpha);
		border-radius: var(--radius-md);
		margin: var(--spacing-md) 0;
	}
	
	.custom-scrollbar::-webkit-scrollbar-thumb {
		background: var(--bg-scrollbar);
		border-radius: var(--radius-md);
		border: 2px solid var(--color-slate-50-alpha);
	}
	
	.custom-scrollbar::-webkit-scrollbar-thumb:hover {
		background: var(--bg-scrollbar-hover);
	}
</style>
