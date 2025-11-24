<script lang="ts">
	interface Props {
		open?: boolean;
		title?: string;
		message?: string;
		confirmText?: string;
		cancelText?: string;
		isDangerous?: boolean;
		onConfirm?: () => void | Promise<void>;
		onCancel?: () => void;
		isLoading?: boolean;
	}
	
	let { 
		open = false,
		title = "Confirm",
		message = "Are you sure?",
		confirmText = "Yes",
		cancelText = "No",
		isDangerous = false,
		onConfirm,
		onCancel,
		isLoading = false
	}: Props = $props();
	
	let loading = $state(false);
	
	async function handleConfirm() {
		loading = true;
		try {
			if (onConfirm) {
				await onConfirm();
			}
			open = false;
		} finally {
			loading = false;
		}
	}
	
	function handleCancel() {
		if (onCancel) {
			onCancel();
		}
		open = false;
	}
	
	// Handle ESC key to close
	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Escape' && !loading) {
			handleCancel();
		}
	}
</script>

{#if open}
	<!-- Backdrop -->
	<div 
		class="dialog-backdrop" 
		onclick={handleCancel}
		role="button"
		tabindex="-1"
		onkeydown={(e) => e.key === 'Escape' && handleCancel()}
	></div>
	
	<!-- Dialog -->
	<div class="dialog-container" role="alertdialog" tabindex="0" onkeydown={handleKeydown}>
		<div class="dialog-content">
			<h2 class="dialog-title">{title}</h2>
			<p class="dialog-message">{message}</p>
			
			<div class="dialog-actions">
				<button 
					class="btn btn-secondary"
					onclick={handleCancel}
					disabled={loading}
				>
					{cancelText}
				</button>
				<button 
					class="btn {isDangerous ? 'btn-danger' : 'btn-primary'}"
					onclick={handleConfirm}
					disabled={loading}
				>
					{#if loading}
						<span class="spinner"></span>
					{/if}
					{confirmText}
				</button>
			</div>
		</div>
	</div>
{/if}

<style>
	.dialog-backdrop {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		background-color: rgba(0, 0, 0, 0.6);
		backdrop-filter: blur(4px);
		z-index: 40;
		animation: fadeIn 0.2s ease-out;
	}
	
	.dialog-container {
		position: fixed;
		top: 50%;
		left: 50%;
		transform: translate(-50%, -50%);
		z-index: 50;
		animation: slideUp 0.3s ease-out;
		outline: none;
	}
	
	.dialog-content {
		background: #ffffff;
		color: #1f2937;
		border-radius: 16px;
		padding: 32px;
		min-width: 360px;
		max-width: 520px;
		box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25), 0 0 0 1px rgba(0, 0, 0, 0.05);
		border: 2px solid #e5e7eb;
	}
	
	.dialog-title {
		font-size: 1.5rem;
		font-weight: 800;
		margin: 0 0 16px 0;
		color: #111827;
		letter-spacing: -0.5px;
	}
	
	.dialog-message {
		font-size: 1rem;
		line-height: 1.6;
		color: #4b5563;
		margin: 0 0 28px 0;
	}
	
	.dialog-actions {
		display: flex;
		gap: 12px;
		justify-content: flex-end;
	}
	
	.btn {
		padding: 12px 20px;
		border: none;
		border-radius: 8px;
		font-weight: 700;
		font-size: 0.9rem;
		cursor: pointer;
		transition: all 0.2s ease;
		display: flex;
		align-items: center;
		gap: 8px;
		white-space: nowrap;
		text-transform: uppercase;
		letter-spacing: 0.5px;
	}
	
	.btn:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}
	
	.btn-primary {
		background-color: #3b82f6;
		color: #ffffff;
		box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.3);
	}
	
	.btn-primary:hover:not(:disabled) {
		background-color: #2563eb;
		box-shadow: 0 10px 15px -3px rgba(59, 130, 246, 0.4);
		transform: translateY(-2px);
	}
	
	.btn-secondary {
		background-color: #e5e7eb;
		color: #374151;
		border: 2px solid #d1d5db;
	}
	
	.btn-secondary:hover:not(:disabled) {
		background-color: #d1d5db;
		border-color: #9ca3af;
	}
	
	.btn-danger {
		background-color: #ef4444;
		color: #ffffff;
		box-shadow: 0 4px 6px -1px rgba(239, 68, 68, 0.3);
	}
	
	.btn-danger:hover:not(:disabled) {
		background-color: #dc2626;
		box-shadow: 0 10px 15px -3px rgba(239, 68, 68, 0.4);
		transform: translateY(-2px);
	}
	
	.spinner {
		display: inline-block;
		width: 14px;
		height: 14px;
		border: 2px solid currentColor;
		border-radius: 50%;
		border-top-color: transparent;
		animation: spin 0.6s linear infinite;
	}
	
	@keyframes fadeIn {
		0% { opacity: 0; }
		100% { opacity: 1; }
	}
	
	@keyframes slideUp {
		0% {
			opacity: 0;
			transform: translate(-50%, -40%);
		}
		100% {
			opacity: 1;
			transform: translate(-50%, -50%);
		}
	}
	
	@keyframes spin {
		0% { transform: rotate(0deg); }
		100% { transform: rotate(360deg); }
	}
</style>
