package com.habitrpg.taskmanager.data.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import java.util.UUID;

@Entity(tableName = "guild_messages")
public class GuildMessage {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "message_id")
    private String messageId;

    @ColumnInfo(name = "guild_id")
    private String guildId;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "username")
    private String username;

    @ColumnInfo(name = "message_text")
    private String messageText;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "is_system_message")
    private boolean isSystemMessage;

    public GuildMessage() {
        this.messageId = UUID.randomUUID().toString();
        this.guildId = "";
        this.userId = "";
        this.username = "";
        this.messageText = "";
        this.timestamp = System.currentTimeMillis();
        this.isSystemMessage = false;
    }

    @Ignore
    public GuildMessage(@NonNull String messageId, String guildId, String userId, String username, String messageText, long timestamp, boolean isSystemMessage) {
        this.messageId = messageId;
        this.guildId = guildId;
        this.userId = userId;
        this.username = username;
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.isSystemMessage = isSystemMessage;
    }

    @NonNull
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(@NonNull String messageId) {
        this.messageId = messageId;
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

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSystemMessage() {
        return isSystemMessage;
    }

    public void setSystemMessage(boolean systemMessage) {
        isSystemMessage = systemMessage;
    }
}

