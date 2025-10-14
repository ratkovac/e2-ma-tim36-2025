package com.habitrpg.taskmanager.data.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_statistics")
public class UserStatistics {
    @PrimaryKey
    @NonNull
    private String userId;
    
    private int totalDaysActive;
    private int totalTasksCreated;
    private int totalTasksCompleted;
    private int totalTasksPending;
    private int totalTasksCancelled;
    private int longestStreak;
    private int currentStreak;
    private int totalXP;
    private int totalSpecialMissionsStarted;
    private int totalSpecialMissionsCompleted;
    private long lastActiveDate;
    private long firstActiveDate;
    
    public UserStatistics() {}
    
    @Ignore
    public UserStatistics(String userId) {
        this.userId = userId;
        this.totalDaysActive = 0;
        this.totalTasksCreated = 0;
        this.totalTasksCompleted = 0;
        this.totalTasksPending = 0;
        this.totalTasksCancelled = 0;
        this.longestStreak = 0;
        this.currentStreak = 0;
        this.totalXP = 0;
        this.totalSpecialMissionsStarted = 0;
        this.totalSpecialMissionsCompleted = 0;
        this.lastActiveDate = System.currentTimeMillis();
        this.firstActiveDate = System.currentTimeMillis();
    }
    
    // Getters and Setters
    @NonNull
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }
    
    public int getTotalDaysActive() {
        return totalDaysActive;
    }
    
    public void setTotalDaysActive(int totalDaysActive) {
        this.totalDaysActive = totalDaysActive;
    }
    
    public int getTotalTasksCreated() {
        return totalTasksCreated;
    }
    
    public void setTotalTasksCreated(int totalTasksCreated) {
        this.totalTasksCreated = totalTasksCreated;
    }
    
    public int getTotalTasksCompleted() {
        return totalTasksCompleted;
    }
    
    public void setTotalTasksCompleted(int totalTasksCompleted) {
        this.totalTasksCompleted = totalTasksCompleted;
    }
    
    public int getTotalTasksPending() {
        return totalTasksPending;
    }
    
    public void setTotalTasksPending(int totalTasksPending) {
        this.totalTasksPending = totalTasksPending;
    }
    
    public int getTotalTasksCancelled() {
        return totalTasksCancelled;
    }
    
    public void setTotalTasksCancelled(int totalTasksCancelled) {
        this.totalTasksCancelled = totalTasksCancelled;
    }
    
    public int getLongestStreak() {
        return longestStreak;
    }
    
    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }
    
    public int getCurrentStreak() {
        return currentStreak;
    }
    
    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }
    
    public int getTotalXP() {
        return totalXP;
    }
    
    public void setTotalXP(int totalXP) {
        this.totalXP = totalXP;
    }
    
    public int getTotalSpecialMissionsStarted() {
        return totalSpecialMissionsStarted;
    }
    
    public void setTotalSpecialMissionsStarted(int totalSpecialMissionsStarted) {
        this.totalSpecialMissionsStarted = totalSpecialMissionsStarted;
    }
    
    public int getTotalSpecialMissionsCompleted() {
        return totalSpecialMissionsCompleted;
    }
    
    public void setTotalSpecialMissionsCompleted(int totalSpecialMissionsCompleted) {
        this.totalSpecialMissionsCompleted = totalSpecialMissionsCompleted;
    }
    
    public long getLastActiveDate() {
        return lastActiveDate;
    }
    
    public void setLastActiveDate(long lastActiveDate) {
        this.lastActiveDate = lastActiveDate;
    }
    
    public long getFirstActiveDate() {
        return firstActiveDate;
    }
    
    public void setFirstActiveDate(long firstActiveDate) {
        this.firstActiveDate = firstActiveDate;
    }
}
