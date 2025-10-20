package com.habitrpg.taskmanager.service;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.entities.Boss;
import com.habitrpg.taskmanager.data.database.entities.Equipment;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.data.database.entities.TaskCompletion;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.repository.UserRepository;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BossService {
    
    private static BossService instance;
    private final AppDatabase database;
    private final UserRepository userRepository;
    private final UserPreferences userPreferences;
    private final com.habitrpg.taskmanager.data.repository.SpecialMissionRepository specialMissionRepository;
    
    // Boss fight constants
    private static final int FIRST_BOSS_HP = 200;
    private static final int MAX_ATTACKS = 5;
    private static final int BASE_COIN_REWARD = 200;
    private static final double COIN_INCREASE_RATE = 0.20; // 20% increase
    private static final double EQUIPMENT_DROP_CHANCE = 1.0; // 100% chance - always drop equipment
    private static final double CLOTHING_DROP_CHANCE = 0.50; // 50% of equipment drops
    private static final double WEAPON_DROP_CHANCE = 0.50; // 50% of equipment drops
    private static final double PARTIAL_VICTORY_THRESHOLD = 0.50; // 50% HP damage for partial victory
    
    private BossService(Context context) {
        database = AppDatabase.getDatabase(context);
        userRepository = UserRepository.getInstance(context);
        userPreferences = UserPreferences.getInstance(context);
        specialMissionRepository = com.habitrpg.taskmanager.data.repository.SpecialMissionRepository.getInstance(context);
    }
    
    public static synchronized BossService getInstance(Context context) {
        if (instance == null) {
            instance = new BossService(context);
        }
        return instance;
    }
    
    public interface BossCallback {
        void onSuccess(String message);
        void onError(String error);
        void onBossRetrieved(Boss boss);
        void onBossFightResult(BossFightResult result);
        default void onDefeatedBossCountRetrieved(int count) {}
    }
    
    public static class BossFightResult {
        private boolean victory;
        private boolean partialVictory;
        private int coinsEarned;
        private String equipmentEarned;
        private boolean equipmentDropped;
        private int damageDealt;
        private int attacksUsed;
        private int bossMaxHp;
        private int bossCurrentHp;
        
        public BossFightResult(boolean victory, boolean partialVictory, int coinsEarned, 
                             String equipmentEarned, boolean equipmentDropped, int damageDealt, 
                             int attacksUsed, int bossMaxHp, int bossCurrentHp) {
            this.victory = victory;
            this.partialVictory = partialVictory;
            this.coinsEarned = coinsEarned;
            this.equipmentEarned = equipmentEarned;
            this.equipmentDropped = equipmentDropped;
            this.damageDealt = damageDealt;
            this.attacksUsed = attacksUsed;
            this.bossMaxHp = bossMaxHp;
            this.bossCurrentHp = bossCurrentHp;
        }
        
        // Getters
        public boolean isVictory() { return victory; }
        public boolean isPartialVictory() { return partialVictory; }
        public int getCoinsEarned() { return coinsEarned; }
        public String getEquipmentEarned() { return equipmentEarned; }
        public boolean isEquipmentDropped() { return equipmentDropped; }
        public int getDamageDealt() { return damageDealt; }
        public int getAttacksUsed() { return attacksUsed; }
        public int getBossMaxHp() { return bossMaxHp; }
        public int getBossCurrentHp() { return bossCurrentHp; }
        public float getHpDamagePercentage() { 
            return (float) damageDealt / bossMaxHp * 100f; 
        }
    }
    
    /**
     * Creates a new boss for the specified level
     */
    public void createBossForLevel(int level, BossCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        // Use background thread for database operations
        new Thread(() -> {
            // Check if boss already exists for this level
            Boss existingBoss = database.bossDao().getBossByUserAndLevel(userId, level);
            if (existingBoss != null && !existingBoss.isDefeated()) {
                callback.onBossRetrieved(existingBoss);
                return;
            }
            
            // Calculate boss HP using the formula
            int bossHp = calculateBossHp(level);
            
            Boss newBoss = new Boss(userId, level, bossHp);
            database.bossDao().insertBoss(newBoss);
            
            callback.onBossRetrieved(newBoss);
        }).start();
    }
    
    /**
     * Calculates boss HP based on boss level (not user level)
     * Boss level 1 = 200 HP, Boss level 2 = 500 HP, etc.
     */
    private int calculateBossHp(int bossLevel) {
        if (bossLevel <= 1) {
            return FIRST_BOSS_HP; // 200 HP for first boss
        }
        
        // For boss level > 1, calculate based on boss level
        // Use a simple formula: 200 * (2.5 ^ (bossLevel - 1))
        // This ensures boss level 1 = 200, boss level 2 = 500, boss level 3 = 1250, etc.
        return (int) (FIRST_BOSS_HP * Math.pow(2.5, bossLevel - 1));
    }
    
    /**
     * Gets boss for specific level transition - used when user levels up
     * This ensures the correct boss is created/retrieved for the level transition
     */
    public void getBossForLevelTransition(int userLevel, BossCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        userRepository.getUserById(userId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError("Failed to get user: " + error);
            }
            
            @Override
            public void onUserRetrieved(User user) {
                if (user != null) {
                    new Thread(() -> {
                        // First, check if there are any undefeated bosses with level LOWER than current boss level
                        int expectedBossLevel = userLevel - 1; // Boss level for this transition
                        List<Boss> undefeatedBosses = database.bossDao().getActiveBossesByUser(userId);
                        
                        // Filter to only get bosses with level lower than expected boss level
                        Boss oldestUndefeatedBoss = null;
                        for (Boss boss : undefeatedBosses) {
                            if (boss.getLevel() < expectedBossLevel) {
                                if (oldestUndefeatedBoss == null || boss.getLevel() < oldestUndefeatedBoss.getLevel()) {
                                    oldestUndefeatedBoss = boss;
                                }
                            }
                        }
                        
                        if (oldestUndefeatedBoss != null) {
                            // Return the oldest undefeated boss (lowest level)
                            Boss finalBoss = oldestUndefeatedBoss;
                            callback.onBossRetrieved(finalBoss);
                            return;
                        }
                        
                        // No old undefeated bosses, check if boss for current level transition exists
                        Boss currentLevelBoss = database.bossDao().getBossByUserAndLevel(userId, expectedBossLevel);
                        if (currentLevelBoss != null) {
                            callback.onBossRetrieved(currentLevelBoss);
                        } else {
                            // Create new boss for the current level transition
                            createBossForLevel(expectedBossLevel, callback);
                        }
                    }).start();
                } else {
                    callback.onError("User not found");
                }
            }
        });
    }
    
    /**
     * Gets existing bosses without creating new ones - used for checking if there are bosses to fight
     */
    public void getExistingBosses(BossCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        // Use background thread for database operations
        new Thread(() -> {
            // Check if there are any undefeated bosses
            List<Boss> undefeatedBosses = database.bossDao().getActiveBossesByUser(userId);
            
            if (!undefeatedBosses.isEmpty()) {
                // Return the first undefeated boss (lowest level)
                Boss firstUndefeatedBoss = undefeatedBosses.get(0);
                callback.onBossRetrieved(firstUndefeatedBoss);
            } else {
                // No undefeated bosses found - return error to indicate no bosses available
                callback.onError("No bosses available");
            }
        }).start();
    }
    
    /**
     * Gets the current boss - prioritizes undefeated bosses over creating new ones
     */
    public void getCurrentBoss(BossCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        userRepository.getUserById(userId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError("Failed to get user: " + error);
            }
            
            @Override
            public void onUserRetrieved(User user) {
                if (user != null) {
                    // Use background thread for database operations
                    new Thread(() -> {
                        // First, check if there are any undefeated bosses
                        List<Boss> undefeatedBosses = database.bossDao().getActiveBossesByUser(userId);
                        
                        if (!undefeatedBosses.isEmpty()) {
                            // Return the first undefeated boss (lowest level)
                            Boss firstUndefeatedBoss = undefeatedBosses.get(0);
                            callback.onBossRetrieved(firstUndefeatedBoss);
                            return;
                        }
                        
                        // No undefeated bosses found, create new boss for next level
                        int defeatedBosses = database.bossDao().getDefeatedBossCount(userId);
                        int bossLevel = defeatedBosses + 1; // Next boss to fight
                        
                        Boss currentBoss = database.bossDao().getCurrentBossForLevel(userId, bossLevel);
                        if (currentBoss != null) {
                            callback.onBossRetrieved(currentBoss);
                        } else {
                            // Create new boss for the next level
                            createBossForLevel(bossLevel, callback);
                        }
                    }).start();
                } else {
                    callback.onError("User not found");
                }
            }
        });
    }
    
    /**
     * Gets the number of defeated bosses for the current user
     */
    public void getDefeatedBossCount(BossCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        new Thread(() -> {
            try {
                int defeatedBosses = database.bossDao().getDefeatedBossCount(userId);
                callback.onDefeatedBossCountRetrieved(defeatedBosses);
            } catch (Exception e) {
                callback.onError("Failed to get defeated boss count: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Calculates task success rate for the current stage (between levels)
     */
    public void calculateTaskSuccessRate(BossCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        userRepository.getUserById(userId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError("Failed to get user: " + error);
            }
            
            @Override
            public void onUserRetrieved(User user) {
                if (user != null) {
                    // Use background thread for database operations
                    new Thread(() -> {
                        // Get stage times
                        long currentStageStartTime = userPreferences.getCurrentStageStartTime();
                        long previousStageStartTime = userPreferences.getPreviousStageStartTime();
                        long currentTime = System.currentTimeMillis();
                        
                        // Debug logging
                        System.out.println("DEBUG: User level: " + user.getLevel());
                        System.out.println("DEBUG: Current stage start time: " + currentStageStartTime);
                        System.out.println("DEBUG: Previous stage start time: " + previousStageStartTime);
                        System.out.println("DEBUG: Current time: " + currentTime);
                        
                        // Get all tasks for user
                        List<Task> allTasks = database.taskDao().getAllTasksByUser(userId);
                        
                        // Debug logging
                        System.out.println("DEBUG: Total tasks: " + allTasks.size());
                        
                        // Filter tasks based on stage transition
                        List<Task> stageTasks = filterTasksByStage(allTasks, previousStageStartTime, currentStageStartTime, user.getLevel());
                        
                        // Debug logging
                        System.out.println("DEBUG: Stage tasks: " + stageTasks.size());
                        
                        // Count valid tasks and completed tasks for this stage
                        int validTasks = 0;
                        int completedTasks = 0;
                        
                        for (Task task : stageTasks) {
                            String status = task.getStatus();
                            if (!"paused".equals(status) && !"cancelled".equals(status)) {
                                validTasks++;
                                if ("completed".equals(status)) {
                                    completedTasks++;
                                }
                            }
                        }
                        
                        // Debug logging
                        System.out.println("DEBUG: Stage valid tasks: " + validTasks);
                        System.out.println("DEBUG: Stage completed tasks: " + completedTasks);
                        
                        // Calculate success rate based on task status for this stage
                        int successRate = 0;
                        if (validTasks > 0) {
                            successRate = (completedTasks * 100) / validTasks;
                            if (successRate > 100) {
                                successRate = 100;
                            }
                        }
                        
                        System.out.println("DEBUG: Final stage success rate: " + successRate + "%");
                        callback.onSuccess(String.valueOf(successRate));
                    }).start();
                } else {
                    callback.onError("User not found");
                }
            }
        });
    }
    
    /**
     * Filters tasks based on stage transition logic
     * - Level 1 to 2: All tasks (no previous stage time)
     * - Level 2+: Tasks created between previousStageStartTime and currentStageStartTime
     */
    private List<Task> filterTasksByStage(List<Task> allTasks, long previousStageStartTime, long currentStageStartTime, int currentLevel) {
        List<Task> stageTasks = new ArrayList<>();
        
        for (Task task : allTasks) {
            // For level 1 to 2 transition, include all tasks
            if (currentLevel == 2 && previousStageStartTime == 0) {
                stageTasks.add(task);
                continue;
            }
            
            // For level 2+ transitions, filter by stage time period
            if (currentLevel > 2 && previousStageStartTime > 0) {
                long taskTimestamp = convertStartDateToTimestamp(task.getStartDate());
                
                // Include tasks created between previousStageStartTime and currentStageStartTime
                if (taskTimestamp >= previousStageStartTime && taskTimestamp < currentStageStartTime) {
                    stageTasks.add(task);
                }
            }
        }
        
        return stageTasks;
    }
    
    /**
     * Converts start_date string to timestamp for comparison
     */
    private long convertStartDateToTimestamp(String startDate) {
        if (startDate == null || startDate.isEmpty()) {
            return 0;
        }
        
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(startDate);
            return date.getTime();
        } catch (Exception e) {
            System.out.println("DEBUG: Error parsing start_date: " + startDate + ", error: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Performs a boss attack
     */
    public void performBossAttack(int bossId, int playerPp, BossCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        // Use background thread for database operations
        new Thread(() -> {
            Boss boss = database.bossDao().getBossByUserAndLevel(userId, bossId);
            if (boss == null || boss.isDefeated()) {
                callback.onError("Boss not found or already defeated");
                return;
            }
        
            // Calculate success rate
            calculateTaskSuccessRate(new BossCallback() {
                @Override
                public void onSuccess(String message) {
                    int successRate = Integer.parseInt(message);
                    
                    // Determine if attack hits
                    Random random = new Random();
                    boolean attackHits = random.nextInt(100) < successRate;
                    
                    if (attackHits) {
                        boss.takeDamage(playerPp);
                        database.bossDao().updateBoss(boss);

                        // Inform special mission progress about a successful regular boss hit
                        String currentUserId = userPreferences.getCurrentUserId();
                        if (currentUserId != null) {
                            specialMissionRepository.recordRegularBossHit(currentUserId, new com.habitrpg.taskmanager.data.repository.SpecialMissionRepository.ProgressUpdateCallback() {
                                @Override
                                public void onUpdated(String message) { /* no-op */ }
                                @Override
                                public void onNoActiveMission() { /* no-op */ }
                                @Override
                                public void onError(String error) { /* no-op */ }
                            });
                        }
                        
                        if (boss.isDefeated()) {
                            // Boss defeated - calculate rewards
                            calculateBossRewards(boss, callback);
                        } else {
                            // Return updated boss data along with success message
                            callback.onBossRetrieved(boss);
                            callback.onSuccess("Attack successful! Boss took " + playerPp + " damage.");
                        }
                    } else {
                        callback.onSuccess("Attack missed! Boss remains unharmed.");
                    }
                }
                
                @Override
                public void onError(String error) {
                    callback.onError("Failed to calculate success rate: " + error);
                }
                
                @Override
                public void onBossRetrieved(Boss boss) {}
                
                @Override
                public void onBossFightResult(BossFightResult result) {}
            });
        }).start();
    }
    
    /**
     * Ends boss fight after 5 attacks and calculates rewards
     */
    public void endBossFight(Boss boss, int attacksUsed, BossCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        // Use background thread for database operations
        new Thread(() -> {
            final boolean victory = boss.isDefeated();
            final boolean partialVictory = !victory && (boss.getHpPercentage() <= 50.0f);
            
            final int damageDealt = boss.getMaxHp() - boss.getCurrentHp();
            
            // Calculate base rewards - use boss level instead of defeated bosses count
            int bossLevel = boss.getLevel();
            int baseCoinReward = (int) (BASE_COIN_REWARD * Math.pow(1 + COIN_INCREASE_RATE, bossLevel - 1));
            
            // Apply coin reward bonus from Bow and Arrow
            double coinRewardBonus = getCoinRewardBonus(userId);
            int boostedCoinReward = (int) (baseCoinReward * (1 + coinRewardBonus / 100.0));
            
            // Adjust rewards based on result
            final int finalCoinReward;
            double equipmentChance;
            
            if (victory) {
                finalCoinReward = boostedCoinReward;
                equipmentChance = EQUIPMENT_DROP_CHANCE;
            } else if (partialVictory) {
                finalCoinReward = boostedCoinReward / 2; // Half rewards
                equipmentChance = EQUIPMENT_DROP_CHANCE / 2; // Half chance
            } else {
                finalCoinReward = 0; // No rewards for defeat
                equipmentChance = 0;
            }
            
            // Check for equipment drop
            Random random = new Random();
            final boolean equipmentDropped = random.nextDouble() < equipmentChance;
            final String equipmentEarned;
            
            if (equipmentDropped) {
                if (random.nextDouble() < CLOTHING_DROP_CHANCE) {
                    equipmentEarned = "Clothing";
                } else {
                    equipmentEarned = "Weapon";
                }
            } else {
                equipmentEarned = null;
            }
            
            // Update user coins if any rewards
            if (finalCoinReward > 0) {
                userRepository.getUserById(userId, new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(String message) {}
                    
                    @Override
                    public void onError(String error) {
                        callback.onError("Failed to update user coins: " + error);
                    }
                    
                    @Override
                    public void onUserRetrieved(User user) {
                        if (user != null) {
                            user.setCoins(user.getCoins() + finalCoinReward);
                            userRepository.updateUser(user, new UserRepository.UserCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    BossFightResult result = new BossFightResult(
                                        victory, partialVictory, finalCoinReward, equipmentEarned, 
                                        equipmentDropped, damageDealt, attacksUsed, 
                                        boss.getMaxHp(), boss.getCurrentHp()
                                    );
                                    callback.onBossFightResult(result);
                                }
                                
                                @Override
                                public void onError(String error) {
                                    callback.onError("Failed to update user: " + error);
                                }
                                
                                @Override
                                public void onUserRetrieved(User user) {}
                            });
                        } else {
                            callback.onError("User not found");
                        }
                    }
                });
            } else {
                // No coins to update, just return result
                BossFightResult result = new BossFightResult(
                    victory, partialVictory, finalCoinReward, equipmentEarned, 
                    equipmentDropped, damageDealt, attacksUsed, 
                    boss.getMaxHp(), boss.getCurrentHp()
                );
                callback.onBossFightResult(result);
            }
        }).start();
    }
    
    /**
     * Gets the coin reward bonus from Bow and Arrow equipment
     */
    private double getCoinRewardBonus(String userId) {
        double coinRewardBonus = 0.0;
        try {
            List<Equipment> userEquipment = database.equipmentDao().getUserEquipment(userId);
            for (Equipment equipment : userEquipment) {
                if (equipment.isActive() && "coin_bonus".equals(equipment.getBonusType())) {
                    coinRewardBonus += equipment.getBonusValue();
                }
            }
        } catch (Exception e) {
            android.util.Log.e("BossService", "Error getting coin reward bonus: " + e.getMessage());
        }
        return coinRewardBonus;
    }
    
    /**
     * Calculates rewards for defeating a boss
     */
    private void calculateBossRewards(Boss boss, BossCallback callback) {
        // Use background thread for database operations
        new Thread(() -> {
            // Calculate coin reward - use boss level instead of defeated bosses count
            int bossLevel = boss.getLevel();
            int baseCoinReward = (int) (BASE_COIN_REWARD * Math.pow(1 + COIN_INCREASE_RATE, bossLevel - 1));
            
            // Apply coin reward bonus from Bow and Arrow
            String userId = boss.getUserId();
            double coinRewardBonus = getCoinRewardBonus(userId);
            int coinReward = (int) (baseCoinReward * (1 + coinRewardBonus / 100.0));
        
            // Check for equipment drop
            Random random = new Random();
            boolean equipmentDropped = random.nextDouble() < EQUIPMENT_DROP_CHANCE;
            final String equipmentEarned;
            
            if (equipmentDropped) {
                if (random.nextDouble() < CLOTHING_DROP_CHANCE) {
                    equipmentEarned = "Clothing";
                } else {
                    equipmentEarned = "Weapon";
                }
            } else {
                equipmentEarned = null;
            }
            
            // Update user coins
            userRepository.getUserById(userId, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(String message) {}
                
                @Override
                public void onError(String error) {
                    callback.onError("Failed to update user coins: " + error);
                }
                
                @Override
                public void onUserRetrieved(User user) {
                    if (user != null) {
                        user.setCoins(user.getCoins() + coinReward);
                        userRepository.updateUser(user, new UserRepository.UserCallback() {
                            @Override
                            public void onSuccess(String message) {
                                BossFightResult result = new BossFightResult(
                                    true, false, coinReward, equipmentEarned, equipmentDropped, 
                                    boss.getMaxHp(), MAX_ATTACKS, boss.getMaxHp(), boss.getCurrentHp()
                                );
                                callback.onBossFightResult(result);
                            }
                            
                            @Override
                            public void onError(String error) {
                                callback.onError("Failed to update user: " + error);
                            }
                            
                            @Override
                            public void onUserRetrieved(User user) {}
                        });
                    } else {
                        callback.onError("User not found");
                    }
                }
            });
        }).start();
    }
    
    /**
     * Helper method to check if a task was created during the current stage
     */
    private boolean isTaskInStage(Task task, long stageStartTime, long stageEndTime) {
        if (task.getStartDate() == null || task.getStartDate().isEmpty()) {
            System.out.println("DEBUG: Task " + task.getId() + " has no start date");
            return false;
        }
        
        try {
            // Parse task start date (format: YYYY-MM-DD HH:MM)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date taskDate = sdf.parse(task.getStartDate());
            long taskTimestamp = taskDate.getTime();
            
            System.out.println("DEBUG: Task " + task.getId() + " start date: " + task.getStartDate() + 
                             " -> timestamp: " + taskTimestamp + 
                             " (stage start: " + stageStartTime + ", stage end: " + stageEndTime + ")");
            
            // Check if task was created between stage start and end
            boolean inStage = taskTimestamp >= stageStartTime && taskTimestamp <= stageEndTime;
            System.out.println("DEBUG: Task " + task.getId() + " in stage: " + inStage);
            return inStage;
        } catch (Exception e) {
            // If parsing fails, exclude the task
            System.out.println("DEBUG: Failed to parse task " + task.getId() + " start date: " + task.getStartDate() + " - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method to check if a task completion happened during the current stage
     */
    private boolean isCompletionInStage(TaskCompletion completion, long stageStartTime, long stageEndTime) {
        // Use createdAt timestamp for completion time
        long completionTimestamp = completion.getCreatedAt();
        
        System.out.println("DEBUG: Completion " + completion.getId() + " timestamp: " + completionTimestamp + 
                         " (stage start: " + stageStartTime + ", stage end: " + stageEndTime + ")");
        
        // Check if completion happened between stage start and end
        boolean inStage = completionTimestamp >= stageStartTime && completionTimestamp <= stageEndTime;
        System.out.println("DEBUG: Completion " + completion.getId() + " in stage: " + inStage);
        return inStage;
    }
}
