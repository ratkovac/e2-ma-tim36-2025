package com.habitrpg.taskmanager.service;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.entities.Friend;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;
import com.habitrpg.taskmanager.data.repository.FriendRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Map;

public class FriendshipListenerService {
    
    private static FirebaseFirestore firestore;
    private static UserPreferences userPreferences;
    private static FriendRepository friendRepository;
    private static Context appContext;
    private static ListenerRegistration listenerRegistration;
    
    public static void startListening(Context context) {
        appContext = context.getApplicationContext();
        firestore = FirebaseFirestore.getInstance();
        userPreferences = UserPreferences.getInstance(appContext);
        friendRepository = FriendRepository.getInstance(appContext);
        
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            return;
        }
        
        listenerRegistration = firestore.collection("friends")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("status", "accepted")
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    System.out.println("Error listening for friendships: " + error.getMessage());
                    return;
                }
                
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        if (doc.getMetadata().hasPendingWrites()) {
                            continue;
                        }
                        
                        Map<String, Object> data = doc.getData();
                        String friendshipId = (String) data.get("friendshipId");
                        String userId = (String) data.get("userId");
                        String friendUserId = (String) data.get("friendUserId");
                        String friendUsername = (String) data.get("friendUsername");
                        String friendEmail = (String) data.get("friendEmail");
                        int friendAvatarId = ((Number) data.get("friendAvatarId")).intValue();
                        long createdAt = ((Number) data.get("createdAt")).longValue();
                        
                        Friend friend = new Friend(
                            friendshipId, userId, friendUserId, friendUsername,
                            friendEmail, friendAvatarId, "accepted", createdAt
                        );
                        
                        new Thread(() -> {
                            try {
                                if (friendRepository == null) {
                                    System.out.println("FriendshipListenerService: friendRepository is null, ignoring friendship");
                                    return;
                                }
                                Friend existingFriend = friendRepository.getFriendByIdSync(friendshipId);
                                if (existingFriend == null) {
                                    friendRepository.insertFriendSync(friend);
                                    System.out.println("Friendship synced from Firebase: " + friendUsername);
                                }
                            } catch (Exception e) {
                                System.out.println("Failed to sync friendship: " + e.getMessage());
                            }
                        }).start();
                    }
                }
            });
    }
    
    public static void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        appContext = null;
        firestore = null;
        userPreferences = null;
        friendRepository = null;
    }
}

