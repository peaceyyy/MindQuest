<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	
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
	let portalContainer: HTMLDivElement | null = null;
	
	// Create portal container on mount
	onMount(() => {
		portalContainer = document.createElement('div');
		portalContainer.id = 'confirm-dialog-portal';
		document.body.appendChild(portalContainer);
	});
	
	// Clean up portal on destroy
	onDestroy(() => {
		if (portalContainer && document.body.contains(portalContainer)) {
			document.body.removeChild(portalContainer);
		}
	});
	
	async function handleConfirm() {
		loading = true;
		try {
			if (onConfirm) {
				await onConfirm();
			}
		} finally {
			loading = false;
		}
	}
	
	function handleCancel() {
		if (onCancel) {
			onCancel();
		}
	}
	
	// Handle ESC key to close
	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Escape' && !loading) {
			handleCancel();
		}
	}
	
	// Teleport effect - render dialog in body when open
	$effect(() => {
		if (!portalContainer) return;
		
		if (open) {
			portalContainer.innerHTML = '';
			
			// Create backdrop
			const backdrop = document.createElement('div');
			backdrop.className = 'dialog-backdrop-portal';
			backdrop.onclick = handleCancel;
			
			// Create dialog container
			const container = document.createElement('div');
			container.className = 'dialog-container-portal';
			container.setAttribute('role', 'alertdialog');
			container.tabIndex = 0;
			container.onkeydown = handleKeydown;
			
			// Create dialog content
			container.innerHTML = `
				<div class="dialog-content-portal">
					<h2 class="dialog-title-portal">${title}</h2>
					<p class="dialog-message-portal">${message}</p>
					<div class="dialog-actions-portal">
						<button class="btn-portal btn-secondary-portal" id="dialog-cancel">${cancelText}</button>
						<button class="btn-portal ${isDangerous ? 'btn-danger-portal' : 'btn-primary-portal'}" id="dialog-confirm">
							${loading ? '<span class="spinner-portal"></span>' : ''}${confirmText}
						</button>
					</div>
				</div>
			`;
			
			portalContainer.appendChild(backdrop);
			portalContainer.appendChild(container);
			
			// Attach event listeners
			const cancelBtn = container.querySelector('#dialog-cancel');
			const confirmBtn = container.querySelector('#dialog-confirm');
			if (cancelBtn) cancelBtn.addEventListener('click', handleCancel);
			if (confirmBtn) confirmBtn.addEventListener('click', handleConfirm);
			
			container.focus();
		} else {
			portalContainer.innerHTML = '';
		}
	});
</script>

<!-- Inject global styles for portal -->
<svelte:head>
	<style>
		.dialog-backdrop-portal {
			position: fixed;
			top: 0;
			left: 0;
			right: 0;
			bottom: 0;
			width: 100%;
			height: 100%;
			background-color: rgba(0, 0, 0, 0.6);
			backdrop-filter: blur(4px);
			z-index: 99998;
			animation: fadeInPortal 0.2s ease-out;
		}
		
		.dialog-container-portal {
			position: fixed;
			top: 50%;
			left: 50%;
			transform: translate(-50%, -50%);
			z-index: 99999;
			animation: slideUpPortal 0.3s ease-out;
			outline: none;
		}
		
		.dialog-content-portal {
			background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
			color: #e2e8f0;
			border-radius: 16px;
			padding: 24px;
			min-width: 320px;
			max-width: 420px;
			box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5), 0 0 0 2px rgba(71, 85, 105, 0.5);
			border: 2px solid #334155;
			font-family: system-ui, sans-serif;
		}
		
		.dialog-title-portal {
			font-size: 1.25rem;
			font-weight: 800;
			margin: 0 0 12px 0;
			color: #f1f5f9;
			font-family: 'Press Start 2P', system-ui, monospace;
			font-size: 0.875rem;
			letter-spacing: 0.05em;
		}
		
		.dialog-message-portal {
			font-size: 0.9rem;
			line-height: 1.6;
			color: #94a3b8;
			margin: 0 0 24px 0;
		}
		
		.dialog-actions-portal {
			display: flex;
			gap: 12px;
			justify-content: flex-end;
		}
		
		.btn-portal {
			padding: 10px 18px;
			border: none;
			border-radius: 8px;
			font-weight: 700;
			font-size: 0.8rem;
			cursor: pointer;
			transition: all 0.2s ease;
			display: flex;
			align-items: center;
			gap: 8px;
			white-space: nowrap;
			text-transform: uppercase;
			letter-spacing: 0.5px;
			font-family: 'Press Start 2P', system-ui, monospace;
			font-size: 0.5rem;
		}
		
		.btn-portal:disabled {
			opacity: 0.6;
			cursor: not-allowed;
		}
		
		.btn-primary-portal {
			background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
			color: #ffffff;
			box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.3);
			border: 2px solid #60a5fa;
		}
		
		.btn-primary-portal:hover:not(:disabled) {
			background: linear-gradient(180deg, #60a5fa 0%, #3b82f6 100%);
			box-shadow: 0 6px 15px -3px rgba(59, 130, 246, 0.5);
			transform: translateY(-2px);
		}
		
		.btn-secondary-portal {
			background: linear-gradient(180deg, #475569 0%, #334155 100%);
			color: #e2e8f0;
			border: 2px solid #64748b;
		}
		
		.btn-secondary-portal:hover:not(:disabled) {
			background: linear-gradient(180deg, #64748b 0%, #475569 100%);
		}
		
		.btn-danger-portal {
			background: linear-gradient(180deg, #ef4444 0%, #dc2626 100%);
			color: #ffffff;
			box-shadow: 0 4px 6px -1px rgba(239, 68, 68, 0.3);
			border: 2px solid #f87171;
		}
		
		.btn-danger-portal:hover:not(:disabled) {
			background: linear-gradient(180deg, #f87171 0%, #ef4444 100%);
			box-shadow: 0 6px 15px -3px rgba(239, 68, 68, 0.5);
			transform: translateY(-2px);
		}
		
		.spinner-portal {
			display: inline-block;
			width: 12px;
			height: 12px;
			border: 2px solid currentColor;
			border-radius: 50%;
			border-top-color: transparent;
			animation: spinPortal 0.6s linear infinite;
		}
		
		@keyframes fadeInPortal {
			0% { opacity: 0; }
			100% { opacity: 1; }
		}
		
		@keyframes slideUpPortal {
			0% {
				opacity: 0;
				transform: translate(-50%, -40%);
			}
			100% {
				opacity: 1;
				transform: translate(-50%, -50%);
			}
		}
		
		@keyframes spinPortal {
			0% { transform: rotate(0deg); }
			100% { transform: rotate(360deg); }
		}
	</style>
</svelte:head>
