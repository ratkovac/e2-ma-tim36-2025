package com.habitrpg.taskmanager.service;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.data.database.entities.TaskCompletion;
import com.habitrpg.taskmanager.data.database.entities.UserStatistics;
import com.habitrpg.taskmanager.data.database.repository.UserStatisticsRepository;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;
import com.habitrpg.taskmanager.util.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatisticsService {
    
    private static StatisticsService instance;
    private AppDatabase database;
    private UserStatisticsRepository userStatisticsRepository;
    private UserPreferences userPreferences;
    private ExecutorService executor;
    
    private StatisticsService(Context context) {
        database = AppDatabase.getDatabase(context);
        userStatisticsRepository = new UserStatisticsRepository(context);
        userPreferences = UserPreferences.getInstance(context);
        executor = Executors.newFixedThreadPool(2);
    }
    
    public static synchronized StatisticsService getInstance(Context context) {
        if (instance == null) {
            instance = new StatisticsService(context.getApplicationContext());
        }
        return instance;
    }
    
    public void getUserStatistics(StatisticsCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        userStatisticsRepository.getUserStatisticsByUserId(userId, new UserStatisticsRepository.UserStatisticsCallback() {
            @Override
            public void onSuccess(String message) {
                // This won't be called for getUserStatisticsByUserId
            }
            
            @Override
            public void onUserStatisticsRetrieved(UserStatistics statistics) {
                if (statistics == null) {
                    statistics = new UserStatistics(userId);
                    userStatisticsRepository.insertUserStatistics(statistics, null);
                }
                
                updateStatistics(statistics);
                callback.onStatisticsRetrieved(statistics);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void getTasksByCategory(StatisticsCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        executor.execute(() -> {
            try {
                List<Task> completedTasks = database.taskDao().getCompletedTasksByUserId(userId);
                Map<String, Integer> categoryCounts = new HashMap<>();
                
                for (Task task : completedTasks) {
                    Category category = database.categoryDao().getCategoryById(task.getCategoryId());
                    if (category != null) {
                        String categoryName = category.getName();
                        categoryCounts.put(categoryName, categoryCounts.getOrDefault(categoryName, 0) + 1);
                    }
                }
                
                callback.onCategoryStatsRetrieved(categoryCounts);
                
            } catch (Exception e) {
                callback.onError("Failed to get category statistics: " + e.getMessage());
            }
        });
    }
    
    public void getXPProgressLast7Days(StatisticsCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        executor.execute(() -> {
            try {
                String[] weekDates = DateUtils.getCurrentWeekDates();
                List<TaskCompletion> completions = database.taskCompletionDao()
                    .getCompletionsByUserIdAndDateRange(userId, weekDates[0], weekDates[1]);
                
                Map<String, Integer> dailyXP = new HashMap<>();
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -6);
                
                for (int i = 0; i < 7; i++) {
                    String date = DateUtils.getCurrentDateString();
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    dailyXP.put(date, 0);
                }
                
                for (TaskCompletion completion : completions) {
                    String date = completion.getCompletedDate();
                    dailyXP.put(date, dailyXP.getOrDefault(date, 0) + completion.getXpEarned());
                }
                
                callback.onXPProgressRetrieved(dailyXP);
                
            } catch (Exception e) {
                callback.onError("Failed to get XP progress: " + e.getMessage());
            }
        });
    }
    
    public void getAverageDifficultyXP(StatisticsCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        executor.execute(() -> {
            try {
                List<Task> completedTasks = database.taskDao().getCompletedTasksByUserId(userId);
                Map<String, Integer> difficultyXP = new HashMap<>();
                
                for (Task task : completedTasks) {
                    String difficulty = task.getDifficulty();
                    int xp = task.getXpValue();
                    difficultyXP.put(difficulty, difficultyXP.getOrDefault(difficulty, 0) + xp);
                }
                
                callback.onDifficultyStatsRetrieved(difficultyXP);
                
            } catch (Exception e) {
                callback.onError("Failed to get difficulty statistics: " + e.getMessage());
            }
        });
    }
    
    private void updateStatistics(UserStatistics stats) {
        String userId = stats.getUserId();
        
        List<Task> allTasks = database.taskDao().getTasksByUserId(userId);
        List<TaskCompletion> allCompletions = database.taskCompletionDao().getCompletionsByUserId(userId);
        
        int created = 0, completed = 0, pending = 0, cancelled = 0;
        int specialStarted = 0, specialCompleted = 0;
        int totalXP = 0;
        
        for (Task task : allTasks) {
            created++;
            if ("completed".equals(task.getStatus())) {
                completed++;
                if ("special".equals(task.getImportance())) {
                    specialCompleted++;
                }
            } else if ("cancelled".equals(task.getStatus())) {
                cancelled++;
            } else {
                pending++;
            }
            
            if ("special".equals(task.getImportance())) {
                specialStarted++;
            }
        }
        
        for (TaskCompletion completion : allCompletions) {
            totalXP += completion.getXpEarned();
        }
        
        stats.setTotalTasksCreated(created);
        stats.setTotalTasksCompleted(completed);
        stats.setTotalTasksPending(pending);
        stats.setTotalTasksCancelled(cancelled);
        stats.setTotalSpecialMissionsStarted(specialStarted);
        stats.setTotalSpecialMissionsCompleted(specialCompleted);
        stats.setTotalXP(totalXP);
        
        calculateStreaks(stats, allCompletions);
        calculateActiveDays(stats);
        
        database.userStatisticsDao().updateUserStatistics(stats);
        
        userStatisticsRepository.updateUserStatistics(stats, null);
    }
    
    private void calculateStreaks(UserStatistics stats, List<TaskCompletion> completions) {
        Map<String, Integer> dailyCompletions = new HashMap<>();
        
        for (TaskCompletion completion : completions) {
            String date = completion.getCompletedDate();
            dailyCompletions.put(date, dailyCompletions.getOrDefault(date, 0) + 1);
        }
        
        int currentStreak = 0;
        int longestStreak = 0;
        int tempStreak = 0;
        
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 365; i++) {
            String date = DateUtils.getCurrentDateString();
            cal.add(Calendar.DAY_OF_YEAR, -1);
            
            if (dailyCompletions.containsKey(date)) {
                tempStreak++;
                if (i == 0) {
                    currentStreak = tempStreak;
                }
                longestStreak = Math.max(longestStreak, tempStreak);
            } else {
                tempStreak = 0;
            }
        }
        
        stats.setCurrentStreak(currentStreak);
        stats.setLongestStreak(longestStreak);
    }
    
    private void calculateActiveDays(UserStatistics stats) {
        String userId = stats.getUserId();
        List<TaskCompletion> completions = database.taskCompletionDao().getCompletionsByUserId(userId);
        
        Map<String, Boolean> activeDays = new HashMap<>();
        for (TaskCompletion completion : completions) {
            activeDays.put(completion.getCompletedDate(), true);
        }
        
        stats.setTotalDaysActive(activeDays.size());
    }
    
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
        if (userStatisticsRepository != null) {
            userStatisticsRepository.shutdown();
        }
    }
    
    public interface StatisticsCallback {
        void onStatisticsRetrieved(UserStatistics statistics);
        void onCategoryStatsRetrieved(Map<String, Integer> categoryStats);
        void onXPProgressRetrieved(Map<String, Integer> xpProgress);
        void onDifficultyStatsRetrieved(Map<String, Integer> difficultyStats);
        void onError(String error);
    }
}
