package com.habitrpg.taskmanager.presentation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Friend;

import java.util.ArrayList;
import java.util.List;

public class GuildInviteFriendAdapter extends RecyclerView.Adapter<GuildInviteFriendAdapter.ViewHolder> {
    
    private List<Friend> friends;
    private List<Friend> selectedFriends;
    private OnFriendSelectionListener listener;
    
    public interface OnFriendSelectionListener {
        void onFriendSelectionChanged(List<Friend> selectedFriends);
    }
    
    public GuildInviteFriendAdapter(List<Friend> friends, OnFriendSelectionListener listener) {
        this.friends = friends != null ? friends : new ArrayList<>();
        this.selectedFriends = new ArrayList<>();
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guild_invite_friend, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend friend = friends.get(position);
        holder.bind(friend);
    }
    
    @Override
    public int getItemCount() {
        return friends.size();
    }
    
    public List<Friend> getSelectedFriends() {
        return selectedFriends;
    }
    
    public class ViewHolder extends RecyclerView.ViewHolder {
        private CheckBox friendCheckbox;
        private TextView friendUsernameText;
        private TextView friendEmailText;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            friendCheckbox = itemView.findViewById(R.id.friend_checkbox);
            friendUsernameText = itemView.findViewById(R.id.friend_username_text);
            friendEmailText = itemView.findViewById(R.id.friend_email_text);
        }
        
        public void bind(Friend friend) {
            friendUsernameText.setText(friend.getFriendUsername());
            friendEmailText.setText(friend.getFriendEmail());
            
            friendCheckbox.setChecked(selectedFriends.contains(friend));
            
            friendCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedFriends.contains(friend)) {
                        selectedFriends.add(friend);
                    }
                } else {
                    selectedFriends.remove(friend);
                }
                
                if (listener != null) {
                    listener.onFriendSelectionChanged(selectedFriends);
                }
            });
            
            itemView.setOnClickListener(v -> {
                friendCheckbox.setChecked(!friendCheckbox.isChecked());
            });
        }
    }
}
