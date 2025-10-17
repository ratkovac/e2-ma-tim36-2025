package com.habitrpg.taskmanager.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPreferences {
    
    private static final String PREF_NAME = "HabitRPGPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_THEME_PREFERENCE = "theme_preference";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";
    private static final String KEY_CURRENT_STAGE_START_TIME = "current_stage_start_time";
    private static final String KEY_PREVIOUS_STAGE_START_TIME = "previous_stage_start_time";
    
    private static UserPreferences instance;
    private SharedPreferences sharedPreferences;
    
    private UserPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized UserPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new UserPreferences(context.getApplicationContext());
        }
        return instance;
    }
    
    // Login state management
    public void setLoggedIn(boolean isLoggedIn) {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            .apply();
    }
    
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    // Current user management
    public void setCurrentUserId(String userId) {
        sharedPreferences.edit()
            .putString(KEY_CURRENT_USER_ID, userId)
            .apply();
    }
    
    public String getCurrentUserId() {
        return sharedPreferences.getString(KEY_CURRENT_USER_ID, null);
    }
    
    // Theme preference
    public void setThemePreference(String theme) {
        sharedPreferences.edit()
            .putString(KEY_THEME_PREFERENCE, theme)
            .apply();
    }
    
    public String getThemePreference() {
        return sharedPreferences.getString(KEY_THEME_PREFERENCE, "default");
    }
    
    // Notification settings
    public void setNotificationEnabled(boolean enabled) {
        sharedPreferences.edit()
            .putBoolean(KEY_NOTIFICATION_ENABLED, enabled)
            .apply();
    }
    
    public boolean isNotificationEnabled() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATION_ENABLED, true);
    }
    
    // First launch detection
    public void setFirstLaunch(boolean isFirstLaunch) {
        sharedPreferences.edit()
            .putBoolean(KEY_FIRST_LAUNCH, isFirstLaunch)
            .apply();
    }
    
    public boolean isFirstLaunch() {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true);
    }
    
    // Sync time tracking
    public void setLastSyncTime(long timestamp) {
        sharedPreferences.edit()
            .putLong(KEY_LAST_SYNC_TIME, timestamp)
            .apply();
    }
    
    public long getLastSyncTime() {
        return sharedPreferences.getLong(KEY_LAST_SYNC_TIME, 0);
    }
    
    // Stage tracking methods
    public void setCurrentStageStartTime(long timestamp) {
        System.out.println("DEBUG: Setting current stage start time to: " + timestamp);
        sharedPreferences.edit()
            .putLong(KEY_CURRENT_STAGE_START_TIME, timestamp)
            .apply();
    }
    
    public long getCurrentStageStartTime() {
        long value = sharedPreferences.getLong(KEY_CURRENT_STAGE_START_TIME, 0);
        System.out.println("DEBUG: Getting current stage start time: " + value);
        return value;
    }
    
    public void setPreviousStageStartTime(long timestamp) {
        System.out.println("DEBUG: Setting previous stage start time to: " + timestamp);
        sharedPreferences.edit()
            .putLong(KEY_PREVIOUS_STAGE_START_TIME, timestamp)
            .apply();
    }
    
    public long getPreviousStageStartTime() {
        long value = sharedPreferences.getLong(KEY_PREVIOUS_STAGE_START_TIME, 0);
        System.out.println("DEBUG: Getting previous stage start time: " + value);
        return value;
    }
    
    // Clear all preferences (logout)
    public void clearAllPreferences() {
        sharedPreferences.edit().clear().apply();
    }
    
    // Clear user-specific data but keep app settings
    public void clearUserData() {
        sharedPreferences.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_CURRENT_USER_ID)
            .remove(KEY_LAST_SYNC_TIME)
            .remove(KEY_CURRENT_STAGE_START_TIME)
            .remove(KEY_PREVIOUS_STAGE_START_TIME)
            .apply();
    }
}
