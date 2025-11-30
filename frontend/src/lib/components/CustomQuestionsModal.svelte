<script lang="ts">
    import CustomQuestionsUploader from './CustomQuestionsUploader.svelte';
    import { onMount } from 'svelte';

    let { onclose, ontopicselect } = $props<{
        onclose: () => void;
        ontopicselect: (topicName: string) => void;
    }>();

    type TopicItem = {
        name: string;
        type: 'csv' | 'xlsx' | 'json';
    };

    let customTopics = $state<TopicItem[]>([]);
    let loading = $state(true);
    let error = $state<string | null>(null);
    let selectedFilter = $state<'all' | 'csv' | 'xlsx' | 'json'>('all');

    onMount(async () => {
        await loadCustomTopics();
    });

    // Filter computed derived state
    let filteredTopics = $derived(
        selectedFilter === 'all' 
            ? customTopics 
            : customTopics.filter(t => t.type === selectedFilter)
    );

    // Helper functions for type display
    function getTypeLabel(type: 'csv' | 'xlsx' | 'json'): string {
        return type.toUpperCase();
    }

    function getTypeColor(type: 'csv' | 'xlsx' | 'json'): string {
        switch(type) {
            case 'csv': return '#10b981'; // green
            case 'xlsx': return '#3b82f6'; // blue
            case 'json': return '#f59e0b'; // amber
            default: return '#6b7280'; // gray
        }
    }

    async function loadCustomTopics() {
        loading = true;
        error = null;
        try {
            // Fetch CSV topics
            const csvRes = await fetch('http://localhost:7070/api/debug/list-external?type=csv');
            const csvData = await csvRes.json();
            
            // Fetch XLSX topics
            const xlsxRes = await fetch('http://localhost:7070/api/debug/list-external?type=xlsx');
            const xlsxData = await xlsxRes.json();
            
            // Fetch JSON topics
            const jsonRes = await fetch('http://localhost:7070/api/debug/list-external?type=json');
            const jsonData = await jsonRes.json();
            
            // Build topics with type info
            const topics: TopicItem[] = [
                ...(csvData.topics || []).map((name: string) => ({ name, type: 'csv' as const })),
                ...(xlsxData.topics || []).map((name: string) => ({ name, type: 'xlsx' as const })),
                ...(jsonData.topics || []).map((name: string) => ({ name, type: 'json' as const }))
            ];
            
            // Sort by name
            customTopics = topics.sort((a, b) => a.name.localeCompare(b.name));
        } catch (err: any) {
            console.error('Failed to load custom topics:', err);
            error = 'Failed to load custom question sets. Make sure the server is running.';
        } finally {
            loading = false;
        }
    }

    function close() {
        onclose();
    }

    function selectTopic(topicName: string) {
        ontopicselect(topicName);
    }

    async function handleUploadSuccess(detail: { customTopicName: string }) {
        // Reload the list after successful upload
        await loadCustomTopics();
    }

</script>

