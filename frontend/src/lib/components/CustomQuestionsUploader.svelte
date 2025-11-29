<script lang="ts">
    let { onuploadsuccess, onuploaderror } = $props<{
        onuploadsuccess?: (detail: { customTopicName: string }) => void;
        onuploaderror?: (detail: { message: string }) => void;
    }>();

    let fileInput: HTMLInputElement;
    let selectedFile = $state<File | null>(null);
    let isUploading = $state(false);
    let errorMessage = $state<string | null>(null);
    let successMessage = $state<string | null>(null);

    function handleFileSelect(event: Event) {
        const target = event.target as HTMLInputElement;
        if (target.files && target.files.length > 0) {
            selectedFile = target.files[0];
            errorMessage = null;
            successMessage = null;
        }
    }

    async function uploadFile() {
        if (!selectedFile) {
            errorMessage = "Please select a file first.";
            return;
        }

        isUploading = true;
        errorMessage = null;
        successMessage = null;

        const formData = new FormData();
        formData.append('questions', selectedFile);

        try {
            const response = await fetch('http://localhost:7070/api/upload/questions', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Upload failed');
            }

            const data = await response.json();
            successMessage = `Successfully uploaded topic: ${data.customTopicName}`;
            onuploadsuccess?.({ customTopicName: data.customTopicName });
            
            // Clear selection after success
            selectedFile = null;
            if (fileInput) fileInput.value = '';

        } catch (error: any) {
            console.error('Upload error:', error);
            errorMessage = error.message || "An error occurred during upload.";
            onuploaderror?.({ message: errorMessage });
        } finally {
            isUploading = false;
        }
    }
</script>

<div class="uploader-container">
    <h3>Upload Custom Questions</h3>
    
    <div class="file-input-wrapper">
        <input 
            type="file" 
            accept=".csv, .xlsx, .json" 
            bind:this={fileInput}
            onchange={handleFileSelect}
        />
        <p class="help-text">Supported formats: .csv, .xlsx, .json</p>
    </div>

    {#if selectedFile}
        <div class="selected-file">
            <span>Selected: <strong>{selectedFile.name}</strong></span>
            <button onclick={uploadFile} disabled={isUploading}>
                {isUploading ? 'Uploading...' : 'Upload'}
            </button>
        </div>
    {/if}

    {#if errorMessage}
        <div class="error-message">
            {errorMessage}
        </div>
    {/if}

    {#if successMessage}
        <div class="success-message">
            {successMessage}
        </div>
    {/if}
</div>

<style>
    .uploader-container {
        padding: 1.5rem;
        background: rgba(0, 0, 0, 0.8);
        border: 1px solid #444;
        border-radius: 8px;
        color: white;
        max-width: 400px;
        margin: 0 auto;
    }

    h3 {
        margin-top: 0;
        color: #4ecdc4;
    }

    .file-input-wrapper {
        margin-bottom: 1rem;
    }

    input[type="file"] {
        width: 100%;
        padding: 0.5rem;
        background: #222;
        border: 1px solid #555;
        border-radius: 4px;
        color: #ddd;
    }

    .help-text {
        font-size: 0.8rem;
        color: #aaa;
        margin-top: 0.25rem;
    }

    .selected-file {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        margin-bottom: 1rem;
        padding: 0.5rem;
        background: #333;
        border-radius: 4px;
    }

    button {
        background: #ff6b6b;
        color: white;
        border: none;
        padding: 0.5rem 1rem;
        border-radius: 4px;
        cursor: pointer;
        font-weight: bold;
        transition: background 0.2s;
    }

    button:hover:not(:disabled) {
        background: #ff5252;
    }

    button:disabled {
        background: #888;
        cursor: not-allowed;
    }

    .error-message {
        color: #ff6b6b;
        background: rgba(255, 107, 107, 0.1);
        padding: 0.5rem;
        border-radius: 4px;
        margin-top: 0.5rem;
    }

    .success-message {
        color: #4ecdc4;
        background: rgba(78, 205, 196, 0.1);
        padding: 0.5rem;
        border-radius: 4px;
        margin-top: 0.5rem;
    }
</style>
