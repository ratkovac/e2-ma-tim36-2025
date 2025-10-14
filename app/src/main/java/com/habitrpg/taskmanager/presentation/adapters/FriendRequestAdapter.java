package com.habitrpg.taskmanager.presentation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.FriendRequest;

import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder> {
    
    private List<FriendRequest> requests;
    private OnAcceptRequestClickListener onAcceptRequestClickListener;
    private OnDeclineRequestClickListener onDeclineRequestClickListener;
    
    public interface OnAcceptRequestClickListener {
        void onAcceptRequest(FriendRequest request);
    }
    
    public interface OnDeclineRequestClickListener {
        void onDeclineRequest(FriendRequest request);
    }
    
    public FriendRequestAdapter(List<FriendRequest> requests, OnAcceptRequestClickListener onAcceptRequestClickListener, OnDeclineRequestClickListener onDeclineRequestClickListener) {
        this.requests = requests;
        this.onAcceptRequestClickListener = onAcceptRequestClickListener;
        this.onDeclineRequestClickListener = onDeclineRequestClickListener;
    }
    
    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_request, parent, false);
        return new FriendRequestViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position) {
        FriendRequest request = requests.get(position);
        holder.bind(request);
    }
    
    @Override
    public int getItemCount() {
        return requests.size();
    }
    
    class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewAvatar;
        private TextView textViewUsername;
        private TextView textViewEmail;
        private TextView textViewAccept;
        private TextView textViewDecline;
        
        public FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            textViewAccept = itemView.findViewById(R.id.textViewAccept);
            textViewDecline = itemView.findViewById(R.id.textViewDecline);
        }
        
        public void bind(FriendRequest request) {
            textViewUsername.setText(request.getFromUsername());
            textViewEmail.setText(request.getFromEmail());
            
            // Set avatar based on avatarId
            int avatarResource = getAvatarResource(request.getFromAvatarId());
            imageViewAvatar.setImageResource(avatarResource);
            
            textViewAccept.setOnClickListener(v -> {
                if (onAcceptRequestClickListener != null) {
                    onAcceptRequestClickListener.onAcceptRequest(request);
                }
            });
            
            textViewDecline.setOnClickListener(v -> {
                if (onDeclineRequestClickListener != null) {
                    onDeclineRequestClickListener.onDeclineRequest(request);
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

