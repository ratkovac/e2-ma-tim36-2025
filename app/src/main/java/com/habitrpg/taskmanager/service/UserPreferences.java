package com.habitrpg.taskmanager.service;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserPreferences {
    private static final String PREF_NAME = "user_preferences";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    
    private static UserPreferences instance;
    private SharedPreferences preferences;
    private FirebaseAuth firebaseAuth;
    
    private UserPreferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        firebaseAuth = FirebaseAuth.getInstance();
    }
    
    public static synchronized UserPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new UserPreferences(context.getApplicationContext());
        }
        return instance;
    }
    
    public String getCurrentUserId() {
        // First try to get from Firebase
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            return firebaseUser.getUid();
        }
        
        // Fallback to SharedPreferences
        return preferences.getString(KEY_CURRENT_USER_ID, null);
    }
    
    public void setCurrentUserId(String userId) {
        preferences.edit().putString(KEY_CURRENT_USER_ID, userId).apply();
    }
    
    public void clearCurrentUserId() {
        preferences.edit().remove(KEY_CURRENT_USER_ID).apply();
    }
}
