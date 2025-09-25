package com.habitrpg.taskmanager.business.tasks;

import android.content.Context;
import com.habitrpg.taskmanager.business.xp.XPCalculator;
import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.data.database.entities.TaskCompletion;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskManager {
    
    private static TaskManager instance;
    private AppDatabase database;
    private FirebaseManager firebaseManager;
    private UserPreferences userPreferences;
    private ExecutorService executor;
    
    private TaskManager(Context context) {
        database = AppDatabase.getDatabase(context);
        firebaseManager = FirebaseManager.getInstance();
        userPreferences = UserPreferences.getInstance(context);
        executor = Executors.newFixedThreadPool(2);
    }
    
    public static synchronized TaskManager getInstance(Context context) {
        if (instance == null) {
            instance = new TaskManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Create a new task
     */
    public void createTask(Task task, TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        task.setUserId(userId);
        
        // Validate task quota limits
        validateTaskQuota(task, isValid -> {
            if (!isValid) {
                callback.onError("Task quota limit exceeded for this difficulty/importance combination");
                return;
            }
            
            // Calculate XP value
            int xpValue = XPCalculator.calculateTaskXP(task.getDifficulty(), task.getImportance());
            task.setXpValue(xpValue);
            
            // Save to local database
            executor.execute(() -> {
                long taskId = database.taskDao().insertTask(task);
                task.setId((int) taskId);
                
                // Save to Firebase
                firebaseManager.saveTask(task, (success, exception) -> {
                    if (success) {
                        callback.onSuccess("Task created successfully");
                    } else {
                        callback.onError("Failed to sync task to cloud: " + 
                            (exception != null ? exception.getMessage() : "Unknown error"));
                    }
                });
            });
        });
    }
    
    /**
     * Complete a task
     */
    public void completeTask(int taskId, TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        executor.execute(() -> {
            Task task = database.taskDao().getTaskById(taskId);
            if (task == null) {
                callback.onError("Task not found");
                return;
            }
            
            if (!task.getUserId().equals(userId)) {
                callback.onError("Unauthorized to complete this task");
                return;
            }
            
            if ("completed".equals(task.getStatus())) {
                callback.onError("Task already completed");
                return;
            }
            
            // Update task status
            database.taskDao().updateTaskStatus(taskId, "completed");
            
            // Create task completion record
            String currentDate = getCurrentDateString();
            TaskCompletion completion = new TaskCompletion(taskId, currentDate, task.getXpValue());
            database.taskCompletionDao().insertTaskCompletion(completion);
            
            // Update user XP
            User user = database.userDao().getUserById(userId);
            if (user != null) {
                int newXP = user.getExperiencePoints() + task.getXpValue();
                int oldLevel = user.getLevel();
                int newLevel = XPCalculator.calculateLevelFromXP(newXP);
                
                user.setExperiencePoints(newXP);
                
                boolean leveledUp;
                if (newLevel > oldLevel) {
                    user.setLevel(newLevel);
                    user.setTitle(XPCalculator.getTitleForLevel(newLevel));
                    leveledUp = true;
                } else {
                    leveledUp = false;
                }

                database.userDao().updateUser(user);
                
                // Sync to Firebase
                firebaseManager.updateUserDocument(user, (success, exception) -> {
                    String message = "Task completed! +" + task.getXpValue() + " XP";
                    if (leveledUp) {
                        message += "\nLevel up! You are now level " + newLevel + " (" + user.getTitle() + ")";
                    }
                    callback.onSuccess(message);
                });
            } else {
                callback.onError("User data not found");
            }
        });
    }
    
    /**
     * Get active tasks for current user
     */
    public void getActiveTasks(TaskListCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onTasksRetrieved(null);
            return;
        }
        
        executor.execute(() -> {
            List<Task> tasks = database.taskDao().getActiveTasksByUserId(userId);
            callback.onTasksRetrieved(tasks);
        });
    }
    
    /**
     * Update task
     */
    public void updateTask(Task task, TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        if (!task.getUserId().equals(userId)) {
            callback.onError("Unauthorized to update this task");
            return;
        }
        
        // Recalculate XP value
        int xpValue = XPCalculator.calculateTaskXP(task.getDifficulty(), task.getImportance());
        task.setXpValue(xpValue);
        
        executor.execute(() -> {
            database.taskDao().updateTask(task);
            
            // Sync to Firebase
            firebaseManager.updateTask(String.valueOf(task.getId()), task, (success, exception) -> {
                if (success) {
                    callback.onSuccess("Task updated successfully");
                } else {
                    callback.onError("Failed to sync task update to cloud");
                }
            });
        });
    }
    
    /**
     * Cancel/Delete task
     */
    public void cancelTask(int taskId, TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        executor.execute(() -> {
            Task task = database.taskDao().getTaskById(taskId);
            if (task == null) {
                callback.onError("Task not found");
                return;
            }
            
            if (!task.getUserId().equals(userId)) {
                callback.onError("Unauthorized to cancel this task");
                return;
            }
            
            database.taskDao().updateTaskStatus(taskId, "cancelled");
            callback.onSuccess("Task cancelled");
        });
    }
    
    /**
     * Validate task quota limits according to PRD
     */
    private void validateTaskQuota(Task task, QuotaValidationCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        String currentDate = getCurrentDateString();
        
        executor.execute(() -> {
            String difficulty = task.getDifficulty();
            String importance = task.getImportance();
            
            // Check daily limits for non-special tasks
            if (!"special".equals(importance)) {
                int dailyCount = database.taskDao().getTaskCountByDifficultyAndImportanceForDate(
                    userId, difficulty, importance, currentDate);
                
                // Very easy + normal: max 5/day
                if ("very_easy".equals(difficulty) && "normal".equals(importance) && dailyCount >= 5) {
                    callback.onValidationResult(false);
                    return;
                }
                
                // Easy + important: max 5/day  
                if ("easy".equals(difficulty) && "important".equals(importance) && dailyCount >= 5) {
                    callback.onValidationResult(false);
                    return;
                }
                
                // Hard + very important: max 2/day
                if ("hard".equals(difficulty) && "very_important".equals(importance) && dailyCount >= 2) {
                    callback.onValidationResult(false);
                    return;
                }
            }
            
            // Check weekly limit for extreme tasks
            if ("extreme".equals(difficulty)) {
                String[] weekDates = getCurrentWeekDates();
                int weeklyCount = database.taskDao().getExtremeTaskCountForWeek(
                    userId, weekDates[0], weekDates[1]);
                if (weeklyCount >= 1) {
                    callback.onValidationResult(false);
                    return;
                }
            }
            
            // Check monthly limit for special tasks
            if ("special".equals(importance)) {
                String[] monthDates = getCurrentMonthDates();
                int monthlyCount = database.taskDao().getSpecialTaskCountForMonth(
                    userId, monthDates[0], monthDates[1]);
                if (monthlyCount >= 1) {
                    callback.onValidationResult(false);
                    return;
                }
            }
            
            callback.onValidationResult(true);
        });
    }
    
    // Helper methods
    private String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    private String[] getCurrentWeekDates() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        String weekStart = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        
        cal.add(Calendar.DAY_OF_WEEK, 6);
        String weekEnd = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        
        return new String[]{weekStart, weekEnd};
    }
    
    private String[] getCurrentMonthDates() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String monthStart = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        String monthEnd = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        
        return new String[]{monthStart, monthEnd};
    }
    
    // Callback interfaces
    public interface TaskCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public interface TaskListCallback {
        void onTasksRetrieved(List<Task> tasks);
    }
    
    private interface QuotaValidationCallback {
        void onValidationResult(boolean isValid);
    }
}
