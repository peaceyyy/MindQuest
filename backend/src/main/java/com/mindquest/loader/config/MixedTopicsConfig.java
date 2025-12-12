package com.mindquest.loader.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for Mixed Topics Mode. This class is intentionally lightweight
 * and immutable after construction. Use the Builder to create instances.
 */
public class MixedTopicsConfig {

    public enum MixingStrategy { RANDOM, ROUND_ROBIN, PATTERNED }
    public enum DifficultyMode { UNIFIED, MIXED }
    public enum FallbackPolicy { SKIP_TOPIC_ON_ERROR, FILL_FROM_LOCAL, FAIL_ROUND }

    private final List<String> selectedTopics;
    private final int maxTopics;
    private final MixingStrategy mixingStrategy;
    private final long seed;
    private final DifficultyMode difficultyMode;
    private final String difficulty; // Unified difficulty for all topics
    private final SourceConfig sourceConfig; // reuses existing SourceConfig
    private final int questionsPerRound;
    private final int perTopicLimit;
    private final int llmTimeoutMillis;
    private final FallbackPolicy fallbackPolicy;

    private MixedTopicsConfig(Builder b) {
        this.selectedTopics = Collections.unmodifiableList(new ArrayList<>(b.selectedTopics));
        this.maxTopics = b.maxTopics;
        this.mixingStrategy = b.mixingStrategy;
        this.seed = b.seed;
        this.difficultyMode = b.difficultyMode;
        this.difficulty = b.difficulty;
        this.sourceConfig = b.sourceConfig;
        this.questionsPerRound = b.questionsPerRound;
        this.perTopicLimit = b.perTopicLimit;
        this.llmTimeoutMillis = b.llmTimeoutMillis;
        this.fallbackPolicy = b.fallbackPolicy;
    }

    public List<String> getSelectedTopics() { return selectedTopics; }
    public int getMaxTopics() { return maxTopics; }
    public MixingStrategy getMixingStrategy() { return mixingStrategy; }
    public long getSeed() { return seed; }
    public DifficultyMode getDifficultyMode() { return difficultyMode; }
    public String getDifficulty() { return difficulty; }
    public SourceConfig getSourceConfig() { return sourceConfig; }
    public int getQuestionsPerRound() { return questionsPerRound; }
    public int getPerTopicLimit() { return perTopicLimit; }
    public int getLlmTimeoutMillis() { return llmTimeoutMillis; }
    public FallbackPolicy getFallbackPolicy() { return fallbackPolicy; }

    public static class Builder {
        private List<String> selectedTopics = new ArrayList<>();
        private int maxTopics = 3;
        private MixingStrategy mixingStrategy = MixingStrategy.RANDOM;
        private long seed = System.currentTimeMillis();
        private DifficultyMode difficultyMode = DifficultyMode.UNIFIED;
        private String difficulty = "Medium";
        private SourceConfig sourceConfig = null;
        private int questionsPerRound = 5;
        private int perTopicLimit = Integer.MAX_VALUE;
        private int llmTimeoutMillis = 5000;
        private FallbackPolicy fallbackPolicy = FallbackPolicy.FILL_FROM_LOCAL;

        public Builder selectedTopics(List<String> topics) {
            if (topics != null) this.selectedTopics = new ArrayList<>(topics);
            return this;
        }

        public Builder maxTopics(int maxTopics) { this.maxTopics = maxTopics; return this; }
        public Builder mixingStrategy(MixingStrategy strategy) { this.mixingStrategy = strategy; return this; }
        public Builder seed(long s) { this.seed = s; return this; }
        public Builder difficultyMode(DifficultyMode mode) { this.difficultyMode = mode; return this; }
        public Builder difficulty(String d) { this.difficulty = d; return this; }
        public Builder sourceConfig(SourceConfig cfg) { this.sourceConfig = cfg; return this; }
        public Builder questionsPerRound(int q) { this.questionsPerRound = q; return this; }
        public Builder perTopicLimit(int l) { this.perTopicLimit = l; return this; }
        public Builder llmTimeoutMillis(int ms) { this.llmTimeoutMillis = ms; return this; }
        public Builder fallbackPolicy(FallbackPolicy p) { this.fallbackPolicy = p; return this; }

        public MixedTopicsConfig build() { return new MixedTopicsConfig(this); }
    }
}
