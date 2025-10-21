package com.habitrpg.taskmanager.service;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.entities.Guild;
import com.habitrpg.taskmanager.data.database.entities.GuildInvite;
import com.habitrpg.taskmanager.data.database.entities.GuildMember;
import com.habitrpg.taskmanager.data.database.entities.GuildMessage;
import com.habitrpg.taskmanager.data.repository.GuildRepository;
import com.habitrpg.taskmanager.data.repository.UserRepository;
import com.habitrpg.taskmanager.service.UserPreferences;

import java.util.List;

public class GuildService {
    
    private static GuildService instance;
    private final GuildRepository guildRepository;
    private final UserRepository userRepository;
    private final UserPreferences userPreferences;
    private final NotificationService notificationService;
    
    private GuildService(Context context) {
        this.guildRepository = GuildRepository.getInstance(context);
        this.userRepository = UserRepository.getInstance(context);
        this.userPreferences = UserPreferences.getInstance(context);
        this.notificationService = NotificationService.getInstance(context);
    }
    
    public static synchronized GuildService getInstance(Context context) {
        if (instance == null) {
            instance = new GuildService(context);
        }
        return instance;
    }
    
    // Guild management
    public void createGuild(String guildName, String description, int maxMembers, GuildCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError("Failed to get user info: " + error);
            }
            
            @Override
            public void onUserRetrieved(com.habitrpg.taskmanager.data.database.entities.User user) {
                guildRepository.createGuild(guildName, description, currentUserId, 
                    user.getUsername(), maxMembers, new GuildRepository.GuildCallback() {
                        @Override
                        public void onSuccess(String message, Guild guild) {
                            callback.onSuccess(message, guild);
                        }
                        
                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                    });
            }
        });
    }
    
    public void getCurrentUserGuild(GuildCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        guildRepository.getUserGuild(currentUserId, new GuildRepository.GuildCallback() {
            @Override
            public void onSuccess(String message, Guild guild) {
                callback.onSuccess(message, guild);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void getGuildById(String guildId, GuildCallback callback) {
        guildRepository.getGuildById(guildId, new GuildRepository.GuildCallback() {
            @Override
            public void onSuccess(String message, Guild guild) {
                callback.onSuccess(message, guild);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void getAllActiveGuilds(GuildListCallback callback) {
        guildRepository.getAllActiveGuilds(new GuildRepository.GuildListCallback() {
            @Override
            public void onSuccess(String message, List<Guild> guilds) {
                callback.onSuccess(message, guilds);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void disbandGuild(String guildId, GuildCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        guildRepository.disbandGuild(guildId, currentUserId, new GuildRepository.GuildCallback() {
            @Override
            public void onSuccess(String message, Guild guild) {
                callback.onSuccess(message, guild);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    // Guild member management
    public void getGuildMembers(String guildId, GuildMemberListCallback callback) {
        guildRepository.getGuildMembers(guildId, new GuildRepository.GuildMemberListCallback() {
            @Override
            public void onSuccess(String message, List<GuildMember> members) {
                callback.onSuccess(message, members);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void leaveGuild(GuildCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        guildRepository.leaveGuild(currentUserId, new GuildRepository.GuildCallback() {
            @Override
            public void onSuccess(String message, Guild guild) {
                callback.onSuccess(message, guild);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    // Guild invite management
    public void sendGuildInvite(String guildId, String friendUserId, GuildCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        // Get current user info for notification
        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(String message) {
                // Not used here
            }
            
            @Override
            public void onError(String error) {
                callback.onError("Failed to get user info: " + error);
            }
            
            @Override
            public void onUserRetrieved(com.habitrpg.taskmanager.data.database.entities.User user) {
                // Get guild info
                guildRepository.getGuildById(guildId, new GuildRepository.GuildCallback() {
                    @Override
                    public void onSuccess(String message, Guild guild) {
                        // Send invite to repository
                        guildRepository.sendGuildInvite(guildId, currentUserId, friendUserId, user.getUsername(), 
                            new GuildRepository.GuildInviteCallback() {
                                @Override
                                public void onSuccess(String message, GuildInvite invite) {
                                    // Send notification to friend
                                    notificationService.showGuildInviteNotification(invite);
                                    
                                    callback.onSuccess(message, guild);
                                }
                                
                                @Override
                                public void onError(String error) {
                                    callback.onError(error);
                                }
                            });
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError("Failed to get guild info: " + error);
                    }
                });
            }
        });
    }
    
    public void getPendingInvites(GuildInviteListCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        guildRepository.getPendingInvites(currentUserId, new GuildRepository.GuildInviteListCallback() {
            @Override
            public void onSuccess(String message, List<GuildInvite> invites) {
                callback.onSuccess(message, invites);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void acceptGuildInvite(String inviteId, GuildCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        guildRepository.acceptGuildInvite(inviteId, currentUserId, new GuildRepository.GuildCallback() {
            @Override
            public void onSuccess(String message, Guild guild) {
                callback.onSuccess(message, guild);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void declineGuildInvite(String inviteId, GuildCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        guildRepository.declineGuildInvite(inviteId, currentUserId, new GuildRepository.GuildCallback() {
            @Override
            public void onSuccess(String message, Guild guild) {
                callback.onSuccess(message, guild);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    // Utility methods
    public boolean isUserInGuild() {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }
        
        // This is a synchronous check - in real implementation you might want to use a different approach
        // For now, we'll assume the UI will handle this through callbacks
        return false;
    }
    
    public boolean canUserLeaveGuild() {
        // This would need to check if mission is started
        // For now, we'll let the repository handle this logic
        return true;
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
    
    public void getCurrentGuild(GuildCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        guildRepository.getCurrentGuild(currentUserId, new GuildRepository.GuildCallback() {
            @Override
            public void onSuccess(String message, Guild guild) {
                callback.onSuccess(message, guild);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void getGuildMessages(String guildId, GuildMessageCallback callback) {
        guildRepository.getGuildMessages(guildId, new GuildRepository.GuildMessageCallback() {
            @Override
            public void onSuccess(String message, List<GuildMessage> messages) {
                callback.onSuccess(message, messages);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void sendGuildMessage(String guildId, String messageText, GuildMessageCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        // Get current user info
        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError("Failed to get user info: " + error);
            }
            
            @Override
            public void onUserRetrieved(com.habitrpg.taskmanager.data.database.entities.User user) {
                if (user != null) {
                    // Show local echo notification so demo works on one device
                    try {
                        notificationService.showGuildMessageNotification(guildId, null, user.getUsername(), messageText);
                    } catch (Exception ignored) {}
                    
                    guildRepository.sendGuildMessage(guildId, currentUserId, user.getUsername(), messageText, new GuildRepository.GuildMessageCallback() {
                        @Override
                        public void onSuccess(String message, List<GuildMessage> messages) {
                            callback.onSuccess(message, messages);
                        }
                        
                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                    });
                } else {
                    callback.onError("User not found");
                }
            }
        });
    }
    
    public interface GuildMessageCallback {
        void onSuccess(String message, List<GuildMessage> messages);
        void onError(String error);
    }
}
