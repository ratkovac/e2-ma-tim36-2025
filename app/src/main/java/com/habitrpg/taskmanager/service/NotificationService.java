package com.habitrpg.taskmanager.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.GuildInvite;
import com.habitrpg.taskmanager.presentation.activities.MainActivity;

import java.util.List;

public class NotificationService {
    
    private static final String TAG = "NotificationService";
    private static final String GUILD_INVITE_CHANNEL_ID = "guild_invites";
    private static final String GUILD_INVITE_CHANNEL_NAME = "Guild Invites";
    private static final String GUILD_INVITE_CHANNEL_DESCRIPTION = "Notifications for guild invitations";
    
    private static NotificationService instance;
    private final Context context;
    private final NotificationManagerCompat notificationManager;
    
    private NotificationService(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannels();
    }
    
    public static synchronized NotificationService getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationService(context);
        }
        return instance;
    }
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                GUILD_INVITE_CHANNEL_ID,
                GUILD_INVITE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(GUILD_INVITE_CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    public void showGuildInviteNotification(GuildInvite invite) {
        try {
            // Create intent for accepting invite
            Intent acceptIntent = new Intent(context, MainActivity.class);
            acceptIntent.putExtra("action", "accept_guild_invite");
            acceptIntent.putExtra("invite_id", invite.getInviteId());
            acceptIntent.putExtra("guild_id", invite.getGuildId());
            acceptIntent.putExtra("guild_name", invite.getGuildName());
            acceptIntent.putExtra("from_username", invite.getFromUsername());
            acceptIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            PendingIntent acceptPendingIntent = PendingIntent.getActivity(
                context, 
                invite.getInviteId().hashCode(), 
                acceptIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Create intent for declining invite
            Intent declineIntent = new Intent(context, MainActivity.class);
            declineIntent.putExtra("action", "decline_guild_invite");
            declineIntent.putExtra("invite_id", invite.getInviteId());
            declineIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            PendingIntent declinePendingIntent = PendingIntent.getActivity(
                context, 
                invite.getInviteId().hashCode() + 1000, 
                declineIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Create main intent for opening the app
            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.putExtra("action", "open_guild_invites");
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            PendingIntent mainPendingIntent = PendingIntent.getActivity(
                context, 
                invite.getInviteId().hashCode() + 2000, 
                mainIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Build notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, GUILD_INVITE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Guild Invitation")
                .setContentText(invite.getFromUsername() + " invited you to join " + invite.getGuildName())
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(invite.getFromUsername() + " invited you to join the guild \"" + invite.getGuildName() + "\". Tap to accept or decline."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setAutoCancel(false) // Don't auto-cancel - user must respond
                .setOngoing(true) // Make it persistent
                .setContentIntent(mainPendingIntent)
                .addAction(R.drawable.ic_check, "Accept", acceptPendingIntent)
                .addAction(R.drawable.ic_close, "Decline", declinePendingIntent);
            
            // Show notification
            notificationManager.notify(invite.getInviteId().hashCode(), builder.build());
            
            Log.d(TAG, "Guild invite notification shown for invite: " + invite.getInviteId());
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing guild invite notification", e);
        }
    }
    
    public void showGuildInviteAcceptedNotification(String guildName, String username) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("action", "open_guild");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                (int) (System.currentTimeMillis() % Integer.MAX_VALUE), 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, GUILD_INVITE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Guild Update")
                .setContentText(username + " joined " + guildName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
            
            notificationManager.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
            
            Log.d(TAG, "Guild join notification shown for: " + username + " in " + guildName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing guild join notification", e);
        }
    }
    
    public void cancelGuildInviteNotification(String inviteId) {
        try {
            notificationManager.cancel(inviteId.hashCode());
            Log.d(TAG, "Cancelled notification for invite: " + inviteId);
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling notification for invite: " + inviteId, e);
        }
    }
    
    public void cancelAllGuildInviteNotifications() {
        try {
            // This is a simple approach - in a real app you might want to track notification IDs
            notificationManager.cancelAll();
            Log.d(TAG, "Cancelled all notifications");
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling all notifications", e);
        }
    }
    
    public void showMultipleGuildInvitesNotification(List<GuildInvite> invites) {
        if (invites.isEmpty()) {
            return;
        }
        
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("action", "open_guild_invites");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                (int) (System.currentTimeMillis() % Integer.MAX_VALUE), 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            String title = invites.size() == 1 ? "Guild Invitation" : invites.size() + " Guild Invitations";
            String contentText = invites.size() == 1 ? 
                invites.get(0).getFromUsername() + " invited you to join " + invites.get(0).getGuildName() :
                "You have " + invites.size() + " pending guild invitations";
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, GUILD_INVITE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.InboxStyle()
                    .setBigContentTitle(title)
                    .addLine(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(pendingIntent);
            
            notificationManager.notify(999999, builder.build());
            
            Log.d(TAG, "Multiple guild invites notification shown for " + invites.size() + " invites");
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing multiple guild invites notification", e);
        }
    }
}
