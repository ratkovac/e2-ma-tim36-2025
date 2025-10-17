package com.habitrpg.taskmanager.data.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.habitrpg.taskmanager.data.database.entities.Guild;
import com.habitrpg.taskmanager.data.database.entities.GuildInvite;
import com.habitrpg.taskmanager.data.database.entities.GuildMember;
import com.habitrpg.taskmanager.data.database.entities.GuildMessage;

import java.util.List;

@Dao
public interface GuildDao {
    
    // Guild queries
    @Query("SELECT * FROM guilds WHERE guild_id = :guildId")
    Guild getGuildById(String guildId);
    
    @Query("SELECT * FROM guilds WHERE leader_id = :leaderId AND is_active = 1")
    Guild getGuildByLeaderId(String leaderId);
    
    @Query("SELECT * FROM guilds WHERE is_active = 1 ORDER BY created_at DESC")
    List<Guild> getAllActiveGuilds();
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGuild(Guild guild);
    
    @Update
    void updateGuild(Guild guild);
    
    @Delete
    void deleteGuild(Guild guild);
    
    @Query("UPDATE guilds SET is_active = 0 WHERE guild_id = :guildId")
    void deactivateGuild(String guildId);
    
    @Query("UPDATE guilds SET current_members = :memberCount WHERE guild_id = :guildId")
    void updateGuildMemberCount(String guildId, int memberCount);
    
    @Query("UPDATE guilds SET mission_started = :missionStarted WHERE guild_id = :guildId")
    void updateGuildMissionStatus(String guildId, boolean missionStarted);
    
    // GuildMember queries
    @Query("SELECT * FROM guild_members WHERE member_id = :memberId")
    GuildMember getGuildMemberById(String memberId);
    
    @Query("SELECT * FROM guild_members WHERE guild_id = :guildId AND is_active = 1")
    List<GuildMember> getGuildMembersByGuildId(String guildId);
    
    @Query("SELECT * FROM guild_members WHERE user_id = :userId AND is_active = 1")
    GuildMember getGuildMemberByUserId(String userId);
    
    @Query("SELECT * FROM guild_members WHERE guild_id = :guildId AND user_id = :userId")
    GuildMember getGuildMemberByGuildAndUser(String guildId, String userId);
    
    @Query("SELECT COUNT(*) FROM guild_members WHERE guild_id = :guildId AND is_active = 1")
    int getGuildMemberCount(String guildId);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGuildMember(GuildMember guildMember);
    
    @Update
    void updateGuildMember(GuildMember guildMember);
    
    @Delete
    void deleteGuildMember(GuildMember guildMember);
    
    @Query("UPDATE guild_members SET is_active = 0 WHERE member_id = :memberId")
    void deactivateGuildMember(String memberId);
    
    @Query("DELETE FROM guild_members WHERE guild_id = :guildId")
    void deleteAllGuildMembers(String guildId);
    
    // GuildInvite queries
    @Query("SELECT * FROM guild_invites WHERE invite_id = :inviteId")
    GuildInvite getGuildInviteById(String inviteId);
    
    @Query("SELECT * FROM guild_invites WHERE to_user_id = :userId AND status = 'pending'")
    List<GuildInvite> getPendingInvitesByUserId(String userId);
    
    @Query("SELECT * FROM guild_invites WHERE from_user_id = :userId AND status = 'pending'")
    List<GuildInvite> getSentInvitesByUserId(String userId);
    
    @Query("SELECT * FROM guild_invites WHERE guild_id = :guildId AND status = 'pending'")
    List<GuildInvite> getPendingInvitesByGuildId(String guildId);
    
    @Query("SELECT * FROM guild_invites WHERE guild_id = :guildId AND to_user_id = :userId AND status = 'pending'")
    GuildInvite getPendingInviteByGuildAndUser(String guildId, String userId);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGuildInvite(GuildInvite guildInvite);
    
    @Update
    void updateGuildInvite(GuildInvite guildInvite);
    
    @Delete
    void deleteGuildInvite(GuildInvite guildInvite);
    
    @Query("UPDATE guild_invites SET status = :status, responded_at = :respondedAt WHERE invite_id = :inviteId")
    void updateInviteStatus(String inviteId, String status, long respondedAt);
    
    @Query("DELETE FROM guild_invites WHERE guild_id = :guildId")
    void deleteAllGuildInvites(String guildId);
    
    @Query("DELETE FROM guild_invites WHERE to_user_id = :userId")
    void deleteAllInvitesForUser(String userId);
    
    // Guild Message operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGuildMessage(GuildMessage message);
    
    @Query("SELECT * FROM guild_messages WHERE guild_id = :guildId ORDER BY timestamp ASC")
    List<GuildMessage> getGuildMessages(String guildId);
    
    @Query("SELECT * FROM guild_messages WHERE guild_id = :guildId ORDER BY timestamp DESC LIMIT :limit")
    List<GuildMessage> getRecentGuildMessages(String guildId, int limit);
    
    @Query("DELETE FROM guild_messages WHERE guild_id = :guildId")
    void deleteAllGuildMessages(String guildId);
}

