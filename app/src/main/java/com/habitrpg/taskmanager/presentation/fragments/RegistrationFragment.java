package com.habitrpg.taskmanager.presentation.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.habitrpg.taskmanager.service.AuthService;
import com.habitrpg.taskmanager.databinding.FragmentRegistrationBinding;
import com.habitrpg.taskmanager.presentation.activities.AuthActivity;
import com.habitrpg.taskmanager.presentation.activities.MainActivity;

public class RegistrationFragment extends Fragment {
    
    private FragmentRegistrationBinding binding;
    private AuthService authService;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistrationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        authService = AuthService.getInstance(requireContext());
        
        setupClickListeners();
        setupAvatarSelection();
    }
    
    private void setupClickListeners() {
        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvLogin.setOnClickListener(v -> showLoginFragment());
    }
    
    private int selectedAvatarId = 1;
    
    private void setupAvatarSelection() {
        ImageView[] avatars = {
            binding.avatar1, binding.avatar2, binding.avatar3, 
            binding.avatar4, binding.avatar5
        };
        
        avatars[0].setSelected(true);
        
        for (ImageView avatar : avatars) {
            avatar.setOnClickListener(v -> selectAvatar(avatars, v));
        }
    }
    
    private void selectAvatar(ImageView[] avatars, View clickedView) {
        for (ImageView avatar : avatars) {
            avatar.setSelected(false);
        }
        
        clickedView.setSelected(true);
        selectedAvatarId = Integer.parseInt((String) clickedView.getTag());
    }
    
    private void registerUser() {
        String email = binding.etEmail.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();
        
        AuthService.ValidationResult validation = authService.validateRegistrationInput(email, username, password, confirmPassword);
        if (!validation.isValid()) {
            showError(validation.getErrorMessage());
            return;
        }
        
        showLoading(true);
        
        authService.registerUser(email, password, username, selectedAvatarId, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    showLoginFragment();
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void showError(String message) {
        if (message.contains("Email")) {
            binding.etEmail.setError(message);
            binding.etEmail.requestFocus();
        } else if (message.contains("Username")) {
            binding.etUsername.setError(message);
            binding.etUsername.requestFocus();
        } else if (message.contains("Password")) {
            if (message.contains("confirm")) {
                binding.etConfirmPassword.setError(message);
                binding.etConfirmPassword.requestFocus();
            } else {
                binding.etPassword.setError(message);
                binding.etPassword.requestFocus();
            }
        }
    }
    
    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!show);
        binding.etEmail.setEnabled(!show);
        binding.etUsername.setEnabled(!show);
        binding.etPassword.setEnabled(!show);
        binding.etConfirmPassword.setEnabled(!show);
    }
    
    private void showLoginFragment() {
        if (getActivity() instanceof AuthActivity) {
            ((AuthActivity) getActivity()).showLoginFragment();
        }
    }
    
    
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
