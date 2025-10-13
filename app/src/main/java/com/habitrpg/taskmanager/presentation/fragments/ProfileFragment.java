package com.habitrpg.taskmanager.presentation.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.habitrpg.taskmanager.presentation.dialogs.ChangePasswordDialog;
import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.service.AuthService;
import com.habitrpg.taskmanager.service.XPService;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.databinding.FragmentProfileBinding;
import com.habitrpg.taskmanager.presentation.activities.AuthActivity;
import com.habitrpg.taskmanager.util.QRCodeGenerator;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private AuthService authService;
    private User currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authService = AuthService.getInstance(requireContext());

        setupClickListeners();
        loadUserProfile();
    }

    private void setupClickListeners() {
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.btnLogout.setOnClickListener(v -> logoutUser());
        binding.btnGenerateQR.setOnClickListener(v -> generateQRCode());
        binding.btnViewBadges.setOnClickListener(v -> showBadges());
        binding.btnViewEquipment.setOnClickListener(v -> showEquipment());
        binding.btnLevelProgress.setOnClickListener(v -> showLevelProgress());
    }

    private void showChangePasswordDialog() {
        ChangePasswordDialog dialog = ChangePasswordDialog.newInstance(
                authService,
                new ChangePasswordDialog.OnPasswordChangedListener() {
                    @Override
                    public void onPasswordChanged() {
                        Toast.makeText(getContext(), "Password changed successfully!", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        dialog.show(getParentFragmentManager(), "ChangePasswordDialog");
    }

    private void generateQRCode() {
        if (currentUser != null) {
            String qrData = QRCodeGenerator.generateUserQRData(
                    currentUser.getId(),
                    currentUser.getUsername(),
                    currentUser.getEmail()
            );

            Bitmap qrBitmap = QRCodeGenerator.generateQRCode(qrData, 400, 400);
            if (qrBitmap != null) {
                binding.imageViewQRCode.setImageBitmap(qrBitmap);
                binding.imageViewQRCode.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getContext(), "Failed to generate QR code", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showBadges() {
        Toast.makeText(getContext(), "Badges feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void showEquipment() {
        Toast.makeText(getContext(), "Equipment feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void showLevelProgress() {
        Navigation.findNavController(requireView()).navigate(R.id.navigation_level_progress);
    }

    private void loadUserProfile() {
        showLoading(true);

        authService.getCurrentUser(new AuthService.UserCallback() {
            @Override
            public void onUserRetrieved(User user) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);

                        if (user != null) {
                            currentUser = user;
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
        binding.tvUsername.setText(user.getUsername());
        binding.tvTitle.setText(user.getTitle());
        binding.tvLevel.setText(String.valueOf(user.getLevel()));
        binding.tvPowerPoints.setText(String.valueOf(user.getPowerPoints()));
        binding.tvCoins.setText(String.valueOf(user.getCoins()));

        int totalXP = user.getExperiencePoints();
        int currentLevel = user.getLevel();

        // Calculate XP for current level progress
        int xpForCurrentLevel = XPService.getTotalXPRequiredForLevel(currentLevel);
        int xpForNextLevel = XPService.getTotalXPRequiredForLevel(currentLevel + 1);
        int xpNeededForThisLevel = xpForNextLevel - xpForCurrentLevel;
        int xpProgressInThisLevel = totalXP - xpForCurrentLevel;
        int xpRemaining = xpForNextLevel - totalXP;

        // Calculate progress percentage
        float progress = XPService.calculateLevelProgress(totalXP, currentLevel);
        binding.progressBarXP.setProgress((int) progress);

        // Display XP progress - show progress within current level
        binding.tvXP.setText(
                String.format("%d / %d XP (%d to next level)",
                        xpProgressInThisLevel, xpNeededForThisLevel, xpRemaining)
        );

        setAvatar(user.getAvatarId());
    }

    private void setAvatar(int avatarId) {
        int avatarResource;
        switch (avatarId) {
            case 1:
                avatarResource = R.drawable.creeper;
                break;
            case 2:
                avatarResource = R.drawable.grinch;
                break;
            case 3:
                avatarResource = R.drawable.shrek;
                break;
            case 4:
                avatarResource = R.drawable.transformers;
                break;
            case 5:
                avatarResource = R.drawable.spyro;
                break;
            default:
                avatarResource = R.drawable.creeper;
                break;
        }
        binding.imageViewAvatar.setImageResource(avatarResource);
    }

    private void logoutUser() {
        authService.logoutUser(new AuthService.AuthCallback() {
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
        Intent intent = new Intent(getActivity(), AuthActivity.class);
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
        loadUserProfile();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}