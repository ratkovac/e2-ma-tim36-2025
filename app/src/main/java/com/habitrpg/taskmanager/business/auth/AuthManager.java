package com.habitrpg.taskmanager.business.auth;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthManager {
    
    private static AuthManager instance;
    private FirebaseManager firebaseManager;
    private UserPreferences userPreferences;
    private AppDatabase database;
    private ExecutorService executor;
    
    private AuthManager(Context context) {
        firebaseManager = FirebaseManager.getInstance();
        userPreferences = UserPreferences.getInstance(context);
        database = AppDatabase.getDatabase(context);
        executor = Executors.newFixedThreadPool(2);
    }
    
    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Register a new user
     */
    public void registerUser(String email, String password, String username, int avatarId, AuthCallback callback) {
        // Disable reCAPTCHA for debug builds
        FirebaseAuth auth = firebaseManager.getAuth();
        auth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = firebaseManager.getCurrentUser();
                    if (firebaseUser != null) {
                        String userId = firebaseUser.getUid();
                        User user = new User(userId, email, username, avatarId);
                        
                        // Save user to Firebase
                        firebaseManager.createUserDocument(user, (success, exception) -> {
                            if (success) {
                                // Save user to local database
                                executor.execute(() -> {
                                    database.userDao().insertUser(user);
                                    callback.onSuccess("Registration successful. Please check your email for activation.");
                                });
                            } else {
                                callback.onError("Failed to create user profile: " + 
                                    (exception != null ? exception.getMessage() : "Unknown error"));
                            }
                        });
                    } else {
                        callback.onError("Failed to get user information after registration");
                    }
                } else {
                    String errorMessage = "Registration failed";
                    if (task.getException() != null) {
                        errorMessage += ": " + task.getException().getMessage();
                    }
                    callback.onError(errorMessage);
                }
            });
    }
    
    /**
     * Login user
     */
    public void loginUser(String email, String password, AuthCallback callback) {
        firebaseManager.getAuth().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = firebaseManager.getCurrentUser();
                    if (firebaseUser != null) {
                        String userId = firebaseUser.getUid();
                        
                        // Check if account is activated (fetch from Firebase)
                        firebaseManager.getUserDocument(userId, userData -> {
                            if (userData != null) {
                                Boolean activated = (Boolean) userData.get("activated");
                                if (activated != null && activated) {
                                    // Save session
                                    userPreferences.setLoggedIn(true);
                                    userPreferences.setCurrentUserId(userId);
                                    
                                    // Update local database
                                    executor.execute(() -> {
                                        database.userDao().logoutAllUsers();
                                        database.userDao().loginUser(userId);
                                    });
                                    
                                    callback.onSuccess("Login successful");
                                } else {
                                    firebaseManager.getAuth().signOut();
                                    callback.onError("Account not activated. Please check your email.");
                                }
                            } else {
                                firebaseManager.getAuth().signOut();
                                callback.onError("User profile not found");
                            }
                        });
                    } else {
                        callback.onError("Failed to get user information");
                    }
                } else {
                    String errorMessage = "Login failed";
                    if (task.getException() != null) {
                        errorMessage += ": " + task.getException().getMessage();
                    }
                    callback.onError(errorMessage);
                }
            });
    }
    
    /**
     * Logout user
     */
    public void logoutUser(AuthCallback callback) {
        // Clear local session
        userPreferences.clearUserData();
        
        // Update local database
        executor.execute(() -> {
            database.userDao().logoutAllUsers();
        });
        
        // Sign out from Firebase
        firebaseManager.getAuth().signOut();
        
        if (callback != null) {
            callback.onSuccess("Logged out successfully");
        }
    }
    
    /**
     * Get current logged-in user
     */
    public void getCurrentUser(UserCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId != null) {
            executor.execute(() -> {
                User user = database.userDao().getUserById(userId);
                callback.onUserRetrieved(user);
            });
        } else {
            callback.onUserRetrieved(null);
        }
    }
    
    /**
     * Check if user is logged in
     */
    public boolean isUserLoggedIn() {
        return userPreferences.isLoggedIn() && firebaseManager.isUserLoggedIn();
    }
    
    /**
     * Update user password
     */
    public void updatePassword(String newPassword, AuthCallback callback) {
        FirebaseUser user = firebaseManager.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess("Password updated successfully");
                    } else {
                        String errorMessage = "Password update failed";
                        if (task.getException() != null) {
                            errorMessage += ": " + task.getException().getMessage();
                        }
                        callback.onError(errorMessage);
                    }
                });
        } else {
            callback.onError("No user logged in");
        }
    }
    
    /**
     * Create default categories for new user
     */
    public void createDefaultCategories(String userId) {
        executor.execute(() -> {
            // Create default categories as per PRD
            String[] defaultCategories = {"Health", "Study", "Work", "Personal"};
            String[] defaultColors = {"#4CAF50", "#2196F3", "#FF9800", "#9C27B0"};
            
            for (int i = 0; i < defaultCategories.length; i++) {
                com.habitrpg.taskmanager.data.database.entities.Category category = 
                    new com.habitrpg.taskmanager.data.database.entities.Category(
                        userId, defaultCategories[i], defaultColors[i]);
                database.categoryDao().insertCategory(category);
            }
        });
    }
    
    // Callback interfaces
    public interface AuthCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public interface UserCallback {
        void onUserRetrieved(User user);
    }
}
