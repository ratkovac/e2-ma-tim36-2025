package com.habitrpg.taskmanager.data.database.repository;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.entities.UserStatistics;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserStatisticsRepository {
    
    private AppDatabase database;
    private ExecutorService executor;
    
    public UserStatisticsRepository(Context context) {
        database = AppDatabase.getDatabase(context);
        executor = Executors.newFixedThreadPool(2);
    }
    
    public void insertUserStatistics(UserStatistics userStatistics, UserStatisticsCallback callback) {
        executor.execute(() -> {
            try {
                database.userStatisticsDao().insertUserStatistics(userStatistics);
                if (callback != null) {
                    callback.onSuccess("User statistics saved successfully");
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Failed to save user statistics: " + e.getMessage());
                }
            }
        });
    }
    
    public void updateUserStatistics(UserStatistics userStatistics, UserStatisticsCallback callback) {
        executor.execute(() -> {
            try {
                database.userStatisticsDao().updateUserStatistics(userStatistics);
                if (callback != null) {
                    callback.onSuccess("User statistics updated successfully");
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Failed to update user statistics: " + e.getMessage());
                }
            }
        });
    }
    
    public void getUserStatisticsByUserId(String userId, UserStatisticsCallback callback) {
        executor.execute(() -> {
            try {
                UserStatistics statistics = database.userStatisticsDao().getUserStatisticsByUserId(userId);
                if (callback != null) {
                    callback.onUserStatisticsRetrieved(statistics);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Failed to get user statistics: " + e.getMessage());
                }
            }
        });
    }
    
    public void deleteUserStatisticsByUserId(String userId, UserStatisticsCallback callback) {
        executor.execute(() -> {
            try {
                database.userStatisticsDao().deleteUserStatisticsByUserId(userId);
                if (callback != null) {
                    callback.onSuccess("User statistics deleted successfully");
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Failed to delete user statistics: " + e.getMessage());
                }
            }
        });
    }
    
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
    
    public interface UserStatisticsCallback {
        void onSuccess(String message);
        void onUserStatisticsRetrieved(UserStatistics userStatistics);
        void onError(String error);
    }
}
