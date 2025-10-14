package com.habitrpg.taskmanager.service;

public class XPService {

    // Base XP Constants (Level 1)
    public static final int VERY_EASY_XP_BASE = 1;
    public static final int EASY_XP_BASE = 3;
    public static final int HARD_XP_BASE = 7;
    public static final int EXTREME_XP_BASE = 20;

    public static final int NORMAL_XP_BASE = 1;
    public static final int IMPORTANT_XP_BASE = 3;
    public static final int VERY_IMPORTANT_XP_BASE = 10;
    public static final int SPECIAL_XP_BASE = 100;

    public static final int LEVEL_1_PP_REWARD = 40;

    // Titles for first 5 levels
    public static final String[] LEVEL_TITLES = {
            "Beginner",      // Level 1
            "Apprentice",    // Level 2
            "Journeyman",    // Level 3
            "Expert",        // Level 4
            "Master"         // Level 5
    };

    // XP Calculation Methods
    public static int calculateTaskXP(String difficulty, String importance, int userLevel) {
        int difficultyXP = getDifficultyXP(difficulty, userLevel);
        int importanceXP = getImportanceXP(importance, userLevel);
        return difficultyXP + importanceXP;
    }

    public static int getDifficultyXP(String difficulty, int userLevel) {
        int baseXP = getBaseDifficultyXP(difficulty);
        return calculateScaledXP(baseXP, userLevel);
    }

    public static int getImportanceXP(String importance, int userLevel) {
        int baseXP = getBaseImportanceXP(importance);
        return calculateScaledXP(baseXP, userLevel);
    }

    private static int getBaseDifficultyXP(String difficulty) {
        switch (difficulty.toLowerCase()) {
            case "very_easy":
                return VERY_EASY_XP_BASE;
            case "easy":
                return EASY_XP_BASE;
            case "hard":
                return HARD_XP_BASE;
            case "extreme":
                return EXTREME_XP_BASE;
            default:
                return VERY_EASY_XP_BASE;
        }
    }

    private static int getBaseImportanceXP(String importance) {
        switch (importance.toLowerCase()) {
            case "normal":
                return NORMAL_XP_BASE;
            case "important":
                return IMPORTANT_XP_BASE;
            case "very_important":
                return VERY_IMPORTANT_XP_BASE;
            case "special":
                return SPECIAL_XP_BASE;
            default:
                return NORMAL_XP_BASE;
        }
    }

    private static int calculateScaledXP(int baseXP, int userLevel) {
        if (userLevel <= 1) {
            return baseXP;
        }

        int scaledXP = baseXP;
        for (int level = 2; level <= userLevel; level++) {
            scaledXP = scaledXP + scaledXP / 2;
            scaledXP = Math.round(scaledXP);
        }

        return scaledXP;
    }

    /**
     * Returns XP required to level up FROM a specific level TO the next level
     * From level 1 to 2: 200 XP
     * From level 2 to 3: 300 XP
     * From level 3 to 4: 750 XP
     */
    public static int getXPRequiredForLevel(int fromLevel) {
        if (fromLevel == 1) {
            return 200; // Need 200 total XP to reach level 2
        }
        if (fromLevel == 2) {
            return 300; // Need 300 more XP (500 total) to reach level 3
        }

        // Level 3+: calculate using formula
        int previousLevelTotal = getTotalXPRequiredForLevel(fromLevel);
        int nextLevelTotal = previousLevelTotal * 2 + previousLevelTotal / 2;

        // Round to nearest hundred
        nextLevelTotal = ((nextLevelTotal + 99) / 100) * 100;

        return nextLevelTotal - previousLevelTotal;
    }