<!-- Modal Overlay -->
<div class="modal-overlay" onclick={close} role="button" tabindex="-1" onkeydown={(e) => e.key === 'Escape' && close()}>
    <!-- Modal Content -->
    <div class="modal-content" onclick={(e) => e.stopPropagation()} onkeydown={() => {}} role="dialog" tabindex="-1">
        <button class="close-button" onclick={close}>&times;</button>
        
        <div class="modal-inner">
            <h2 class="modal-title">Custom Question Sets</h2>
            
            <!-- Upload Section -->
            <div class="upload-section">
                <CustomQuestionsUploader 
                    onuploadsuccess={handleUploadSuccess}
                />
            </div>
            
            <!-- Divider -->
            <div class="divider">
                <span>Available Question Sets</span>
            </div>

            <!-- Filter Buttons -->
            <div class="filter-section">
                <button 
                    class="filter-btn {selectedFilter === 'all' ? 'active' : ''}"
                    onclick={() => selectedFilter = 'all'}
                >
                    All ({customTopics.length})
                </button>
                <button 
                    class="filter-btn {selectedFilter === 'csv' ? 'active' : ''}"
                    onclick={() => selectedFilter = 'csv'}
                >
                    CSV ({customTopics.filter(t => t.type === 'csv').length})
                </button>
                <button 
                    class="filter-btn {selectedFilter === 'xlsx' ? 'active' : ''}"
                    onclick={() => selectedFilter = 'xlsx'}
                >
                    XLSX ({customTopics.filter(t => t.type === 'xlsx').length})
                </button>
                <button 
                    class="filter-btn {selectedFilter === 'json' ? 'active' : ''}"
                    onclick={() => selectedFilter = 'json'}
                >
                    JSON ({customTopics.filter(t => t.type === 'json').length})
                </button>
            </div>
            
            <!-- Topic List Section -->
            <div class="topics-section">
                {#if loading}
                    <div class="loading-state">
                        <div class="spinner"></div>
                        <p>Loading custom question sets...</p>
                    </div>
                {:else if error}
                    <div class="error-state">
                        <p>{error}</p>
                        <button onclick={loadCustomTopics} class="retry-button">Retry</button>
                    </div>
                {:else if customTopics.length === 0}
                    <div class="empty-state">
                        <p>No custom question sets found.</p>
                        <p class="hint">Upload a file above to get started!</p>
                    </div>
                {:else if filteredTopics.length === 0}
                    <div class="empty-state">
                        <p>No {selectedFilter.toUpperCase()} files found.</p>
                        <p class="hint">Try a different filter or upload a file above.</p>
                    </div>
                {:else}
                    <div class="topics-grid">
                        {#each filteredTopics as topic}
                            <button
                                class="topic-card"
                                onclick={() => selectTopic(topic.name)}
                            >
                                <div class="topic-info">
                                    <div class="topic-name">{topic.name.toUpperCase()}</div>
                                    <span class="type-badge" style="background-color: {getTypeColor(topic.type)}">
                                        {getTypeLabel(topic.type)}
                                    </span>
                                </div>
                                <div class="topic-action">Select →</div>
                            </button>
                        {/each}
                    </div>
                {/if}
            </div>
        </div>
    </div>
</div>

<style>
    .modal-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.85);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 1000;
        backdrop-filter: blur(8px);
        animation: fadeIn 0.2s ease-out;
    }

    @keyframes fadeIn {
        from { opacity: 0; }
        to { opacity: 1; }
    }

    .modal-content {
        position: relative;
        background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
        border: 4px solid #4a5568;
        border-radius: 16px;
        max-width: 600px;
        width: 90%;
        max-height: 85vh;
        overflow-y: auto;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
        animation: slideUp 0.3s ease-out;
    }

    @keyframes slideUp {
        from {
            opacity: 0;
            transform: translateY(30px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }

    .modal-inner {
        padding: 2rem;
    }

    .close-button {
        position: absolute;
        top: -12px;
        right: -12px;
        background: #ff6b6b;
        color: white;
        border: 3px solid white;
        border-radius: 50%;
        width: 40px;
        height: 40px;
        font-size: 1.5rem;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1001;
        box-shadow: 0 4px 8px rgba(0,0,0,0.4);
        transition: all 0.2s;
    }

    .close-button:hover {
        background: #ff5252;
        transform: scale(1.1);
    }

    .modal-title {
        margin: 0 0 1.5rem 0;
        color: #fbbf24;
        font-size: 1.75rem;
        font-weight: 900;
        text-align: center;
        text-transform: uppercase;
        letter-spacing: 0.1em;
        text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
    }

    .upload-section {
        margin-bottom: 1.5rem;
    }

    .divider {
        position: relative;
        text-align: center;
        margin: 2rem 0 1.5rem;
        color: #9ca3af;
        font-size: 0.875rem;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.05em;
    }

    .divider::before,
    .divider::after {
        content: '';
        position: absolute;
        top: 50%;
        width: 40%;
        height: 2px;
        background: linear-gradient(90deg, transparent, #4a5568, transparent);
    }

    .divider::before {
        left: 0;
    }

    .divider::after {
        right: 0;
    }

    .divider span {
        background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
        padding: 0 1rem;
        position: relative;
        z-index: 1;
    }

    .topics-section {
        min-height: 200px;
    }

    .loading-state,
    .error-state,
    .empty-state {
        text-align: center;
        padding: 3rem 1rem;
        color: #d1d5db;
    }

    .spinner {
        border: 4px solid rgba(255, 255, 255, 0.1);
        border-top: 4px solid #fbbf24;
        border-radius: 50%;
        width: 40px;
        height: 40px;
        animation: spin 1s linear infinite;
        margin: 0 auto 1rem;
    }

    @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
    }

    .empty-state .hint {
        margin-top: 0.5rem;
        font-size: 0.875rem;
        color: #9ca3af;
    }

    .error-state p {
        color: #ff6b6b;
        margin-bottom: 1rem;
    }

    .retry-button {
        background: #4a5568;
        color: white;
        border: 2px solid #6b7280;
        padding: 0.5rem 1.5rem;
        border-radius: 8px;
        font-weight: 700;
        cursor: pointer;
        transition: all 0.2s;
    }

    .retry-button:hover {
        background: #6b7280;
        transform: scale(1.05);
    }

    .topics-grid {
        display: grid;
        gap: 0.75rem;
        grid-template-columns: 1fr;
    }

    .topic-card {
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
        border: 3px solid #fbbf24;
        border-radius: 12px;
        padding: 1rem 1.25rem;
        cursor: pointer;
        transition: all 0.2s;
        display: flex;
        justify-content: space-between;
        align-items: center;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
    }

    .topic-card:hover {
        transform: translateY(-2px) scale(1.02);
        box-shadow: 0 8px 16px rgba(251, 191, 36, 0.4);
        border-color: #fde68a;
    }

    .topic-card:active {
        transform: translateY(0) scale(0.98);
    }

    .topic-info {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        align-items: flex-start;
    }

    .topic-name {
        font-weight: 900;
        font-size: 1.125rem;
        color: white;
        text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.5);
        letter-spacing: 0.05em;
    }

    .type-badge {
        font-size: 0.625rem;
        font-weight: 700;
        color: white;
        padding: 0.25rem 0.5rem;
        border-radius: 4px;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    }

    .topic-action {
        font-weight: 700;
        font-size: 0.875rem;
        color: #fef3c7;
        opacity: 0.9;
    }

    .filter-section {
        display: flex;
        gap: 0.5rem;
        margin-bottom: 1.5rem;
        flex-wrap: wrap;
        justify-content: center;
    }

    .filter-btn {
        background: #374151;
        color: #d1d5db;
        border: 2px solid #4b5563;
        padding: 0.5rem 1rem;
        border-radius: 8px;
        font-weight: 700;
        font-size: 0.875rem;
        cursor: pointer;
        transition: all 0.2s;
        text-transform: uppercase;
        letter-spacing: 0.05em;
    }

    .filter-btn:hover {
        background: #4b5563;
        border-color: #6b7280;
    }

    .filter-btn.active {
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
        border-color: #fbbf24;
        color: white;
        box-shadow: 0 4px 8px rgba(251, 191, 36, 0.3);
    }

    .filter-btn.active:hover {
        background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
        transform: translateY(-1px);
        box-shadow: 0 6px 12px rgba(251, 191, 36, 0.4);
    }

</style>
