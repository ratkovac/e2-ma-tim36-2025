package com.habitrpg.taskmanager.presentation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.User;

import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserSearchViewHolder> {
    
    private List<User> users;
    private OnUserClickListener onUserClickListener;
    
    public interface OnUserClickListener {
        void onUserClick(User user);
    }
    
    public UserSearchAdapter(List<User> users, OnUserClickListener onUserClickListener) {
        this.users = users;
        this.onUserClickListener = onUserClickListener;
    }
    
    @NonNull
    @Override
    public UserSearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_search, parent, false);
        return new UserSearchViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull UserSearchViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user);
    }
    
    @Override
    public int getItemCount() {
        return users.size();
    }
    
    class UserSearchViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewAvatar;
        private TextView textViewUsername;
        private TextView textViewEmail;
        
        public UserSearchViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
        }
        
        public void bind(User user) {
            textViewUsername.setText(user.getUsername());
            textViewEmail.setText(user.getEmail());
            
            // Set avatar based on avatarId
            int avatarResource = getAvatarResource(user.getAvatarId());
            imageViewAvatar.setImageResource(avatarResource);
            
            itemView.setOnClickListener(v -> {
                if (onUserClickListener != null) {
                    onUserClickListener.onUserClick(user);
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


