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

    public void addTreasureEquipment(EquipmentCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }

        // 50% chance for weapon, 50% chance for clothing
        boolean isWeapon = Math.random() < 0.5;
        
        String equipmentName;
        String equipmentType;
        String equipmentDescription;
        String iconResource;
        String bonusType;
        double bonusValue;
        
        if (isWeapon) {
            // 50% chance for sword, 50% chance for bow
            boolean isSword = Math.random() < 0.5;
            if (isSword) {
                equipmentName = "Magic Sword";
                equipmentType = "weapon";
                equipmentDescription = "Sword that permanently increases strength by 5%";
                iconResource = "ic_sword";
                bonusType = "strength";
                bonusValue = 5.0;
            } else {
                equipmentName = "Bow and Arrow";
                equipmentType = "weapon";
                equipmentDescription = "Bow that permanently increases coin rewards by 5%";
                iconResource = "ic_bow";
                bonusType = "coin_bonus";
                bonusValue = 5.0;
            }
        } else {
            // Random clothing (equal chance for each)
            String[] clothingOptions = {
                "Power Gloves", "Defense Shield", "Speed Boots"
            };
            String[] clothingDescriptions = {
                "Gloves that increase strength by 10%",
                "Shield that increases attack success chance by 10%",
                "Boots that give 40% chance for extra attack"
            };
            String[] clothingIcons = {
                "ic_gloves", "ic_shield", "ic_boots"
            };
            String[] clothingBonusTypes = {
                "strength", "attack_chance", "extra_attack"
            };
            double[] clothingBonusValues = {
                10.0, 10.0, 40.0
            };
            
            int randomIndex = (int) (Math.random() * clothingOptions.length);
            equipmentName = clothingOptions[randomIndex];
            equipmentType = "clothing";
            equipmentDescription = clothingDescriptions[randomIndex];
            iconResource = clothingIcons[randomIndex];
            bonusType = clothingBonusTypes[randomIndex];
            bonusValue = clothingBonusValues[randomIndex];
        }
        
        // Add equipment directly to inventory (no cost)
        equipmentRepository.purchaseEquipment(currentUserId, equipmentName, equipmentType,
                equipmentDescription, 0, iconResource, bonusType, bonusValue,
                "permanent", new EquipmentRepository.EquipmentCallback() {
                    @Override
                    public void onSuccess(String message, List<Equipment> equipment) {
                        callback.onSuccess("Found " + equipmentName + "!", equipment);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
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
