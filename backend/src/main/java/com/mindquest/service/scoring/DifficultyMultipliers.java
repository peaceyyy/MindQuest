package com.mindquest.service.scoring;

/**
 * Encapsulates difficulty-based scoring multipliers for game balance.
 * Centralizes multiplier configuration to facilitate tuning and maintain Single Responsibility Principle.
 * 
 * <p>Design Decision: Extracted from GameService to separate game balance configuration
 * from game logic execution. This follows the Open/Closed Principle - extending difficulty
 * tiers doesn't require modifying GameService.</p>
 */
public final class DifficultyMultipliers {
    
    // XP (Experience Points) Multipliers
    private static final double EASY_XP_MULTIPLIER = 1.0;     // Baseline - fundamentals
    private static final double MEDIUM_XP_MULTIPLIER = 1.5;   // 50% bonus - moderate challenge
    private static final double HARD_XP_MULTIPLIER = 2.5;     // 150% bonus - high difficulty reward
    
    // Critical Hit Multipliers (speed bonus)
    private static final double EASY_CRIT_MULTIPLIER = 1.05;  // 5% bonus - minimal reward for speed on simple questions
    private static final double MEDIUM_CRIT_MULTIPLIER = 1.15; // 15% bonus - baseline critical hit
    private static final double HARD_CRIT_MULTIPLIER = 1.25;  // 25% bonus - reward careful, fast reading on difficult content
    
    // Default fallback values
    private static final double DEFAULT_XP_MULTIPLIER = EASY_XP_MULTIPLIER;
    private static final double DEFAULT_CRIT_MULTIPLIER = MEDIUM_CRIT_MULTIPLIER;
    
    /**
     * Private constructor prevents instantiation of utility class.
     */
    private DifficultyMultipliers() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Gets the experience points multiplier for a given difficulty level.
     * 
     * <p>Multiplier Tiers:</p>
     * <ul>
     *   <li>Easy: 1.0× (baseline scoring for fundamental questions)</li>
     *   <li>Medium: 1.5× (50% XP boost for moderate difficulty)</li>
     *   <li>Hard: 2.5× (150% XP boost for challenging content)</li>
     * </ul>
     * 
     * @param difficulty The difficulty level (case-insensitive: "Easy", "Medium", "Hard")
     * @return The XP multiplier (1.0 for Easy, 1.5 for Medium, 2.5 for Hard)
     */
    public static double getXpMultiplier(String difficulty) {
        if (difficulty == null) {
            return DEFAULT_XP_MULTIPLIER;
        }
        
        return switch (difficulty.toLowerCase()) {
            case "easy" -> EASY_XP_MULTIPLIER;
            case "medium" -> MEDIUM_XP_MULTIPLIER;
            case "hard" -> HARD_XP_MULTIPLIER;
            default -> DEFAULT_XP_MULTIPLIER;
        };
    }
    
    /**
     * Gets the critical hit multiplier for a given difficulty level.
     * Critical hits occur when the player answers within a short time threshold.
     * 
     * <p>Multiplier Tiers (Inverted Design):</p>
     * <ul>
     *   <li>Easy: 1.05× (5% bonus - speed matters less for simple questions)</li>
     *   <li>Medium: 1.15× (15% bonus - baseline critical hit reward)</li>
     *   <li>Hard: 1.25× (25% bonus - higher reward for demonstrating mastery under pressure)</li>
     * </ul>
     * 
     * @param difficulty The difficulty level (case-insensitive: "Easy", "Medium", "Hard")
     * @return The critical hit multiplier (1.05 for Easy, 1.15 for Medium, 1.25 for Hard)
     */
    public static double getCritMultiplier(String difficulty) {
        if (difficulty == null) {
            return DEFAULT_CRIT_MULTIPLIER;
        }
        
        return switch (difficulty.toLowerCase()) {
            case "easy" -> EASY_CRIT_MULTIPLIER;
            case "medium" -> MEDIUM_CRIT_MULTIPLIER;
            case "hard" -> HARD_CRIT_MULTIPLIER;
            default -> DEFAULT_CRIT_MULTIPLIER;
        };
    }
}
