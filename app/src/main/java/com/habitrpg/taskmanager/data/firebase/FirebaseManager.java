package com.habitrpg.taskmanager.data.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.CollectionReference;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.data.database.entities.Category;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FirebaseManager {
    
    private static FirebaseManager instance;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    private FirebaseManager() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Configure Firestore settings
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
            .build();
        db.setFirestoreSettings(settings);
    }
    
    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }
    
    // Authentication methods
    public FirebaseAuth getAuth() {
        return mAuth;
    }
    
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }
    
    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }
    
    // User management
    public void createUserDocument(User user, OnCompleteListener listener) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("username", user.getUsername());
        userData.put("avatarId", user.getAvatarId());
        userData.put("level", user.getLevel());
        userData.put("title", user.getTitle());
        userData.put("powerPoints", user.getPowerPoints());
        userData.put("experiencePoints", user.getExperiencePoints());
        userData.put("coins", user.getCoins());
        userData.put("createdAt", System.currentTimeMillis());
        
        db.collection("users")
            .document(user.getId())
            .set(userData)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task.isSuccessful(), task.getException());
                }
            });
    }
    
    public void updateUserDocument(User user, OnCompleteListener listener) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", user.getUsername());
        userData.put("avatarId", user.getAvatarId());
        userData.put("level", user.getLevel());
        userData.put("title", user.getTitle());
        userData.put("powerPoints", user.getPowerPoints());
        userData.put("experiencePoints", user.getExperiencePoints());
        userData.put("coins", user.getCoins());
        
        db.collection("users")
            .document(user.getId())
            .update(userData)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task.isSuccessful(), task.getException());
                }
            });
    }
    
    public void getUserDocument(String userId, OnUserRetrievedListener listener) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Object> data = documentSnapshot.getData();
                    if (data != null && listener != null) {
                        listener.onUserRetrieved(data);
                    }
                } else if (listener != null) {
                    listener.onUserRetrieved(null);
                }
            })
            .addOnFailureListener(e -> {
                if (listener != null) {
                    listener.onUserRetrieved(null);
                }
            });
    }
    
    // Task management
    public void saveTask(Task task, OnCompleteListener listener) {
        Map<String, Object> taskData = createTaskMap(task);
        
        db.collection("tasks")
            .add(taskData)
            .addOnCompleteListener(taskResult -> {
                if (listener != null) {
                    listener.onComplete(taskResult.isSuccessful(), taskResult.getException());
                }
            });
    }
    
    public void updateTask(String taskId, Task task, OnCompleteListener listener) {
        Map<String, Object> taskData = createTaskMap(task);
        
        db.collection("tasks")
            .document(taskId)
            .update(taskData)
            .addOnCompleteListener(taskResult -> {
                if (listener != null) {
                    listener.onComplete(taskResult.isSuccessful(), taskResult.getException());
                }
            });
    }
    
    // Category management
    public void saveCategory(Category category, OnCompleteListener listener) {
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("userId", category.getUserId());
        categoryData.put("name", category.getName());
        categoryData.put("color", category.getColor());
        categoryData.put("createdAt", category.getCreatedAt());
        
        db.collection("categories")
            .add(categoryData)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task.isSuccessful(), task.getException());
                }
            });
    }
    
    // Helper methods
    private Map<String, Object> createTaskMap(Task task) {
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("userId", task.getUserId());
        taskData.put("categoryId", task.getCategoryId());
        taskData.put("name", task.getName());
        taskData.put("description", task.getDescription());
        taskData.put("difficulty", task.getDifficulty());
        taskData.put("importance", task.getImportance());
        taskData.put("xpValue", task.getXpValue());
        taskData.put("isRecurring", task.isRecurring());
        taskData.put("recurrenceInterval", task.getRecurrenceInterval());
        taskData.put("recurrenceUnit", task.getRecurrenceUnit());
        taskData.put("startDate", task.getStartDate());
        taskData.put("endDate", task.getEndDate());
        taskData.put("executionTime", task.getExecutionTime());
        taskData.put("status", task.getStatus());
        taskData.put("createdAt", task.getCreatedAt());
        return taskData;
    }
    
    public void sendVerificationEmail(OnCompleteListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (listener != null) {
                        listener.onComplete(task.isSuccessful(), task.getException());
                    }
                });
        } else if (listener != null) {
            listener.onComplete(false, new Exception("No user logged in"));
        }
    }
    
    // Callback interfaces
    public interface OnCompleteListener {
        void onComplete(boolean success, Exception exception);
    }
    
    public interface OnUserRetrievedListener {
        void onUserRetrieved(Map<String, Object> userData);
    }
}
