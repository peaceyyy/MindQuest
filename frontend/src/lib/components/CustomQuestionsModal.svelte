<script lang="ts">
    import CustomQuestionsUploader from './CustomQuestionsUploader.svelte';
    import { onMount } from 'svelte';

    let { onclose, ontopicselect } = $props<{
        onclose: () => void;
        ontopicselect: (topicName: string, source?: 'file' | 'gemini' | 'saved', geminiQuestions?: any[]) => void;
    }>();

    // Tab state - now only files and ai (renamed from gemini)
    let activeTab = $state<'files' | 'ai'>('files');
    
    // === LLM PROVIDER STATE ===
    type LlmProvider = {
        id: string;
        name: string;
        available: boolean;
        type: 'cloud' | 'local' | 'mock';
        loadedModel?: string;
        endpoint?: string;
        unavailableReason?: string;
    };
    let llmProviders = $state<LlmProvider[]>([]);
    let selectedProvider = $state<string>('gemini'); // Default to gemini
    let providersLoading = $state(true);

    // === FILES TAB STATE ===
    type TopicItem = {
        name: string;
        type: 'csv' | 'xlsx' | 'json';
    };

    let customTopics = $state<TopicItem[]>([]);
    let loading = $state(true);
    let error = $state<string | null>(null);
    let selectedFilter = $state<'all' | 'csv' | 'xlsx' | 'json'>('all');

    // === GEMINI TAB STATE ===
    let geminiAvailable = $state(false);
    let geminiLoading = $state(true);
    let geminiError = $state<string | null>(null);
    let geminiTopic = $state('');
    let geminiDifficulty = $state<'easy' | 'medium' | 'hard'>('medium');
    let geminiQuestionCount = $state(5);
    let geminiGenerating = $state(false);
    let geminiGeneratedQuestions = $state<any[]>([]);
    let geminiGenerationTime = $state(0);
    let currentProviderName = $state('AI'); // For display purposes

    // === SAVED SETS TAB STATE ===
    type SavedSet = {
        id: string;
        name: string;
        topic: string;
        difficulty: string;
        questionCount: number;
        provider: string;
        createdAt: number;
    };
    let savedSets = $state<SavedSet[]>([]);
    let savedSetsLoading = $state(false);
    let savedSetsError = $state<string | null>(null);
    let loadingSavedSetId = $state<string | null>(null);
    let deletingSetId = $state<string | null>(null);

    // === SAVE MODAL STATE ===
    let showSaveModal = $state(false);
    let saveSetName = $state('');
    let saving = $state(false);
    let saveError = $state<string | null>(null);

    // === SAVED SETS MODAL STATE ===
    let showSavedSetsModal = $state(false);

    // Topic character limit
    const TOPIC_MAX_LENGTH = 100;

    onMount(async () => {
        await Promise.all([
            loadCustomTopics(),
            loadLlmProviders(),
            loadSavedSets()
        ]);
    });
    
    // Load available LLM providers from backend
    async function loadLlmProviders() {
        providersLoading = true;
        try {
            const res = await fetch('http://localhost:7070/api/llm/providers');
            const data = await res.json();
            llmProviders = data.providers || [];
            
            // Set default provider to first available one
            const availableProvider = llmProviders.find(p => p.available);
            if (availableProvider) {
                selectedProvider = availableProvider.id;
                updateCurrentProviderName();
            }
            
            // Update gemini availability based on provider data
            const geminiProvider = llmProviders.find(p => p.id === 'gemini');
            geminiAvailable = geminiProvider?.available ?? false;
            if (geminiProvider && !geminiProvider.available) {
                geminiError = geminiProvider.unavailableReason || 'Gemini not available';
            }
            
            geminiLoading = false;
        } catch (err: any) {
            console.error('Failed to load LLM providers:', err);
            geminiError = 'Could not connect to server';
            geminiLoading = false;
        } finally {
            providersLoading = false;
        }
    }
    
    function updateCurrentProviderName() {
        const provider = llmProviders.find(p => p.id === selectedProvider);
        currentProviderName = provider?.name || 'AI';
    }
    
    function selectProvider(providerId: string) {
        const provider = llmProviders.find(p => p.id === providerId);
        if (provider && provider.available) {
            selectedProvider = providerId;
            updateCurrentProviderName();
            geminiError = null;
            geminiGeneratedQuestions = [];
        }
    }
    
    // Get the currently selected provider object
    let currentProvider = $derived(llmProviders.find(p => p.id === selectedProvider));
    let anyProviderAvailable = $derived(llmProviders.some(p => p.available && p.id !== 'mock'));

    // Filter computed derived state
    let filteredTopics = $derived(
        selectedFilter === 'all' 
            ? customTopics 
            : customTopics.filter(t => t.type === selectedFilter)
    );

    // Topic character count
    let topicCharCount = $derived(geminiTopic.length);
    let topicValid = $derived(geminiTopic.trim().length >= 3 && geminiTopic.length <= TOPIC_MAX_LENGTH);

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

    async function checkGeminiStatus() {
        // This is now handled by loadLlmProviders, kept for backward compatibility
        geminiLoading = true;
        try {
            const res = await fetch('http://localhost:7070/api/gemini/status');
            const data = await res.json();
            geminiAvailable = data.available;
            if (!data.available) {
                geminiError = data.message || 'Gemini API key not configured';
            }
        } catch (err: any) {
            console.error('Failed to check Gemini status:', err);
            geminiError = 'Could not connect to server';
            geminiAvailable = false;
        } finally {
            geminiLoading = false;
        }
    }

    async function generateGeminiQuestions() {
        if (!topicValid || geminiGenerating) return;
        
        geminiGenerating = true;
        geminiError = null;
        geminiGeneratedQuestions = [];
        const startTime = Date.now();
        
        // Determine which endpoint to use based on selected provider
        let generateEndpoint = 'http://localhost:7070/api/gemini/generate';
        if (selectedProvider === 'local') {
            generateEndpoint = 'http://localhost:7070/api/llm/local/generate';
        }
        
        try {
            const res = await fetch(generateEndpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    topic: geminiTopic.trim(),
                    difficulty: geminiDifficulty,
                    count: geminiQuestionCount
                })
            });
            
            const data = await res.json();
            
            if (!res.ok) {
                throw new Error(data.message || data.error || 'Generation failed');
            }
            
            geminiGeneratedQuestions = data.questions || [];
            geminiGenerationTime = Date.now() - startTime;
            
        } catch (err: any) {
            console.error('Question generation failed:', err);
            geminiError = err.message || 'Failed to generate questions';
        } finally {
            geminiGenerating = false;
        }
    }

    function useGeminiQuestions() {
        if (geminiGeneratedQuestions.length === 0) return;
        ontopicselect(geminiTopic.trim(), 'gemini', geminiGeneratedQuestions);
    }

    function close() {
        onclose();
    }

    function selectTopic(topicName: string) {
        ontopicselect(topicName, 'file');
    }

    async function handleUploadSuccess(detail: { customTopicName: string }) {
        // Reload the list after successful upload
        await loadCustomTopics();
    }

    // === SAVED SETS FUNCTIONS ===
    async function loadSavedSets() {
        savedSetsLoading = true;
        savedSetsError = null;
        try {
            const res = await fetch('http://localhost:7070/api/saved-sets');
            const data = await res.json();
            savedSets = data.sets || [];
        } catch (err: any) {
            console.error('Failed to load saved sets:', err);
            savedSetsError = 'Failed to load saved question sets.';
        } finally {
            savedSetsLoading = false;
        }
    }

    function openSaveModal() {
        // Generate default name from topic
        saveSetName = `${geminiTopic.trim()} - ${geminiDifficulty.charAt(0).toUpperCase() + geminiDifficulty.slice(1)}`;
        saveError = null;
        showSaveModal = true;
    }

    function closeSaveModal() {
        showSaveModal = false;
        saveSetName = '';
        saveError = null;
    }

    async function saveQuestionSet() {
        if (!saveSetName.trim() || geminiGeneratedQuestions.length === 0) return;
        
        saving = true;
        saveError = null;
        
        try {
            const res = await fetch('http://localhost:7070/api/saved-sets', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: saveSetName.trim(),
                    topic: geminiTopic.trim(),
                    difficulty: geminiDifficulty,
                    provider: selectedProvider,
                    questions: geminiGeneratedQuestions
                })
            });
            
            const data = await res.json();
            
            if (!res.ok) {
                throw new Error(data.error || 'Failed to save');
            }
            
            // Reload saved sets and close modal
            await loadSavedSets();
            closeSaveModal();
            
            // Open the saved sets modal to show the new set
            showSavedSetsModal = true;
            
        } catch (err: any) {
            console.error('Failed to save question set:', err);
            saveError = err.message || 'Failed to save question set';
        } finally {
            saving = false;
        }
    }

    async function selectSavedSet(setId: string) {
        loadingSavedSetId = setId;
        
        try {
            const res = await fetch(`http://localhost:7070/api/saved-sets/${setId}/questions`);
            const data = await res.json();
            
            if (!res.ok) {
                throw new Error(data.error || 'Failed to load questions');
            }
            
            // Use the saved questions
            ontopicselect(data.topic, 'saved', data.questions);
            
        } catch (err: any) {
            console.error('Failed to load saved set:', err);
            savedSetsError = err.message || 'Failed to load question set';
        } finally {
            loadingSavedSetId = null;
        }
    }

    async function deleteSavedSet(setId: string, setName: string) {
        if (!confirm(`Delete "${setName}"? This cannot be undone.`)) return;
        
        deletingSetId = setId;
        try {
            const res = await fetch(`http://localhost:7070/api/saved-sets/${setId}`, {
                method: 'DELETE'
            });
            
            if (!res.ok) {
                const data = await res.json();
                throw new Error(data.error || 'Failed to delete');
            }
            
            // Reload the list
            await loadSavedSets();
            
        } catch (err: any) {
            console.error('Failed to delete saved set:', err);
            savedSetsError = err.message || 'Failed to delete question set';
        } finally {
            deletingSetId = null;
        }
    }

    function formatDate(timestamp: number): string {
        return new Date(timestamp).toLocaleDateString(undefined, {
            month: 'short',
            day: 'numeric',
            year: 'numeric'
        });
    }

    function getDifficultyColor(difficulty: string): string {
        switch (difficulty.toLowerCase()) {
            case 'easy': return '#10b981';
            case 'medium': return '#f59e0b';
            case 'hard': return '#ef4444';
            default: return '#6b7280';
        }
    }


