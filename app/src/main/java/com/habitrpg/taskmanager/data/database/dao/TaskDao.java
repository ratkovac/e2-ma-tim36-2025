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
    
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND status = 'active' ORDER BY created_at DESC")
    List<Task> getActiveTasksByUserId(String userId);
    
    @Query("SELECT * FROM tasks WHERE user_id = :userId ORDER BY created_at DESC")
    List<Task> getAllTasksByUserId(String userId);
    
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    Task getTaskById(int taskId);
    
    @Query("SELECT * FROM tasks WHERE category_id = :categoryId")
    List<Task> getTasksByCategoryId(int categoryId);
    
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND status = :status")
    List<Task> getTasksByStatus(String userId, String status);
    
    @Query("UPDATE tasks SET status = :status WHERE id = :taskId")
    void updateTaskStatus(int taskId, String status);
    
    @Query("SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND difficulty = :difficulty AND importance = :importance AND start_date = :date AND status = 'active'")
    int getTaskCountByDifficultyAndImportanceForDate(String userId, String difficulty, String importance, String date);
    
    @Query("SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND importance = 'special' AND start_date >= :monthStart AND start_date <= :monthEnd AND status = 'active'")
    int getSpecialTaskCountForMonth(String userId, String monthStart, String monthEnd);
    
    @Query("SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND difficulty = 'extreme' AND start_date >= :weekStart AND start_date <= :weekEnd AND status = 'active'")
    int getExtremeTaskCountForWeek(String userId, String weekStart, String weekEnd);
    
    @Query("DELETE FROM tasks WHERE user_id = :userId")
    void deleteAllTasksForUser(String userId);

    @Query("SELECT COUNT(*) FROM tasks WHERE category_id = :categoryId AND status = 'active'")
    int getActiveTaskCountForCategory(int categoryId);
    
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND start_date = :date ORDER BY execution_time ASC")
    List<Task> getTasksByDate(String userId, String date);
    
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND start_date >= :startDate AND start_date <= :endDate ORDER BY start_date ASC, execution_time ASC")
    List<Task> getTasksInDateRange(String userId, String startDate, String endDate);
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    void deleteTaskById(int taskId);
    
    @Query("DELETE FROM tasks WHERE user_id = :userId AND name LIKE :namePattern AND start_date > :currentDate AND is_recurring = 0")
    void deleteFutureRecurringInstances(String userId, String namePattern, String currentDate);
}
