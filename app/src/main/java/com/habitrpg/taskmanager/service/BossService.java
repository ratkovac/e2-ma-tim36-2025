package com.habitrpg.taskmanager.service;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.entities.Boss;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.data.database.entities.TaskCompletion;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.repository.UserRepository;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;

import java.util.List;
import java.util.Random;

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
                        // Get all tasks for user
                        List<Task> allTasks = database.taskDao().getAllTasksByUser(userId);
                        
                        // Get all completed tasks for user
                        List<TaskCompletion> allCompletions = database.taskCompletionDao().getAllCompletionsByUser(userId);
                        
                        // Calculate success rate
                        int totalTasks = allTasks.size();
                        int completedTasks = allCompletions.size();
                        
                        // Exclude paused and cancelled tasks
                        int validTasks = 0;
                        for (Task task : allTasks) {
                            if (!"paused".equals(task.getStatus()) && !"cancelled".equals(task.getStatus())) {
                                validTasks++;
                            }
                        }
                        
                        int successRate = 0;
                        if (validTasks > 0) {
                            successRate = (completedTasks * 100) / validTasks;
                        }
                        
                        callback.onSuccess(String.valueOf(successRate));
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
            
            // Calculate base rewards
            int defeatedBosses = database.bossDao().getDefeatedBossCount(userId);
            int baseCoinReward = (int) (BASE_COIN_REWARD * Math.pow(1 + COIN_INCREASE_RATE, defeatedBosses));
            
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
            // Calculate coin reward
            int defeatedBosses = database.bossDao().getDefeatedBossCount(boss.getUserId());
            int coinReward = (int) (BASE_COIN_REWARD * Math.pow(1 + COIN_INCREASE_RATE, defeatedBosses));
        
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
}
