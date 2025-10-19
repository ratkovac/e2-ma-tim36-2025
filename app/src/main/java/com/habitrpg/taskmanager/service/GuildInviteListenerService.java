package com.habitrpg.taskmanager.service;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.entities.GuildInvite;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;
import com.habitrpg.taskmanager.data.repository.GuildRepository;
import com.google.firebase.firestore.ListenerRegistration;

public class GuildInviteListenerService {
    
    private static FirebaseManager firebaseManager;
    private static UserPreferences userPreferences;
    private static GuildRepository guildRepository;
    private static NotificationService notificationService;
    private static Context appContext;
    private static ListenerRegistration listenerRegistration;
    
    public static void startListening(Context context) {
        appContext = context.getApplicationContext();
        firebaseManager = FirebaseManager.getInstance();
        userPreferences = UserPreferences.getInstance(appContext);
        guildRepository = GuildRepository.getInstance(appContext);
        notificationService = NotificationService.getInstance(appContext);
        
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            return;
        }
        
        firebaseManager.listenForGuildInvites(currentUserId, new FirebaseManager.GuildInviteListener() {
            @Override
            public void onInviteReceived(String inviteId, String guildId, String guildName,
                                        String fromUserId, String fromUsername,
                                        String toUserId, String toUsername) {
                
                if (guildRepository == null || notificationService == null || appContext == null) {
                    System.out.println("GuildInviteListenerService: Services are null, ignoring invite");
                    return;
                }
                
                GuildInvite invite = new GuildInvite(inviteId, guildId, guildName,
                    fromUserId, fromUsername, toUserId, toUsername);
                
                guildRepository.insertGuildInviteFromFirebase(invite, new GuildRepository.GuildInviteCallback() {
                    @Override
                    public void onSuccess(String message, GuildInvite savedInvite) {
                        if (notificationService != null) {
                            notificationService.showGuildInviteNotification(savedInvite);
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        System.out.println("Failed to save invite from Firebase: " + error);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                System.out.println("Error listening for guild invites: " + error);
            }
        });
    }
    
    public static void stopListening() {
        appContext = null;
        firebaseManager = null;
        userPreferences = null;
        guildRepository = null;
        notificationService = null;
    }
}

