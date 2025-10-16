package com.habitrpg.taskmanager.service;

import android.content.Context;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.data.database.entities.TaskCompletion;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;
import com.habitrpg.taskmanager.data.repository.TaskRepository;
import com.habitrpg.taskmanager.data.repository.UserRepository;
import com.habitrpg.taskmanager.service.XPService;
import com.habitrpg.taskmanager.util.DateUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskService {
    
    private static TaskService instance;
    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private UserPreferences userPreferences;

    private TaskService(Context context) {
        taskRepository = TaskRepository.getInstance(context);
        userRepository = UserRepository.getInstance(context);
        userPreferences = UserPreferences.getInstance(context);
    }
    
    public static synchronized TaskService getInstance(Context context) {
        if (instance == null) {
            instance = new TaskService(context.getApplicationContext());
        }
        return instance;
    }
    
    public void createTask(Task task, TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        task.setUserId(userId);
        // Ensure start_date is set for single tasks to current date and time if not provided
        if (!task.isRecurring() && (task.getStartDate() == null || task.getStartDate().isEmpty())) {
            task.setStartDate(DateUtils.getCurrentDateTimeString());
        }
        
        validateTaskQuota(task, isValid -> {
            if (!isValid) {
                callback.onError("Task quota limit exceeded for this difficulty/importance combination");
                return;
            }
            
            // Get user level for XP calculation
            userRepository.getUserById(userId, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(String message) {}
                
                @Override
                public void onError(String error) {
                    callback.onError("Failed to get user data: " + error);
                }
                
                @Override
                public void onUserRetrieved(User user) {
                    if (user != null) {
                        int userLevel = user.getLevel();
                        int xpValue = XPService.calculateTaskXP(task.getDifficulty(), task.getImportance(), userLevel);
                        task.setXpValue(xpValue);
                        
                        if (task.isRecurring()) {
                            createRecurringTasks(task, callback);
                        } else {
                            createSingleTask(task, callback);
                        }
                    } else {
                        callback.onError("User not found");
                    }
                }
            });
        });
    }
    
    private void createSingleTask(Task task, TaskCallback callback) {
        taskRepository.insertTask(task, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess("Task created successfully!");
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {}
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
            
            @Override
            public void onTaskCountRetrieved(int count) {}
        });
    }
    
    private void createRecurringTasks(Task task, TaskCallback callback) {
        try {
            // Create only individual instances (no template)
            createRecurringInstances(task, callback);
            
        } catch (Exception e) {
            callback.onError("Failed to create recurring tasks: " + e.getMessage());
        }
    }
    
    private void createRecurringInstances(Task templateTask, TaskCallback callback) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            
            // Parse start date and time from template
            String[] startDateTimeParts = templateTask.getStartDate().split(" ");
            String startDateStr = startDateTimeParts[0];
            String startTimeStr = startDateTimeParts.length > 1 ? startDateTimeParts[1] : "00:00";
            
            java.util.Date startDate = sdf.parse(startDateStr);
            java.util.Date endDate = sdf.parse(templateTask.getEndDate());
            
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(startDate);
            
            int taskCount = 0;
            while (cal.getTime().before(endDate) || cal.getTime().equals(endDate)) {
                Task recurringTask = new Task();
                recurringTask.setUserId(templateTask.getUserId());
                recurringTask.setCategoryId(templateTask.getCategoryId());
                recurringTask.setName(templateTask.getName());
                recurringTask.setDescription(templateTask.getDescription());
                recurringTask.setDifficulty(templateTask.getDifficulty());
                recurringTask.setImportance(templateTask.getImportance());
                recurringTask.setXpValue(templateTask.getXpValue());
                recurringTask.setRecurring(true); // Instance is from recurring task
                
                // Copy recurrence settings from template
                recurringTask.setRecurrenceInterval(templateTask.getRecurrenceInterval());
                recurringTask.setRecurrenceUnit(templateTask.getRecurrenceUnit());
                recurringTask.setEndDate(templateTask.getEndDate());
                
                // Combine date and time for this instance
                String instanceDate = sdf.format(cal.getTime());
                recurringTask.setStartDate(instanceDate + " " + startTimeStr);
                
                String taskDate = sdf.format(cal.getTime());
                recurringTask.setName(templateTask.getName() + " (" + taskDate + ")");
                
                taskRepository.insertTask(recurringTask, new TaskRepository.TaskCallback() {
                    @Override
                    public void onSuccess(String message) {}
                    
                    @Override
                    public void onError(String error) {}
                    
                    @Override
                    public void onTaskRetrieved(Task task) {}
                    
                    @Override
                    public void onTasksRetrieved(List<Task> tasks) {}
                    
                    @Override
                    public void onTaskCountRetrieved(int count) {}
                });
                
                taskCount++;
                
                if ("day".equals(templateTask.getRecurrenceUnit())) {
                    cal.add(java.util.Calendar.DAY_OF_MONTH, templateTask.getRecurrenceInterval());
                } else if ("week".equals(templateTask.getRecurrenceUnit())) {
                    cal.add(java.util.Calendar.WEEK_OF_YEAR, templateTask.getRecurrenceInterval());
                }
            }
            
            callback.onSuccess("Recurring task created successfully! Generated " + taskCount + " instances.");
            
        } catch (Exception e) {
            callback.onError("Failed to create recurring task instances: " + e.getMessage());
        }
    }
    
    public void completeTask(int taskId, TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        taskRepository.getTaskById(taskId, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {
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
                
                // Validacija: Samo aktivan zadatak može biti označen kao urađen
                if (!"active".equals(task.getStatus())) {
                    callback.onError("Only active tasks can be marked as completed");
                    return;
                }
                
                // Validacija: Ne može se označiti budući zadatak kao urađen
                if (!isTaskDateValidForCompletion(task)) {
                    callback.onError("Cannot complete future tasks");
                    return;
                }
                
                // Validacija: Zadatak se može označiti do 3 dana unazad
                if (!isWithinCompletionWindow(task)) {
                    callback.onError("Task can only be completed within 3 days of its scheduled date");
                    return;
                }
                
                taskRepository.updateTaskStatus(taskId, "completed", new TaskRepository.TaskCallback() {
                    @Override
                    public void onSuccess(String message) {
                        String currentDate = DateUtils.getCurrentDateString();
                        TaskCompletion completion = new TaskCompletion(taskId, currentDate, task.getXpValue());
                        
                        taskRepository.insertTaskCompletion(completion, new TaskRepository.TaskCallback() {
                            @Override
                            public void onSuccess(String message) {
                                updateUserXP(task, callback);
                            }
                            
                            @Override
                            public void onError(String error) {
                                callback.onError("Failed to record completion: " + error);
                            }
                            
                            @Override
                            public void onTaskRetrieved(Task task) {}
                            
                            @Override
                            public void onTasksRetrieved(List<Task> tasks) {}
                            
                            @Override
                            public void onTaskCountRetrieved(int count) {}
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError("Failed to update task status: " + error);
                    }
                    
                    @Override
                    public void onTaskRetrieved(Task task) {}
                    
                    @Override
                    public void onTasksRetrieved(List<Task> tasks) {}
                    
                    @Override
                    public void onTaskCountRetrieved(int count) {}
                });
            }
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
            
            @Override
            public void onTaskCountRetrieved(int count) {}
        });
    }
    
    private void updateUserXP(Task task, TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        // XP pravila: Pauzirani i otkazani zadaci ne daju XP
        if ("paused".equals(task.getStatus()) || "cancelled".equals(task.getStatus())) {
            callback.onSuccess("Task status updated (no XP awarded for " + task.getStatus() + " tasks)");
            return;
        }
        
        userRepository.getUserById(userId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError("Failed to get user: " + error);
            }
            
            @Override
            public void onUserRetrieved(User user) {
                if (user != null) {
                    int newXP = user.getExperiencePoints() + task.getXpValue();
                    int oldLevel = user.getLevel();
                    int newLevel = XPService.calculateLevelFromXP(newXP);
                    
                    user.setExperiencePoints(newXP);
                    
                    boolean leveledUp;
                    final int ppEarned;
                    if (newLevel > oldLevel) {
                        user.setLevel(newLevel);
                        user.setTitle(XPService.getTitleForLevel(newLevel));
                        leveledUp = true;
                        
                        // Calculate PP rewards for all levels gained
                        int totalPPEarned = 0;
                        for (int level = oldLevel + 1; level <= newLevel; level++) {
                            totalPPEarned += XPService.getPPRewardForLevel(level);
                        }
                        user.setPowerPoints(user.getPowerPoints() + totalPPEarned);
                        ppEarned = totalPPEarned;
                    } else {
                        leveledUp = false;
                        ppEarned = 0;
                    }

                    userRepository.updateUser(user, new UserRepository.UserCallback() {
                        @Override
                        public void onSuccess(String message) {
                            String resultMessage = "Task completed! +" + task.getXpValue() + " XP";
                            if (leveledUp) {
                                resultMessage += "\nLevel up! You are now level " + newLevel + " (" + user.getTitle() + ")";
                                if (ppEarned > 0) {
                                    resultMessage += "\n+" + ppEarned + " Power Points earned!";
                                }
                                // Trigger boss fight for level up
                                callback.onLevelUp(newLevel, ppEarned);
                            }
                            callback.onSuccess(resultMessage);
                        }
                        
                        @Override
                        public void onError(String error) {
                            callback.onError("Failed to update user: " + error);
                        }
                        
                        @Override
                        public void onUserRetrieved(User user) {}
                    });
                } else {
                    callback.onError("User data not found");
                }
            }
        });
    }
    
    public void getActiveTasks(TaskListCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onTasksRetrieved(null);
            return;
        }
        
        taskRepository.getActiveTasksByUserId(userId, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onTasksRetrieved(null);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {}
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {
                callback.onTasksRetrieved(tasks);
            }
            
            @Override
            public void onTaskCountRetrieved(int count) {}
        });
    }
    
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
        
        int xpValue = XPService.calculateTaskXP(task.getDifficulty(), task.getImportance());
        task.setXpValue(xpValue);
        
        taskRepository.updateTask(task, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {}
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
            
            @Override
            public void onTaskCountRetrieved(int count) {}
        });
    }
    
    public void cancelTask(int taskId, TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        taskRepository.getTaskById(taskId, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {
                if (task == null) {
                    callback.onError("Task not found");
                    return;
                }
                
                if (!task.getUserId().equals(userId)) {
                    callback.onError("Unauthorized to cancel this task");
                    return;
                }
                
                taskRepository.updateTaskStatus(taskId, "cancelled", new TaskRepository.TaskCallback() {
                    @Override
                    public void onSuccess(String message) {
                        callback.onSuccess("Task cancelled");
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError("Failed to cancel task: " + error);
                    }
                    
                    @Override
                    public void onTaskRetrieved(Task task) {}
                    
                    @Override
                    public void onTasksRetrieved(List<Task> tasks) {}
                    
                    @Override
                    public void onTaskCountRetrieved(int count) {}
                });
            }
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
            
            @Override
            public void onTaskCountRetrieved(int count) {}
        });
    }
    
    public void getTaskById(int taskId, TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        taskRepository.getTaskById(taskId, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {
                if (task == null) {
                    callback.onError("Task not found");
                    return;
                }
                
                if (!task.getUserId().equals(userId)) {
                    callback.onError("Unauthorized to view this task");
                    return;
                }
                
                List<Task> tasks = new java.util.ArrayList<>();
                tasks.add(task);
                callback.onTasksRetrieved(tasks);
            }
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
            
            @Override
            public void onTaskCountRetrieved(int count) {}
        });
    }
    
    public void updateTaskStatus(int taskId, String status, TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        taskRepository.getTaskById(taskId, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {
                if (task == null) {
                    callback.onError("Task not found");
                    return;
                }
                
                if (!task.getUserId().equals(userId)) {
                    callback.onError("Unauthorized to update this task");
                    return;
                }
                
                // Validacija statusnih promena prema pravilima
                if (!isStatusChangeValid(task, status)) {
                    callback.onError(getStatusChangeErrorMessage(task, status));
                    return;
                }
                
                taskRepository.updateTaskStatus(taskId, status, new TaskRepository.TaskCallback() {
                    @Override
                    public void onSuccess(String message) {
                        callback.onSuccess("Task status updated successfully");
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError("Failed to update task status: " + error);
                    }
                    
                    @Override
                    public void onTaskRetrieved(Task task) {}
                    
                    @Override
                    public void onTasksRetrieved(List<Task> tasks) {}
                    
                    @Override
                    public void onTaskCountRetrieved(int count) {}
                });
            }
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
            
            @Override
            public void onTaskCountRetrieved(int count) {}
        });
    }
    
    public void getTasksByCategory(long categoryId, TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        taskRepository.getTasksByUserId(userId, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {}
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {
                List<Task> filteredTasks = tasks.stream()
                    .filter(task -> task.getCategoryId() == categoryId)
                    .collect(java.util.stream.Collectors.toList());
                callback.onTasksRetrieved(filteredTasks);
            }
            
            @Override
            public void onTaskCountRetrieved(int count) {}
        });
    }
    
    public void getAllTasks(TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        taskRepository.getAllTasks(userId, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {}
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {
                callback.onTasksRetrieved(tasks);
            }
            
            @Override
            public void onTaskCountRetrieved(int count) {}
        });
    }
    
    public void getTaskCountByDifficultyAndImportanceForDate(String userId, String difficulty, String importance, String date, TaskCountCallback callback) {
        taskRepository.getTaskCountByDifficultyAndImportanceForDate(userId, difficulty, importance, date, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {}
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
            
            @Override
            public void onTaskCountRetrieved(int count) {
                callback.onTaskCountRetrieved(count);
            }
        });
    }
    
    public void getExtremeTaskCountForWeek(String userId, String weekStart, String weekEnd, TaskCountCallback callback) {
        taskRepository.getExtremeTaskCountForWeek(userId, weekStart, weekEnd, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {}
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
            
            @Override
            public void onTaskCountRetrieved(int count) {
                callback.onTaskCountRetrieved(count);
            }
        });
    }
    
    public void getSpecialTaskCountForMonth(String userId, String monthStart, String monthEnd, TaskCountCallback callback) {
        taskRepository.getSpecialTaskCountForMonth(userId, monthStart, monthEnd, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {}
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
            
            @Override
            public void onTaskCountRetrieved(int count) {
                callback.onTaskCountRetrieved(count);
            }
        });
    }
    
    private void validateTaskQuota(Task task, QuotaValidationCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        String currentDate = DateUtils.getCurrentDateString();
        
        String difficulty = task.getDifficulty();
        String importance = task.getImportance();
        
        if (!"special".equals(importance)) {
            getTaskCountByDifficultyAndImportanceForDate(userId, difficulty, importance, currentDate, new TaskCountCallback() {
                @Override
                public void onTaskCountRetrieved(int dailyCount) {
                    if ("very_easy".equals(difficulty) && "normal".equals(importance) && dailyCount >= 5) {
                        callback.onValidationResult(false);
                        return;
                    }
                    
                    if ("easy".equals(difficulty) && "important".equals(importance) && dailyCount >= 5) {
                        callback.onValidationResult(false);
                        return;
                    }
                    
                    if ("hard".equals(difficulty) && "very_important".equals(importance) && dailyCount >= 2) {
                        callback.onValidationResult(false);
                        return;
                    }
                    
                    checkWeeklyAndMonthlyLimits(task, callback);
                }
                
                @Override
                public void onError(String error) {
                    callback.onValidationResult(false);
                }
            });
        } else {
            checkWeeklyAndMonthlyLimits(task, callback);
        }
    }
    
    private void checkWeeklyAndMonthlyLimits(Task task, QuotaValidationCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        String difficulty = task.getDifficulty();
        String importance = task.getImportance();
        
        if ("extreme".equals(difficulty)) {
            String[] weekDates = DateUtils.getCurrentWeekDates();
            getExtremeTaskCountForWeek(userId, weekDates[0], weekDates[1], new TaskCountCallback() {
                @Override
                public void onTaskCountRetrieved(int weeklyCount) {
                    if (weeklyCount >= 1) {
                        callback.onValidationResult(false);
                        return;
                    }
                    checkSpecialTaskLimit(task, callback);
                }
                
                @Override
                public void onError(String error) {
                    callback.onValidationResult(false);
                }
            });
        } else {
            checkSpecialTaskLimit(task, callback);
        }
    }
    
    private void checkSpecialTaskLimit(Task task, QuotaValidationCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        String importance = task.getImportance();
        
        if ("special".equals(importance)) {
            String[] monthDates = DateUtils.getCurrentMonthDates();
            getSpecialTaskCountForMonth(userId, monthDates[0], monthDates[1], new TaskCountCallback() {
                @Override
                public void onTaskCountRetrieved(int monthlyCount) {
                    if (monthlyCount >= 1) {
                        callback.onValidationResult(false);
                        return;
                    }
                    callback.onValidationResult(true);
                }
                
                @Override
                public void onError(String error) {
                    callback.onValidationResult(false);
                }
            });
        } else {
            callback.onValidationResult(true);
        }
    }
    
    public void getTasksByDate(String date, TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        taskRepository.getTasksByDate(userId, date, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {}
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {
                callback.onTasksRetrieved(tasks);
            }
            
            @Override
            public void onTaskCountRetrieved(int count) {}
        });
    }
    
    public void getTasksInDateRange(String startDate, String endDate, TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        taskRepository.getTasksInDateRange(userId, startDate, endDate, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {}
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {
                callback.onTasksRetrieved(tasks);
            }
            
            @Override
            public void onTaskCountRetrieved(int count) {}
        });
    }
    
    public void deleteTask(int taskId, TaskCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        // First get task to check status and if it's recurring
        taskRepository.getTaskById(taskId, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onTaskRetrieved(Task task) {
                if (task == null) {
                    callback.onError("Task not found");
                    return;
                }
                
                if (!task.getUserId().equals(userId)) {
                    callback.onError("Unauthorized to delete this task");
                    return;
                }
                
                // Ne može se obrisati završen zadatak
                if ("completed".equals(task.getStatus())) {
                    callback.onError("Cannot delete completed tasks");
                    return;
                }
                
                // Briši zadatak (bilo da je ponavljajući ili obični)
                taskRepository.deleteTask(taskId, new TaskRepository.TaskCallback() {
                    @Override
                    public void onSuccess(String message) {
                        callback.onSuccess("Task deleted successfully");
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                    
                    @Override
                    public void onTaskRetrieved(Task task) {}
                    
                    @Override
                    public void onTasksRetrieved(List<Task> tasks) {}
                    
                    @Override
                    public void onTaskCountRetrieved(int count) {}
                });
            }
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
            
            @Override
            public void onTaskCountRetrieved(int count) {}
        });
    }
    
    public String getCurrentUserId() {
        return userPreferences.getCurrentUserId();
    }
    
    public interface TaskCountCallback {
        void onTaskCountRetrieved(int count);
        void onError(String error);
    }
    
    public interface TaskCallback {
        void onSuccess(String message);
        void onError(String error);
        void onTasksRetrieved(List<Task> tasks);
        default void onLevelUp(int newLevel, int ppEarned) {
            // Default implementation - can be overridden
        }
    }
    
    public interface TaskListCallback {
        void onTasksRetrieved(List<Task> tasks);
    }
    
    private interface QuotaValidationCallback {
        void onValidationResult(boolean isValid);
    }
    
    // Validacija datuma za označavanje zadatka kao urađenog
    private boolean isTaskDateValidForCompletion(Task task) {
        if (task.getStartDate() == null) {
            return true; // Ako nema datum, dozvoli
        }
        
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            java.util.Date taskDateTime = sdf.parse(task.getStartDate());
            java.util.Date currentDateTime = sdf.parse(DateUtils.getCurrentDateTimeString());
            
            return taskDateTime.compareTo(currentDateTime) <= 0; // Zadatak mora biti sada ili u prošlosti
        } catch (Exception e) {
            return false;
        }
    }
    
    // Validacija da li je zadatak u okviru od 3 dana za označavanje kao urađen
    private boolean isWithinCompletionWindow(Task task) {
        if (task.getStartDate() == null) {
            return true; // Ako nema datum, dozvoli
        }
        
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            java.util.Date taskDateTime = sdf.parse(task.getStartDate());
            java.util.Date currentDateTime = sdf.parse(DateUtils.getCurrentDateTimeString());
            
            long diffInMillis = currentDateTime.getTime() - taskDateTime.getTime();
            long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);
            
            return diffInDays <= 3; // Do 3 dana unazad
        } catch (Exception e) {
            return false;
        }
    }
    
    // Validacija da li je statusna promena dozvoljena
    private boolean isStatusChangeValid(Task task, String newStatus) {
        String currentStatus = task.getStatus();
        
        // Ne može se menjati status završenih, neurađenih i otkazanih zadataka
        if ("completed".equals(currentStatus) || "incomplete".equals(currentStatus) || "cancelled".equals(currentStatus)) {
            return false;
        }
        
        // Samo aktivan zadatak može biti označen kao urađen, otkazan ili pauziran
        if ("active".equals(currentStatus)) {
            return "completed".equals(newStatus) || "cancelled".equals(newStatus) || 
                   ("paused".equals(newStatus) && task.isRecurring());
        }
        
        // Pauziran zadatak može samo da se aktivira (samo za ponavljajuće)
        if ("paused".equals(currentStatus)) {
            return "active".equals(newStatus) && task.isRecurring();
        }
        
        return false;
    }
    
    // Poruka o grešci za nevažeće statusne promene
    private String getStatusChangeErrorMessage(Task task, String newStatus) {
        String currentStatus = task.getStatus();
        
        if ("completed".equals(currentStatus) || "incomplete".equals(currentStatus) || "cancelled".equals(currentStatus)) {
            return "Cannot modify " + getStatusText(currentStatus) + " tasks";
        }
        
        if ("active".equals(currentStatus)) {
            if ("paused".equals(newStatus) && !task.isRecurring()) {
                return "Only recurring tasks can be paused";
            }
        }
        
        if ("paused".equals(currentStatus)) {
            if (!task.isRecurring()) {
                return "Only recurring tasks can be paused";
            }
        }
        
        return "Invalid status change from " + getStatusText(currentStatus) + " to " + getStatusText(newStatus);
    }
    
    // Pomocna metoda za tekst statusa
    private String getStatusText(String status) {
        switch (status) {
            case "active": return "active";
            case "completed": return "completed";
            case "incomplete": return "incomplete";
            case "paused": return "paused";
            case "cancelled": return "cancelled";
            default: return status;
        }
    }
    
    
    // Automatsko prebacivanje zadataka u neurađen status nakon 3 dana
    public void checkAndUpdateOverdueTasks() {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) return;
        
        taskRepository.getActiveTasksByUserId(userId, new TaskRepository.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {}
            
            @Override
            public void onTaskRetrieved(Task task) {}
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {
                if (tasks != null) {
                    for (Task task : tasks) {
                        if (task.getStartDate() != null && !isWithinCompletionWindow(task)) {
                            // Zadatak je prošao 3 dana, označi kao neurađen
                            taskRepository.updateTaskStatus(task.getId(), "incomplete", new TaskRepository.TaskCallback() {
                                @Override
                                public void onSuccess(String message) {}
                                
                                @Override
                                public void onError(String error) {}
                                
                                @Override
                                public void onTaskRetrieved(Task task) {}
                                
                                @Override
                                public void onTasksRetrieved(List<Task> tasks) {}
                                
                                @Override
                                public void onTaskCountRetrieved(int count) {}
                            });
                        }
                    }
                }
            }
            
            @Override
            public void onTaskCountRetrieved(int count) {}
        });
    }
}
