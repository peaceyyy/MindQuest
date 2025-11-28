<script lang="ts">
    import CustomQuestionsUploader from './CustomQuestionsUploader.svelte';
    import { createEventDispatcher } from 'svelte';

    const dispatch = createEventDispatcher();

    function close() {
        dispatch('close');
    }

    function handleUploadSuccess(event: CustomEvent) {
        dispatch('uploadSuccess', event.detail);
    }
</script>

<!-- Modal Overlay -->
<div class="modal-overlay" on:click={close}>
    <!-- Modal Content -->
    <div class="modal-content" on:click|stopPropagation>
        <button class="close-button" on:click={close}>&times;</button>
        <CustomQuestionsUploader 
            on:uploadSuccess={handleUploadSuccess}
        />
    </div>
</div>

<style>
    .modal-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.7);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 1000;
        backdrop-filter: blur(4px);
    }

    .modal-content {
        position: relative;
        background: transparent;
        padding: 0;
        border-radius: 8px;
        max-width: 90%;
        max-height: 90vh;
        overflow-y: auto;
    }

    .close-button {
        position: absolute;
        top: -10px;
        right: -10px;
        background: #ff6b6b;
        color: white;
        border: none;
        border-radius: 50%;
        width: 30px;
        height: 30px;
        font-size: 1.2rem;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1001;
        box-shadow: 0 2px 4px rgba(0,0,0,0.3);
    }

    .close-button:hover {
        background: #ff5252;
    }
</style>
