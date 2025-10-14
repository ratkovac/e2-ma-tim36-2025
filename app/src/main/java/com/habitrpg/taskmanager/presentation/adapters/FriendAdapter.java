package com.habitrpg.taskmanager.presentation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Friend;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {
    
    private List<Friend> friends;
    private OnFriendClickListener onFriendClickListener;
    private OnRemoveFriendClickListener onRemoveFriendClickListener;
    
    public interface OnFriendClickListener {
        void onFriendClick(Friend friend);
    }
    
    public interface OnRemoveFriendClickListener {
        void onRemoveFriend(Friend friend);
    }
    
    public FriendAdapter(List<Friend> friends, OnFriendClickListener onFriendClickListener, OnRemoveFriendClickListener onRemoveFriendClickListener) {
        this.friends = friends;
        this.onFriendClickListener = onFriendClickListener;
        this.onRemoveFriendClickListener = onRemoveFriendClickListener;
    }
    
    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = friends.get(position);
        holder.bind(friend);
    }
    
    @Override
    public int getItemCount() {
        return friends.size();
    }
    
    class FriendViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewAvatar;
        private TextView textViewUsername;
        private TextView textViewEmail;
        private TextView textViewRemove;
        
        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            textViewRemove = itemView.findViewById(R.id.textViewRemove);
        }
        
        public void bind(Friend friend) {
            textViewUsername.setText(friend.getFriendUsername());
            textViewEmail.setText(friend.getFriendEmail());
            
            // Set avatar based on avatarId
            int avatarResource = getAvatarResource(friend.getFriendAvatarId());
            imageViewAvatar.setImageResource(avatarResource);
            
            itemView.setOnClickListener(v -> {
                if (onFriendClickListener != null) {
                    onFriendClickListener.onFriendClick(friend);
                }
            });
            
            textViewRemove.setOnClickListener(v -> {
                if (onRemoveFriendClickListener != null) {
                    onRemoveFriendClickListener.onRemoveFriend(friend);
                }
            });
        }
        
        private int getAvatarResource(int avatarId) {
            switch (avatarId) {
                case 1: return R.drawable.creeper;
                case 2: return R.drawable.grinch;
                case 3: return R.drawable.shrek;
                case 4: return R.drawable.transformers;
                case 5: return R.drawable.spyro;
                default: return R.drawable.creeper;
            }
        }
    }
}

