package com.habitrpg.taskmanager.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Guild;
import com.habitrpg.taskmanager.data.database.entities.GuildMessage;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.databinding.FragmentGuildChatBinding;
import com.habitrpg.taskmanager.presentation.adapters.GuildChatAdapter;
import com.habitrpg.taskmanager.service.AuthService;
import com.habitrpg.taskmanager.service.GuildService;
import com.habitrpg.taskmanager.service.UserPreferences;
import com.habitrpg.taskmanager.service.GuildChatListenerService;
import com.habitrpg.taskmanager.data.repository.UserRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuildChatFragment extends Fragment {

    private FragmentGuildChatBinding binding;
    private GuildService guildService;
    private AuthService authService;
    private UserRepository userRepository;
    
    private Guild currentGuild;
    private User currentUser;
    private List<GuildMessage> messages = new ArrayList<>();
    private GuildChatAdapter messageAdapter;
    
    private static final int MAX_MESSAGES_DISPLAY = 50;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGuildChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        guildService = GuildService.getInstance(requireContext());
        authService = AuthService.getInstance(requireContext());
        userRepository = UserRepository.getInstance(requireContext());
        
        setupRecyclerView();
        setupClickListeners();
        loadCurrentGuild();
    }
    
    private void setupRecyclerView() {
        messageAdapter = new GuildChatAdapter(messages, this::isCurrentUserMessage);
        binding.recyclerViewMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewMessages.setAdapter(messageAdapter);
    }
    
    private void setupClickListeners() {
        binding.btnSendMessage.setOnClickListener(v -> sendMessage());
        
        // Auto-scroll to bottom when new message is added
        messageAdapter.registerAdapterDataObserver(new androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if (binding.recyclerViewMessages.getLayoutManager() != null) {
                    binding.recyclerViewMessages.scrollToPosition(messages.size() - 1);
                }
            }
        });
    }
    
    private void loadCurrentGuild() {
        String currentUserId = UserPreferences.getInstance(requireContext()).getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get current user info
        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error loading user: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onUserRetrieved(User user) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentUser = user;
                        loadGuildData();
                    });
                }
            }
        });
    }
    
    private void loadGuildData() {
        guildService.getCurrentGuild(new GuildService.GuildCallback() {
            @Override
            public void onSuccess(String message, Guild guild) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentGuild = guild;
                        updateUI();
                        loadMessages();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "You are not in any guild", Toast.LENGTH_SHORT).show();
                        requireActivity().onBackPressed();
                    });
                }
            }
        });
    }
    
    private void updateUI() {
        if (currentGuild != null) {
            binding.textViewGuildName.setText(currentGuild.getGuildName());
            binding.textViewGuildDescription.setText(currentGuild.getDescription());
        }
    }
    
    private void loadMessages() {
        if (currentGuild == null) return;
        
        GuildChatListenerService.startListening(requireContext(), currentGuild.getGuildId(), 
            () -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(this::refreshMessages);
                }
            });
        
        refreshMessages();
    }
    
    private void refreshMessages() {
        if (currentGuild == null) return;
        
        guildService.getGuildMessages(currentGuild.getGuildId(), new GuildService.GuildMessageCallback() {
            @Override
            public void onSuccess(String message, List<GuildMessage> messageList) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        messages.clear();
                        messages.addAll(messageList);
                        messageAdapter.notifyDataSetChanged();
                        
                        // Scroll to bottom
                        if (messages.size() > 0) {
                            binding.recyclerViewMessages.scrollToPosition(messages.size() - 1);
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to load messages: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void sendMessage() {
        String messageText = binding.editTextMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentGuild == null || currentUser == null) {
            Toast.makeText(requireContext(), "Unable to send message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Clear input
        binding.editTextMessage.setText("");
        
        // Send message
        guildService.sendGuildMessage(currentGuild.getGuildId(), messageText, new GuildService.GuildMessageCallback() {
            @Override
            public void onSuccess(String message, List<GuildMessage> messageList) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Message sent", Toast.LENGTH_SHORT).show();
                        loadMessages(); // Refresh messages
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to send message: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private boolean isCurrentUserMessage(GuildMessage message) {
        return currentUser != null && currentUser.getId().equals(message.getUserId());
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        GuildChatListenerService.stopListening();
        binding = null;
    }
}
