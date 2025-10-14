package com.habitrpg.taskmanager.presentation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.GuildInvite;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GuildInviteAdapter extends RecyclerView.Adapter<GuildInviteAdapter.InviteViewHolder> {
    
    private final List<GuildInvite> invites;
    private final OnAcceptClickListener acceptListener;
    private final OnDeclineClickListener declineListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    public interface OnAcceptClickListener {
        void onAcceptClick(GuildInvite invite);
    }
    
    public interface OnDeclineClickListener {
        void onDeclineClick(GuildInvite invite);
    }
    
    public GuildInviteAdapter(List<GuildInvite> invites, OnAcceptClickListener acceptListener, OnDeclineClickListener declineListener) {
        this.invites = invites;
        this.acceptListener = acceptListener;
        this.declineListener = declineListener;
    }
    
    @NonNull
    @Override
    public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guild_invite, parent, false);
        return new InviteViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull InviteViewHolder holder, int position) {
        GuildInvite invite = invites.get(position);
        holder.bind(invite);
    }
    
    @Override
    public int getItemCount() {
        return invites.size();
    }
    
    class InviteViewHolder extends RecyclerView.ViewHolder {
        private final TextView guildNameText;
        private final TextView fromUsernameText;
        private final TextView inviteDateText;
        private final Button acceptButton;
        private final Button declineButton;
        
        public InviteViewHolder(@NonNull View itemView) {
            super(itemView);
            guildNameText = itemView.findViewById(R.id.invite_guild_name_text);
            fromUsernameText = itemView.findViewById(R.id.invite_from_username_text);
            inviteDateText = itemView.findViewById(R.id.invite_date_text);
            acceptButton = itemView.findViewById(R.id.accept_invite_button);
            declineButton = itemView.findViewById(R.id.decline_invite_button);
            
            acceptButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    acceptListener.onAcceptClick(invites.get(position));
                }
            });
            
            declineButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    declineListener.onDeclineClick(invites.get(position));
                }
            });
        }
        
        public void bind(GuildInvite invite) {
            guildNameText.setText(invite.getGuildName());
            fromUsernameText.setText("From: " + invite.getFromUsername());
            inviteDateText.setText("Invited: " + dateFormat.format(new Date(invite.getCreatedAt())));
        }
    }
}
