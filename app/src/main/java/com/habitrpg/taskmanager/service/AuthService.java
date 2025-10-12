package com.habitrpg.taskmanager.service;

import android.content.Context;
import android.text.TextUtils;
import android.util.Patterns;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.data.repository.UserRepository;
import com.habitrpg.taskmanager.data.repository.CategoryRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

public class AuthService {
    
    private static AuthService instance;
    private FirebaseManager firebaseManager;
    private UserPreferences userPreferences;
    private UserRepository userRepository;
    private CategoryRepository categoryRepository;
    private ExecutorService executor;
    
    private AuthService(Context context) {
        firebaseManager = FirebaseManager.getInstance();
        userPreferences = UserPreferences.getInstance(context);
        userRepository = UserRepository.getInstance(context);
        categoryRepository = CategoryRepository.getInstance(context);
        executor = Executors.newFixedThreadPool(2);
    }
    
    public static synchronized AuthService getInstance(Context context) {
        if (instance == null) {
            instance = new AuthService(context.getApplicationContext());
        }
        return instance;
    }
    
    public void loginUser(String email, String password, AuthCallback callback) {
        firebaseManager.getAuth().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = firebaseManager.getCurrentUser();
                    if (firebaseUser != null) {
                        String userId = firebaseUser.getUid();
                        
                        if (firebaseUser.isEmailVerified()) {
                            userPreferences.setLoggedIn(true);
                            userPreferences.setCurrentUserId(userId);
                            
                            userRepository.loginUser(userId, new UserRepository.UserCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    callback.onSuccess("Login successful");
                                }
                                
                                @Override
                                public void onError(String error) {
                                    callback.onError("Failed to login user: " + error);
                                }
                                
                                @Override
                                public void onUserRetrieved(User user) {}
                            });
                        } else {
                            firebaseManager.getAuth().signOut();
                            callback.onError("Please verify your email address before logging in. Check your email and click the verification link.");
                        }
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
    
    public void registerUser(String email, String password, String username, int avatarId, AuthCallback callback) {
        FirebaseAuth auth = firebaseManager.getAuth();
        auth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = firebaseManager.getCurrentUser();
                    if (firebaseUser != null) {
                        String userId = firebaseUser.getUid();
                        User user = new User(userId, email, username, avatarId);
                        
                        userRepository.createUserDocument(user, new UserRepository.UserCallback() {
                            @Override
                            public void onSuccess(String message) {
                                firebaseManager.sendVerificationEmail((emailSuccess, emailException) -> {
                                    if (emailSuccess) {
                                        userRepository.insertUser(user, new UserRepository.UserCallback() {
                                            @Override
                                            public void onSuccess(String message) {
                                                createDefaultCategories(userId);
                                                callback.onSuccess("Registration successful. Please check your email and click the verification link.");
                                            }
                                            
                                            @Override
                                            public void onError(String error) {
                                                callback.onError("Failed to save user locally: " + error);
                                            }
                                            
                                            @Override
                                            public void onUserRetrieved(User user) {}
                                        });
                                    } else {
                                        callback.onError("Registration successful but failed to send verification email: " + 
                                            (emailException != null ? emailException.getMessage() : "Unknown error"));
                                    }
                                });
                            }
                            
                            @Override
                            public void onError(String error) {
                                callback.onError("Failed to create user profile: " + error);
                            }
                            
                            @Override
                            public void onUserRetrieved(User user) {}
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
    
    public void logoutUser(AuthCallback callback) {
        userPreferences.clearUserData();
        
        userRepository.logoutAllUsers(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(String message) {
                firebaseManager.getAuth().signOut();
                if (callback != null) {
                    callback.onSuccess("Logged out successfully");
                }
            }
            
            @Override
            public void onError(String error) {
                firebaseManager.getAuth().signOut();
                if (callback != null) {
                    callback.onError("Logout completed with errors: " + error);
                }
            }
            
            @Override
            public void onUserRetrieved(User user) {}
        });
    }
    
    public void getCurrentUser(UserCallback callback) {
        userRepository.getCurrentUser(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onUserRetrieved(null);
            }
            
            @Override
            public void onUserRetrieved(User user) {
                callback.onUserRetrieved(user);
            }
        });
    }
    
    public boolean isUserLoggedIn() {
        return userPreferences.isLoggedIn() && firebaseManager.isUserLoggedIn();
    }
    
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
    
    public void createDefaultCategories(String userId) {
        String[] defaultCategories = {"Health", "Study", "Work", "Personal"};
        String[] defaultColors = {"#4CAF50", "#2196F3", "#FF9800", "#9C27B0"};
        
        for (int i = 0; i < defaultCategories.length; i++) {
            Category category = new Category(userId, defaultCategories[i], defaultColors[i]);
            categoryRepository.insertCategory(category, new CategoryRepository.CategoryCallback() {
                @Override
                public void onSuccess(String message) {}
                
                @Override
                public void onError(String error) {}
                
                @Override
                public void onCategoryRetrieved(Category category) {}
                
                @Override
                public void onCategoriesRetrieved(List<Category> categories) {}
            });
        }
    }
    
    
    public ValidationResult validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return new ValidationResult(false, "Email is required");
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return new ValidationResult(false, "Please enter a valid email");
        }
        
        return new ValidationResult(true, null);
    }
    
    public ValidationResult validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            return new ValidationResult(false, "Password is required");
        }
        
        if (password.length() < 6) {
            return new ValidationResult(false, "Password must be at least 6 characters");
        }
        
        return new ValidationResult(true, null);
    }
    
    public ValidationResult validateUsername(String username) {
        if (TextUtils.isEmpty(username)) {
            return new ValidationResult(false, "Username is required");
        }
        
        if (username.length() < 3 || username.length() > 20) {
            return new ValidationResult(false, "Username must be 3-20 characters");
        }
        
        return new ValidationResult(true, null);
    }
    
    public ValidationResult validatePasswordMatch(String password, String confirmPassword) {
        if (TextUtils.isEmpty(confirmPassword)) {
            return new ValidationResult(false, "Please confirm your password");
        }
        
        if (!password.equals(confirmPassword)) {
            return new ValidationResult(false, "Passwords do not match");
        }
        
        return new ValidationResult(true, null);
    }
    
    public ValidationResult validateLoginInput(String email, String password) {
        ValidationResult emailResult = validateEmail(email);
        if (!emailResult.isValid()) {
            return emailResult;
        }
        
        ValidationResult passwordResult = validatePassword(password);
        if (!passwordResult.isValid()) {
            return passwordResult;
        }
        
        return new ValidationResult(true, null);
    }
    
    public ValidationResult validateRegistrationInput(String email, String username, String password, String confirmPassword) {
        ValidationResult emailResult = validateEmail(email);
        if (!emailResult.isValid()) {
            return emailResult;
        }
        
        ValidationResult usernameResult = validateUsername(username);
        if (!usernameResult.isValid()) {
            return usernameResult;
        }
        
        ValidationResult passwordResult = validatePassword(password);
        if (!passwordResult.isValid()) {
            return passwordResult;
        }
        
        ValidationResult confirmResult = validatePasswordMatch(password, confirmPassword);
        if (!confirmResult.isValid()) {
            return confirmResult;
        }
        
        return new ValidationResult(true, null);
    }
    
    
    public interface AuthCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public interface UserCallback {
        void onUserRetrieved(User user);
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
