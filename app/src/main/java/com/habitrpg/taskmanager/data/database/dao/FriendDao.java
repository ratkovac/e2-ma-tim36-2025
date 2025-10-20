package com.habitrpg.taskmanager.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import com.habitrpg.taskmanager.data.database.entities.Friend;
import com.habitrpg.taskmanager.data.database.entities.FriendRequest;
import java.util.List;

@Dao
public interface FriendDao {
    
    // Friend operations
    @Query("SELECT * FROM friends WHERE user_id = :userId AND status = 'accepted' ORDER BY created_at DESC")
    List<Friend> getFriendsByUserId(String userId);
    
    @Query("SELECT * FROM friends WHERE user_id = :userId AND friend_user_id = :friendUserId")
    Friend getFriendByUserIdAndFriendId(String userId, String friendUserId);
    
    @Query("SELECT * FROM friends WHERE id = :friendshipId")
    Friend getFriendById(String friendshipId);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFriend(Friend friend);
    
    @Update
    void updateFriend(Friend friend);
    
    @Delete
    void deleteFriend(Friend friend);
    
    @Query("DELETE FROM friends WHERE user_id = :userId AND friend_user_id = :friendUserId")
    void deleteFriendByUserIdAndFriendId(String userId, String friendUserId);
    
    // FriendRequest operations
    @Query("SELECT * FROM friend_requests WHERE to_user_id = :userId AND status = 'pending' ORDER BY created_at DESC")
    List<FriendRequest> getPendingRequestsByUserId(String userId);
    
    @Query("SELECT * FROM friend_requests WHERE from_user_id = :userId AND status = 'pending' ORDER BY created_at DESC")
    List<FriendRequest> getSentRequestsByUserId(String userId);
    
    @Query("SELECT * FROM friend_requests WHERE id = :requestId")
    FriendRequest getFriendRequestById(String requestId);
    
    @Query("SELECT * FROM friend_requests WHERE from_user_id = :fromUserId AND to_user_id = :toUserId AND status = 'pending'")
    FriendRequest getPendingRequestByUsers(String fromUserId, String toUserId);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFriendRequest(FriendRequest friendRequest);
    
    @Update
    void updateFriendRequest(FriendRequest friendRequest);
    
    @Delete
    void deleteFriendRequest(FriendRequest friendRequest);
    
    @Query("DELETE FROM friend_requests WHERE id = :requestId")
    void deleteFriendRequestById(String requestId);
    
    // Cleanup operations
    @Query("DELETE FROM friends WHERE user_id = :userId")
    void deleteAllFriendsForUser(String userId);
    
    @Query("DELETE FROM friend_requests WHERE from_user_id = :userId OR to_user_id = :userId")
    void deleteAllFriendRequestsForUser(String userId);
}
