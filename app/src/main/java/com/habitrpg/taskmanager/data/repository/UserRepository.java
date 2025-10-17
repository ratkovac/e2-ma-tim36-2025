package com.habitrpg.taskmanager.data.repository;

import android.content.Context;
import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {
    
    private static UserRepository instance;
    private AppDatabase database;
    private FirebaseManager firebaseManager;
    private UserPreferences userPreferences;
    private ExecutorService executor;
    
    private UserRepository(Context context) {
        database = AppDatabase.getDatabase(context);
        firebaseManager = FirebaseManager.getInstance();
        userPreferences = UserPreferences.getInstance(context);
        executor = Executors.newFixedThreadPool(2);
    }
    
    public static synchronized UserRepository getInstance(Context context) {
        if (instance == null) {
            instance = new UserRepository(context.getApplicationContext());
        }
        return instance;
    }
    
    private void ensureExecutorActive() {
        if (executor.isShutdown()) {
            executor = Executors.newFixedThreadPool(2);
        }
    }
    
    public void getUserById(String userId, UserCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                User user = database.userDao().getUserById(userId);
                callback.onUserRetrieved(user);
            } catch (Exception e) {
                callback.onError("Failed to get user: " + e.getMessage());
            }
        });
    }
    
    public void getCurrentUser(UserCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId != null) {
            getUserById(userId, callback);
        } else {
            callback.onUserRetrieved(null);
        }
    }
    
    public void insertUser(User user, UserCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                database.userDao().insertUser(user);
                callback.onSuccess("User inserted successfully");
            } catch (Exception e) {
                callback.onError("Failed to insert user: " + e.getMessage());
            }
        });
    }
    
    public void updateUser(User user, UserCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                database.userDao().updateUser(user);
                
                firebaseManager.updateUserDocument(user, (success, exception) -> {
                    if (success) {
                        callback.onSuccess("User updated successfully");
                    } else {
                        callback.onError("Failed to sync user update: " + 
                            (exception != null ? exception.getMessage() : "Unknown error"));
                    }
                });
            } catch (Exception e) {
                callback.onError("Failed to update user: " + e.getMessage());
            }
        });
    }
    
    public void loginUser(String userId, UserCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                // First check if user exists in local database
                User localUser = database.userDao().getUserById(userId);
                
                if (localUser == null) {
                    // User doesn't exist locally, fetch from Firebase
                    firebaseManager.getUserDocument(userId, userData -> {
                        if (userData != null) {
                            // Create user from Firebase data
                            User user = new User();
                            user.setId(userId);
                            user.setEmail((String) userData.get("email"));
                            user.setUsername((String) userData.get("username"));
                            
                            Object avatarIdObj = userData.get("avatarId");
                            user.setAvatarId(avatarIdObj != null ? ((Number) avatarIdObj).intValue() : 1);
                            
                            Object levelObj = userData.get("level");
                            user.setLevel(levelObj != null ? ((Number) levelObj).intValue() : 1);
                            
                            user.setTitle((String) userData.getOrDefault("title", "Beginner"));
                            
                            Object powerPointsObj = userData.get("powerPoints");
                            user.setPowerPoints(powerPointsObj != null ? ((Number) powerPointsObj).intValue() : 0);
                            
                            Object xpObj = userData.get("experiencePoints");
                            user.setExperiencePoints(xpObj != null ? ((Number) xpObj).intValue() : 0);
                            
                            Object coinsObj = userData.get("coins");
                            user.setCoins(coinsObj != null ? ((Number) coinsObj).intValue() : 300);
                            
                            // Insert user into local database
                            executor.execute(() -> {
                                try {
                                    database.userDao().insertUser(user);
                                    database.userDao().logoutAllUsers();
                                    database.userDao().loginUser(userId);
                                    
                                    // Don't initialize stage start time for level 1 users
                                    // Stage start time will be set when user levels up
                                    
                                    callback.onSuccess("User logged in successfully");
                                } catch (Exception e) {
                                    callback.onError("Failed to save user locally: " + e.getMessage());
                                }
                            });
                        } else {
                            callback.onError("User data not found in Firebase");
                        }
                    });
                } else {
                    // User exists locally, just update login status
                    database.userDao().logoutAllUsers();
                    database.userDao().loginUser(userId);
                    
                    // Don't initialize stage start time for level 1 users
                    // Stage start time will be set when user levels up
                    
                    callback.onSuccess("User logged in successfully");
                }
            } catch (Exception e) {
                callback.onError("Failed to login user: " + e.getMessage());
            }
        });
    }
    
    public void logoutAllUsers(UserCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                database.userDao().logoutAllUsers();
                callback.onSuccess("All users logged out successfully");
            } catch (Exception e) {
                callback.onError("Failed to logout users: " + e.getMessage());
            }
        });
    }
    
    public void createUserDocument(User user, UserCallback callback) {
        firebaseManager.createUserDocument(user, (success, exception) -> {
            if (success) {
                callback.onSuccess("User document created successfully");
            } else {
                callback.onError("Failed to create user document: " + 
                    (exception != null ? exception.getMessage() : "Unknown error"));
            }
        });
    }
    
    public interface UserCallback {
        void onSuccess(String message);
        void onError(String error);
        void onUserRetrieved(User user);
    }
}
