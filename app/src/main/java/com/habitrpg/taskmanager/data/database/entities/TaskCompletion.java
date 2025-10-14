package com.habitrpg.taskmanager.data.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "task_completions")
public class TaskCompletion {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "task_id")
    private int taskId;

    @ColumnInfo(name = "completed_date")
    private String completedDate; // Format: YYYY-MM-DD

    @ColumnInfo(name = "xp_earned")
    private int xpEarned;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public TaskCompletion() {}

    @Ignore
    public TaskCompletion(int taskId, String completedDate, int xpEarned) {
        this.taskId = taskId;
        this.completedDate = completedDate;
        this.xpEarned = xpEarned;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(String completedDate) {
        this.completedDate = completedDate;
    }

    public int getXpEarned() {
        return xpEarned;
    }

    public void setXpEarned(int xpEarned) {
        this.xpEarned = xpEarned;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
