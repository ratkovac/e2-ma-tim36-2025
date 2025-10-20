package com.habitrpg.taskmanager.data.repository;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.dao.GuildDao;
import com.habitrpg.taskmanager.data.database.dao.UserDao;
import com.habitrpg.taskmanager.data.database.entities.Guild;
import com.habitrpg.taskmanager.data.database.entities.GuildInvite;
import com.habitrpg.taskmanager.data.database.entities.GuildMember;
import com.habitrpg.taskmanager.data.database.entities.GuildMessage;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GuildRepository {
    
    private static GuildRepository instance;
    private final GuildDao guildDao;
    private final UserDao userDao;
    private final FirebaseManager firebaseManager;
    private ExecutorService executor;
    
    private GuildRepository(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        this.guildDao = database.guildDao();
        this.userDao = database.userDao();
        this.firebaseManager = FirebaseManager.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public static synchronized GuildRepository getInstance(Context context) {
        if (instance == null) {
            instance = new GuildRepository(context);
        }
        return instance;
    }
    
    private void ensureExecutorActive() {
        if (executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }
    }
    
    // Guild operations
    public void createGuild(String guildName, String description, String leaderId, 
                           String leaderUsername, int maxMembers, GuildCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                // Check if user is already in a guild
                GuildMember existingMember = guildDao.getGuildMemberByUserId(leaderId);
                if (existingMember != null) {
                    callback.onError("You are already a member of a guild");
                    return;
                }
                
                // Create guild
                String guildId = UUID.randomUUID().toString();
                Guild guild = new Guild(guildId, guildName, description, leaderId, leaderUsername, maxMembers);
                guildDao.insertGuild(guild);
                
                // Add leader as first member
                String memberId = UUID.randomUUID().toString();
                User leader = userDao.getUserById(leaderId);
                GuildMember leaderMember = new GuildMember(memberId, guildId, leaderId, 
                    leader != null ? leader.getUsername() : leaderUsername, 
                    leader != null ? leader.getEmail() : "", 
                    leader != null ? String.valueOf(leader.getAvatarId()) : "", true);
                guildDao.insertGuildMember(leaderMember);
                
                // Sync guild with Firebase
                firebaseManager.createGuildDocument(guildId, guildName, description, 
                    leaderId, leaderUsername, maxMembers, 
                    (success, exception) -> {
                        if (!success) {
                            System.out.println("Failed to sync guild to Firebase: " + 
                                (exception != null ? exception.getMessage() : "Unknown error"));
                        }
                    });
                
                // Sync leader member with Firebase
                firebaseManager.addGuildMemberDocument(memberId, guildId, leaderId,
                    leader != null ? leader.getUsername() : leaderUsername,
                    leader != null ? leader.getEmail() : "",
                    leader != null ? leader.getAvatarId() : 0,
                    true,
                    (success, exception) -> {
                        if (!success) {
                            System.out.println("Failed to sync leader member to Firebase: " + 
                                (exception != null ? exception.getMessage() : "Unknown error"));
                        }
                    });
                
                callback.onSuccess("Guild created successfully", guild);
            } catch (Exception e) {
                callback.onError("Failed to create guild: " + e.getMessage());
            }
        });
    }
    
    public void getGuildByLeaderId(String leaderId, GuildCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                Guild guild = guildDao.getGuildByLeaderId(leaderId);
                if (guild != null) {
                    callback.onSuccess("Guild found", guild);
                } else {
                    callback.onError("No guild found");
                }
            } catch (Exception e) {
                callback.onError("Failed to get guild: " + e.getMessage());
            }
        });
    }
    
    public void getGuildById(String guildId, GuildCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                Guild guild = guildDao.getGuildById(guildId);
                if (guild != null) {
                    callback.onSuccess("Guild found", guild);
                } else {
                    callback.onError("Guild not found");
                }
            } catch (Exception e) {
                callback.onError("Failed to get guild: " + e.getMessage());
            }
        });
    }
    
    public void getAllActiveGuilds(GuildListCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                List<Guild> guilds = guildDao.getAllActiveGuilds();
                callback.onSuccess("Guilds retrieved", guilds);
            } catch (Exception e) {
                callback.onError("Failed to get guilds: " + e.getMessage());
            }
        });
    }
    
    public void disbandGuild(String guildId, String leaderId, GuildCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                Guild guild = guildDao.getGuildById(guildId);
                if (guild == null) {
                    callback.onError("Guild not found");
                    return;
                }
                
                if (!guild.getLeaderId().equals(leaderId)) {
                    callback.onError("Only the guild leader can disband the guild");
                    return;
                }
                
                if (guild.isMissionStarted()) {
                    callback.onError("Cannot disband guild while mission is active");
                    return;
                }
                
                // Deactivate guild and remove related data
                guildDao.deactivateGuild(guildId);
                guildDao.deleteAllGuildMembers(guildId);
                guildDao.deleteAllGuildInvites(guildId);
                guildDao.deleteAllGuildMessages(guildId);
                // Hard delete guild row
                guildDao.deleteGuild(guild);
                
                // Sync with Firebase
                firebaseManager.disbandGuildDocument(guildId, 
                    (success, exception) -> {
                        if (!success) {
                            System.out.println("Failed to disband guild in Firebase: " + 
                                (exception != null ? exception.getMessage() : "Unknown error"));
                        }
                    });
                
                callback.onSuccess("Guild disbanded successfully", guild);
            } catch (Exception e) {
                callback.onError("Failed to disband guild: " + e.getMessage());
            }
        });
    }
    
    // Guild member operations
    public void getGuildMembers(String guildId, GuildMemberListCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                List<GuildMember> members = guildDao.getGuildMembersByGuildId(guildId);
                callback.onSuccess("Members retrieved", members);
            } catch (Exception e) {
                callback.onError("Failed to get members: " + e.getMessage());
            }
        });
    }
    
    public void getUserGuild(String userId, GuildCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                GuildMember member = guildDao.getGuildMemberByUserId(userId);
                if (member != null) {
                    Guild guild = guildDao.getGuildById(member.getGuildId());
                    callback.onSuccess("User guild found", guild);
                } else {
                    callback.onError("User is not in any guild");
                }
            } catch (Exception e) {
                callback.onError("Failed to get user guild: " + e.getMessage());
            }
        });
    }
    
    public void leaveGuild(String userId, GuildCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                GuildMember member = guildDao.getGuildMemberByUserId(userId);
                if (member == null) {
                    callback.onError("You are not in any guild");
                    return;
                }
                
                Guild guild = guildDao.getGuildById(member.getGuildId());
                if (guild == null) {
                    callback.onError("Guild not found");
                    return;
                }
                
                if (member.isLeader()) {
                    callback.onError("Guild leader cannot leave the guild");
                    return;
                }
                
                if (guild.isMissionStarted()) {
                    callback.onError("Cannot leave guild while mission is active");
                    return;
                }
                
                // Remove member
                guildDao.deactivateGuildMember(member.getMemberId());
                
                // Update member count
                int newCount = guildDao.getGuildMemberCount(guild.getGuildId());
                guildDao.updateGuildMemberCount(guild.getGuildId(), newCount);
                
                // Sync with Firebase
                firebaseManager.removeGuildMemberDocument(member.getMemberId(), 
                    (success, exception) -> {
                        if (!success) {
                            System.out.println("Failed to remove member from Firebase: " + 
                                (exception != null ? exception.getMessage() : "Unknown error"));
                        }
                    });
                
                callback.onSuccess("Left guild successfully", guild);
            } catch (Exception e) {
                callback.onError("Failed to leave guild: " + e.getMessage());
            }
        });
    }
    
    // Guild invite operations
    public void sendGuildInvite(String guildId, String fromUserId, String toUserId, 
                              String toUsername, GuildInviteCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                Guild guild = guildDao.getGuildById(guildId);
                if (guild == null) {
                    callback.onError("Guild not found");
                    return;
                }
                
                // Check if user is already in a guild
                GuildMember existingMember = guildDao.getGuildMemberByUserId(toUserId);
                if (existingMember != null) {
                    callback.onError("User is already in a guild");
                    return;
                }
                
                // Check if invite already exists
                GuildInvite existingInvite = guildDao.getPendingInviteByGuildAndUser(guildId, toUserId);
                if (existingInvite != null) {
                    callback.onError("Invite already sent to this user");
                    return;
                }
                
                // Create invite
                String inviteId = UUID.randomUUID().toString();
                User fromUser = userDao.getUserById(fromUserId);
                String fromUsername = fromUser != null ? fromUser.getUsername() : "";
                GuildInvite invite = new GuildInvite(inviteId, guildId, guild.getGuildName(), 
                    fromUserId, fromUsername, toUserId, toUsername);
                guildDao.insertGuildInvite(invite);
                
                // Sync with Firebase - THIS IS THE KEY FOR NOTIFICATIONS!
                firebaseManager.sendGuildInviteDocument(inviteId, guildId, guild.getGuildName(),
                    fromUserId, fromUsername, toUserId, toUsername,
                    (success, exception) -> {
                        if (!success) {
                            System.out.println("Failed to sync invite to Firebase: " + 
                                (exception != null ? exception.getMessage() : "Unknown error"));
                        }
                    });
                
                callback.onSuccess("Invite sent successfully", invite);
            } catch (Exception e) {
                callback.onError("Failed to send invite: " + e.getMessage());
            }
        });
    }
    
    public void insertGuildInviteFromFirebase(GuildInvite invite, GuildInviteCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                GuildInvite existingInvite = guildDao.getGuildInviteById(invite.getInviteId());
                if (existingInvite == null) {
                    guildDao.insertGuildInvite(invite);
                    callback.onSuccess("Invite saved from Firebase", invite);
                } else {
                    callback.onError("Invite already exists");
                }
            } catch (Exception e) {
                callback.onError("Failed to save invite: " + e.getMessage());
            }
        });
    }
    
    public void getPendingInvites(String userId, GuildInviteListCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                List<GuildInvite> invites = guildDao.getPendingInvitesByUserId(userId);
                callback.onSuccess("Invites retrieved", invites);
            } catch (Exception e) {
                callback.onError("Failed to get invites: " + e.getMessage());
            }
        });
    }
    
    public void acceptGuildInvite(String inviteId, String userId, GuildCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                GuildInvite invite = guildDao.getGuildInviteById(inviteId);
                if (invite == null) {
                    callback.onError("Invite not found");
                    return;
                }
                
                if (!invite.getToUserId().equals(userId)) {
                    callback.onError("This invite is not for you");
                    return;
                }
                
                if (!invite.getStatus().equals("pending")) {
                    callback.onError("This invite has already been responded to");
                    return;
                }
                
                Guild guild = guildDao.getGuildById(invite.getGuildId());
                if (guild == null) {
                    // Guild not in local DB, try to fetch from Firebase
                    firebaseManager.getGuildDocument(invite.getGuildId(), new FirebaseManager.GuildListener() {
                        @Override
                        public void onGuildRetrieved(Map<String, Object> guildData) {
                            // Move to background thread to insert into DB
                            ensureExecutorActive();
                            executor.execute(() -> {
                                try {
                                    // Create Guild object from Firebase data
                                    Guild firebaseGuild = new Guild(
                                        (String) guildData.get("guildId"),
                                        (String) guildData.get("guildName"),
                                        (String) guildData.get("description"),
                                        (String) guildData.get("leaderId"),
                                        (String) guildData.get("leaderUsername"),
                                        ((Number) guildData.get("maxMembers")).intValue()
                                    );
                                    
                                    // Insert guild into local DB
                                    guildDao.insertGuild(firebaseGuild);
                                    
                                    // Continue with accept process
                                    continueAcceptGuildInvite(inviteId, userId, firebaseGuild, invite, callback);
                                } catch (Exception e) {
                                    callback.onError("Failed to process guild from Firebase: " + e.getMessage());
                                }
                            });
                        }
                        
                        @Override
                        public void onError(String error) {
                            callback.onError("Guild not found: " + error);
                        }
                    });
                    return;
                }
                
                // Guild exists locally, continue with accept process
                continueAcceptGuildInvite(inviteId, userId, guild, invite, callback);
            } catch (Exception e) {
                callback.onError("Failed to accept invite: " + e.getMessage());
            }
        });
    }
    
    private void continueAcceptGuildInvite(String inviteId, String userId, Guild guild, 
                                          GuildInvite invite, GuildCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                // Check if user is already in a guild
                GuildMember existingMember = guildDao.getGuildMemberByUserId(userId);
                if (existingMember != null) {
                    // User is in another guild, they need to leave first
                    callback.onError("You are already in a guild. Leave your current guild first.");
                    return;
                }
                
                // Check guild capacity
                int currentMembers = guildDao.getGuildMemberCount(guild.getGuildId());
                if (currentMembers >= guild.getMaxMembers()) {
                    callback.onError("Guild is full");
                    return;
                }
                
                // Add user to guild
                String memberId = UUID.randomUUID().toString();
                User user = userDao.getUserById(userId);
                GuildMember member = new GuildMember(memberId, guild.getGuildId(), userId, 
                    user != null ? user.getUsername() : invite.getToUsername(), 
                    user != null ? user.getEmail() : "", 
                    user != null ? String.valueOf(user.getAvatarId()) : "", false);
                guildDao.insertGuildMember(member);
                
                // Update invite status
                guildDao.updateInviteStatus(inviteId, "accepted", System.currentTimeMillis());
                
                // Update member count
                guildDao.updateGuildMemberCount(guild.getGuildId(), currentMembers + 1);
                
                // Sync invite status with Firebase
                firebaseManager.updateGuildInviteStatus(inviteId, "accepted", 
                    (success, exception) -> {
                        if (!success) {
                            System.out.println("Failed to update invite status in Firebase: " + 
                                (exception != null ? exception.getMessage() : "Unknown error"));
                        }
                    });
                
                // Sync new member with Firebase
                firebaseManager.addGuildMemberDocument(memberId, guild.getGuildId(), userId,
                    user != null ? user.getUsername() : invite.getToUsername(),
                    user != null ? user.getEmail() : "",
                    user != null ? user.getAvatarId() : 0,
                    false,
                    (success, exception) -> {
                        if (!success) {
                            System.out.println("Failed to sync member to Firebase: " + 
                                (exception != null ? exception.getMessage() : "Unknown error"));
                        }
                    });
                
                callback.onSuccess("Joined guild successfully", guild);
            } catch (Exception e) {
                callback.onError("Failed to accept invite: " + e.getMessage());
            }
        });
    }
    
    public void declineGuildInvite(String inviteId, String userId, GuildCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                GuildInvite invite = guildDao.getGuildInviteById(inviteId);
                if (invite == null) {
                    callback.onError("Invite not found");
                    return;
                }
                
                if (!invite.getToUserId().equals(userId)) {
                    callback.onError("This invite is not for you");
                    return;
                }
                
                if (!invite.getStatus().equals("pending")) {
                    callback.onError("This invite has already been responded to");
                    return;
                }
                
                // Update invite status
                guildDao.updateInviteStatus(inviteId, "declined", System.currentTimeMillis());
                
                // Sync with Firebase
                firebaseManager.updateGuildInviteStatus(inviteId, "declined", 
                    (success, exception) -> {
                        if (!success) {
                            System.out.println("Failed to update invite status in Firebase: " + 
                                (exception != null ? exception.getMessage() : "Unknown error"));
                        }
                    });
                
                callback.onSuccess("Invite declined", null);
            } catch (Exception e) {
                callback.onError("Failed to decline invite: " + e.getMessage());
            }
        });
    }
    
    public GuildMember getGuildMemberByIdSync(String memberId) {
        try {
            return guildDao.getGuildMemberById(memberId);
        } catch (Exception e) {
            android.util.Log.e("GuildRepository", "Error getting guild member by ID: " + e.getMessage());
            return null;
        }
    }
    
    public void insertGuildMemberSync(GuildMember member) {
        try {
            guildDao.insertGuildMember(member);
        } catch (Exception e) {
            android.util.Log.e("GuildRepository", "Error inserting guild member: " + e.getMessage());
        }
    }
    
    public void deactivateGuildMemberSync(String memberId) {
        try {
            guildDao.deactivateGuildMember(memberId);
        } catch (Exception e) {
            android.util.Log.e("GuildRepository", "Error deactivating guild member: " + e.getMessage());
        }
    }
    
    // Callback interfaces
    public interface GuildCallback {
        void onSuccess(String message, Guild guild);
        void onError(String error);
    }
    
    public interface GuildListCallback {
        void onSuccess(String message, List<Guild> guilds);
        void onError(String error);
    }
    
    public interface GuildMemberListCallback {
        void onSuccess(String message, List<GuildMember> members);
        void onError(String error);
    }
    
    public interface GuildInviteCallback {
        void onSuccess(String message, GuildInvite invite);
        void onError(String error);
    }
    
    public interface GuildInviteListCallback {
        void onSuccess(String message, List<GuildInvite> invites);
        void onError(String error);
    }
    
    public void getCurrentGuild(String userId, GuildCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                GuildMember member = guildDao.getGuildMemberByUserId(userId);
                if (member != null) {
                    Guild guild = guildDao.getGuildById(member.getGuildId());
                    callback.onSuccess("Guild loaded successfully", guild);
                } else {
                    callback.onError("User is not in any guild");
                }
            } catch (Exception e) {
                callback.onError("Failed to load guild: " + e.getMessage());
            }
        });
    }
    
    public void getGuildMessages(String guildId, GuildMessageCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                List<GuildMessage> messages = guildDao.getGuildMessages(guildId);
                callback.onSuccess("Messages loaded successfully", messages);
            } catch (Exception e) {
                callback.onError("Failed to load messages: " + e.getMessage());
            }
        });
    }
    
    public void sendGuildMessage(String guildId, String userId, String username, String messageText, GuildMessageCallback callback) {
        ensureExecutorActive();
        executor.execute(() -> {
            try {
                GuildMessage message = new GuildMessage();
                message.setGuildId(guildId);
                message.setUserId(userId);
                message.setUsername(username);
                message.setMessageText(messageText);
                long timestamp = System.currentTimeMillis();
                message.setTimestamp(timestamp);
                message.setSystemMessage(false);
                
                guildDao.insertGuildMessage(message);
                
                // Sync with Firebase
                firebaseManager.sendGuildMessageDocument(
                    message.getMessageId(),
                    guildId,
                    userId,
                    username,
                    messageText,
                    timestamp,
                    (success, exception) -> {
                        if (!success) {
                            System.out.println("Failed to sync message to Firebase: " + 
                                (exception != null ? exception.getMessage() : "Unknown error"));
                        }
                    }
                );
                
                // Return updated message list
                List<GuildMessage> messages = guildDao.getGuildMessages(guildId);
                callback.onSuccess("Message sent successfully", messages);
            } catch (Exception e) {
                callback.onError("Failed to send message: " + e.getMessage());
            }
        });
    }
    
    public void insertGuildMessageSync(GuildMessage message) {
        try {
            guildDao.insertGuildMessage(message);
        } catch (Exception e) {
            android.util.Log.e("GuildRepository", "Error inserting guild message: " + e.getMessage());
        }
    }
    
    public GuildMessage getGuildMessageByIdSync(String messageId) {
        try {
            return guildDao.getGuildMessageById(messageId);
        } catch (Exception e) {
            android.util.Log.e("GuildRepository", "Error getting guild message by ID: " + e.getMessage());
            return null;
        }
    }
    
    public interface GuildMessageCallback {
        void onSuccess(String message, List<GuildMessage> messages);
        void onError(String error);
    }
}
