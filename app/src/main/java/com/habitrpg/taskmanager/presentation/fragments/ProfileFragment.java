package com.habitrpg.taskmanager.presentation.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.habitrpg.taskmanager.business.auth.AuthManager;
import com.habitrpg.taskmanager.business.xp.XPCalculator;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.databinding.FragmentProfileBinding;
import com.habitrpg.taskmanager.presentation.activities.LoginActivity;

public class ProfileFragment extends Fragment {
    
    private FragmentProfileBinding binding;
    private AuthManager authManager;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        authManager = AuthManager.getInstance(requireContext());
        
        setupClickListeners();
        loadUserProfile();
    }
    
    private void setupClickListeners() {
        binding.btnChangePassword.setOnClickListener(v -> {
            // TODO: Implement change password dialog
            Toast.makeText(getContext(), "Change password feature coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnLogout.setOnClickListener(v -> logoutUser());
    }
    
    private void loadUserProfile() {
        showLoading(true);
        
        authManager.getCurrentUser(new AuthManager.UserCallback() {
            @Override
            public void onUserRetrieved(User user) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        
                        if (user != null) {
                            updateUI(user);
                        } else {
                            Toast.makeText(getContext(), "Failed to load user profile", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
    
    private void updateUI(User user) {
        // Set user information
        binding.tvUsername.setText(user.getUsername());
        binding.tvTitle.setText(user.getTitle());
        binding.tvLevel.setText(String.valueOf(user.getLevel()));
        binding.tvPowerPoints.setText(String.valueOf(user.getPowerPoints()));
        binding.tvCoins.setText(String.valueOf(user.getCoins()));
        
        // Calculate and display XP progress
        int currentXP = user.getExperiencePoints();
        int currentLevel = user.getLevel();
        int xpForNextLevel = XPCalculator.getXPRequiredForLevel(currentLevel + 1);
        int totalXPForNextLevel = XPCalculator.getTotalXPRequiredForLevel(currentLevel + 1);
        
        float progress = XPCalculator.calculateLevelProgress(currentXP, currentLevel);
        binding.progressBarXP.setProgress((int) progress);
        
        int xpRemaining = XPCalculator.getXPRemainingToNextLevel(currentXP, currentLevel);
        binding.tvXP.setText(currentXP + " / " + totalXPForNextLevel + " XP (" + xpRemaining + " to next level)");
        
        // TODO: Set avatar based on user.getAvatarId()
    }
    
    private void logoutUser() {
        authManager.logoutUser(new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
    
    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh profile when fragment becomes visible
        loadUserProfile();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
