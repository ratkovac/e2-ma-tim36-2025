package com.habitrpg.taskmanager.presentation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.GuildMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GuildChatAdapter extends RecyclerView.Adapter<GuildChatAdapter.MessageViewHolder> {

    private final List<GuildMessage> messages;
    private final MessageUserChecker userChecker;
    private final SimpleDateFormat timeFormat;

    public interface MessageUserChecker {
        boolean isCurrentUserMessage(GuildMessage message);
    }

    public GuildChatAdapter(List<GuildMessage> messages, MessageUserChecker userChecker) {
        this.messages = messages;
        this.userChecker = userChecker;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guild_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        GuildMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final TextView usernameText;
        private final TextView timestampText;
        private final View messageContainer;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textViewMessage);
            usernameText = itemView.findViewById(R.id.textViewUsername);
            timestampText = itemView.findViewById(R.id.textViewTimestamp);
            messageContainer = itemView.findViewById(R.id.messageContainer);
        }

        public void bind(GuildMessage message) {
            messageText.setText(message.getMessageText());
            usernameText.setText(message.getUsername());
            timestampText.setText(timeFormat.format(new Date(message.getTimestamp())));

            // Check if this is current user's message
            boolean isCurrentUser = userChecker.isCurrentUserMessage(message);
            
            if (isCurrentUser) {
                // Current user's message - align right, different background
                messageContainer.setBackgroundResource(R.drawable.message_bubble_current_user);
            } else {
                // Other user's message - align left, different background
                messageContainer.setBackgroundResource(R.drawable.message_bubble_other_user);
            }
        }
    }
}
