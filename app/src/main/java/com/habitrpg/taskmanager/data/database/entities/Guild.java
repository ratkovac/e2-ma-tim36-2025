package com.habitrpg.taskmanager.data.database.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "guilds")
public class Guild implements Serializable {
    
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "guild_id")
    private String guildId;
    
    @ColumnInfo(name = "guild_name")
    private String guildName;
    
    @ColumnInfo(name = "description")
    private String description;
    
    @ColumnInfo(name = "leader_id")
    private String leaderId;
    
    @ColumnInfo(name = "leader_username")
    private String leaderUsername;
    
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    @ColumnInfo(name = "is_active")
    private boolean isActive;
    
    @ColumnInfo(name = "mission_started")
    private boolean missionStarted;
    
    @ColumnInfo(name = "max_members")
    private int maxMembers;
    
    @ColumnInfo(name = "current_members")
    private int currentMembers;
    
    public Guild() {
        this.guildId = "";
        this.guildName = "";
        this.description = "";
        this.leaderId = "";
        this.leaderUsername = "";
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
        this.missionStarted = false;
        this.maxMembers = 10;
        this.currentMembers = 0;
    }
    
    @Ignore
    public Guild(String guildId, String guildName, String description, String leaderId, 
                 String leaderUsername, int maxMembers) {
        this.guildId = guildId;
        this.guildName = guildName;
        this.description = description;
        this.leaderId = leaderId;
        this.leaderUsername = leaderUsername;
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
        this.missionStarted = false;
        this.maxMembers = maxMembers;
        this.currentMembers = 1; // Leader is first member
    }
    
    // Getters and Setters
    @NonNull
    public String getGuildId() {
        return guildId;
    }
    
    public void setGuildId(@NonNull String guildId) {
        this.guildId = guildId;
    }
    
    public String getGuildName() {
        return guildName;
    }
    
    public void setGuildName(String guildName) {
        this.guildName = guildName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getLeaderId() {
        return leaderId;
    }
    
    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }
    
    public String getLeaderUsername() {
        return leaderUsername;
    }
    
    public void setLeaderUsername(String leaderUsername) {
        this.leaderUsername = leaderUsername;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public boolean isMissionStarted() {
        return missionStarted;
    }
    
    public void setMissionStarted(boolean missionStarted) {
        this.missionStarted = missionStarted;
    }
    
    public int getMaxMembers() {
        return maxMembers;
    }
    
    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }
    
    public int getCurrentMembers() {
        return currentMembers;
    }
    
    public void setCurrentMembers(int currentMembers) {
        this.currentMembers = currentMembers;
    }
}
