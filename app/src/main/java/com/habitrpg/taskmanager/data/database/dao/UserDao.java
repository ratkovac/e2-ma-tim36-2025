package com.habitrpg.taskmanager.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.habitrpg.taskmanager.data.database.entities.User;

@Dao
public interface UserDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User user);
    
    @Update
    void updateUser(User user);
    
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    User getUserById(String userId);
    
    @Query("SELECT * FROM users WHERE is_logged_in = 1 LIMIT 1")
    User getLoggedInUser();
    
    @Query("UPDATE users SET is_logged_in = 0")
    void logoutAllUsers();
    
    @Query("UPDATE users SET is_logged_in = 1 WHERE id = :userId")
    void loginUser(String userId);
    
    @Query("UPDATE users SET experience_points = :xp WHERE id = :userId")
    void updateUserXP(String userId, int xp);
    
    @Query("UPDATE users SET level = :level WHERE id = :userId")
    void updateUserLevel(String userId, int level);
    
    @Query("UPDATE users SET coins = :coins WHERE id = :userId")
    void updateUserCoins(String userId, int coins);
    
    @Query("DELETE FROM users")
    void deleteAllUsers();
}
