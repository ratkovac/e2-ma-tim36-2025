package com.habitrpg.taskmanager.data.repository;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.dao.EquipmentDao;
import com.habitrpg.taskmanager.data.database.entities.Equipment;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EquipmentRepository {
    private static volatile EquipmentRepository INSTANCE;
    private final EquipmentDao equipmentDao;
    private ExecutorService executor;

    private EquipmentRepository(Context context) {
        equipmentDao = AppDatabase.getDatabase(context).equipmentDao();
        executor = Executors.newFixedThreadPool(4);
    }

    public static EquipmentRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (EquipmentRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new EquipmentRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    private void ensureExecutorActive() {
        if (executor.isShutdown()) {
            executor = Executors.newFixedThreadPool(4);
        }
    }

    public void getUserEquipment(String userId, EquipmentCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                List<Equipment> equipment = equipmentDao.getUserEquipment(userId);
                callback.onSuccess("Equipment loaded successfully", equipment);
            } catch (Exception e) {
                callback.onError("Failed to load equipment: " + e.getMessage());
            }
        });
    }

    public void getUserEquipmentByType(String userId, String equipmentType, EquipmentCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                List<Equipment> equipment = equipmentDao.getUserEquipmentByType(userId, equipmentType);
                callback.onSuccess("Equipment loaded successfully", equipment);
            } catch (Exception e) {
                callback.onError("Failed to load equipment: " + e.getMessage());
            }
        });
    }

    public void getActiveEquipment(String userId, EquipmentCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                List<Equipment> equipment = equipmentDao.getActiveEquipment(userId);
                callback.onSuccess("Active equipment loaded successfully", equipment);
            } catch (Exception e) {
                callback.onError("Failed to load active equipment: " + e.getMessage());
            }
        });
    }

    public void getActiveEquipmentByBonusType(String userId, String bonusType, EquipmentCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                List<Equipment> equipment = equipmentDao.getActiveEquipmentByBonusType(userId, bonusType);
                callback.onSuccess("Active equipment loaded successfully", equipment);
            } catch (Exception e) {
                callback.onError("Failed to load active equipment: " + e.getMessage());
            }
        });
    }

    public void purchaseEquipment(String userId, String equipmentName, String equipmentType, 
                                String equipmentDescription, int price, String iconResource,
                                String bonusType, double bonusValue, String bonusDuration, 
                                EquipmentCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                Equipment equipment = new Equipment();
                equipment.setEquipmentId(UUID.randomUUID().toString());
                equipment.setUserId(userId);
                equipment.setEquipmentType(equipmentType);
                equipment.setEquipmentName(equipmentName);
                equipment.setEquipmentDescription(equipmentDescription);
                equipment.setPrice(price);
                equipment.setIconResource(iconResource);
                equipment.setBonusType(bonusType);
                equipment.setBonusValue(bonusValue);
                equipment.setBonusDuration(bonusDuration);
                equipment.setPurchaseDate(System.currentTimeMillis());
                equipment.setActive(false); // Equipment is not active by default
                
                // Set durability based on equipment type
                if ("potion".equals(equipmentType)) {
                    if ("permanent".equals(bonusDuration)) {
                        equipment.setDurability(-1); // Permanent potions last forever
                    } else {
                        equipment.setDurability(1); // Single-use potions last 1 fight
                    }
                } else if ("weapon".equals(equipmentType)) {
                    equipment.setDurability(-1); // Weapons last forever
                } else {
                    equipment.setDurability(2); // Clothing lasts 2 fights
                }

                equipmentDao.insertEquipment(equipment);
                callback.onSuccess("Equipment purchased successfully", null);
            } catch (Exception e) {
                callback.onError("Failed to purchase equipment: " + e.getMessage());
            }
        });
    }

    public void activateEquipment(String equipmentId, EquipmentCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                equipmentDao.updateEquipmentActiveStatus(equipmentId, true);
                callback.onSuccess("Equipment activated successfully", null);
            } catch (Exception e) {
                callback.onError("Failed to activate equipment: " + e.getMessage());
            }
        });
    }

    public void deactivateEquipment(String equipmentId, EquipmentCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                equipmentDao.updateEquipmentActiveStatus(equipmentId, false);
                callback.onSuccess("Equipment deactivated successfully", null);
            } catch (Exception e) {
                callback.onError("Failed to deactivate equipment: " + e.getMessage());
            }
        });
    }

    public void getEquipmentCount(String userId, String equipmentName, EquipmentCountCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                int count = equipmentDao.getEquipmentCount(userId, equipmentName);
                callback.onSuccess("Equipment count retrieved successfully", count);
            } catch (Exception e) {
                callback.onError("Failed to get equipment count: " + e.getMessage());
            }
        });
    }

    public void reduceEquipmentDurability(List<Equipment> activeEquipment, EquipmentCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                for (Equipment equipment : activeEquipment) {
                    if (equipment.isActive() && equipment.getDurability() != -1) {
                        int newDurability = equipment.getDurability() - 1;
                        equipmentDao.updateEquipmentDurability(equipment.getEquipmentId(), newDurability);
                    }
                }
                
                // Delete equipment with durability <= 0 (but not -1 which means forever)
                equipmentDao.deleteBrokenEquipment();
                
                callback.onSuccess("Equipment durability updated successfully", null);
            } catch (Exception e) {
                callback.onError("Failed to update equipment durability: " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
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
