package com.habitrpg.taskmanager.service;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.entities.GuildMessage;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;
import com.habitrpg.taskmanager.data.repository.GuildRepository;
import com.google.firebase.firestore.ListenerRegistration;

public class GuildChatListenerService {
    
    private static FirebaseManager firebaseManager;
    private static GuildRepository guildRepository;
    private static Context appContext;
    private static ListenerRegistration listenerRegistration;
    private static String currentGuildId;
    private static GuildChatUpdateListener updateListener;
    
    public interface GuildChatUpdateListener {
        void onNewMessage();
    }
    
    public static void startListening(Context context, String guildId, GuildChatUpdateListener listener) {
        if (guildId == null || guildId.isEmpty()) {
            return;
        }
        
        stopListening();
        
        appContext = context.getApplicationContext();
        firebaseManager = FirebaseManager.getInstance();
        guildRepository = GuildRepository.getInstance(appContext);
        currentGuildId = guildId;
        updateListener = listener;
        
        listenerRegistration = firebaseManager.listenForGuildMessages(guildId, 
            new FirebaseManager.GuildMessagesListener() {
                @Override
                public void onMessageReceived(String messageId, String guildId, String userId,
                                             String username, String messageText, long timestamp) {
                    
                    if (guildRepository == null || appContext == null) {
                        System.out.println("GuildChatListenerService: Services are null, ignoring message");
                        return;
                    }
                    
                    GuildMessage message = new GuildMessage();
                    message.setMessageId(messageId);
                    message.setGuildId(guildId);
                    message.setUserId(userId);
                    message.setUsername(username);
                    message.setMessageText(messageText);
                    message.setTimestamp(timestamp);
                    message.setSystemMessage(false);
                    
                    new Thread(() -> {
                        try {
                            if (guildRepository == null) {
                                return;
                            }
                            GuildMessage existingMessage = guildRepository.getGuildMessageByIdSync(messageId);
                            if (existingMessage == null) {
                                guildRepository.insertGuildMessageSync(message);
                                System.out.println("Guild message synced from Firebase: " + username);
                                
                                if (updateListener != null) {
                                    updateListener.onNewMessage();
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Failed to sync guild message: " + e.getMessage());
                        }
                    }).start();
                }
                
                @Override
                public void onError(String error) {
                    System.out.println("Error listening for guild messages: " + error);
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
        guildRepository = null;
        currentGuildId = null;
        updateListener = null;
    }
}


