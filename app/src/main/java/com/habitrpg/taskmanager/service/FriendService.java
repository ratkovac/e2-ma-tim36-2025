package com.habitrpg.taskmanager.service;

import android.content.Context;
import com.habitrpg.taskmanager.data.database.entities.Friend;
import com.habitrpg.taskmanager.data.database.entities.FriendRequest;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;
import com.habitrpg.taskmanager.data.repository.FriendRepository;
import com.habitrpg.taskmanager.data.repository.UserRepository;
import com.habitrpg.taskmanager.util.QRCodeGenerator;
import java.util.List;
import java.util.UUID;

public class FriendService {
    
    private static FriendService instance;
    private FriendRepository friendRepository;
    private UserRepository userRepository;
    private FirebaseManager firebaseManager;
    private UserPreferences userPreferences;
    
    private FriendService(Context context) {
        friendRepository = FriendRepository.getInstance(context);
        userRepository = UserRepository.getInstance(context);
        firebaseManager = FirebaseManager.getInstance();
        userPreferences = UserPreferences.getInstance(context);
    }
    
    public static synchronized FriendService getInstance(Context context) {
        if (instance == null) {
            instance = new FriendService(context.getApplicationContext());
        }
        return instance;
    }
    
    public void getFriends(FriendCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        friendRepository.getFriends(userId, new FriendRepository.FriendCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onFriendsRetrieved(List<Friend> friends) {
                callback.onFriendsRetrieved(friends);
            }
        });
    }
    
    public void getPendingRequests(FriendRequestCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        friendRepository.getPendingRequests(userId, new FriendRepository.FriendRequestCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onFriendRequestsRetrieved(List<FriendRequest> requests) {
                callback.onFriendRequestsRetrieved(requests);
            }
            
            @Override
            public void onFriendRequestChecked(FriendRequest request) {}
        });
    }
    
    public void searchUsers(String query, UserSearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onError("Search query cannot be empty");
            return;
        }
        
        firebaseManager.searchUsersByUsername(query.trim(), new FirebaseManager.UserSearchListener() {
            @Override
            public void onUsersFound(List<User> users) {
                callback.onUsersFound(users);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void addFriendByUsername(String username, FriendCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        if (username == null || username.trim().isEmpty()) {
            callback.onError("Username cannot be empty");
            return;
        }
        
        firebaseManager.getUserByUsername(username.trim(), new FirebaseManager.UserListener() {
            @Override
            public void onUserRetrieved(User user) {
                if (user == null) {
                    callback.onError("User not found");
                    return;
                }
                
                if (user.getId().equals(currentUserId)) {
                    callback.onError("Cannot add yourself as a friend");
                    return;
                }
                
                checkAndSendFriendRequest(currentUserId, user, callback);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void addFriendByQRCode(String qrData, FriendCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        try {
            String[] qrParts = qrData.split("\\|");
            if (qrParts.length != 3) {
                callback.onError("Invalid QR code format");
                return;
            }
            
            String friendUserId = qrParts[0];
            String friendUsername = qrParts[1];
            String friendEmail = qrParts[2];
            
            if (friendUserId.equals(currentUserId)) {
                callback.onError("Cannot add yourself as a friend");
                return;
            }
            
            User friendUser = new User();
            friendUser.setId(friendUserId);
            friendUser.setUsername(friendUsername);
            friendUser.setEmail(friendEmail);
            
            checkAndSendFriendRequest(currentUserId, friendUser, callback);
            
        } catch (Exception e) {
            callback.onError("Invalid QR code: " + e.getMessage());
        }
    }
    
    private void checkAndSendFriendRequest(String currentUserId, User friendUser, FriendCallback callback) {
        friendRepository.checkExistingRequest(currentUserId, friendUser.getId(), new FriendRepository.FriendRequestCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onFriendRequestsRetrieved(List<FriendRequest> requests) {}
            
            @Override
            public void onFriendRequestChecked(FriendRequest existingRequest) {
                if (existingRequest != null) {
                    callback.onError("Friend request already sent");
                    return;
                }
                
                userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(String message) {}
                    
                    @Override
                    public void onError(String error) {
                        callback.onError("Failed to get current user: " + error);
                    }
                    
                    @Override
                    public void onUserRetrieved(User currentUser) {
                        if (currentUser != null) {
                            sendFriendRequest(currentUser, friendUser, callback);
                        } else {
                            callback.onError("Current user not found");
                        }
                    }
                });
            }
        });
    }
    
    private void sendFriendRequest(User currentUser, User friendUser, FriendCallback callback) {
        FriendRequest request = new FriendRequest(
            UUID.randomUUID().toString(),
            currentUser.getId(),
            friendUser.getId(),
            currentUser.getUsername(),
            currentUser.getEmail(),
            currentUser.getAvatarId(),
            "pending",
            System.currentTimeMillis()
        );
        
        friendRepository.sendFriendRequest(request, new FriendRepository.FriendRequestCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess("Friend request sent successfully");
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onFriendRequestsRetrieved(List<FriendRequest> requests) {}
            
            @Override
            public void onFriendRequestChecked(FriendRequest request) {}
        });
    }
    
    public void acceptFriendRequest(String requestId, FriendCallback callback) {
        friendRepository.acceptFriendRequest(requestId, new FriendRepository.FriendCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onFriendsRetrieved(List<Friend> friends) {}
        });
    }
    
    public void declineFriendRequest(String requestId, FriendCallback callback) {
        friendRepository.declineFriendRequest(requestId, new FriendRepository.FriendCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onFriendsRetrieved(List<Friend> friends) {}
        });
    }
    
    public void removeFriend(String friendUserId, FriendCallback callback) {
        String currentUserId = userPreferences.getCurrentUserId();
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        friendRepository.removeFriend(currentUserId, friendUserId, new FriendRepository.FriendCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onFriendsRetrieved(List<Friend> friends) {}
        });
    }
    
    public void generateUserQRCode(QRCodeCallback callback) {
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
                callback.onError("Failed to get user data: " + error);
            }
            
            @Override
            public void onUserRetrieved(User user) {
                if (user != null) {
                    String qrData = QRCodeGenerator.generateUserQRData(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail()
                    );
                    callback.onQRCodeGenerated(qrData);
                } else {
                    callback.onError("User not found");
                }
            }
        });
    }
    
    public void shutdown() {
        if (friendRepository != null) {
            friendRepository.shutdown();
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
    }
    
    public interface UserSearchCallback {
        void onUsersFound(List<User> users);
        void onError(String error);
    }
    
    public interface QRCodeCallback {
        void onQRCodeGenerated(String qrData);
        void onError(String error);
    }
}
