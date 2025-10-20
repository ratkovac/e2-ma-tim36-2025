package com.habitrpg.taskmanager.service;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.entities.FriendRequest;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;
import com.habitrpg.taskmanager.data.repository.FriendRepository;

public class FriendRequestListenerService {
    
    private static FirebaseManager firebaseManager;
    private static UserPreferences userPreferences;
    private static FriendRepository friendRepository;
    private static NotificationService notificationService;
    private static Context appContext;
    
    public static void startListening(Context context) {
        appContext = context.getApplicationContext();
        firebaseManager = FirebaseManager.getInstance();
        userPreferences = UserPreferences.getInstance(appContext);
        friendRepository = FriendRepository.getInstance(appContext);
        notificationService = NotificationService.getInstance(appContext);
        
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            return;
        }
        
        firebaseManager.listenForFriendRequests(currentUserId, new FirebaseManager.FriendRequestListener() {
            @Override
            public void onRequestReceived(String requestId, String fromUserId, String fromUsername,
                                         String fromEmail, int fromAvatarId,
                                         String toUserId, String toUsername) {
                
                if (friendRepository == null || notificationService == null || appContext == null) {
                    System.out.println("FriendRequestListenerService: Services are null, ignoring request");
                    return;
                }
                
                FriendRequest request = new FriendRequest(
                    requestId, fromUserId, toUserId, fromUsername, fromEmail, fromAvatarId,
                    "pending", System.currentTimeMillis()
                );
                
                friendRepository.insertFriendRequestFromFirebase(request, new FriendRepository.FriendRequestCallback() {
                    @Override
                    public void onSuccess(String message) {
                        System.out.println("Friend request saved from Firebase: " + fromUsername + " -> " + toUsername);
                        if (notificationService != null) {
                            notificationService.showFriendRequestNotification(request);
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        System.out.println("Failed to save friend request from Firebase: " + error);
                    }
                    
                    @Override
                    public void onFriendRequestsRetrieved(java.util.List<FriendRequest> requests) {
                    }
                    
                    @Override
                    public void onFriendRequestChecked(FriendRequest request) {
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                System.out.println("Error listening for friend requests: " + error);
            }
        });
    }
    
    public static void stopListening() {
        appContext = null;
        firebaseManager = null;
        userPreferences = null;
        friendRepository = null;
        notificationService = null;
    }
}

