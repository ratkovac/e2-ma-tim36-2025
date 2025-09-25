package com.habitrpg.taskmanager.presentation.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.business.auth.AuthManager;
import com.habitrpg.taskmanager.data.firebase.FirebaseManager;
import com.habitrpg.taskmanager.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {
    
    private ActivityRegisterBinding binding;
    private AuthManager authManager;
    private FirebaseManager firebaseManager;
    private int selectedAvatarId = 1; // Default avatar
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize view binding
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize AuthManager
        authManager = AuthManager.getInstance(this);
        firebaseManager = FirebaseManager.getInstance();
        
        setupClickListeners();
        setupAvatarSelection();
    }
    
    private void setupClickListeners() {
        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvLogin.setOnClickListener(v -> navigateToLogin());
    }
    
    private void setupAvatarSelection() {
        ImageView[] avatars = {
            binding.avatar1, binding.avatar2, binding.avatar3, 
            binding.avatar4, binding.avatar5
        };
        
        // Set default selection
        avatars[0].setSelected(true);
        
        for (ImageView avatar : avatars) {
            avatar.setOnClickListener(v -> {
                // Clear all selections
                for (ImageView av : avatars) {
                    av.setSelected(false);
                }
                
                // Select clicked avatar
                v.setSelected(true);
                selectedAvatarId = Integer.parseInt((String) v.getTag());
            });
        }
    }
    
    private void registerUser() {
        String email = binding.etEmail.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();
        
        if (!validateInput(email, username, password, confirmPassword)) {
            return;
        }
        
        showLoading(true);
        
        authManager.registerUser(email, password, username, selectedAvatarId, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(RegisterActivity.this, "Account created successfully! You can now login.", Toast.LENGTH_LONG).show();
                    
                    // Create default categories for new user
                    if (firebaseManager.getCurrentUser() != null) {
                        authManager.createDefaultCategories(firebaseManager.getCurrentUser().getUid());
                    }
                    
                    navigateToLogin();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private boolean validateInput(String email, String username, String password, String confirmPassword) {
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return false;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Please enter a valid email");
            binding.etEmail.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(username)) {
            binding.etUsername.setError("Username is required");
            binding.etUsername.requestFocus();
            return false;
        }
        
        if (username.length() < 3 || username.length() > 20) {
            binding.etUsername.setError("Username must be 3-20 characters");
            binding.etUsername.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return false;
        }
        
        if (password.length() < 8) {
            binding.etPassword.setError("Password must be at least 8 characters");
            binding.etPassword.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.etConfirmPassword.setError("Please confirm your password");
            binding.etConfirmPassword.requestFocus();
            return false;
        }
        
        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Passwords do not match");
            binding.etConfirmPassword.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!show);
        binding.etEmail.setEnabled(!show);
        binding.etUsername.setEnabled(!show);
        binding.etPassword.setEnabled(!show);
        binding.etConfirmPassword.setEnabled(!show);
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
