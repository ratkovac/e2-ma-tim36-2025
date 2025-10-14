package com.habitrpg.taskmanager.data.database.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "guild_invites")
public class GuildInvite implements Serializable {
    
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "invite_id")
    private String inviteId;
    
    @ColumnInfo(name = "guild_id")
    private String guildId;
    
    @ColumnInfo(name = "guild_name")
    private String guildName;
    
    @ColumnInfo(name = "from_user_id")
    private String fromUserId;
    
    @ColumnInfo(name = "from_username")
    private String fromUsername;
    
    @ColumnInfo(name = "to_user_id")
    private String toUserId;
    
    @ColumnInfo(name = "to_username")
    private String toUsername;
    
    @ColumnInfo(name = "status")
    private String status; // "pending", "accepted", "declined"
    
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    @ColumnInfo(name = "responded_at")
    private long respondedAt;
    
    public GuildInvite() {
        this.inviteId = "";
        this.guildId = "";
        this.guildName = "";
        this.fromUserId = "";
        this.fromUsername = "";
        this.toUserId = "";
        this.toUsername = "";
        this.status = "pending";
        this.createdAt = System.currentTimeMillis();
        this.respondedAt = 0;
    }
    
    @Ignore
    public GuildInvite(String inviteId, String guildId, String guildName, String fromUserId, 
                       String fromUsername, String toUserId, String toUsername) {
        this.inviteId = inviteId;
        this.guildId = guildId;
        this.guildName = guildName;
        this.fromUserId = fromUserId;
        this.fromUsername = fromUsername;
        this.toUserId = toUserId;
        this.toUsername = toUsername;
        this.status = "pending";
        this.createdAt = System.currentTimeMillis();
        this.respondedAt = 0;
    }
    
    // Getters and Setters
    @NonNull
    public String getInviteId() {
        return inviteId;
    }
    
    public void setInviteId(@NonNull String inviteId) {
        this.inviteId = inviteId;
    }
    
    public String getGuildId() {
        return guildId;
    }
    
    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }
    
    public String getGuildName() {
        return guildName;
    }
    
    public void setGuildName(String guildName) {
        this.guildName = guildName;
    }
    
    public String getFromUserId() {
        return fromUserId;
    }
    
    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }
    
    public String getFromUsername() {
        return fromUsername;
    }
    
    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }
    
    public String getToUserId() {
        return toUserId;
    }
    
    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }
    
    public String getToUsername() {
        return toUsername;
    }
    
    public void setToUsername(String toUsername) {
        this.toUsername = toUsername;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getRespondedAt() {
        return respondedAt;
    }
    
    public void setRespondedAt(long respondedAt) {
        this.respondedAt = respondedAt;
    }
}
