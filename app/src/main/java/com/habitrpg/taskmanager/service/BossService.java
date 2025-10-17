package com.habitrpg.taskmanager.service;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.entities.Boss;
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
    
    // Boss fight constants
    private static final int FIRST_BOSS_HP = 200;
    private static final int MAX_ATTACKS = 5;
    private static final int BASE_COIN_REWARD = 200;
    private static final double COIN_INCREASE_RATE = 0.20; // 20% increase
    private static final double EQUIPMENT_DROP_CHANCE = 0.20; // 20% chance
    private static final double CLOTHING_DROP_CHANCE = 0.95; // 95% of equipment drops
    private static final double WEAPON_DROP_CHANCE = 0.05; // 5% of equipment drops
    private static final double PARTIAL_VICTORY_THRESHOLD = 0.50; // 50% HP damage for partial victory
    
    private BossService(Context context) {
        database = AppDatabase.getDatabase(context);
        userRepository = UserRepository.getInstance(context);
        userPreferences = UserPreferences.getInstance(context);
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
     * Gets the current boss based on defeated bosses count
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
                        // Calculate boss level based on defeated bosses count
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
                        
                        // Get all completed tasks for user
                        List<TaskCompletion> allCompletions = database.taskCompletionDao().getAllCompletionsByUser(userId);
                        
                        // Debug logging
                        System.out.println("DEBUG: Total tasks: " + allTasks.size());
                        System.out.println("DEBUG: Total completions: " + allCompletions.size());
                        
                        // Filter tasks for current stage only
                        List<Task> stageTasks = new ArrayList<>();
                        List<TaskCompletion> stageCompletions = new ArrayList<>();
                        
                        if (previousStageStartTime == 0) {
                            // If no previous stage start time (level 1), use all tasks
                            System.out.println("DEBUG: Using all tasks (level 1)");
                            stageTasks.addAll(allTasks);
                            stageCompletions.addAll(allCompletions);
                        } else {
                            // Filter tasks created between previousStageStartTime and currentStageStartTime
                            System.out.println("DEBUG: Filtering tasks between previous and current stage start");
                            for (Task task : allTasks) {
                                if (isTaskInStage(task, previousStageStartTime, currentStageStartTime)) {
                                    stageTasks.add(task);
                                }
                            }
                            
                            // Filter completions completed between previousStageStartTime and currentStageStartTime
                            for (TaskCompletion completion : allCompletions) {
                                if (isCompletionInStage(completion, previousStageStartTime, currentStageStartTime)) {
                                    stageCompletions.add(completion);
                                }
                            }
                        }
                        
                        // Debug logging
                        System.out.println("DEBUG: Stage tasks: " + stageTasks.size());
                        System.out.println("DEBUG: Stage completions: " + stageCompletions.size());
                        
                        // Calculate success rate for current stage
                        int validTasks = 0;
                        for (Task task : stageTasks) {
                            if (!"paused".equals(task.getStatus()) && !"cancelled".equals(task.getStatus())) {
                                validTasks++;
                            }
                        }
                        
                        // Debug logging
                        System.out.println("DEBUG: Valid tasks: " + validTasks);
                        System.out.println("DEBUG: Stage completions: " + stageCompletions.size());
                        
                        int successRate = 0;
                        if (validTasks > 0) {
                            successRate = (stageCompletions.size() * 100) / validTasks;
                        }
                        
                        System.out.println("DEBUG: Final success rate: " + successRate + "%");
                        callback.onSuccess(String.valueOf(successRate));
                        
                        // After calculating success rate, update currentStageStartTime for next boss fight
                        if (previousStageStartTime != 0) {
                            long newCurrentStageStartTime = currentTime;
                            userPreferences.setCurrentStageStartTime(newCurrentStageStartTime);
                            System.out.println("DEBUG: Updated current stage start time after boss fight calculation: " + newCurrentStageStartTime);
                        }
                    }).start();
                } else {
                    callback.onError("User not found");
                }
            }
        });
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
            
            // Adjust rewards based on result
            final int finalCoinReward;
            double equipmentChance;
            
            if (victory) {
                finalCoinReward = baseCoinReward;
                equipmentChance = EQUIPMENT_DROP_CHANCE;
            } else if (partialVictory) {
                finalCoinReward = baseCoinReward / 2; // Half rewards
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
     * Calculates rewards for defeating a boss
     */
    private void calculateBossRewards(Boss boss, BossCallback callback) {
        // Use background thread for database operations
        new Thread(() -> {
            // Calculate coin reward - use boss level instead of defeated bosses count
            int bossLevel = boss.getLevel();
            int coinReward = (int) (BASE_COIN_REWARD * Math.pow(1 + COIN_INCREASE_RATE, bossLevel - 1));
        
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
            String userId = boss.getUserId();
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
