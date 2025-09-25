package com.habitrpg.taskmanager.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.habitrpg.taskmanager.data.database.entities.TaskCompletion;
import java.util.List;

@Dao
public interface TaskCompletionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertTaskCompletion(TaskCompletion taskCompletion);
    
    @Query("SELECT * FROM task_completions WHERE task_id = :taskId ORDER BY completed_date DESC")
    List<TaskCompletion> getCompletionsByTaskId(int taskId);
    
    @Query("SELECT * FROM task_completions WHERE completed_date = :date")
    List<TaskCompletion> getCompletionsByDate(String date);
    
    @Query("SELECT * FROM task_completions WHERE completed_date >= :startDate AND completed_date <= :endDate")
    List<TaskCompletion> getCompletionsByDateRange(String startDate, String endDate);
    
    @Query("SELECT SUM(xp_earned) FROM task_completions tc " +
           "INNER JOIN tasks t ON tc.task_id = t.id " +
           "WHERE t.user_id = :userId AND tc.completed_date = :date")
    int getTotalXpEarnedByUserForDate(String userId, String date);
    
    @Query("SELECT SUM(xp_earned) FROM task_completions tc " +
           "INNER JOIN tasks t ON tc.task_id = t.id " +
           "WHERE t.user_id = :userId")
    int getTotalXpEarnedByUser(String userId);
    
    @Query("DELETE FROM task_completions WHERE task_id IN " +
           "(SELECT id FROM tasks WHERE user_id = :userId)")
    void deleteAllCompletionsForUser(String userId);
}
