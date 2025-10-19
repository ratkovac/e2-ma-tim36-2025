package com.habitrpg.taskmanager.service;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.entities.Equipment;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.repository.EquipmentRepository;
import com.habitrpg.taskmanager.data.repository.UserRepository;

import java.util.List;

public class EquipmentService {
    private static volatile EquipmentService INSTANCE;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final UserPreferences userPreferences;

    private EquipmentService(Context context) {
        equipmentRepository = EquipmentRepository.getInstance(context);
        userRepository = UserRepository.getInstance(context);
        userPreferences = UserPreferences.getInstance(context);
    }

    public static EquipmentService getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (EquipmentService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new EquipmentService(context);
                }
            }
        }
        return INSTANCE;
    }

    public void getUserEquipment(EquipmentCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }

        equipmentRepository.getUserEquipment(currentUserId, new EquipmentRepository.EquipmentCallback() {
            @Override
            public void onSuccess(String message, List<Equipment> equipment) {
                callback.onSuccess(message, equipment);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getUserEquipmentByType(String equipmentType, EquipmentCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }

        equipmentRepository.getUserEquipmentByType(currentUserId, equipmentType, new EquipmentRepository.EquipmentCallback() {
            @Override
            public void onSuccess(String message, List<Equipment> equipment) {
                callback.onSuccess(message, equipment);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getActiveEquipment(EquipmentCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }

        equipmentRepository.getActiveEquipment(currentUserId, new EquipmentRepository.EquipmentCallback() {
            @Override
            public void onSuccess(String message, List<Equipment> equipment) {
                callback.onSuccess(message, equipment);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void purchaseEquipment(String equipmentName, String equipmentType, String equipmentDescription,
                                int price, String iconResource, String bonusType, double bonusValue,
                                String bonusDuration, EquipmentCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }

        // Check if user has enough coins
        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(String message) {}

            @Override
            public void onError(String error) {
                callback.onError("Failed to get user info: " + error);
            }

            @Override
            public void onUserRetrieved(User user) {
                if (user == null) {
                    callback.onError("User not found");
                    return;
                }

                if (user.getCoins() < price) {
                    callback.onError("Not enough coins. You have " + user.getCoins() + " coins, but need " + price + " coins.");
                    return;
                }

                // Deduct coins and purchase equipment
                user.setCoins(user.getCoins() - price);
                userRepository.updateUser(user, new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(String message) {
                        // Purchase equipment
                        equipmentRepository.purchaseEquipment(currentUserId, equipmentName, equipmentType,
                                equipmentDescription, price, iconResource, bonusType, bonusValue,
                                bonusDuration, new EquipmentRepository.EquipmentCallback() {
                                    @Override
                                    public void onSuccess(String message, List<Equipment> equipment) {
                                        callback.onSuccess(message, equipment);
                                    }

                                    @Override
                                    public void onError(String error) {
                                        // Refund coins if equipment purchase failed
                                        user.setCoins(user.getCoins() + price);
                                        userRepository.updateUser(user, new UserRepository.UserCallback() {
                                            @Override
                                            public void onSuccess(String message) {}
                                            @Override
                                            public void onError(String error) {}
                                            @Override
                                            public void onUserRetrieved(User user) {}
                                        });
                                        callback.onError(error);
                                    }
                                });
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError("Failed to update user coins: " + error);
                    }

                    @Override
                    public void onUserRetrieved(User user) {}
                });
            }
        });
    }

    public void activateEquipment(String equipmentId, EquipmentCallback callback) {
        equipmentRepository.activateEquipment(equipmentId, new EquipmentRepository.EquipmentCallback() {
            @Override
            public void onSuccess(String message, List<Equipment> equipment) {
                callback.onSuccess(message, equipment);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void deactivateEquipment(String equipmentId, EquipmentCallback callback) {
        equipmentRepository.deactivateEquipment(equipmentId, new EquipmentRepository.EquipmentCallback() {
            @Override
            public void onSuccess(String message, List<Equipment> equipment) {
                callback.onSuccess(message, equipment);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getEquipmentCount(String equipmentName, EquipmentCountCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }

        equipmentRepository.getEquipmentCount(currentUserId, equipmentName, new EquipmentRepository.EquipmentCountCallback() {
            @Override
            public void onSuccess(String message, int count) {
                callback.onSuccess(message, count);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void reduceEquipmentDurability(List<Equipment> activeEquipment, EquipmentCallback callback) {
        equipmentRepository.reduceEquipmentDurability(activeEquipment, new EquipmentRepository.EquipmentCallback() {
            @Override
            public void onSuccess(String message, List<Equipment> equipment) {
                callback.onSuccess(message, equipment);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void upgradeEquipment(String equipmentId, EquipmentCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }

        // Get number of defeated bosses to calculate upgrade cost
        BossService.getInstance(null).getDefeatedBossCount(new BossService.BossCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError("Failed to get boss info: " + error);
            }
            
            @Override
            public void onBossRetrieved(com.habitrpg.taskmanager.data.database.entities.Boss boss) {}
            
            @Override
            public void onBossFightResult(BossService.BossFightResult result) {}
            
            @Override
            public void onDefeatedBossCountRetrieved(int defeatedBossCount) {
                // Calculate upgrade cost based on previous boss reward
                int previousBossLevel = defeatedBossCount == 0 ? 1 : defeatedBossCount;
                int baseCoinReward = (int) (200 * Math.pow(1.20, previousBossLevel - 1));
                int upgradeCost = (int) (baseCoinReward * 0.60); // 60% of base coin reward
                
                // Check if user has enough coins
                userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(String message) {}
                    
                    @Override
                    public void onError(String error) {
                        callback.onError("Failed to get user info: " + error);
                    }
                    
                    @Override
                    public void onUserRetrieved(User user) {
                        if (user == null) {
                            callback.onError("User not found");
                            return;
                        }
                        
                        if (user.getCoins() < upgradeCost) {
                            callback.onError("Not enough coins! You have " + user.getCoins() + 
                                    " coins, but need " + upgradeCost + " coins for upgrade.");
                            return;
                        }
                        
                        // Get equipment to upgrade
                        equipmentRepository.getUserEquipment(currentUserId, new EquipmentRepository.EquipmentCallback() {
                            @Override
                            public void onSuccess(String message, List<Equipment> allEquipment) {
                                Equipment equipmentToUpgrade = null;
                                for (Equipment eq : allEquipment) {
                                    if (eq.getEquipmentId().equals(equipmentId)) {
                                        equipmentToUpgrade = eq;
                                        break;
                                    }
                                }
                                
                                if (equipmentToUpgrade == null) {
                                    callback.onError("Equipment not found");
                                    return;
                                }
                                
                                double newBonusValue = equipmentToUpgrade.getBonusValue() + 1.0; // +1% per upgrade
                                
                                // Deduct coins and upgrade equipment
                                user.setCoins(user.getCoins() - upgradeCost);
                                userRepository.updateUser(user, new UserRepository.UserCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        equipmentRepository.updateEquipmentBonus(equipmentId, newBonusValue, 
                                                new EquipmentRepository.EquipmentCallback() {
                                                    @Override
                                                    public void onSuccess(String message, List<Equipment> equipment) {
                                                        callback.onSuccess("Equipment upgraded to " + 
                                                                String.format("%.0f", newBonusValue) + "%!", equipment);
                                                    }
                                                    
                                                    @Override
                                                    public void onError(String error) {
                                                        // Refund coins if upgrade failed
                                                        user.setCoins(user.getCoins() + upgradeCost);
                                                        userRepository.updateUser(user, new UserRepository.UserCallback() {
                                                            @Override
                                                            public void onSuccess(String message) {}
                                                            @Override
                                                            public void onError(String error) {}
                                                            @Override
                                                            public void onUserRetrieved(User user) {}
                                                        });
                                                        callback.onError(error);
                                                    }
                                                });
                                    }
                                    
                                    @Override
                                    public void onError(String error) {
                                        callback.onError("Failed to update user coins: " + error);
                                    }
                                    
                                    @Override
                                    public void onUserRetrieved(User user) {}
                                });
                            }
                            
                            @Override
                            public void onError(String error) {
                                callback.onError("Failed to get equipment: " + error);
                            }
                        });
                    }
                });
            }
        });
    }

    public void addTreasureEquipment(EquipmentCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }

        String equipmentName;
        String equipmentType;
        String equipmentDescription;
        String iconResource;
        String bonusType;
        double bonusValue;
        
        // 50% chance for weapon, 50% chance for clothing
        boolean isWeapon = Math.random() < 0.5;
        
        if (isWeapon) {
            // 50% chance for sword, 50% chance for bow
            boolean isSword = Math.random() < 0.5;
            if (isSword) {
                equipmentName = "Mač";
                equipmentType = "weapon";
                equipmentDescription = "Mač koji trajno povećava snagu";
                iconResource = "ic_sword";
                bonusType = "strength";
                bonusValue = 5.0;
            } else {
                equipmentName = "Luk i strijela";
                equipmentType = "weapon";
                equipmentDescription = "Luk koji trajno povećava novčane nagrade";
                iconResource = "ic_bow";
                bonusType = "coin_bonus";
                bonusValue = 5.0;
            }
            
            // Check if user already has this weapon
            final String finalEquipmentName = equipmentName;
            final String finalEquipmentType = equipmentType;
            final String finalEquipmentDescription = equipmentDescription;
            final String finalIconResource = iconResource;
            final String finalBonusType = bonusType;
            final double finalBonusValue = bonusValue;
            
            equipmentRepository.getUserEquipmentByType(currentUserId, "weapon", new EquipmentRepository.EquipmentCallback() {
                @Override
                public void onSuccess(String message, List<Equipment> existingWeapons) {
                    // Check if user already has this specific weapon
                    Equipment existingWeapon = null;
                    for (Equipment weapon : existingWeapons) {
                        if (weapon.getEquipmentName().equals(finalEquipmentName)) {
                            existingWeapon = weapon;
                            break;
                        }
                    }
                    
                    if (existingWeapon != null) {
                        // User already has this weapon - increase bonus by 1%
                        double newBonusValue = existingWeapon.getBonusValue() + 1.0;
                        equipmentRepository.updateEquipmentBonus(existingWeapon.getEquipmentId(), newBonusValue, 
                                new EquipmentRepository.EquipmentCallback() {
                                    @Override
                                    public void onSuccess(String message, List<Equipment> equipment) {
                                        callback.onSuccess("Dobili ste: " + finalEquipmentName + " (Bonus povećan na " + 
                                                String.format("%.0f", newBonusValue) + "%)", equipment);
                                    }
                                    
                                    @Override
                                    public void onError(String error) {
                                        callback.onError(error);
                                    }
                                });
                    } else {
                        // User doesn't have this weapon - add new one
                        equipmentRepository.purchaseEquipment(currentUserId, finalEquipmentName, finalEquipmentType,
                                finalEquipmentDescription, 0, finalIconResource, finalBonusType, finalBonusValue,
                                "permanent", new EquipmentRepository.EquipmentCallback() {
                                    @Override
                                    public void onSuccess(String message, List<Equipment> equipment) {
                                        callback.onSuccess("Dobili ste: " + finalEquipmentName, equipment);
                                    }

                                    @Override
                                    public void onError(String error) {
                                        callback.onError(error);
                                    }
                                });
                    }
                }
                
                @Override
                public void onError(String error) {
                    callback.onError("Failed to check existing weapons: " + error);
                }
            });
        } else {
            // Random clothing (equal chance for each) - can have duplicates
            double clothingRandom = Math.random();
            if (clothingRandom < 0.333) {
                equipmentName = "Čizme";
                equipmentType = "clothing";
                equipmentDescription = "Čizme koje daju 40% šanse za dodatni napad";
                iconResource = "ic_boots";
                bonusType = "extra_attack";
                bonusValue = 40.0;
            } else if (clothingRandom < 0.666) {
                equipmentName = "Rukavice";
                equipmentType = "clothing";
                equipmentDescription = "Rukavice koje povećavaju snagu za 10%";
                iconResource = "ic_gloves";
                bonusType = "strength";
                bonusValue = 10.0;
            } else {
                equipmentName = "Štit";
                equipmentType = "clothing";
                equipmentDescription = "Štit koji povećava šansu za uspešan napad za 10%";
                iconResource = "ic_shield";
                bonusType = "attack_chance";
                bonusValue = 10.0;
            }
            
            // Add clothing directly (can have duplicates)
            equipmentRepository.purchaseEquipment(currentUserId, equipmentName, equipmentType,
                    equipmentDescription, 0, iconResource, bonusType, bonusValue,
                    "permanent", new EquipmentRepository.EquipmentCallback() {
                        @Override
                        public void onSuccess(String message, List<Equipment> equipment) {
                            callback.onSuccess("Dobili ste: " + equipmentName, equipment);
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                    });
        }
    }

    public interface EquipmentCallback {
        void onSuccess(String message, List<Equipment> equipment);
        void onError(String error);
    }

    public interface EquipmentCountCallback {
        void onSuccess(String message, int count);
        void onError(String error);
    }
}
