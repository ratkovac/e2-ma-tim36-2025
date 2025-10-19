package com.habitrpg.taskmanager.data.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "bosses")
public class Boss {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "level")
    private int level;

    @ColumnInfo(name = "max_hp")
    private int maxHp;

    @ColumnInfo(name = "current_hp")
    private int currentHp;

    @ColumnInfo(name = "is_defeated")
    private boolean isDefeated;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "defeated_at")
    private long defeatedAt;

    public Boss() {}

    @Ignore
    public Boss(String userId, int level, int maxHp) {
        this.userId = userId;
        this.level = level;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.isDefeated = false;
        this.createdAt = System.currentTimeMillis();
        this.defeatedAt = 0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(int currentHp) {
        this.currentHp = currentHp;
    }

    public boolean isDefeated() {
        return isDefeated;
    }

    public void setDefeated(boolean defeated) {
        isDefeated = defeated;
        if (defeated) {
            this.defeatedAt = System.currentTimeMillis();
        }
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getDefeatedAt() {
        return defeatedAt;
    }

    public void setDefeatedAt(long defeatedAt) {
        this.defeatedAt = defeatedAt;
    }

    // Helper methods
    public boolean isAlive() {
        return currentHp > 0 && !isDefeated;
    }

    public void takeDamage(int damage) {
        currentHp = Math.max(0, currentHp - damage);
        if (currentHp <= 0) {
            setDefeated(true);
        }
    }

    public float getHpPercentage() {
        if (maxHp <= 0) return 0f;
        return (float) currentHp / maxHp * 100f;
    }
}
