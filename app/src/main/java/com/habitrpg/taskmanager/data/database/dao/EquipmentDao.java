package com.habitrpg.taskmanager.data.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.habitrpg.taskmanager.data.database.entities.Equipment;

import java.util.List;

@Dao
public interface EquipmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEquipment(Equipment equipment);

    @Update
    void updateEquipment(Equipment equipment);

    @Delete
    void deleteEquipment(Equipment equipment);

    @Query("SELECT * FROM equipment WHERE user_id = :userId ORDER BY purchase_date DESC")
    List<Equipment> getUserEquipment(String userId);

    @Query("SELECT * FROM equipment WHERE user_id = :userId AND equipment_type = :equipmentType ORDER BY purchase_date DESC")
    List<Equipment> getUserEquipmentByType(String userId, String equipmentType);

    @Query("SELECT * FROM equipment WHERE user_id = :userId AND is_active = 1")
    List<Equipment> getActiveEquipment(String userId);

    @Query("SELECT * FROM equipment WHERE user_id = :userId AND bonus_type = :bonusType AND is_active = 1")
    List<Equipment> getActiveEquipmentByBonusType(String userId, String bonusType);

    @Query("SELECT COUNT(*) FROM equipment WHERE user_id = :userId AND equipment_name = :equipmentName")
    int getEquipmentCount(String userId, String equipmentName);

    @Query("DELETE FROM equipment WHERE user_id = :userId")
    void deleteAllUserEquipment(String userId);

    @Query("UPDATE equipment SET is_active = :isActive WHERE equipment_id = :equipmentId")
    void updateEquipmentActiveStatus(String equipmentId, boolean isActive);

    @Query("SELECT * FROM equipment WHERE equipment_id = :equipmentId")
    Equipment getEquipmentById(String equipmentId);

    @Query("UPDATE equipment SET durability = :durability WHERE equipment_id = :equipmentId")
    void updateEquipmentDurability(String equipmentId, int durability);

    @Query("DELETE FROM equipment WHERE durability <= 0 AND durability != -1")
    void deleteBrokenEquipment();

    @Query("UPDATE equipment SET bonus_value = :bonusValue WHERE equipment_id = :equipmentId")
    void updateEquipmentBonus(String equipmentId, double bonusValue);
}
