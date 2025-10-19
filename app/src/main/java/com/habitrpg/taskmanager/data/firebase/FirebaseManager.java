package com.habitrpg.taskmanager.data.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.database.entities.Category;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class FirebaseManager {
    
    private static FirebaseManager instance;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    private FirebaseManager() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
            .build();
        db.setFirestoreSettings(settings);
    }
    
    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }
    
    public FirebaseAuth getAuth() {
        return mAuth;
    }
    
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }
    
    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }
    
    public void createUserDocument(User user, OnCompleteListener listener) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("username", user.getUsername());
        userData.put("avatarId", user.getAvatarId());
        userData.put("level", user.getLevel());
        userData.put("title", user.getTitle());
        userData.put("powerPoints", user.getPowerPoints());
        userData.put("experiencePoints", user.getExperiencePoints());
        userData.put("coins", user.getCoins());
        userData.put("createdAt", System.currentTimeMillis());
        
        db.collection("users")
            .document(user.getId())
            .set(userData)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task.isSuccessful(), task.getException());
                }
            });
    }
    
    public void updateUserDocument(User user, OnCompleteListener listener) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", user.getUsername());
        userData.put("avatarId", user.getAvatarId());
        userData.put("level", user.getLevel());
        userData.put("title", user.getTitle());
        userData.put("powerPoints", user.getPowerPoints());
        userData.put("experiencePoints", user.getExperiencePoints());
        userData.put("coins", user.getCoins());
        
        db.collection("users")
            .document(user.getId())
            .update(userData)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task.isSuccessful(), task.getException());
                }
            });
    }
    
    public void getUserDocument(String userId, OnUserRetrievedListener listener) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Object> data = documentSnapshot.getData();
                    if (data != null && listener != null) {
                        listener.onUserRetrieved(data);
                    }
                } else if (listener != null) {
                    listener.onUserRetrieved(null);
                }
            })
            .addOnFailureListener(e -> {
                if (listener != null) {
                    listener.onUserRetrieved(null);
                }
            });
    }
    
    public void sendVerificationEmail(OnCompleteListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (listener != null) {
                        listener.onComplete(task.isSuccessful(), task.getException());
                    }
                });
        } else if (listener != null) {
            listener.onComplete(false, new Exception("No user logged in"));
        }
    }
    
    // Friend system methods
    public void searchUsersByUsername(String username, UserSearchListener listener) {
        db.collection("users")
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<User> users = new ArrayList<>();
                    String searchQuery = username.toLowerCase().trim();
                    
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Map<String, Object> data = document.getData();
                        String documentUsername = (String) data.get("username");
                        
                        // Check if username contains the search query (case insensitive)
                        if (documentUsername != null && documentUsername.toLowerCase().contains(searchQuery)) {
                            User user = new User();
                            user.setId(document.getId());
                            user.setUsername(documentUsername);
                            user.setEmail((String) data.get("email"));
                            user.setAvatarId(((Number) data.get("avatarId")).intValue());
                            users.add(user);
                        }
                    }
                    listener.onUsersFound(users);
                } else {
                    listener.onError("Failed to search users: " + task.getException().getMessage());
                }
            });
    }
    
    public void getUserByUsername(String username, UserListener listener) {
        db.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    QueryDocumentSnapshot document = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                    Map<String, Object> data = document.getData();
                    User user = new User();
                    user.setId(document.getId());
                    user.setUsername((String) data.get("username"));
                    user.setEmail((String) data.get("email"));
                    user.setAvatarId(((Number) data.get("avatarId")).intValue());
                    listener.onUserRetrieved(user);
                } else {
                    listener.onUserRetrieved(null);
                }
            })
            .addOnFailureListener(e -> {
                listener.onError("Failed to get user: " + e.getMessage());
            });
    }
    
    public interface OnCompleteListener {
        void onComplete(boolean success, Exception exception);
    }
    
    public interface OnUserRetrievedListener {
        void onUserRetrieved(Map<String, Object> userData);
    }
    
    public interface UserSearchListener {
        void onUsersFound(List<User> users);
        void onError(String error);
    }
    
    public interface UserListener {
        void onUserRetrieved(User user);
        void onError(String error);
    }
    
    public void createGuildDocument(String guildId, String guildName, String description, 
                                   String leaderId, String leaderUsername, int maxMembers, 
                                   OnCompleteListener listener) {
        Map<String, Object> guildData = new HashMap<>();
        guildData.put("guildId", guildId);
        guildData.put("guildName", guildName);
        guildData.put("description", description);
        guildData.put("leaderId", leaderId);
        guildData.put("leaderUsername", leaderUsername);
        guildData.put("maxMembers", maxMembers);
        guildData.put("memberCount", 1);
        guildData.put("isActive", true);
        guildData.put("missionStarted", false);
        guildData.put("createdAt", System.currentTimeMillis());
        
        db.collection("guilds")
            .document(guildId)
            .set(guildData)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task.isSuccessful(), task.getException());
                }
            });
    }
    
    public void getGuildDocument(String guildId, GuildListener listener) {
        db.collection("guilds")
            .document(guildId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Object> data = documentSnapshot.getData();
                    if (data != null && listener != null) {
                        listener.onGuildRetrieved(data);
                    } else if (listener != null) {
                        listener.onError("Guild data is null");
                    }
                } else if (listener != null) {
                    listener.onError("Guild not found in Firebase");
                }
            })
            .addOnFailureListener(e -> {
                if (listener != null) {
                    listener.onError("Failed to get guild: " + e.getMessage());
                }
            });
    }
    
    public interface GuildListener {
        void onGuildRetrieved(Map<String, Object> guildData);
        void onError(String error);
    }
    
    public void addGuildMemberDocument(String memberId, String guildId, String userId, 
                                      String username, String email, int avatarId, 
                                      boolean isLeader, OnCompleteListener listener) {
        Map<String, Object> memberData = new HashMap<>();
        memberData.put("memberId", memberId);
        memberData.put("guildId", guildId);
        memberData.put("userId", userId);
        memberData.put("username", username);
        memberData.put("email", email);
        memberData.put("avatarId", avatarId);
        memberData.put("isLeader", isLeader);
        memberData.put("isActive", true);
        memberData.put("joinedAt", System.currentTimeMillis());
        
        db.collection("guild_members")
            .document(memberId)
            .set(memberData)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task.isSuccessful(), task.getException());
                }
            });
    }
    
    public ListenerRegistration listenForGuildMembers(String guildId, GuildMembersListener listener) {
        return db.collection("guild_members")
            .whereEqualTo("guildId", guildId)
            .whereEqualTo("isActive", true)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    listener.onError("Failed to listen for members: " + error.getMessage());
                    return;
                }
                
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        if (doc.getMetadata().hasPendingWrites()) {
                            continue;
                        }
                        
                        Map<String, Object> data = doc.getData();
                        listener.onMemberAdded(
                            (String) data.get("memberId"),
                            (String) data.get("guildId"),
                            (String) data.get("userId"),
                            (String) data.get("username"),
                            (String) data.get("email"),
                            ((Number) data.get("avatarId")).intValue(),
                            (Boolean) data.get("isLeader")
                        );
                    }
                }
            });
    }
    
    public interface GuildMembersListener {
        void onMemberAdded(String memberId, String guildId, String userId, 
                          String username, String email, int avatarId, boolean isLeader);
        void onError(String error);
    }
    
    public void sendGuildMessageDocument(String messageId, String guildId, String userId, 
                                        String username, String messageText, long timestamp,
                                        OnCompleteListener listener) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", messageId);
        messageData.put("guildId", guildId);
        messageData.put("userId", userId);
        messageData.put("username", username);
        messageData.put("messageText", messageText);
        messageData.put("timestamp", timestamp);
        messageData.put("systemMessage", false);
        
        db.collection("guild_messages")
            .document(messageId)
            .set(messageData)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task.isSuccessful(), task.getException());
                }
            });
    }
    
    public ListenerRegistration listenForGuildMessages(String guildId, GuildMessagesListener listener) {
        return db.collection("guild_messages")
            .whereEqualTo("guildId", guildId)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    listener.onError("Failed to listen for messages: " + error.getMessage());
                    return;
                }
                
                if (snapshots != null) {
                    java.util.List<QueryDocumentSnapshot> sortedDocs = new java.util.ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        if (!doc.getMetadata().hasPendingWrites()) {
                            sortedDocs.add(doc);
                        }
                    }
                    
                    // Sort by timestamp on client side
                    sortedDocs.sort((doc1, doc2) -> {
                        Long ts1 = ((Number) doc1.get("timestamp")).longValue();
                        Long ts2 = ((Number) doc2.get("timestamp")).longValue();
                        return ts1.compareTo(ts2);
                    });
                    
                    for (QueryDocumentSnapshot doc : sortedDocs) {
                        Map<String, Object> data = doc.getData();
                        listener.onMessageReceived(
                            (String) data.get("messageId"),
                            (String) data.get("guildId"),
                            (String) data.get("userId"),
                            (String) data.get("username"),
                            (String) data.get("messageText"),
                            ((Number) data.get("timestamp")).longValue()
                        );
                    }
                }
            });
    }
    
    public interface GuildMessagesListener {
        void onMessageReceived(String messageId, String guildId, String userId, 
                              String username, String messageText, long timestamp);
        void onError(String error);
    }
    
    public void sendGuildInviteDocument(String inviteId, String guildId, String guildName,
                                       String fromUserId, String fromUsername,
                                       String toUserId, String toUsername,
                                       OnCompleteListener listener) {
        Map<String, Object> inviteData = new HashMap<>();
        inviteData.put("inviteId", inviteId);
        inviteData.put("guildId", guildId);
        inviteData.put("guildName", guildName);
        inviteData.put("fromUserId", fromUserId);
        inviteData.put("fromUsername", fromUsername);
        inviteData.put("toUserId", toUserId);
        inviteData.put("toUsername", toUsername);
        inviteData.put("status", "pending");
        inviteData.put("createdAt", System.currentTimeMillis());
        
        db.collection("guild_invites")
            .document(inviteId)
            .set(inviteData)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task.isSuccessful(), task.getException());
                }
            });
    }
    
    public void updateGuildInviteStatus(String inviteId, String status, OnCompleteListener listener) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", status);
        updateData.put("respondedAt", System.currentTimeMillis());
        
        db.collection("guild_invites")
            .document(inviteId)
            .update(updateData)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task.isSuccessful(), task.getException());
                }
            });
    }
    
    public void listenForGuildInvites(String userId, GuildInviteListener listener) {
        db.collection("guild_invites")
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    listener.onError("Failed to listen for invites: " + error.getMessage());
                    return;
                }
                
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> data = doc.getData();
                        listener.onInviteReceived(
                            (String) data.get("inviteId"),
                            (String) data.get("guildId"),
                            (String) data.get("guildName"),
                            (String) data.get("fromUserId"),
                            (String) data.get("fromUsername"),
                            (String) data.get("toUserId"),
                            (String) data.get("toUsername")
                        );
                    }
                }
            });
    }
    
    public interface GuildInviteListener {
        void onInviteReceived(String inviteId, String guildId, String guildName,
                            String fromUserId, String fromUsername,
                            String toUserId, String toUsername);
        void onError(String error);
    }
    
    public void sendFriendRequestDocument(String requestId, String fromUserId, String fromUsername,
                                         String fromEmail, int fromAvatarId,
                                         String toUserId, String toUsername,
                                         OnCompleteListener listener) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("requestId", requestId);
        requestData.put("fromUserId", fromUserId);
        requestData.put("fromUsername", fromUsername);
        requestData.put("fromEmail", fromEmail);
        requestData.put("fromAvatarId", fromAvatarId);
        requestData.put("toUserId", toUserId);
        requestData.put("toUsername", toUsername);
        requestData.put("status", "pending");
        requestData.put("createdAt", System.currentTimeMillis());
        
        db.collection("friend_requests")
            .document(requestId)
            .set(requestData)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task.isSuccessful(), task.getException());
                }
            });
    }
    
    public void updateFriendRequestStatus(String requestId, String status, OnCompleteListener listener) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", status);
        updateData.put("respondedAt", System.currentTimeMillis());
        
        db.collection("friend_requests")
            .document(requestId)
            .update(updateData)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task.isSuccessful(), task.getException());
                }
            });
    }
    
    public void createFriendDocument(String friendshipId, String userId, String friendUserId,
                                    String friendUsername, String friendEmail, int friendAvatarId,
                                    OnCompleteListener listener) {
        Map<String, Object> friendData = new HashMap<>();
        friendData.put("friendshipId", friendshipId);
        friendData.put("userId", userId);
        friendData.put("friendUserId", friendUserId);
        friendData.put("friendUsername", friendUsername);
        friendData.put("friendEmail", friendEmail);
        friendData.put("friendAvatarId", friendAvatarId);
        friendData.put("status", "accepted");
        friendData.put("createdAt", System.currentTimeMillis());
        
        db.collection("friends")
            .document(friendshipId)
            .set(friendData)
            .addOnCompleteListener(task -> {
                if (listener != null) {
                    listener.onComplete(task.isSuccessful(), task.getException());
                }
            });
    }
    
    public void listenForFriendRequests(String userId, FriendRequestListener listener) {
        db.collection("friend_requests")
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    listener.onError("Failed to listen for friend requests: " + error.getMessage());
                    return;
                }
                
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> data = doc.getData();
                        listener.onRequestReceived(
                            (String) data.get("requestId"),
                            (String) data.get("fromUserId"),
                            (String) data.get("fromUsername"),
                            (String) data.get("fromEmail"),
                            ((Number) data.get("fromAvatarId")).intValue(),
                            (String) data.get("toUserId"),
                            (String) data.get("toUsername")
                        );
                    }
                }
            });
    }
    
    public interface FriendRequestListener {
        void onRequestReceived(String requestId, String fromUserId, String fromUsername,
                              String fromEmail, int fromAvatarId,
                              String toUserId, String toUsername);
        void onError(String error);
    }
}
