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
    List<Task> getTasksByUserId(String userId);
    
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND status = 'completed' ORDER BY created_at DESC")
    List<Task> getCompletedTasksByUserId(String userId);
    
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    Task getTaskById(int taskId);
    
    @Query("UPDATE tasks SET status = :status WHERE id = :taskId")
    void updateTaskStatus(int taskId, String status);
    
    @Query("DELETE FROM tasks WHERE user_id = :userId")
    void deleteAllTasksForUser(String userId);
}
