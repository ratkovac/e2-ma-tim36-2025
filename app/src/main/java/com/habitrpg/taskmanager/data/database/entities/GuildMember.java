package com.habitrpg.taskmanager.data.database.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "guild_members")
public class GuildMember implements Serializable {
    
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "member_id")
    private String memberId;
    
    @ColumnInfo(name = "guild_id")
    private String guildId;
    
    @ColumnInfo(name = "user_id")
    private String userId;
    
    @ColumnInfo(name = "username")
    private String username;
    
    @ColumnInfo(name = "email")
    private String email;
    
    @ColumnInfo(name = "avatar_id")
    private String avatarId;
    
    @ColumnInfo(name = "joined_at")
    private long joinedAt;
    
    @ColumnInfo(name = "is_leader")
    private boolean isLeader;
    
    @ColumnInfo(name = "is_active")
    private boolean isActive;
    
    public GuildMember() {
        this.memberId = "";
        this.guildId = "";
        this.userId = "";
        this.username = "";
        this.email = "";
        this.avatarId = "";
        this.joinedAt = System.currentTimeMillis();
        this.isLeader = false;
        this.isActive = true;
    }
    
    @Ignore
    public GuildMember(String memberId, String guildId, String userId, String username, 
                       String email, String avatarId, boolean isLeader) {
        this.memberId = memberId;
        this.guildId = guildId;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.avatarId = avatarId;
        this.joinedAt = System.currentTimeMillis();
        this.isLeader = isLeader;
        this.isActive = true;
    }
    
    // Getters and Setters
    @NonNull
    public String getMemberId() {
        return memberId;
    }
    
    public void setMemberId(@NonNull String memberId) {
        this.memberId = memberId;
    }
    
    public String getGuildId() {
        return guildId;
    }
    
    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getAvatarId() {
        return avatarId;
    }
    
    public void setAvatarId(String avatarId) {
        this.avatarId = avatarId;
    }
    
    public long getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }
    
    public boolean isLeader() {
        return isLeader;
    }
    
    public void setLeader(boolean leader) {
        isLeader = leader;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
}
