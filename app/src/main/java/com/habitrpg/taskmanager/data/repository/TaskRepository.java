package com.habitrpg.taskmanager.data.repository;

import android.content.Context;
import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.data.database.entities.TaskCompletion;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {

    private static TaskRepository instance;
    private AppDatabase database;
    private FirebaseManager firebaseManager;
    private UserPreferences userPreferences;
    private ExecutorService executor;

    private TaskRepository(Context context) {
        database = AppDatabase.getDatabase(context);
        firebaseManager = FirebaseManager.getInstance();
        userPreferences = UserPreferences.getInstance(context);
        executor = Executors.newFixedThreadPool(2);
    }

    public static synchronized TaskRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TaskRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void insertTask(Task task, TaskCallback callback) {
        executor.execute(() -> {
            try {
                long taskId = database.taskDao().insertTask(task);
                task.setId((int) taskId);

                firebaseManager.saveTask(task, (success, exception) -> {
                    if (success) {
                        callback.onSuccess("Task created successfully");
                    } else {
                        callback.onError("Failed to sync task to cloud: " +
                                (exception != null ? exception.getMessage() : "Unknown error"));
                    }
                });
            } catch (Exception e) {
                callback.onError("Failed to create task: " + e.getMessage());
            }
        });
    }

    public void updateTask(Task task, TaskCallback callback) {
        executor.execute(() -> {
            try {
                database.taskDao().updateTask(task);

                firebaseManager.updateTask(String.valueOf(task.getId()), task, (success, exception) -> {
                    if (success) {
                        callback.onSuccess("Task updated successfully");
                    } else {
                        callback.onError("Failed to sync task update to cloud");
                    }
                });
            } catch (Exception e) {
                callback.onError("Failed to update task: " + e.getMessage());
            }
        });
    }

    public void updateTaskStatus(int taskId, String status, TaskCallback callback) {
        executor.execute(() -> {
            try {
                database.taskDao().updateTaskStatus(taskId, status);
                callback.onSuccess("Task status updated successfully");
            } catch (Exception e) {
                callback.onError("Failed to update task status: " + e.getMessage());
            }
        });
    }

    public void getTaskById(int taskId, TaskCallback callback) {
        executor.execute(() -> {
            try {
                Task task = database.taskDao().getTaskById(taskId);
                callback.onTaskRetrieved(task);
            } catch (Exception e) {
                callback.onError("Failed to get task: " + e.getMessage());
            }
        });
    }

    public void getActiveTasksByUserId(String userId, TaskCallback callback) {
        executor.execute(() -> {
            try {
                List<Task> tasks = database.taskDao().getActiveTasksByUserId(userId);
                callback.onTasksRetrieved(tasks);
            } catch (Exception e) {
                callback.onError("Failed to get active tasks: " + e.getMessage());
            }
        });
    }

    public void getTasksByUserId(String userId, TaskCallback callback) {
        executor.execute(() -> {
            try {
                List<Task> tasks = database.taskDao().getActiveTasksByUserId(userId);
                callback.onTasksRetrieved(tasks);
            } catch (Exception e) {
                callback.onError("Failed to get tasks by user: " + e.getMessage());
            }
        });
    }

    public void getAllTasks(String userId, TaskCallback callback) {
        getTasksByUserId(userId, callback);
    }

    public void insertTaskCompletion(TaskCompletion completion, TaskCallback callback) {
        executor.execute(() -> {
            try {
                database.taskCompletionDao().insertTaskCompletion(completion);
                callback.onSuccess("Task completion recorded successfully");
            } catch (Exception e) {
                callback.onError("Failed to record task completion: " + e.getMessage());
            }
        });
    }

    public void getTaskCountByDifficultyAndImportanceForDate(String userId, String difficulty, String importance, String date, TaskCallback callback) {
        executor.execute(() -> {
            try {
                int count = database.taskDao().getTaskCountByDifficultyAndImportanceForDate(userId, difficulty, importance, date);
                callback.onTaskCountRetrieved(count);
            } catch (Exception e) {
                callback.onError("Failed to get daily quota count: " + e.getMessage());
            }
        });
    }

    public void getExtremeTaskCountForWeek(String userId, String weekStart, String weekEnd, TaskCallback callback) {
        executor.execute(() -> {
            try {
                int count = database.taskDao().getExtremeTaskCountForWeek(userId, weekStart, weekEnd);
                callback.onTaskCountRetrieved(count);
            } catch (Exception e) {
                callback.onError("Failed to get weekly extreme count: " + e.getMessage());
            }
        });
    }

    public void getSpecialTaskCountForMonth(String userId, String monthStart, String monthEnd, TaskCallback callback) {
        executor.execute(() -> {
            try {
                int count = database.taskDao().getSpecialTaskCountForMonth(userId, monthStart, monthEnd);
                callback.onTaskCountRetrieved(count);
            } catch (Exception e) {
                callback.onError("Failed to get monthly special count: " + e.getMessage());
            }
        });
    }
    
    public void getTasksByDate(String userId, String date, TaskCallback callback) {
        executor.execute(() -> {
            try {
                List<Task> tasks = database.taskDao().getTasksByDate(userId, date);
                callback.onTasksRetrieved(tasks);
            } catch (Exception e) {
                callback.onError("Failed to get tasks by date: " + e.getMessage());
            }
        });
    }
    
    public void getTasksInDateRange(String userId, String startDate, String endDate, TaskCallback callback) {
        executor.execute(() -> {
            try {
                List<Task> tasks = database.taskDao().getTasksInDateRange(userId, startDate, endDate);
                callback.onTasksRetrieved(tasks);
            } catch (Exception e) {
                callback.onError("Failed to get tasks in date range: " + e.getMessage());
            }
        });
    }
    
    public void deleteTask(int taskId, TaskCallback callback) {
        executor.execute(() -> {
            try {
                database.taskDao().deleteTaskById(taskId);
                callback.onSuccess("Task deleted successfully");
            } catch (Exception e) {
                callback.onError("Failed to delete task: " + e.getMessage());
            }
        });
    }
    
    public void deleteFutureRecurringInstances(String userId, String taskBaseName, String currentDate, TaskCallback callback) {
        executor.execute(() -> {
            try {
                String namePattern = taskBaseName + " (%)";
                database.taskDao().deleteFutureRecurringInstances(userId, namePattern, currentDate);
                callback.onSuccess("Future recurring instances deleted successfully");
            } catch (Exception e) {
                callback.onError("Failed to delete future instances: " + e.getMessage());
            }
        });
    }

    public interface TaskCallback {
        void onSuccess(String message);
        void onError(String error);
        void onTaskRetrieved(Task task);
        void onTasksRetrieved(List<Task> tasks);
        void onTaskCountRetrieved(int count);
    }
}
