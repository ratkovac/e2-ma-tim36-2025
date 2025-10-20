package com.habitrpg.taskmanager.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.databinding.FragmentUserProfileBinding;
import com.habitrpg.taskmanager.service.FriendService;

public class UserProfileFragment extends Fragment {
    
    private FragmentUserProfileBinding binding;
    private FriendService friendService;
    private User userProfile;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        friendService = FriendService.getInstance(requireContext());
        
        // Get user data from arguments
        Bundle args = getArguments();
        if (args != null) {
            userProfile = new User();
            userProfile.setId(args.getString("userId", ""));
            userProfile.setUsername(args.getString("username", ""));
            userProfile.setEmail(args.getString("email", ""));
            userProfile.setAvatarId(args.getInt("avatarId", 1));
            
            displayUserProfile();
            setupClickListeners();
        } else {
            Toast.makeText(requireContext(), "No user data provided", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        }
    }
    
    private void displayUserProfile() {
        if (userProfile != null) {
            binding.textViewUsername.setText(userProfile.getUsername());
            binding.textViewEmail.setText(userProfile.getEmail());
            
            // Set avatar based on avatarId
            int avatarResource = getAvatarResource(userProfile.getAvatarId());
            binding.imageViewAvatar.setImageResource(avatarResource);
        }
    }
    
    private void setupClickListeners() {
        binding.btnAddFriend.setOnClickListener(v -> addFriend());
        binding.btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
    }
    
    private void addFriend() {
        if (userProfile != null) {
            friendService.addFriendByUsername(userProfile.getUsername(), new FriendService.FriendCallback() {
                @Override
                public void onSuccess(String message) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        binding.btnAddFriend.setText("Request Sent");
                        binding.btnAddFriend.setEnabled(false);
                    });
                }
                
                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onFriendsRetrieved(java.util.List<com.habitrpg.taskmanager.data.database.entities.Friend> friends) {}
            });
        }
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



