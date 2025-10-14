package com.habitrpg.taskmanager.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Guild;
import com.habitrpg.taskmanager.data.database.entities.GuildInvite;
import com.habitrpg.taskmanager.data.database.entities.GuildMember;
import com.habitrpg.taskmanager.presentation.adapters.GuildInviteAdapter;
import com.habitrpg.taskmanager.presentation.adapters.GuildMemberAdapter;
import com.habitrpg.taskmanager.presentation.dialogs.CreateGuildDialog;
import com.habitrpg.taskmanager.presentation.dialogs.GuildInviteDialog;
import com.habitrpg.taskmanager.service.GuildService;

import java.util.ArrayList;
import java.util.List;

public class GuildFragment extends Fragment {
    
    private GuildService guildService;
    private Guild currentGuild;
    private List<GuildMember> guildMembers;
    private List<GuildInvite> pendingInvites;
    
    // Views
    private TextView guildNameText;
    private TextView guildDescriptionText;
    private TextView guildLeaderText;
    private TextView memberCountText;
    private Button createGuildButton;
    private Button inviteFriendsButton;
    private Button leaveGuildButton;
    private Button disbandGuildButton;
    private RecyclerView membersRecyclerView;
    private RecyclerView invitesRecyclerView;
    private TextView noGuildText;
    private TextView noInvitesText;
    
