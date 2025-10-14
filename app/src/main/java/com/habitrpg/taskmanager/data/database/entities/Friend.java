package com.habitrpg.taskmanager.data.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "friends")
public class Friend {
    @PrimaryKey
    @NonNull
    private String id;
    
    @ColumnInfo(name = "user_id")
    private String userId;
    
    @ColumnInfo(name = "friend_user_id")
    private String friendUserId;
    
    @ColumnInfo(name = "friend_username")
    private String friendUsername;
    
    @ColumnInfo(name = "friend_email")
    private String friendEmail;
    
    @ColumnInfo(name = "friend_avatar_id")
    private int friendAvatarId;
    
    private String status;
    
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    public Friend() {}
    
    @Ignore
    public Friend(@NonNull String id, String userId, String friendUserId, String friendUsername, 
                 String friendEmail, int friendAvatarId, String status, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.friendUserId = friendUserId;
        this.friendUsername = friendUsername;
        this.friendEmail = friendEmail;
        this.friendAvatarId = friendAvatarId;
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
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getFriendUserId() {
        return friendUserId;
    }
    
    public void setFriendUserId(String friendUserId) {
        this.friendUserId = friendUserId;
    }
    
    public String getFriendUsername() {
        return friendUsername;
    }
    
    public void setFriendUsername(String friendUsername) {
        this.friendUsername = friendUsername;
    }
    
    public String getFriendEmail() {
        return friendEmail;
    }
    
    public void setFriendEmail(String friendEmail) {
        this.friendEmail = friendEmail;
    }
    
    public int getFriendAvatarId() {
        return friendAvatarId;
    }
    
    public void setFriendAvatarId(int friendAvatarId) {
        this.friendAvatarId = friendAvatarId;
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
