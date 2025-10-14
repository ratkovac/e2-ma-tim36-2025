package com.habitrpg.taskmanager.data.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import java.io.Serializable;

@Entity(tableName = "friend_requests")
public class FriendRequest implements Serializable {
    @PrimaryKey
    @NonNull
    private String id;
    
    @ColumnInfo(name = "from_user_id")
    private String fromUserId;
    
    @ColumnInfo(name = "to_user_id")
    private String toUserId;
    
    @ColumnInfo(name = "from_username")
    private String fromUsername;
    
    @ColumnInfo(name = "from_email")
    private String fromEmail;
    
    @ColumnInfo(name = "from_avatar_id")
    private int fromAvatarId;
    
    private String status;
    
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    public FriendRequest() {}
    
    @Ignore
    public FriendRequest(@NonNull String id, String fromUserId, String toUserId, 
                       String fromUsername, String fromEmail, int fromAvatarId, 
                       String status, long createdAt) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.fromUsername = fromUsername;
        this.fromEmail = fromEmail;
        this.fromAvatarId = fromAvatarId;
        this.status = status;
        this.createdAt = createdAt;
    }
    
    @NonNull
    public String getId() {
        return id;
    }
    
    public void setId(@NonNull String id) {
        this.id = id;
    }
    
    public String getFromUserId() {
        return fromUserId;
    }
    
    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }
    
    public String getToUserId() {
        return toUserId;
    }
    
    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }
    
    public String getFromUsername() {
        return fromUsername;
    }
    
    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }
    
    public String getFromEmail() {
        return fromEmail;
    }
    
    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }
    
    public int getFromAvatarId() {
        return fromAvatarId;
    }
    
    public void setFromAvatarId(int fromAvatarId) {
        this.fromAvatarId = fromAvatarId;
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
}
