package com.habitrpg.taskmanager.data.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.habitrpg.taskmanager.data.database.entities.Task;
import java.util.List;

@Dao
public interface TaskDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertTask(Task task);
    
    @Update
    void updateTask(Task task);
    
    @Delete
    void deleteTask(Task task);
    
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND status = 'active' ORDER BY start_date ASC")
    List<Task> getActiveTasksByUserId(String userId);
    
    @Query("SELECT * FROM tasks WHERE user_id = :userId ORDER BY start_date ASC")
    List<Task> getTasksByUserId(String userId);
    
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND status = 'completed' ORDER BY start_date ASC")
    List<Task> getCompletedTasksByUserId(String userId);
    
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    Task getTaskById(int taskId);
    
    @Query("UPDATE tasks SET status = :status WHERE id = :taskId")
    void updateTaskStatus(int taskId, String status);
    
    @Query("DELETE FROM tasks WHERE user_id = :userId")
    void deleteAllTasksForUser(String userId);

    @Query("SELECT COUNT(*) FROM tasks WHERE category_id = :categoryId AND status = 'active'")
    int getActiveTaskCountForCategory(int categoryId);
    
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND DATE(start_date) = :date ORDER BY start_date ASC")
    List<Task> getTasksByDate(String userId, String date);
    
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND DATE(start_date) >= :startDate AND DATE(start_date) <= :endDate ORDER BY start_date ASC")
    List<Task> getTasksInDateRange(String userId, String startDate, String endDate);
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    void deleteTaskById(int taskId);
    
    
    @Query("SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND difficulty = :difficulty AND importance = :importance AND DATE(start_date) = :date AND status = 'active'")
    int getTaskCountByDifficultyAndImportanceForDate(String userId, String difficulty, String importance, String date);
    
    @Query("SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND importance = 'special' AND DATE(start_date) >= :monthStart AND DATE(start_date) <= :monthEnd AND status = 'active'")
    int getSpecialTaskCountForMonth(String userId, String monthStart, String monthEnd);
    
    @Query("SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND difficulty = 'extreme' AND DATE(start_date) >= :weekStart AND DATE(start_date) <= :weekEnd AND status = 'active'")
    int getExtremeTaskCountForWeek(String userId, String weekStart, String weekEnd);
}