    // Adapters
    private GuildMemberAdapter memberAdapter;
    private GuildInviteAdapter inviteAdapter;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        guildService = GuildService.getInstance(requireContext());
        guildMembers = new ArrayList<>();
        pendingInvites = new ArrayList<>();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guild, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupRecyclerViews();
        setupClickListeners();
        loadData();
    }
    
    private void initializeViews(View view) {
        guildNameText = view.findViewById(R.id.guild_name_text);
        guildDescriptionText = view.findViewById(R.id.guild_description_text);
        guildLeaderText = view.findViewById(R.id.guild_leader_text);
        memberCountText = view.findViewById(R.id.member_count_text);
        createGuildButton = view.findViewById(R.id.create_guild_button);
        inviteFriendsButton = view.findViewById(R.id.invite_friends_button);
        leaveGuildButton = view.findViewById(R.id.leave_guild_button);
        disbandGuildButton = view.findViewById(R.id.disband_guild_button);
        membersRecyclerView = view.findViewById(R.id.members_recycler_view);
        invitesRecyclerView = view.findViewById(R.id.invites_recycler_view);
        noGuildText = view.findViewById(R.id.no_guild_text);
        noInvitesText = view.findViewById(R.id.no_invites_text);
    }
    
    private void setupRecyclerViews() {
        // Members RecyclerView
        memberAdapter = new GuildMemberAdapter(guildMembers, this::onMemberClick);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        membersRecyclerView.setAdapter(memberAdapter);
        
        // Invites RecyclerView
        inviteAdapter = new GuildInviteAdapter(pendingInvites, this::onAcceptInvite, this::onDeclineInvite);
        invitesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        invitesRecyclerView.setAdapter(inviteAdapter);
    }
    
    private void setupClickListeners() {
        createGuildButton.setOnClickListener(v -> showCreateGuildDialog());
        inviteFriendsButton.setOnClickListener(v -> showInviteFriendsDialog());
        leaveGuildButton.setOnClickListener(v -> leaveGuild());
        disbandGuildButton.setOnClickListener(v -> disbandGuild());
    }
    
    private void loadData() {
        loadCurrentGuild();
        loadPendingInvites();
        loadGuildMembers(); // Always load members to clear the list if no guild
    }
    
    private void loadCurrentGuild() {
        guildService.getCurrentUserGuild(new GuildService.GuildCallback() {
            @Override
            public void onSuccess(String message, Guild guild) {
                requireActivity().runOnUiThread(() -> {
                    currentGuild = guild;
                    updateGuildUI();
                    loadGuildMembers();
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    currentGuild = null;
                    updateGuildUI();
                    // Show toast if user is not in any guild
                    if (error.contains("not in any guild")) {
                        Toast.makeText(requireContext(), "You are not in any guild. Create one or wait for an invitation.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
    
    private void loadGuildMembers() {
        if (currentGuild == null) {
            // Clear members list when no guild
            requireActivity().runOnUiThread(() -> {
                guildMembers.clear();
                memberAdapter.notifyDataSetChanged();
                updateMemberCount();
            });
            return;
        }
        
        guildService.getGuildMembers(currentGuild.getGuildId(), new GuildService.GuildMemberListCallback() {
            @Override
            public void onSuccess(String message, List<GuildMember> members) {
                requireActivity().runOnUiThread(() -> {
                    guildMembers.clear();
                    guildMembers.addAll(members);
                    memberAdapter.notifyDataSetChanged();
                    updateMemberCount();
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to load members: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void loadPendingInvites() {
        guildService.getPendingInvites(new GuildService.GuildInviteListCallback() {
            @Override
            public void onSuccess(String message, List<GuildInvite> invites) {
                requireActivity().runOnUiThread(() -> {
                    pendingInvites.clear();
                    pendingInvites.addAll(invites);
                    inviteAdapter.notifyDataSetChanged();
                    updateInvitesUI();
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to load invites: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void updateGuildUI() {
        if (currentGuild != null) {
            guildNameText.setText(currentGuild.getGuildName());
            guildDescriptionText.setText(currentGuild.getDescription());
            guildLeaderText.setText("Leader: " + currentGuild.getLeaderUsername());
            
            createGuildButton.setVisibility(View.GONE);
            leaveGuildButton.setVisibility(View.VISIBLE);
            disbandGuildButton.setVisibility(View.GONE);
            noGuildText.setVisibility(View.GONE);
            
            // Show buttons only for leader
            String currentUserId = com.habitrpg.taskmanager.service.UserPreferences.getInstance(requireContext()).getCurrentUserId();
            boolean isLeader = currentGuild.getLeaderId().equals(currentUserId);
            
            inviteFriendsButton.setVisibility(isLeader ? View.VISIBLE : View.GONE);
            disbandGuildButton.setVisibility(isLeader ? View.VISIBLE : View.GONE);
            
        } else {
            guildNameText.setText("NO GUILD");
            guildDescriptionText.setText("You are not currently a member of any guild");
            guildLeaderText.setText("");
            
            createGuildButton.setVisibility(View.VISIBLE);
            inviteFriendsButton.setVisibility(View.GONE);
            leaveGuildButton.setVisibility(View.GONE);
            disbandGuildButton.setVisibility(View.GONE);
            noGuildText.setVisibility(View.VISIBLE);
        }
        
        // Update member count visibility
        updateMemberCount();
    }
    
    private void updateMemberCount() {
        if (currentGuild != null) {
            memberCountText.setText("Members: " + guildMembers.size() + "/" + currentGuild.getMaxMembers());
            memberCountText.setVisibility(View.VISIBLE);
        } else {
            memberCountText.setText("");
            memberCountText.setVisibility(View.GONE);
        }
    }
    
    private void updateInvitesUI() {
        if (pendingInvites.isEmpty()) {
            noInvitesText.setVisibility(View.VISIBLE);
            invitesRecyclerView.setVisibility(View.GONE);
        } else {
            noInvitesText.setVisibility(View.GONE);
            invitesRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void showCreateGuildDialog() {
        CreateGuildDialog dialog = new CreateGuildDialog();
        dialog.show(getParentFragmentManager(), "CreateGuildDialog");
        
        // Set up listener for dialog result
        getParentFragmentManager().setFragmentResultListener("guild_created", this, (requestKey, bundle) -> {
            loadData(); // Refresh data when guild is created
        });
    }
    
    private void showInviteFriendsDialog() {
        if (currentGuild == null) {
            Toast.makeText(requireContext(), "You must be in a guild to invite friends!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        GuildInviteDialog dialog = GuildInviteDialog.newInstance(currentGuild.getGuildId());
        dialog.show(getParentFragmentManager(), "GuildInviteDialog");
        
        // Set up listener for dialog result
        getParentFragmentManager().setFragmentResultListener("invite_sent", this, (requestKey, bundle) -> {
            Toast.makeText(requireContext(), "Invite sent successfully!", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void leaveGuild() {
        if (currentGuild == null) {
            Toast.makeText(requireContext(), "You are not in any guild!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        guildService.leaveGuild(new GuildService.GuildCallback() {
            @Override
            public void onSuccess(String message, Guild guild) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    loadData(); // Refresh data
                    
                    // Navigate back to previous fragment
                    Navigation.findNavController(requireView()).popBackStack();
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
    
    private void disbandGuild() {
        if (currentGuild == null) {
            Toast.makeText(requireContext(), "You are not in any guild!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        guildService.disbandGuild(currentGuild.getGuildId(), new GuildService.GuildCallback() {
            @Override
            public void onSuccess(String message, Guild guild) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    loadData(); // Refresh data
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
    
    private void onMemberClick(GuildMember member) {
        // Handle member click - could show member details or actions
        Toast.makeText(requireContext(), "Member: " + member.getUsername(), Toast.LENGTH_SHORT).show();
    }
    
    private void onAcceptInvite(GuildInvite invite) {
        guildService.acceptGuildInvite(invite.getInviteId(), new GuildService.GuildCallback() {
            @Override
            public void onSuccess(String message, Guild guild) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    loadData(); // Refresh data
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
    
    private void onDeclineInvite(GuildInvite invite) {
        guildService.declineGuildInvite(invite.getInviteId(), new GuildService.GuildCallback() {
            @Override
            public void onSuccess(String message, Guild guild) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    loadData(); // Refresh data
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
    
    @Override
    public void onResume() {
        super.onResume();
        loadData(); // Refresh data when fragment resumes
    }
}
