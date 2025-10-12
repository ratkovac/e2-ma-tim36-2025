package com.habitrpg.taskmanager.service;

import android.content.Context;
import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class XPService {
    
    private static XPService instance;
    private AppDatabase database;
    private FirebaseManager firebaseManager;
    private ExecutorService executor;
    
    public static final int VERY_EASY_XP = 1;
    public static final int EASY_XP = 3;
    public static final int HARD_XP = 7;
    public static final int EXTREME_XP = 20;
    
    public static final int NORMAL_XP = 1;
    public static final int IMPORTANT_XP = 3;
    public static final int VERY_IMPORTANT_XP = 10;
    public static final int SPECIAL_XP = 100;
    
    public static final int LEVEL_1_XP_REQUIREMENT = 200;
    
    private XPService(Context context) {
        database = AppDatabase.getDatabase(context);
        firebaseManager = FirebaseManager.getInstance();
        executor = Executors.newFixedThreadPool(2);
    }
    
    public static synchronized XPService getInstance(Context context) {
        if (instance == null) {
            instance = new XPService(context.getApplicationContext());
        }
        return instance;
    }
    
    public static int calculateTaskXP(String difficulty, String importance) {
        int difficultyXP = getDifficultyXP(difficulty);
        int importanceXP = getImportanceXP(importance);
        return difficultyXP + importanceXP;
    }
    
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
    
    public static int getXPRequiredForLevel(int level) {
        if (level <= 1) {
            return LEVEL_1_XP_REQUIREMENT;
        }
        
        int previousLevelXP = getXPRequiredForLevel(level - 1);
        return previousLevelXP * 2 + previousLevelXP / 2;
    }
    
    public static int getTotalXPRequiredForLevel(int level) {
        int totalXP = 0;
        for (int i = 1; i <= level; i++) {
            totalXP += getXPRequiredForLevel(i);
        }
        return totalXP;
    }
    
    public static int calculateLevelFromXP(int totalXP) {
        int level = 1;
        int xpAccumulated = 0;
        
        while (xpAccumulated + getXPRequiredForLevel(level) <= totalXP) {
            xpAccumulated += getXPRequiredForLevel(level);
            level++;
        }
        
        return level - 1;
    }
    
    public static float calculateLevelProgress(int currentXP, int currentLevel) {
        int xpForCurrentLevel = getTotalXPRequiredForLevel(currentLevel);
        int xpForNextLevel = getTotalXPRequiredForLevel(currentLevel + 1);
        int xpNeededForNextLevel = xpForNextLevel - xpForCurrentLevel;
        int xpProgressToNextLevel = currentXP - xpForCurrentLevel;
        
        if (xpNeededForNextLevel <= 0) return 100f;
        
        return Math.max(0f, Math.min(100f, (float) xpProgressToNextLevel / xpNeededForNextLevel * 100f));
    }
    
    public static int getXPRemainingToNextLevel(int currentXP, int currentLevel) {
        int xpForNextLevel = getTotalXPRequiredForLevel(currentLevel + 1);
        return Math.max(0, xpForNextLevel - currentXP);
    }
    
    public static boolean didLevelUp(int previousXP, int newXP) {
        int previousLevel = calculateLevelFromXP(previousXP);
        int newLevel = calculateLevelFromXP(newXP);
        return newLevel > previousLevel;
    }
    
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
    
    public void addXP(int xpAmount, String userId, XPCallback callback) {
        executor.execute(() -> {
            try {
                User user = database.userDao().getUserById(userId);
                if (user != null) {
                    int newXP = user.getExperiencePoints() + xpAmount;
                    user.setExperiencePoints(newXP);
                    
                    int newLevel = calculateLevelFromXP(newXP);
                    if (newLevel > user.getLevel()) {
                        user.setLevel(newLevel);
                        user.setTitle(getTitleForLevel(newLevel));
                        
                        if (callback != null) {
                            callback.onLevelUp(newLevel, user.getTitle());
                        }
                    }
                    
                    database.userDao().updateUser(user);
                    
                    firebaseManager.updateUserDocument(user, (success, exception) -> {
                        if (callback != null) {
                            if (success) {
                                callback.onSuccess("XP added successfully");
                            } else {
                                callback.onError("Failed to sync XP: " + 
                                    (exception != null ? exception.getMessage() : "Unknown error"));
                            }
                        }
                    });
                } else {
                    if (callback != null) {
                        callback.onError("User not found");
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Failed to add XP: " + e.getMessage());
                }
            }
        });
    }
    
    public void addCoins(int coinAmount, String userId, XPCallback callback) {
        executor.execute(() -> {
            try {
                User user = database.userDao().getUserById(userId);
                if (user != null) {
                    user.setCoins(user.getCoins() + coinAmount);
                    database.userDao().updateUser(user);
                    
                    firebaseManager.updateUserDocument(user, (success, exception) -> {
                        if (callback != null) {
                            if (success) {
                                callback.onSuccess("Coins added successfully");
                            } else {
                                callback.onError("Failed to sync coins: " + 
                                    (exception != null ? exception.getMessage() : "Unknown error"));
                            }
                        }
                    });
                } else {
                    if (callback != null) {
                        callback.onError("User not found");
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Failed to add coins: " + e.getMessage());
                }
            }
        });
    }
    
    public interface XPCallback {
        void onSuccess(String message);
        void onError(String error);
        void onLevelUp(int newLevel, String title);
    }
}
