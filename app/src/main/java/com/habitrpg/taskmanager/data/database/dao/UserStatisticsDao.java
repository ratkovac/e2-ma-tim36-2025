package com.habitrpg.taskmanager.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.habitrpg.taskmanager.data.database.entities.UserStatistics;

@Dao
public interface UserStatisticsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUserStatistics(UserStatistics userStatistics);
    
    @Update
    void updateUserStatistics(UserStatistics userStatistics);
    
    @Query("SELECT * FROM user_statistics WHERE userId = :userId")
    UserStatistics getUserStatisticsByUserId(String userId);
    
    @Query("DELETE FROM user_statistics WHERE userId = :userId")
    void deleteUserStatisticsByUserId(String userId);
}