</script>

<!-- Modal Overlay -->
<div class="modal-overlay" onclick={close} role="button" tabindex="-1" onkeydown={(e) => e.key === 'Escape' && close()}>
    <!-- Modal Content -->
    <div class="modal-content" onclick={(e) => e.stopPropagation()} onkeydown={() => {}} role="dialog" tabindex="-1">
        <button class="close-button" onclick={close}>&times;</button>
        
        <div class="modal-inner">
            <h2 class="modal-title">Custom Question Sets</h2>
            
            <!-- Tab Navigation -->
            <div class="tab-nav">
                <button 
                    class="tab-btn {activeTab === 'files' ? 'active' : ''}"
                    onclick={() => activeTab = 'files'}
                >
                    <span class="tab-icon">📁</span>
                    Files
                </button>
                <button 
                    class="tab-btn {activeTab === 'ai' ? 'active' : ''}"
                    onclick={() => activeTab = 'ai'}
                >
                    <span class="tab-icon">🤖</span>
                    LLM Providers
                    {#if !providersLoading && anyProviderAvailable}
                        <span class="status-dot available"></span>
                    {:else if !providersLoading && !anyProviderAvailable}
                        <span class="status-dot unavailable"></span>
                    {/if}
                </button>
            </div>

            <!-- FILES TAB -->
            {#if activeTab === 'files'}
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
                                    <div class="topic-action">Select -></div>
                                </button>
                            {/each}
                        </div>
                    {/if}
                </div>
            {/if}

            <!-- AI GENERATION TAB -->
            {#if activeTab === 'ai'}
                <div class="gemini-section">
                    {#if providersLoading}
                        <div class="loading-state">
                            <div class="spinner"></div>
                            <p>Loading AI providers...</p>
                        </div>
                    {:else if !anyProviderAvailable}
                        <div class="gemini-unavailable">
                            <div class="unavailable-icon">🤖</div>
                            <h3>No AI Providers Available</h3>
                            <p>Configure at least one provider to generate questions.</p>
                            <div class="setup-instructions">
                                <p><strong>Option 1: Cloud (Gemini)</strong></p>
                                <ol>
                                    <li>Get an API key from <a href="https://makersuite.google.com/app/apikey" target="_blank" rel="noopener">Google AI Studio</a></li>
                                    <li>Add <code>GOOGLE_API_KEY=your_key</code> to your <code>.env</code> file</li>
                                    <li>Restart the backend server</li>
                                </ol>
                                <p style="margin-top: 1rem;"><strong>Option 2: Local (LM Studio)</strong></p>
                                <ol>
                                    <li>Download <a href="https://lmstudio.ai" target="_blank" rel="noopener">LM Studio</a></li>
                                    <li>Load a model (Llama 3, Mistral, Phi, etc.)</li>
                                    <li>Go to Developer → Start Server</li>
                                </ol>
                            </div>
                            <button onclick={loadLlmProviders} class="retry-button">Check Again</button>
                        </div>
                    {:else}
                        <!-- Provider Selector -->
                        <div class="provider-selector">
                            <label for="llm-provider">AI Provider</label>
                            <div class="provider-options">
                                {#each llmProviders.filter(p => p.id !== 'mock') as provider (provider.id)}
                                    <button
                                        class="provider-btn {selectedProvider === provider.id ? 'active' : ''} {!provider.available ? 'disabled' : ''} {provider.type}"
                                        onclick={() => selectProvider(provider.id)}
                                        disabled={!provider.available}
                                        title={provider.available ? provider.name : provider.unavailableReason}
                                    >
                                        <span class="provider-icon">
                                            {#if provider.type === 'cloud'}☁️{:else}💻{/if}
                                        </span>
                                        <span class="provider-name">{provider.name}</span>
                                        {#if provider.available}
                                            <span class="provider-status available">●</span>
                                        {:else}
                                            <span class="provider-status unavailable">○</span>
                                        {/if}
                                        {#if provider.loadedModel}
                                            <span class="provider-model">{provider.loadedModel.split('/').pop()}</span>
                                        {/if}
                                    </button>
                                {/each}
                            </div>
                            {#if currentProvider?.type === 'local' && currentProvider?.loadedModel}
                                <p class="provider-hint">Using model: {currentProvider.loadedModel}</p>
                            {/if}
                        </div>
                        
                        <!-- Question Generation Form -->
                        <div class="gemini-form">
                            <!-- Topic Input -->
                            <div class="form-group">
                                <label for="gemini-topic">
                                    What topic do you want to be quizzed on?
                                    <span class="char-count {topicCharCount > TOPIC_MAX_LENGTH ? 'over' : ''}">
                                        {topicCharCount}/{TOPIC_MAX_LENGTH}
                                    </span>
                                </label>
                                <input 
                                    id="gemini-topic"
                                    type="text"
                                    bind:value={geminiTopic}
                                    placeholder="e.g., Machine Learning Basics, World War II, JavaScript Promises..."
                                    maxlength={TOPIC_MAX_LENGTH}
                                    class="topic-input"
                                    disabled={geminiGenerating}
                                />
                                <p class="input-hint">Be specific! The AI will generate questions based on your topic.</p>
                            </div>
                            
                            <!-- Difficulty Selection -->
                            <fieldset class="form-group">
                                <legend>Difficulty Level</legend>
                                <div class="difficulty-selector">
                                    <button 
                                        class="diff-btn {geminiDifficulty === 'easy' ? 'active easy' : ''}"
                                        onclick={() => geminiDifficulty = 'easy'}
                                        disabled={geminiGenerating}
                                    >
                                        Easy
                                    </button>
                                    <button 
                                        class="diff-btn {geminiDifficulty === 'medium' ? 'active medium' : ''}"
                                        onclick={() => geminiDifficulty = 'medium'}
                                        disabled={geminiGenerating}
                                    >
                                        Medium
                                    </button>
                                    <button 
                                        class="diff-btn {geminiDifficulty === 'hard' ? 'active hard' : ''}"
                                        onclick={() => geminiDifficulty = 'hard'}
                                        disabled={geminiGenerating}
                                    >
                                        Hard
                                    </button>
                                </div>
                            </fieldset>
                            
                            <!-- Question Count Slider -->
                            <div class="form-group">
                                <label for="question-count">
                                    Number of Questions: <strong>{geminiQuestionCount}</strong>
                                </label>
                                <div class="slider-container">
                                    <span class="slider-label">5</span>
                                    <input 
                                        id="question-count"
                                        type="range"
                                        min="5"
                                        max="10"
                                        bind:value={geminiQuestionCount}
                                        class="question-slider"
                                        disabled={geminiGenerating}
                                    />
                                    <span class="slider-label">10</span>
                                </div>
                            </div>
                            
                            <!-- Generate Button -->
                            <button 
                                class="generate-btn {selectedProvider === 'local' ? 'local' : 'cloud'}"
                                onclick={generateGeminiQuestions}
                                disabled={!topicValid || geminiGenerating || !currentProvider?.available}
                            >
                                {#if geminiGenerating}
                                    <div class="btn-spinner"></div>
                                    Generating with {currentProviderName}...
                                {:else}
                                    🎯 Generate Questions
                                {/if}
                            </button>
                            
                            {#if geminiError}
                                <div class="gemini-error">
                                    <span>Warning:</span> {geminiError}
                                </div>
                            {/if}
                        </div>
                        
                        <!-- Saved AI Question Sets Button -->
                        <div class="saved-sets-section">
                            <div class="divider">
                                <span>Or Play Saved Questions</span>
                            </div>
                            <button 
                                class="view-saved-btn"
                                onclick={() => { loadSavedSets(); showSavedSetsModal = true; }}
                            >
                                <span class="btn-icon">📁</span>
                                Saved AI Question Sets
                                {#if savedSets.length > 0}
                                    <span class="saved-count">{savedSets.length}</span>
                                {/if}
                            </button>
                        </div>
                        
                        <!-- Generated Questions Preview -->
                        {#if geminiGeneratedQuestions.length > 0}
                            <div class="generated-preview">
                                <div class="preview-header">
                                    <h3>Generated {geminiGeneratedQuestions.length} Questions</h3>
                                    <span class="generation-time">({(geminiGenerationTime / 1000).toFixed(1)}s)</span>
                                </div>
                                
                                <div class="questions-preview">
                                    {#each geminiGeneratedQuestions.slice(0, 3) as q, i}
                                        <div class="preview-question">
                                            <span class="q-number">Q{i + 1}.</span>
                                            <span class="q-text">{q.questionText.length > 80 ? q.questionText.slice(0, 80) + '...' : q.questionText}</span>
                                        </div>
                                    {/each}
                                    {#if geminiGeneratedQuestions.length > 3}
                                        <div class="preview-more">
                                            + {geminiGeneratedQuestions.length - 3} more questions
                                        </div>
                                    {/if}
                                </div>
                                
                                <div class="preview-actions">
                                    <button class="save-set-btn" onclick={openSaveModal}>
                                        Save Question Set
                                    </button>
                                    <button class="use-questions-btn" onclick={useGeminiQuestions}>
                                        Start Battle
                                    </button>
                                </div>
                            </div>
                        {/if}
                    {/if}
                </div>
            {/if}
        </div>
    </div>
</div>

<!-- Save Modal -->
{#if showSaveModal}
    <div class="save-modal-overlay" onclick={closeSaveModal} onkeydown={(e) => e.key === 'Escape' && closeSaveModal()} role="button" tabindex="-1">
        <div class="save-modal" onclick={(e) => e.stopPropagation()} onkeydown={() => {}} role="dialog" tabindex="-1">
            <h3>Save Question Set</h3>
            <div class="save-form">
                <label for="set-name">Name this question set:</label>
                <input 
                    id="set-name"
                    type="text"
                    bind:value={saveSetName}
                    placeholder="e.g., World War II - Easy"
                    maxlength="100"
                    disabled={saving}
                />
                
                <div class="save-info">
                    <span>{geminiGeneratedQuestions.length} questions</span>
                    <span style="color: {getDifficultyColor(geminiDifficulty)}">{geminiDifficulty}</span>
                </div>
                
                {#if saveError}
                    <div class="save-error">{saveError}</div>
                {/if}
                
                <div class="save-buttons">
                    <button class="cancel-btn" onclick={closeSaveModal} disabled={saving}>
                        Cancel
                    </button>
                    <button 
                        class="confirm-save-btn" 
                        onclick={saveQuestionSet}
                        disabled={!saveSetName.trim() || saving}
                    >
                        {#if saving}
                            <div class="btn-spinner small"></div>
                            Saving...
                        {:else}
                            Save
                        {/if}
                    </button>
                </div>
            </div>
        </div>
    </div>
{/if}

<!-- Nested Saved Sets Modal -->
{#if showSavedSetsModal}
    <div 
        class="saved-sets-modal-overlay" 
        onclick={() => showSavedSetsModal = false} 
        onkeydown={(e) => e.key === 'Escape' && (showSavedSetsModal = false)} 
        role="button" 
        tabindex="-1"
    >
        <div 
            class="saved-sets-modal" 
            onclick={(e) => e.stopPropagation()} 
            onkeydown={() => {}} 
            role="dialog" 
            tabindex="-1"
        >
            <div class="saved-sets-header">
                <h3>Saved AI Question Sets</h3>
                <button class="close-saved-modal-btn" onclick={() => showSavedSetsModal = false}>
                    Close
                </button>
            </div>
            
            <div class="saved-sets-content">
                {#if savedSetsLoading}
                    <div class="loading-state">
                        <div class="spinner"></div>
                        <span>Loading saved sets...</span>
                    </div>
                {:else if savedSetsError}
                    <div class="error-state">
                        <span>{savedSetsError}</span>
                        <button class="retry-btn" onclick={loadSavedSets}>Retry</button>
                    </div>
                {:else if savedSets.length === 0}
                    <div class="empty-state">
                        <span class="empty-icon">📚</span>
                        <p>No saved question sets yet.</p>
                        <p class="empty-hint">Generate questions with Gemini AI and save them for later!</p>
                    </div>
                {:else}
                    <div class="saved-sets-list">
                        {#each savedSets as set (set.id)}
                            <div class="saved-set-card">
                                <div class="set-info">
                                    <span class="set-name">{set.name}</span>
                                    <div class="set-meta">
                                        <span class="set-topic">{set.topic}</span>
                                        <span class="set-difficulty" style="background: {getDifficultyColor(set.difficulty)}">
                                            {set.difficulty}
                                        </span>
                                        <span class="set-count">{set.questionCount} questions</span>
                                        {#if set.provider}
                                            <span class="set-provider">{set.provider}</span>
                                        {/if}
                                        <span class="set-date">{formatDate(set.createdAt)}</span>
                                    </div>
                                </div>
                                <div class="set-actions">
                                    <button 
                                        class="play-set-btn" 
                                        onclick={() => selectSavedSet(set.id)}
                                        disabled={loadingSavedSetId === set.id}
                                    >
                                        {#if loadingSavedSetId === set.id}
                                            <div class="btn-spinner small"></div>
                                        {:else}
                                            Play
                                        {/if}
                                    </button>
                                    <button 
                                        class="delete-set-btn" 
                                        onclick={() => deleteSavedSet(set.id, set.name)}
                                        disabled={deletingSetId === set.id}
                                    >
                                        {#if deletingSetId === set.id}
                                            ...
                                        {:else}
                                            Delete
                                        {/if}
                                    </button>
                                </div>
                            </div>
                        {/each}
                    </div>
                {/if}
            </div>
        </div>
    </div>
{/if}

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
        max-width: 650px;
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

    /* Tab Navigation */
    .tab-nav {
        display: flex;
        gap: 0.5rem;
        margin-bottom: 1.5rem;
        background: rgba(0, 0, 0, 0.3);
        padding: 0.5rem;
        border-radius: 12px;
    }

    .tab-btn {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        padding: 0.75rem 1rem;
        background: transparent;
        border: 2px solid transparent;
        border-radius: 8px;
        color: #9ca3af;
        font-weight: 700;
        font-size: 0.95rem;
        cursor: pointer;
        transition: all 0.2s;
    }

    .tab-btn:hover {
        background: rgba(255, 255, 255, 0.05);
        color: #d1d5db;
    }

    .tab-btn.active {
        background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
        border-color: #a5b4fc;
        color: white;
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
    }

    .tab-icon {
        font-size: 1.1rem;
    }

    .saved-count {
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        color: white;
        font-size: 0.7rem;
        font-weight: 600;
        padding: 0.15rem 0.4rem;
        border-radius: 0.75rem;
        min-width: 1.2rem;
        text-align: center;
    }

    .status-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        margin-left: 0.25rem;
    }

    .status-dot.available {
        background: #10b981;
        box-shadow: 0 0 6px #10b981;
    }

    .status-dot.unavailable {
        background: #ef4444;
    }

    /* Gemini Tab Styles */
    .gemini-section {
        min-height: 300px;
    }

    .gemini-unavailable {
        text-align: center;
        padding: 2rem 1rem;
    }

    .unavailable-icon {
        font-size: 3rem;
        margin-bottom: 1rem;
        color: #fbbf24;
    }

    .gemini-unavailable h3 {
        color: #fbbf24;
        margin: 0 0 0.5rem 0;
        font-size: 1.25rem;
    }

    .gemini-unavailable > p {
        color: #9ca3af;
        margin-bottom: 1.5rem;
    }

    .setup-instructions {
        background: rgba(0, 0, 0, 0.3);
        border: 2px solid #4b5563;
        border-radius: 12px;
        padding: 1.25rem;
        text-align: left;
        margin-bottom: 1.5rem;
    }

    .setup-instructions p {
        color: #d1d5db;
        margin: 0 0 0.75rem 0;
        font-weight: 600;
    }

    .setup-instructions ol {
        margin: 0;
        padding-left: 1.25rem;
        color: #9ca3af;
    }

    .setup-instructions li {
        margin-bottom: 0.5rem;
    }

    .setup-instructions a {
        color: #60a5fa;
        text-decoration: none;
    }

    .setup-instructions a:hover {
        text-decoration: underline;
    }

    .setup-instructions code {
        background: rgba(0, 0, 0, 0.4);
        padding: 0.125rem 0.375rem;
        border-radius: 4px;
        font-size: 0.875rem;
        color: #fbbf24;
    }

    /* Gemini Form */
    .gemini-form {
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
    }

    .form-group {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
    }

    .form-group label {
        color: #d1d5db;
        font-weight: 600;
        font-size: 0.95rem;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    
    fieldset.form-group {
        border: none;
        padding: 0;
        margin: 0;
    }
    
    fieldset.form-group legend {
        color: #d1d5db;
        font-weight: 600;
        font-size: 0.95rem;
        margin-bottom: 0.5rem;
    }

    .char-count {
        font-size: 0.75rem;
        font-weight: 400;
        color: #6b7280;
    }

    .char-count.over {
        color: #ef4444;
    }

    .topic-input {
        background: rgba(0, 0, 0, 0.4);
        border: 2px solid #4b5563;
        border-radius: 10px;
        padding: 0.875rem 1rem;
        color: white;
        font-size: 1rem;
        transition: all 0.2s;
    }

    .topic-input:focus {
        outline: none;
        border-color: #8b5cf6;
        box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.2);
    }

    .topic-input::placeholder {
        color: #6b7280;
    }

    .topic-input:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }

    .input-hint {
        font-size: 0.8rem;
        color: #6b7280;
        margin: 0;
    }

    .difficulty-selector {
        display: flex;
        gap: 0.5rem;
    }

    .diff-btn {
        flex: 1;
        padding: 0.75rem;
        background: rgba(0, 0, 0, 0.3);
        border: 2px solid #4b5563;
        border-radius: 8px;
        color: #9ca3af;
        font-weight: 700;
        font-size: 0.9rem;
        cursor: pointer;
        transition: all 0.2s;
    }

    .diff-btn:hover:not(:disabled) {
        background: rgba(255, 255, 255, 0.05);
        border-color: #6b7280;
    }

    .diff-btn:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }

    .diff-btn.active.easy {
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        border-color: #34d399;
        color: white;
    }

    .diff-btn.active.medium {
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
        border-color: #fbbf24;
        color: white;
    }

    .diff-btn.active.hard {
        background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
        border-color: #f87171;
        color: white;
    }

    .slider-container {
        display: flex;
        align-items: center;
        gap: 1rem;
    }

    .slider-label {
        color: #6b7280;
        font-weight: 600;
        font-size: 0.875rem;
        min-width: 1.5rem;
        text-align: center;
    }

    .question-slider {
        flex: 1;
        height: 8px;
        -webkit-appearance: none;
        appearance: none;
        background: #4b5563;
        border-radius: 4px;
        outline: none;
    }

    .question-slider::-webkit-slider-thumb {
        -webkit-appearance: none;
        appearance: none;
        width: 24px;
        height: 24px;
        background: linear-gradient(135deg, #8b5cf6 0%, #6366f1 100%);
        border-radius: 50%;
        cursor: pointer;
        border: 3px solid white;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
        transition: transform 0.2s;
    }

    .question-slider::-webkit-slider-thumb:hover {
        transform: scale(1.1);
    }

    .question-slider:disabled {
        opacity: 0.5;
    }

    .generate-btn {
        background: linear-gradient(135deg, #8b5cf6 0%, #6366f1 100%);
        border: 3px solid #a5b4fc;
        border-radius: 12px;
        padding: 1rem 1.5rem;
        color: white;
        font-weight: 900;
        font-size: 1.1rem;
        cursor: pointer;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
    }

    .generate-btn:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(139, 92, 246, 0.4);
    }

    .generate-btn:disabled {
        opacity: 0.5;
        cursor: not-allowed;
        transform: none;
    }

    .btn-spinner {
        width: 20px;
        height: 20px;
        border: 3px solid rgba(255, 255, 255, 0.3);
        border-top-color: white;
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
    }

    .gemini-error {
        background: rgba(239, 68, 68, 0.2);
        border: 2px solid #ef4444;
        border-radius: 8px;
        padding: 0.75rem 1rem;
        color: #fca5a5;
        font-size: 0.9rem;
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }

    /* Generated Preview */
    .generated-preview {
        margin-top: 1.5rem;
        background: rgba(16, 185, 129, 0.1);
        border: 2px solid #10b981;
        border-radius: 12px;
        padding: 1.25rem;
    }

    .preview-header {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-bottom: 1rem;
    }

    .preview-header h3 {
        margin: 0;
        color: #10b981;
        font-size: 1.1rem;
    }

    .generation-time {
        color: #6b7280;
        font-size: 0.85rem;
    }

    .questions-preview {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        margin-bottom: 1rem;
    }

    .preview-question {
        background: rgba(0, 0, 0, 0.3);
        border-radius: 8px;
        padding: 0.75rem;
        display: flex;
        gap: 0.5rem;
    }

    .q-number {
        color: #10b981;
        font-weight: 700;
        flex-shrink: 0;
    }

    .q-text {
        color: #d1d5db;
        font-size: 0.9rem;
    }

    .preview-more {
        text-align: center;
        color: #6b7280;
        font-size: 0.85rem;
        font-style: italic;
    }

    .use-questions-btn {
        width: 100%;
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        border: 3px solid #34d399;
        border-radius: 10px;
        padding: 0.875rem;
        color: white;
        font-weight: 900;
        font-size: 1rem;
        cursor: pointer;
        transition: all 0.2s;
        text-transform: uppercase;
        letter-spacing: 0.05em;
    }

    .use-questions-btn:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(16, 185, 129, 0.4);
    }

    /* Preview Actions (Save button in generated preview) */
    .preview-actions {
        display: flex;
        gap: 0.75rem;
        margin-top: 1rem;
        padding-top: 1rem;
        border-top: 1px solid rgba(255, 255, 255, 0.1);
    }

    .save-set-btn {
        flex: 1;
        padding: 0.75rem 1.5rem;
        border: none;
        border-radius: 0.5rem;
        background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
        color: white;
        font-weight: 600;
        font-size: 0.9rem;
        cursor: pointer;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
    }

    .save-set-btn:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(99, 102, 241, 0.4);
    }

    .save-set-btn:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }

    /* Saved Sets Section in Gemini Tab */
    .saved-sets-section {
        margin-top: 1.5rem;
        padding-top: 1rem;
    }

    .saved-sets-section .divider {
        display: flex;
        align-items: center;
        text-align: center;
        margin-bottom: 1rem;
        color: rgba(255, 255, 255, 0.5);
        font-size: 0.85rem;
    }

    .saved-sets-section .divider::before,
    .saved-sets-section .divider::after {
        content: '';
        flex: 1;
        border-bottom: 1px solid rgba(255, 255, 255, 0.15);
    }

    .saved-sets-section .divider span {
        padding: 0 1rem;
    }

    .btn-icon {
        font-size: 1.1rem;
    }

    .saved-sets-list {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
    }

    .saved-set-card {
        background: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, rgba(139, 92, 246, 0.05) 100%);
        border: 1px solid rgba(99, 102, 241, 0.3);
        border-radius: 0.75rem;
        padding: 1rem;
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 1rem;
        transition: all 0.2s;
    }

    .saved-set-card:hover {
        border-color: rgba(99, 102, 241, 0.5);
        background: linear-gradient(135deg, rgba(99, 102, 241, 0.15) 0%, rgba(139, 92, 246, 0.1) 100%);
    }

    .set-info {
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
        flex: 1;
    }

    .set-name {
        font-weight: 600;
        color: white;
        font-size: 1rem;
    }

    .set-meta {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
        align-items: center;
    }

    .set-topic {
        padding: 0.2rem 0.5rem;
        border-radius: 0.25rem;
        font-size: 0.75rem;
        font-weight: 500;
        background: rgba(56, 189, 248, 0.2);
        color: #38bdf8;
        text-transform: capitalize;
    }

    .set-difficulty {
        padding: 0.2rem 0.5rem;
        border-radius: 0.25rem;
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
        color: white;
    }

    .set-count {
        color: rgba(255, 255, 255, 0.5);
        font-size: 0.8rem;
    }

    .set-provider {
        padding: 0.2rem 0.5rem;
        border-radius: 0.25rem;
        font-size: 0.7rem;
        font-weight: 500;
        background: rgba(99, 102, 241, 0.2);
        color: #a5b4fc;
    }

    .set-date {
        color: rgba(255, 255, 255, 0.4);
        font-size: 0.75rem;
    }

    .set-actions {
        display: flex;
        gap: 0.5rem;
        flex-shrink: 0;
    }

    .play-set-btn {
        padding: 0.5rem 1rem;
        border: none;
        border-radius: 0.375rem;
        font-size: 0.85rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        color: white;
        min-width: 60px;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .play-set-btn:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
    }

    .play-set-btn:disabled {
        opacity: 0.6;
        cursor: not-allowed;
    }

    .delete-set-btn {
        padding: 0.5rem 0.75rem;
        border: none;
        border-radius: 0.375rem;
        font-size: 0.85rem;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;
        background: rgba(239, 68, 68, 0.2);
        color: #ef4444;
        border: 1px solid rgba(239, 68, 68, 0.3);
    }

    .delete-set-btn:hover {
        background: rgba(239, 68, 68, 0.3);
        border-color: rgba(239, 68, 68, 0.5);
    }

    /* Save Modal Overlay */
    .save-modal-overlay {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.7);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1100;
        backdrop-filter: blur(4px);
    }

    .save-modal {
        background: linear-gradient(135deg, #1e1b4b 0%, #0f172a 100%);
        border: 1px solid rgba(99, 102, 241, 0.3);
        border-radius: 1rem;
        padding: 1.5rem;
        width: 90%;
        max-width: 400px;
        box-shadow: 0 25px 50px rgba(0, 0, 0, 0.5);
    }

    .save-modal h3 {
        color: white;
        font-size: 1.25rem;
        margin-bottom: 1rem;
    }

    .save-form {
        display: flex;
        flex-direction: column;
        gap: 1rem;
    }

    .save-form label {
        color: rgba(255, 255, 255, 0.8);
        font-size: 0.9rem;
        margin-bottom: 0.25rem;
    }

    .save-form input[type="text"] {
        width: 100%;
        padding: 0.75rem 1rem;
        border: 1px solid rgba(99, 102, 241, 0.3);
        border-radius: 0.5rem;
        background: rgba(15, 23, 42, 0.8);
        color: white;
        font-size: 1rem;
        transition: all 0.2s;
        box-sizing: border-box;
    }

    .save-form input[type="text"]:focus {
        outline: none;
        border-color: #6366f1;
        box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
    }

    .save-form input[type="text"]::placeholder {
        color: rgba(255, 255, 255, 0.4);
    }

    .save-info {
        display: flex;
        gap: 1rem;
        align-items: center;
        font-size: 0.9rem;
        color: rgba(255, 255, 255, 0.6);
        padding: 0.5rem;
        background: rgba(255, 255, 255, 0.05);
        border-radius: 0.375rem;
    }

    .save-error {
        color: #ef4444;
        font-size: 0.85rem;
        padding: 0.5rem;
        background: rgba(239, 68, 68, 0.1);
        border-radius: 0.375rem;
        border: 1px solid rgba(239, 68, 68, 0.2);
    }

    .save-buttons {
        display: flex;
        gap: 0.75rem;
        margin-top: 0.5rem;
    }

    .cancel-btn,
    .confirm-save-btn {
        flex: 1;
        padding: 0.75rem 1rem;
        border: none;
        border-radius: 0.5rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
    }

    .cancel-btn {
        background: rgba(255, 255, 255, 0.1);
        color: rgba(255, 255, 255, 0.7);
        border: 1px solid rgba(255, 255, 255, 0.2);
    }

    .cancel-btn:hover:not(:disabled) {
        background: rgba(255, 255, 255, 0.15);
        color: white;
    }

    .confirm-save-btn {
        background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
        color: white;
    }

    .confirm-save-btn:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
    }

    .confirm-save-btn:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }

    .btn-spinner.small {
        width: 16px;
        height: 16px;
        border-width: 2px;
    }

    /* Nested Saved Sets Modal */
    .saved-sets-modal-overlay {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.8);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1100;
        backdrop-filter: blur(6px);
    }

    .saved-sets-modal {
        background: linear-gradient(135deg, #1e1b4b 0%, #0f172a 100%);
        border: 1px solid rgba(99, 102, 241, 0.3);
        border-radius: 1rem;
        padding: 1.5rem;
        width: 95%;
        max-width: 600px;
        max-height: 80vh;
        display: flex;
        flex-direction: column;
        box-shadow: 0 25px 50px rgba(0, 0, 0, 0.5);
    }

    .saved-sets-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
        padding-bottom: 1rem;
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .saved-sets-header h3 {
        color: white;
        font-size: 1.25rem;
        margin: 0;
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }

    .close-saved-modal-btn {
        background: rgba(255, 255, 255, 0.1);
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 0.5rem;
        padding: 0.5rem 1rem;
        color: rgba(255, 255, 255, 0.7);
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;
    }

    .close-saved-modal-btn:hover {
        background: rgba(255, 255, 255, 0.15);
        color: white;
    }

    .saved-sets-content {
        flex: 1;
        overflow-y: auto;
        padding-right: 0.5rem;
    }

    .saved-sets-content::-webkit-scrollbar {
        width: 6px;
    }

    .saved-sets-content::-webkit-scrollbar-track {
        background: rgba(255, 255, 255, 0.05);
        border-radius: 3px;
    }

    .saved-sets-content::-webkit-scrollbar-thumb {
        background: rgba(99, 102, 241, 0.4);
        border-radius: 3px;
    }

    .saved-sets-content::-webkit-scrollbar-thumb:hover {
        background: rgba(99, 102, 241, 0.6);
    }

    .view-saved-btn {
        width: 100%;
        padding: 0.875rem 1rem;
        border: 1px solid rgba(139, 92, 246, 0.3);
        border-radius: 0.5rem;
        background: linear-gradient(135deg, rgba(139, 92, 246, 0.15) 0%, rgba(99, 102, 241, 0.1) 100%);
        color: white;
        font-weight: 600;
        font-size: 0.95rem;
        cursor: pointer;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.75rem;
    }

    .view-saved-btn:hover {
        background: linear-gradient(135deg, rgba(139, 92, 246, 0.25) 0%, rgba(99, 102, 241, 0.2) 100%);
        border-color: rgba(139, 92, 246, 0.5);
        transform: translateY(-1px);
    }

    .view-saved-btn .saved-count {
        background: rgba(139, 92, 246, 0.3);
        padding: 0.2rem 0.6rem;
        border-radius: 1rem;
        font-size: 0.8rem;
        font-weight: 600;
    }

    /* Provider Selector Styles */
    .provider-selector {
        margin-bottom: 1.5rem;
        padding-bottom: 1.5rem;
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .provider-selector label {
        display: block;
        color: #d1d5db;
        font-weight: 600;
        font-size: 0.95rem;
        margin-bottom: 0.75rem;
    }

    .provider-options {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
    }

    .provider-btn {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 0.875rem 1rem;
        background: rgba(0, 0, 0, 0.3);
        border: 2px solid #4b5563;
        border-radius: 10px;
        color: #9ca3af;
        font-weight: 600;
        font-size: 0.95rem;
        cursor: pointer;
        transition: all 0.2s;
        text-align: left;
    }

    .provider-btn:hover:not(.disabled) {
        background: rgba(255, 255, 255, 0.05);
        border-color: #6b7280;
    }

    .provider-btn.active {
        border-color: #8b5cf6;
        background: linear-gradient(135deg, rgba(139, 92, 246, 0.2) 0%, rgba(99, 102, 241, 0.1) 100%);
        color: white;
    }

    .provider-btn.active.cloud {
        border-color: #3b82f6;
        background: linear-gradient(135deg, rgba(59, 130, 246, 0.2) 0%, rgba(99, 102, 241, 0.1) 100%);
    }

    .provider-btn.active.local {
        border-color: #10b981;
        background: linear-gradient(135deg, rgba(16, 185, 129, 0.2) 0%, rgba(5, 150, 105, 0.1) 100%);
    }

    .provider-btn.disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }

    .provider-icon {
        font-size: 1.25rem;
    }

    .provider-name {
        flex: 1;
    }

    .provider-status {
        font-size: 0.75rem;
    }

    .provider-status.available {
        color: #10b981;
    }

    .provider-status.unavailable {
        color: #6b7280;
    }

    .provider-model {
        font-size: 0.75rem;
        padding: 0.2rem 0.5rem;
        background: rgba(16, 185, 129, 0.2);
        border-radius: 4px;
        color: #34d399;
        max-width: 150px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    .provider-hint {
        margin-top: 0.5rem;
        font-size: 0.8rem;
        color: #6b7280;
        font-style: italic;
    }

    /* Generate button variants */
    .generate-btn.local {
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        border-color: #34d399;
    }

    .generate-btn.cloud {
        background: linear-gradient(135deg, #8b5cf6 0%, #6366f1 100%);
        border-color: #a5b4fc;
    }

</style>
