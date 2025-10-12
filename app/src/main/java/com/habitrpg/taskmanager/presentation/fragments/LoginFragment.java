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

import com.habitrpg.taskmanager.service.AuthService;
import com.habitrpg.taskmanager.databinding.FragmentLoginBinding;
import com.habitrpg.taskmanager.presentation.activities.AuthActivity;
import com.habitrpg.taskmanager.presentation.activities.MainActivity;

public class LoginFragment extends Fragment {
    
    private FragmentLoginBinding binding;
    private AuthService authService;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        authService = AuthService.getInstance(requireContext());
        
        setupClickListeners();
    }
    
    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvRegister.setOnClickListener(v -> showRegistrationFragment());
    }
    
    private void loginUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        
        AuthService.ValidationResult validation = authService.validateLoginInput(email, password);
        if (!validation.isValid()) {
            showError(validation.getErrorMessage());
            return;
        }
        
        showLoading(true);
        
        authService.loginUser(email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    navigateToMain();
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
        } else if (message.contains("Password")) {
            binding.etPassword.setError(message);
            binding.etPassword.requestFocus();
        }
    }
    
    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!show);
        binding.etEmail.setEnabled(!show);
        binding.etPassword.setEnabled(!show);
    }
    
    private void showRegistrationFragment() {
        if (getActivity() instanceof AuthActivity) {
            ((AuthActivity) getActivity()).showRegistrationFragment();
        }
    }
    
    private void navigateToMain() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
