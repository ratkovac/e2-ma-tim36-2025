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
import com.habitrpg.taskmanager.databinding.FragmentLevelProgressBinding;
import com.habitrpg.taskmanager.service.AuthService;
import com.habitrpg.taskmanager.service.XPService;

public class LevelProgressFragment extends Fragment {

    private FragmentLevelProgressBinding binding;
    private AuthService authService;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLevelProgressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authService = AuthService.getInstance(requireContext());

        loadUserData();
    }

    private void loadUserData() {
        authService.getCurrentUser(new AuthService.UserCallback() {
            @Override
            public void onUserRetrieved(User user) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentUser = user;
                        updateLevelProgressUI();
                    });
                }
            }
        });
    }

    private void updateLevelProgressUI() {
        if (currentUser == null) return;

        int totalXP = currentUser.getExperiencePoints();
        int currentLevel = currentUser.getLevel();
        int powerPoints = currentUser.getPowerPoints();

        // Update basic info
        binding.tvCurrentLevel.setText(String.valueOf(currentLevel));
        binding.tvCurrentTitle.setText(XPService.getTitleForLevel(currentLevel));
        binding.tvCurrentPP.setText(String.valueOf(powerPoints));
        binding.tvCurrentXP.setText(String.valueOf(totalXP));

        // Calculate XP for current level progress
        int xpForNextLevel = XPService.getTotalXPRequiredForLevel(currentLevel + 1);
        int xpRemaining = xpForNextLevel - totalXP;

        // Calculate and set progress bar
        float progress = XPService.calculateLevelProgress(totalXP, currentLevel);
        binding.progressBarLevel.setProgress((int) progress);

        // Update progress text - show total XP / total XP needed for next level
        binding.tvLevelProgress.setText(
                String.format("%d / %d XP (%d to next level)",
                        totalXP, xpForNextLevel, xpRemaining)
        );

        // Update next level info
        binding.tvNextLevel.setText(String.valueOf(currentLevel + 1));
        binding.tvNextTitle.setText(XPService.getNextTitle(currentLevel));

        // Calculate PP reward for next level
        int nextLevelPPReward = XPService.getPPRewardForLevel(currentLevel + 1);
        binding.tvNextLevelPPReward.setText("+" + nextLevelPPReward + " PP");

        // Update XP requirements for next level (total cumulative)
        binding.tvNextLevelXPRequired.setText(String.valueOf(xpForNextLevel) + " XP total");

        // Update level breakdown
        updateLevelBreakdown();

        // Update XP scaling info
        updateXPScalingInfo();
    }

    private void updateLevelBreakdown() {
        StringBuilder breakdown = new StringBuilder();
        breakdown.append("Level Requirements:\n\n");

        for (int level = 1; level <= Math.min(currentUser.getLevel() + 3, 10); level++) {
            int xpForThisLevel = XPService.getXPRequiredForLevel(level);
            int totalXPRequired = XPService.getTotalXPRequiredForLevel(level + 1);
            String title = XPService.getTitleForLevel(level);
            int ppReward = XPService.getPPRewardForLevel(level);

            breakdown.append(String.format("Level %d (%s):\n", level, title));
            breakdown.append(String.format("  XP needed: %d\n", xpForThisLevel));
            breakdown.append(String.format("  Total XP to reach level %d: %d\n", level + 1, totalXPRequired));
            breakdown.append(String.format("  PP Reward: %d\n\n", ppReward));
        }

        binding.tvLevelBreakdown.setText(breakdown.toString());
    }

    private void updateXPScalingInfo() {
        StringBuilder scalingInfo = new StringBuilder();
        scalingInfo.append("XP Scaling (Current Level ").append(currentUser.getLevel()).append("):\n\n");

        // Difficulty XP scaling
        scalingInfo.append("Difficulty XP:\n");
        String[] difficulties = {"very_easy", "easy", "hard", "extreme"};
        String[] difficultyNames = {"Very Easy", "Easy", "Hard", "Extreme"};

        for (int i = 0; i < difficulties.length; i++) {
            int baseXP = XPService.getDifficultyXP(difficulties[i], 1);
            int scaledXP = XPService.getDifficultyXP(difficulties[i], currentUser.getLevel());
            scalingInfo.append(String.format("  %s: %d → %d XP\n", difficultyNames[i], baseXP, scaledXP));
        }

        scalingInfo.append("\nImportance XP:\n");
        String[] importances = {"normal", "important", "very_important", "special"};
        String[] importanceNames = {"Normal", "Important", "Very Important", "Special"};

        for (int i = 0; i < importances.length; i++) {
            int baseXP = XPService.getImportanceXP(importances[i], 1);
            int scaledXP = XPService.getImportanceXP(importances[i], currentUser.getLevel());
            scalingInfo.append(String.format("  %s: %d → %d XP\n", importanceNames[i], baseXP, scaledXP));
        }

        binding.tvXPScaling.setText(scalingInfo.toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}