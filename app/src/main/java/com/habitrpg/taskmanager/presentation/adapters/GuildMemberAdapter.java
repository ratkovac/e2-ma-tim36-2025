package com.habitrpg.taskmanager.presentation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.GuildMember;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GuildMemberAdapter extends RecyclerView.Adapter<GuildMemberAdapter.MemberViewHolder> {
    
    private final List<GuildMember> members;
    private final OnMemberClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    public interface OnMemberClickListener {
        void onMemberClick(GuildMember member);
    }
    
    public GuildMemberAdapter(List<GuildMember> members, OnMemberClickListener listener) {
        this.members = members;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guild_member, parent, false);
        return new MemberViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        GuildMember member = members.get(position);
        holder.bind(member);
    }
    
    @Override
    public int getItemCount() {
        return members.size();
    }
    
    class MemberViewHolder extends RecyclerView.ViewHolder {
        private final TextView usernameText;
        private final TextView joinedDateText;
        private final TextView roleText;
        
        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.member_username_text);
            joinedDateText = itemView.findViewById(R.id.member_joined_date_text);
            roleText = itemView.findViewById(R.id.member_role_text);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onMemberClick(members.get(position));
                }
            });
        }
        
        public void bind(GuildMember member) {
            usernameText.setText(member.getUsername());
            joinedDateText.setText("Joined: " + dateFormat.format(new Date(member.getJoinedAt())));
            roleText.setText(member.isLeader() ? "Leader" : "Member");
            roleText.setTextColor(member.isLeader() ? 
                itemView.getContext().getColor(android.R.color.holo_blue_dark) : 
                itemView.getContext().getColor(android.R.color.darker_gray));
        }
    }
}
