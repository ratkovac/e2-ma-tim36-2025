package com.habitrpg.taskmanager.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.habitrpg.taskmanager.data.database.entities.Boss;

import java.util.List;

@Dao
public interface BossDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBoss(Boss boss);
    
    @Update
    void updateBoss(Boss boss);
    
    @Query("SELECT * FROM bosses WHERE user_id = :userId AND level = :level")
    Boss getBossByUserAndLevel(String userId, int level);
    
    @Query("SELECT * FROM bosses WHERE user_id = :userId AND is_defeated = 0 ORDER BY level ASC")
    List<Boss> getActiveBossesByUser(String userId);
    
    @Query("SELECT * FROM bosses WHERE user_id = :userId ORDER BY level DESC")
    List<Boss> getAllBossesByUser(String userId);
    
    @Query("SELECT * FROM bosses WHERE user_id = :userId AND level = :level AND is_defeated = 0")
    Boss getCurrentBossForLevel(String userId, int level);
    
    @Query("SELECT COUNT(*) FROM bosses WHERE user_id = :userId AND is_defeated = 1")
    int getDefeatedBossCount(String userId);
    
    @Query("DELETE FROM bosses WHERE user_id = :userId")
    void deleteAllBossesForUser(String userId);
}
