package com.habitrpg.taskmanager.service;

import android.content.Context;
import android.content.SharedPreferences;

import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.entities.Boss;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.data.database.entities.TaskCompletion;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.repository.UserRepository;

import java.util.List;
import java.util.Random;

public class BossService {
    
    private static BossService instance;
    private final AppDatabase database;
    private final UserRepository userRepository;
    private final SharedPreferences userPreferences;
    
    // Boss fight constants
    private static final int FIRST_BOSS_HP = 200;
    private static final int MAX_ATTACKS = 5;
    private static final int BASE_COIN_REWARD = 200;
    private static final double COIN_INCREASE_RATE = 0.20; // 20% increase
    private static final double EQUIPMENT_DROP_CHANCE = 0.20; // 20% chance
    private static final double CLOTHING_DROP_CHANCE = 0.95; // 95% of equipment drops
    private static final double WEAPON_DROP_CHANCE = 0.05; // 5% of equipment drops
    
    private BossService(Context context) {
        database = AppDatabase.getDatabase(context);
        userRepository = UserRepository.getInstance(context);
        userPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
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
        private int coinsEarned;
        private String equipmentEarned;
        private boolean equipmentDropped;
        private int damageDealt;
        private int attacksUsed;
        
        public BossFightResult(boolean victory, int coinsEarned, String equipmentEarned, 
                             boolean equipmentDropped, int damageDealt, int attacksUsed) {
            this.victory = victory;
            this.coinsEarned = coinsEarned;
            this.equipmentEarned = equipmentEarned;
            this.equipmentDropped = equipmentDropped;
            this.damageDealt = damageDealt;
            this.attacksUsed = attacksUsed;
        }
        
        // Getters
        public boolean isVictory() { return victory; }
        public int getCoinsEarned() { return coinsEarned; }
        public String getEquipmentEarned() { return equipmentEarned; }
        public boolean isEquipmentDropped() { return equipmentDropped; }
        public int getDamageDealt() { return damageDealt; }
        public int getAttacksUsed() { return attacksUsed; }
    }
    
    /**
     * Creates a new boss for the specified level
     */
    public void createBossForLevel(int level, BossCallback callback) {
        String userId = userPreferences.getString("current_user_id", null);
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
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
    }
    
    /**
     * Calculates boss HP using the formula: HP_previous * 2 + HP_previous / 2
     */
    private int calculateBossHp(int level) {
        if (level <= 1) {
            return FIRST_BOSS_HP;
        }
        
        // Get previous boss HP
        String userId = userPreferences.getString("current_user_id", null);
        Boss previousBoss = database.bossDao().getBossByUserAndLevel(userId, level - 1);
        
        int previousHp;
        if (previousBoss != null) {
            previousHp = previousBoss.getMaxHp();
        } else {
            // If no previous boss exists, calculate recursively
            previousHp = calculateBossHp(level - 1);
        }
        
        // Apply formula: HP_previous * 2 + HP_previous / 2
        return previousHp * 2 + previousHp / 2;
    }
    
    /**
     * Gets the current boss for the user's level
     */
    public void getCurrentBoss(BossCallback callback) {
        String userId = userPreferences.getString("current_user_id", null);
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
                    Boss currentBoss = database.bossDao().getCurrentBossForLevel(userId, user.getLevel());
                    if (currentBoss != null) {
                        callback.onBossRetrieved(currentBoss);
                    } else {
                        // Create new boss for current level
                        createBossForLevel(user.getLevel(), callback);
                    }
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
        String userId = userPreferences.getString("current_user_id", null);
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
        String userId = userPreferences.getString("current_user_id", null);
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
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
    }
    
    /**
     * Calculates rewards for defeating a boss
     */
    private void calculateBossRewards(Boss boss, BossCallback callback) {
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
                                true, coinReward, equipmentEarned, equipmentDropped, 
                                boss.getMaxHp(), MAX_ATTACKS
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
    }
}
