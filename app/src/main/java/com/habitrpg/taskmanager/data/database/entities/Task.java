package com.habitrpg.taskmanager.data.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "user_id")
    @NonNull
    private String userId;

    @ColumnInfo(name = "category_id")
    private int categoryId;

    @NonNull
    private String name;

    private String description;

    @NonNull
    private String difficulty; // 'very_easy', 'easy', 'hard', 'extreme'

    @NonNull
    private String importance; // 'normal', 'important', 'very_important', 'special'

    @ColumnInfo(name = "xp_value")
    private int xpValue;

    @ColumnInfo(name = "is_recurring")
    private boolean isRecurring;

    @ColumnInfo(name = "recurrence_interval")
    private int recurrenceInterval;

    @ColumnInfo(name = "recurrence_unit")
    private String recurrenceUnit; // 'day', 'week'

    @ColumnInfo(name = "start_date")
    private String startDate; // Format: YYYY-MM-DD HH:MM

    @ColumnInfo(name = "end_date")
    private String endDate; // Format: YYYY-MM-DD

    @NonNull
    private String status = "active"; // 'active', 'completed', 'incomplete', 'paused', 'cancelled'

    public Task() {}

    @Ignore
    public Task(@NonNull String userId, int categoryId, @NonNull String name, 
                @NonNull String difficulty, @NonNull String importance, int xpValue) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.name = name;
        this.difficulty = difficulty;
        this.importance = importance;
        this.xpValue = xpValue;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NonNull
    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(@NonNull String difficulty) {
        this.difficulty = difficulty;
    }

    @NonNull
    public String getImportance() {
        return importance;
    }

    public void setImportance(@NonNull String importance) {
        this.importance = importance;
    }

    public int getXpValue() {
        return xpValue;
    }

    public void setXpValue(int xpValue) {
        this.xpValue = xpValue;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public int getRecurrenceInterval() {
        return recurrenceInterval;
    }

    public void setRecurrenceInterval(int recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
    }

    public String getRecurrenceUnit() {
        return recurrenceUnit;
    }

    public void setRecurrenceUnit(String recurrenceUnit) {
        this.recurrenceUnit = recurrenceUnit;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NonNull String status) {
        this.status = status;
    }
}
