package com.habitrpg.taskmanager.service;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.entities.GuildMember;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;
import com.habitrpg.taskmanager.data.repository.GuildRepository;
import com.google.firebase.firestore.ListenerRegistration;

public class GuildMembersListenerService {
    
    private static FirebaseManager firebaseManager;
    private static UserPreferences userPreferences;
    private static GuildRepository guildRepository;
    private static Context appContext;
    private static ListenerRegistration listenerRegistration;
    private static String currentGuildId;
    private static GuildMembersUpdateListener updateListener;
    
    public interface GuildMembersUpdateListener {
        void onMemberAdded();
    }
    
    public static void startListening(Context context, String guildId, GuildMembersUpdateListener listener) {
        if (guildId == null || guildId.isEmpty()) {
            return;
        }
        
        stopListening();
        
        appContext = context.getApplicationContext();
        firebaseManager = FirebaseManager.getInstance();
        userPreferences = UserPreferences.getInstance(appContext);
        guildRepository = GuildRepository.getInstance(appContext);
        currentGuildId = guildId;
        updateListener = listener;
        
        listenerRegistration = firebaseManager.listenForGuildMembers(guildId, 
            new FirebaseManager.GuildMembersListener() {
                @Override
                public void onMemberAdded(String memberId, String guildId, String userId,
                                         String username, String email, int avatarId, boolean isLeader) {
                    
                    if (guildRepository == null || appContext == null) {
                        System.out.println("GuildMembersListenerService: Services are null, ignoring member");
                        return;
                    }
                    
                    GuildMember member = new GuildMember(
                        memberId, guildId, userId, username, email,
                        String.valueOf(avatarId), isLeader
                    );
                    
                    new Thread(() -> {
                        try {
                            if (guildRepository == null) {
                                return;
                            }
                            GuildMember existingMember = guildRepository.getGuildMemberByIdSync(memberId);
                            if (existingMember == null) {
                                guildRepository.insertGuildMemberSync(member);
                                System.out.println("Guild member synced from Firebase: " + username);
                                
                                if (updateListener != null) {
                                    updateListener.onMemberAdded();
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Failed to sync guild member: " + e.getMessage());
                        }
                    }).start();
                }
                
                @Override
                public void onError(String error) {
                    System.out.println("Error listening for guild members: " + error);
                }
            });
    }
    
    public static void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        appContext = null;
        firebaseManager = null;
        userPreferences = null;
        guildRepository = null;
        currentGuildId = null;
        updateListener = null;
    }
}

