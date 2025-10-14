package com.habitrpg.taskmanager.data.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.CollectionReference;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.database.entities.Category;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class FirebaseManager {
    
    private static FirebaseManager instance;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    private FirebaseManager() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
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
    
    public FirebaseAuth getAuth() {
        return mAuth;
    }
    
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }
    
    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }
    
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
    
    // Friend system methods
    public void searchUsersByUsername(String username, UserSearchListener listener) {
        db.collection("users")
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<User> users = new ArrayList<>();
                    String searchQuery = username.toLowerCase().trim();
                    
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Map<String, Object> data = document.getData();
                        String documentUsername = (String) data.get("username");
                        
                        // Check if username contains the search query (case insensitive)
                        if (documentUsername != null && documentUsername.toLowerCase().contains(searchQuery)) {
                            User user = new User();
                            user.setId(document.getId());
                            user.setUsername(documentUsername);
                            user.setEmail((String) data.get("email"));
                            user.setAvatarId(((Number) data.get("avatarId")).intValue());
                            users.add(user);
                        }
                    }
                    listener.onUsersFound(users);
                } else {
                    listener.onError("Failed to search users: " + task.getException().getMessage());
                }
            });
    }
    
    public void getUserByUsername(String username, UserListener listener) {
        db.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    QueryDocumentSnapshot document = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                    Map<String, Object> data = document.getData();
                    User user = new User();
                    user.setId(document.getId());
                    user.setUsername((String) data.get("username"));
                    user.setEmail((String) data.get("email"));
                    user.setAvatarId(((Number) data.get("avatarId")).intValue());
                    listener.onUserRetrieved(user);
                } else {
                    listener.onUserRetrieved(null);
                }
            })
            .addOnFailureListener(e -> {
                listener.onError("Failed to get user: " + e.getMessage());
            });
    }
    
    public interface OnCompleteListener {
        void onComplete(boolean success, Exception exception);
    }
    
    public interface OnUserRetrievedListener {
        void onUserRetrieved(Map<String, Object> userData);
    }
    
    public interface UserSearchListener {
        void onUsersFound(List<User> users);
        void onError(String error);
    }
    
    public interface UserListener {
        void onUserRetrieved(User user);
        void onError(String error);
    }
}
