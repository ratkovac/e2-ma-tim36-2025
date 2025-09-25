package com.habitrpg.taskmanager.business.xp;

public class XPCalculator {
    
    // Difficulty XP values
    public static final int VERY_EASY_XP = 1;
    public static final int EASY_XP = 3;
    public static final int HARD_XP = 7;
    public static final int EXTREME_XP = 20;
    
    // Importance XP values
    public static final int NORMAL_XP = 1;
    public static final int IMPORTANT_XP = 3;
    public static final int VERY_IMPORTANT_XP = 10;
    public static final int SPECIAL_XP = 100;
    
    // Level system constants
    public static final int LEVEL_1_XP_REQUIREMENT = 200;
    
    /**
     * Calculate XP value for a task based on difficulty and importance
     */
    public static int calculateTaskXP(String difficulty, String importance) {
        int difficultyXP = getDifficultyXP(difficulty);
        int importanceXP = getImportanceXP(importance);
        return difficultyXP + importanceXP;
    }
    
    /**
     * Get XP value for difficulty level
     */
    public static int getDifficultyXP(String difficulty) {
        switch (difficulty.toLowerCase()) {
            case "very_easy":
                return VERY_EASY_XP;
            case "easy":
                return EASY_XP;
            case "hard":
                return HARD_XP;
            case "extreme":
                return EXTREME_XP;
            default:
                return VERY_EASY_XP;
        }
    }
    
    /**
     * Get XP value for importance level
     */
    public static int getImportanceXP(String importance) {
        switch (importance.toLowerCase()) {
            case "normal":
                return NORMAL_XP;
            case "important":
                return IMPORTANT_XP;
            case "very_important":
                return VERY_IMPORTANT_XP;
            case "special":
                return SPECIAL_XP;
            default:
                return NORMAL_XP;
        }
    }
    
    /**
     * Calculate required XP for a specific level
     * Formula: Level 1 requires 200 XP, then previous_xp * 2 + previous_xp / 2
     */
    public static int getXPRequiredForLevel(int level) {
        if (level <= 1) {
            return LEVEL_1_XP_REQUIREMENT;
        }
        
        int previousLevelXP = getXPRequiredForLevel(level - 1);
        return previousLevelXP * 2 + previousLevelXP / 2;
    }
    
    /**
     * Calculate total XP required up to a specific level
     */
    public static int getTotalXPRequiredForLevel(int level) {
        int totalXP = 0;
        for (int i = 1; i <= level; i++) {
            totalXP += getXPRequiredForLevel(i);
        }
        return totalXP;
    }
    
    /**
     * Calculate user's level based on total XP
     */
    public static int calculateLevelFromXP(int totalXP) {
        int level = 1;
        int xpAccumulated = 0;
        
        while (xpAccumulated + getXPRequiredForLevel(level) <= totalXP) {
            xpAccumulated += getXPRequiredForLevel(level);
            level++;
        }
        
        return level - 1; // Return the completed level
    }
    
    /**
     * Calculate progress percentage to next level
     */
    public static float calculateLevelProgress(int currentXP, int currentLevel) {
        int xpForCurrentLevel = getTotalXPRequiredForLevel(currentLevel);
        int xpForNextLevel = getTotalXPRequiredForLevel(currentLevel + 1);
        int xpNeededForNextLevel = xpForNextLevel - xpForCurrentLevel;
        int xpProgressToNextLevel = currentXP - xpForCurrentLevel;
        
        if (xpNeededForNextLevel <= 0) return 100f;
        
        return Math.max(0f, Math.min(100f, (float) xpProgressToNextLevel / xpNeededForNextLevel * 100f));
    }
    
    /**
     * Get XP remaining to reach next level
     */
    public static int getXPRemainingToNextLevel(int currentXP, int currentLevel) {
        int xpForNextLevel = getTotalXPRequiredForLevel(currentLevel + 1);
        return Math.max(0, xpForNextLevel - currentXP);
    }
    
    /**
     * Check if user leveled up after gaining XP
     */
    public static boolean didLevelUp(int previousXP, int newXP) {
        int previousLevel = calculateLevelFromXP(previousXP);
        int newLevel = calculateLevelFromXP(newXP);
        return newLevel > previousLevel;
    }
    
    /**
     * Get appropriate title for level
     */
    public static String getTitleForLevel(int level) {
        if (level >= 50) return "Grandmaster";
        if (level >= 40) return "Master";
        if (level >= 30) return "Expert";
        if (level >= 20) return "Professional";
        if (level >= 15) return "Advanced";
        if (level >= 10) return "Skilled";
        if (level >= 5) return "Apprentice";
        return "Beginner";
    }
}
