package com.habitrpg.taskmanager.presentation.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.service.AuthService;
import com.habitrpg.taskmanager.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    
    private ActivityMainBinding binding;
    private AuthService authService;
    private NavController navController;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize view binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize AuthService
        authService = AuthService.getInstance(this);
        
        // Check if user is logged in
        if (!authService.isUserLoggedIn()) {
            navigateToLogin();
            return;
        }
        
        // Delay navigation setup to ensure fragment is ready
        binding.getRoot().post(() -> {
            setupNavigation();
        });
    }
    
    private void setupNavigation() {
        // Get NavController using fragment manager - more reliable
        try {
            navController = Navigation.findNavController(this, R.id.nav_host_fragment);
            // Setup bottom navigation with NavController
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        } catch (IllegalStateException e) {
            // If NavController not ready, set up manually
            setupBottomNavigationManually();
        }
    }
    
    private void setupBottomNavigationManually() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_tasks) {
                // Navigate to tasks
                return true;
            } else if (itemId == R.id.navigation_profile) {
                // Navigate to profile
                return true;
            } else if (itemId == R.id.navigation_categories) {
                // Navigate to categories
                return true;
            }
            return false;
        });
    }
    
    
    private void navigateToLogin() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
