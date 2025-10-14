package com.habitrpg.taskmanager.presentation.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Friend;
import com.habitrpg.taskmanager.presentation.adapters.GuildInviteFriendAdapter;
import com.habitrpg.taskmanager.service.FriendService;
import com.habitrpg.taskmanager.service.GuildService;

import java.util.List;

public class GuildInviteDialog extends DialogFragment {
    
    private String guildId;
    private RecyclerView friendsRecyclerView;
    private TextView noFriendsText;
    private GuildInviteFriendAdapter adapter;
    private List<Friend> friends;
    
    public static GuildInviteDialog newInstance(String guildId) {
        GuildInviteDialog dialog = new GuildInviteDialog();
        Bundle args = new Bundle();
        args.putString("guildId", guildId);
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            guildId = getArguments().getString("guildId");
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        
        // Inflate custom layout
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_guild_invite, null);
        
        friendsRecyclerView = view.findViewById(R.id.friends_recycler_view);
        noFriendsText = view.findViewById(R.id.no_friends_text);
        
        // Setup RecyclerView
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Load friends
        loadFriends();
        
        builder.setView(view)
            .setTitle("Invite Friends to Guild")
            .setPositiveButton("Send Invites", (dialog, which) -> {
                sendInvites();
            })
            .setNegativeButton("Cancel", null);
        
        return builder.create();
    }
    
    private void loadFriends() {
        FriendService friendService = FriendService.getInstance(requireContext());
        friendService.getFriends(new FriendService.FriendCallback() {
            @Override
            public void onSuccess(String message) {
                // Not used here
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to load friends: " + error, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onFriendsRetrieved(List<Friend> friendsList) {
                requireActivity().runOnUiThread(() -> {
                    friends = friendsList;
                    updateUI();
                });
            }
        });
    }
    
    private void updateUI() {
        if (friends == null || friends.isEmpty()) {
            friendsRecyclerView.setVisibility(View.GONE);
            noFriendsText.setVisibility(View.VISIBLE);
        } else {
            friendsRecyclerView.setVisibility(View.VISIBLE);
            noFriendsText.setVisibility(View.GONE);
            
            adapter = new GuildInviteFriendAdapter(friends, selectedFriends -> {
                // Update UI based on selection
            });
            friendsRecyclerView.setAdapter(adapter);
        }
    }
    
    private void sendInvites() {
        if (adapter == null || adapter.getSelectedFriends().isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one friend to invite", Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<Friend> selectedFriends = adapter.getSelectedFriends();
        GuildService guildService = GuildService.getInstance(requireContext());
        
        // Send invites to selected friends
        for (Friend friend : selectedFriends) {
            guildService.sendGuildInvite(guildId, friend.getFriendUserId(), new GuildService.GuildCallback() {
                @Override
                public void onSuccess(String message, com.habitrpg.taskmanager.data.database.entities.Guild guild) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Invite sent to " + friend.getFriendUsername(), Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to send invite to " + friend.getFriendUsername() + ": " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
        
        // Send result to parent fragment
        Bundle result = new Bundle();
        result.putBoolean("invite_sent", true);
        getParentFragmentManager().setFragmentResult("invite_sent", result);
        
        dismiss();
    }
}
