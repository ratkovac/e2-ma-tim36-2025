package com.habitrpg.taskmanager.data.repository;

import android.content.Context;
import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.entities.Friend;
import com.habitrpg.taskmanager.data.database.entities.FriendRequest;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FriendRepository {
    
    private static FriendRepository instance;
    private AppDatabase database;
    private FirebaseManager firebaseManager;
    private ExecutorService executor;
    
    private FriendRepository(Context context) {
        database = AppDatabase.getDatabase(context);
        firebaseManager = FirebaseManager.getInstance();
        executor = Executors.newFixedThreadPool(2);
    }
    
    public static synchronized FriendRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FriendRepository(context.getApplicationContext());
        }
        return instance;
    }
    
    private void ensureExecutorActive() {
        if (executor.isShutdown()) {
            executor = Executors.newFixedThreadPool(2);
        }
    }
    
    public void getFriends(String userId, FriendCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                List<Friend> friends = database.friendDao().getFriendsByUserId(userId);
                callback.onFriendsRetrieved(friends);
            } catch (Exception e) {
                callback.onError("Failed to get friends: " + e.getMessage());
            }
        });
    }
    
    public void getPendingRequests(String userId, FriendRequestCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                List<FriendRequest> requests = database.friendDao().getPendingRequestsByUserId(userId);
                callback.onFriendRequestsRetrieved(requests);
            } catch (Exception e) {
                callback.onError("Failed to get pending requests: " + e.getMessage());
            }
        });
    }
    
    public void getSentRequests(String userId, FriendRequestCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                List<FriendRequest> requests = database.friendDao().getSentRequestsByUserId(userId);
                callback.onFriendRequestsRetrieved(requests);
            } catch (Exception e) {
                callback.onError("Failed to get sent requests: " + e.getMessage());
            }
        });
    }
    
    public void addFriend(Friend friend, FriendCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                database.friendDao().insertFriend(friend);
                callback.onSuccess("Friend added successfully");
            } catch (Exception e) {
                callback.onError("Failed to add friend: " + e.getMessage());
            }
        });
    }
    
    public void removeFriend(String userId, String friendUserId, FriendCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                // Remove friend relationship from both sides
                // Remove User1 -> User2 relationship
                database.friendDao().deleteFriendByUserIdAndFriendId(userId, friendUserId);
                
                // Remove User2 -> User1 relationship
                database.friendDao().deleteFriendByUserIdAndFriendId(friendUserId, userId);
                
                callback.onSuccess("Friend removed successfully");
            } catch (Exception e) {
                callback.onError("Failed to remove friend: " + e.getMessage());
            }
        });
    }
    
    public void sendFriendRequest(FriendRequest request, FriendRequestCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                database.friendDao().insertFriendRequest(request);
                
                // Sync with Firebase
                firebaseManager.sendFriendRequestDocument(
                    request.getId(),
                    request.getFromUserId(),
                    request.getFromUsername(),
                    request.getFromEmail(),
                    request.getFromAvatarId(),
                    request.getToUserId(),
                    "",
                    (success, exception) -> {
                        if (!success) {
                            System.out.println("Failed to sync friend request to Firebase: " + 
                                (exception != null ? exception.getMessage() : "Unknown error"));
                        }
                    });
                
                callback.onSuccess("Friend request sent successfully");
            } catch (Exception e) {
                callback.onError("Failed to send friend request: " + e.getMessage());
            }
        });
    }
    
    public void acceptFriendRequest(String requestId, FriendCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                android.util.Log.d("FriendRepository", "acceptFriendRequest: " + requestId);
                FriendRequest request = database.friendDao().getFriendRequestById(requestId);
                if (request != null) {
                    android.util.Log.d("FriendRepository", "Found request: " + request.getFromUsername() + " -> " + request.getToUserId());
                    request.setStatus("accepted");
                    database.friendDao().updateFriendRequest(request);
                    
                    // Get toUser information from database
                    User toUser = database.userDao().getUserById(request.getToUserId());
                    android.util.Log.d("FriendRepository", "toUser found: " + (toUser != null ? toUser.getUsername() : "null"));
                    
                    if (toUser != null) {
                        // Create Friend record for the person who accepted (toUserId)
                        Friend friend1 = new Friend(
                            requestId + "_friend1",
                            request.getToUserId(),
                            request.getFromUserId(),
                            request.getFromUsername(),
                            request.getFromEmail(),
                            request.getFromAvatarId(),
                            "accepted",
                            System.currentTimeMillis()
                        );
                        database.friendDao().insertFriend(friend1);
                        android.util.Log.d("FriendRepository", "Created friend1: " + friend1.getFriendUsername());
                        
                        // Create Friend record for the person who sent the request (fromUserId)
                        Friend friend2 = new Friend(
                            requestId + "_friend2",
                            request.getFromUserId(),
                            request.getToUserId(),
                            toUser.getUsername(),
                            toUser.getEmail(),
                            toUser.getAvatarId(),
                            "accepted",
                            System.currentTimeMillis()
                        );
                        database.friendDao().insertFriend(friend2);
                        android.util.Log.d("FriendRepository", "Created friend2: " + friend2.getFriendUsername());
                        
                        // Sync with Firebase - update request status
                        firebaseManager.updateFriendRequestStatus(requestId, "accepted",
                            (success, exception) -> {
                                if (!success) {
                                    System.out.println("Failed to update friend request status in Firebase: " + 
                                        (exception != null ? exception.getMessage() : "Unknown error"));
                                }
                            });
                        
                        // Sync with Firebase - create friendship records
                        firebaseManager.createFriendDocument(
                            requestId + "_friend1",
                            request.getToUserId(),
                            request.getFromUserId(),
                            request.getFromUsername(),
                            request.getFromEmail(),
                            request.getFromAvatarId(),
                            (success, exception) -> {
                                if (!success) {
                                    System.out.println("Failed to sync friend1 to Firebase: " + 
                                        (exception != null ? exception.getMessage() : "Unknown error"));
                                }
                            });
                        
                        firebaseManager.createFriendDocument(
                            requestId + "_friend2",
                            request.getFromUserId(),
                            request.getToUserId(),
                            toUser.getUsername(),
                            toUser.getEmail(),
                            toUser.getAvatarId(),
                            (success, exception) -> {
                                if (!success) {
                                    System.out.println("Failed to sync friend2 to Firebase: " + 
                                        (exception != null ? exception.getMessage() : "Unknown error"));
                                }
                            });
                        
                        callback.onSuccess("Friend request accepted");
                    } else {
                        android.util.Log.e("FriendRepository", "toUser not found for ID: " + request.getToUserId());
                        callback.onError("User information not found");
                    }
                } else {
                    android.util.Log.e("FriendRepository", "Request not found: " + requestId);
                    callback.onError("Friend request not found");
                }
            } catch (Exception e) {
                android.util.Log.e("FriendRepository", "Exception in acceptFriendRequest: " + e.getMessage(), e);
                callback.onError("Failed to accept friend request: " + e.getMessage());
            }
        });
    }
    
    public void declineFriendRequest(String requestId, FriendCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                FriendRequest request = database.friendDao().getFriendRequestById(requestId);
                if (request != null) {
                    request.setStatus("declined");
                    database.friendDao().updateFriendRequest(request);
                    
                    // Sync with Firebase
                    firebaseManager.updateFriendRequestStatus(requestId, "declined",
                        (success, exception) -> {
                            if (!success) {
                                System.out.println("Failed to update friend request status in Firebase: " + 
                                    (exception != null ? exception.getMessage() : "Unknown error"));
                            }
                        });
                    
                    callback.onSuccess("Friend request declined");
                } else {
                    callback.onError("Friend request not found");
                }
            } catch (Exception e) {
                callback.onError("Failed to decline friend request: " + e.getMessage());
            }
        });
    }
    
    public void checkExistingRequest(String fromUserId, String toUserId, FriendRequestCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                FriendRequest existingRequest = database.friendDao().getPendingRequestByUsers(fromUserId, toUserId);
                callback.onFriendRequestChecked(existingRequest);
            } catch (Exception e) {
                callback.onError("Failed to check existing request: " + e.getMessage());
            }
        });
    }
    
    public void insertFriendRequestFromFirebase(FriendRequest request, FriendRequestCallback callback) {
        ensureExecutorActive();
        
        executor.execute(() -> {
            try {
                FriendRequest existingRequest = database.friendDao().getFriendRequestById(request.getId());
                if (existingRequest == null) {
                    database.friendDao().insertFriendRequest(request);
                    callback.onSuccess("Friend request saved from Firebase");
                } else {
                    callback.onError("Friend request already exists");
                }
            } catch (Exception e) {
                callback.onError("Failed to save friend request: " + e.getMessage());
            }
        });
    }
    
    public Friend getFriendByIdSync(String friendshipId) {
        try {
            return database.friendDao().getFriendById(friendshipId);
        } catch (Exception e) {
            android.util.Log.e("FriendRepository", "Error getting friend by ID: " + e.getMessage());
            return null;
        }
    }
    
    public void insertFriendSync(Friend friend) {
        try {
            database.friendDao().insertFriend(friend);
        } catch (Exception e) {
            android.util.Log.e("FriendRepository", "Error inserting friend: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
    
    public interface FriendCallback {
        void onSuccess(String message);
        void onError(String error);
        void onFriendsRetrieved(List<Friend> friends);
    }
    
    public interface FriendRequestCallback {
        void onSuccess(String message);
        void onError(String error);
        void onFriendRequestsRetrieved(List<FriendRequest> requests);
        void onFriendRequestChecked(FriendRequest request);
    }
}
