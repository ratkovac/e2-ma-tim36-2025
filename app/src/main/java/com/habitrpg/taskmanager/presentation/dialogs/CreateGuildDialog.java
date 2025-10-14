package com.habitrpg.taskmanager.presentation.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Guild;
import com.habitrpg.taskmanager.service.GuildService;

public class CreateGuildDialog extends DialogFragment {
    
    private EditText guildNameEditText;
    private EditText guildDescriptionEditText;
    private EditText maxMembersEditText;
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        
        // Inflate custom layout
        android.view.LayoutInflater inflater = requireActivity().getLayoutInflater();
        android.view.View view = inflater.inflate(R.layout.dialog_create_guild, null);
        
        guildNameEditText = view.findViewById(R.id.guild_name_edit_text);
        guildDescriptionEditText = view.findViewById(R.id.guild_description_edit_text);
        maxMembersEditText = view.findViewById(R.id.max_members_edit_text);
        
        // Set default max members
        maxMembersEditText.setText("10");
        
        builder.setView(view)
            .setTitle("Create Guild")
            .setPositiveButton("Create", (dialog, which) -> {
                createGuild();
            })
            .setNegativeButton("Cancel", null);
        
        return builder.create();
    }
    
    private void createGuild() {
        String guildName = guildNameEditText.getText().toString().trim();
        String description = guildDescriptionEditText.getText().toString().trim();
        String maxMembersStr = maxMembersEditText.getText().toString().trim();
        
        // Validation
        if (guildName.isEmpty()) {
            Toast.makeText(requireContext(), "Guild name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (guildName.length() < 3) {
            Toast.makeText(requireContext(), "Guild name must be at least 3 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Guild description is required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int maxMembers;
        try {
            maxMembers = Integer.parseInt(maxMembersStr);
            if (maxMembers < 2 || maxMembers > 50) {
                Toast.makeText(requireContext(), "Max members must be between 2 and 50", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Please enter a valid number for max members", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create guild
        GuildService guildService = GuildService.getInstance(requireContext());
        guildService.createGuild(guildName, description, maxMembers, new GuildService.GuildCallback() {
            @Override
            public void onSuccess(String message, Guild guild) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    
                    // Send result to parent fragment
                    Bundle result = new Bundle();
                    result.putBoolean("guild_created", true);
                    getParentFragmentManager().setFragmentResult("guild_created", result);
                    
                    dismiss();
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