    /**
     * Returns total cumulative XP needed to REACH a specific level
     * To reach level 1: 0 XP (you start at level 1)
     * To reach level 2: 200 total XP
     * To reach level 3: 500 total XP (200 + 300)
     * To reach level 4: 1250 total XP (500 + 750)
     */
    public static int getTotalXPRequiredForLevel(int level) {
        if (level <= 1) {
            return 0; // You START at level 1 with 0 XP
        }
        if (level == 2) {
            return 200; // Need 200 total XP to reach level 2
        }
        if (level == 3) {
            return 500; // Need 500 total XP to reach level 3
        }

        // Level 4+: calculate recursively
        int previousTotal = getTotalXPRequiredForLevel(level - 1);
        int calculatedTotal = previousTotal * 2 + previousTotal / 2;

        // Round to nearest hundred
        return ((calculatedTotal + 99) / 100) * 100;
    }

    /**
     * Calculates current level based on total accumulated XP
     * 0-199 XP = Level 1
     * 200-499 XP = Level 2
     * 500-1249 XP = Level 3
     * etc.
     */
    public static int calculateLevelFromXP(int totalXP) {
        if (totalXP < 200) return 1;
        if (totalXP < 500) return 2;
        if (totalXP < 1250) return 3;

        // For higher levels, calculate dynamically
        int level = 3;
        while (totalXP >= getTotalXPRequiredForLevel(level + 1)) {
            level++;
        }

        return level;
    }

    /**
     * Calculates progress percentage toward next level based on total accumulated XP
     */
    public static float calculateLevelProgress(int totalXP, int currentLevel) {
        int xpForCurrentLevel = getTotalXPRequiredForLevel(currentLevel);
        int xpForNextLevel = getTotalXPRequiredForLevel(currentLevel + 1);
        int xpNeededForNextLevel = xpForNextLevel - xpForCurrentLevel;
        int xpProgressToNextLevel = totalXP - xpForCurrentLevel;

        if (xpNeededForNextLevel <= 0) return 100f;

        return Math.max(0f, Math.min(100f, (float) xpProgressToNextLevel / xpNeededForNextLevel * 100f));
    }

    /**
     * Returns XP remaining to reach next level based on total accumulated XP
     */
    public static int getXPRemainingToNextLevel(int totalXP, int currentLevel) {
        int xpForNextLevel = getTotalXPRequiredForLevel(currentLevel + 1);
        return Math.max(0, xpForNextLevel - totalXP);
    }

    /**
     * Alternative calculation (kept for compatibility)
     */
    public static float calculateLevelProgressSimple(int totalXP, int currentLevel) {
        return calculateLevelProgress(totalXP, currentLevel);
    }

    public static int getXPRemainingToNextLevelSimple(int totalXP, int currentLevel) {
        return getXPRemainingToNextLevel(totalXP, currentLevel);
    }

    // PP Calculation Methods
    public static int getPPRewardForLevel(int level) {
        if (level <= 1) {
            return LEVEL_1_PP_REWARD;
        }

        int previousLevelPP = getPPRewardForLevel(level - 1);
        int calculatedPP = previousLevelPP + (3 * previousLevelPP) / 4;

        return calculatedPP;
    }

    public static int getTotalPPEarned(int level) {
        int totalPP = 0;
        for (int i = 1; i <= level; i++) {
            totalPP += getPPRewardForLevel(i);
        }
        return totalPP;
    }

    // Title Methods
    public static String getTitleForLevel(int level) {
        if (level <= 0) {
            return "Beginner";
        }
        if (level > LEVEL_TITLES.length) {
            return "Grandmaster";
        }
        return LEVEL_TITLES[level - 1];
    }

    public static String getNextTitle(int currentLevel) {
        if (currentLevel <= 0) {
            return "Apprentice";
        }
        if (currentLevel >= LEVEL_TITLES.length) {
            return "Grandmaster";
        }
        return LEVEL_TITLES[currentLevel];
    }

    // Legacy methods for backward compatibility
    public static int calculateTaskXP(String difficulty, String importance) {
        return calculateTaskXP(difficulty, importance, 1);
    }

    public static int getDifficultyXP(String difficulty) {
        return getDifficultyXP(difficulty, 1);
    }

    public static int getImportanceXP(String importance) {
        return getImportanceXP(importance, 1);
    }
}