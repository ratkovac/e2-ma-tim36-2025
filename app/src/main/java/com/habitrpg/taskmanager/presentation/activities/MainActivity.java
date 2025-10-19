package com.habitrpg.taskmanager.presentation.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.service.AuthService;
import com.habitrpg.taskmanager.service.TaskService;
import com.habitrpg.taskmanager.service.GuildInviteListenerService;
import com.habitrpg.taskmanager.service.FriendRequestListenerService;
import com.habitrpg.taskmanager.service.FriendshipListenerService;
import com.habitrpg.taskmanager.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    
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
        
        // Request notification permission for Android 13+
        requestNotificationPermission();
        
        // Delay navigation setup to ensure fragment is ready
        binding.getRoot().post(() -> {
            setupNavigation();
            // Check and update overdue tasks
            checkOverdueTasks();
            // Start listening for guild invites
            startGuildInviteListener();
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
                navController.navigate(R.id.navigation_tasks);
                return true;
            } else if (itemId == R.id.navigation_categories) {
                navController.navigate(R.id.navigation_categories);
                return true;
            } else if (itemId == R.id.navigation_statistics) {
                navController.navigate(R.id.navigation_statistics);
                return true;
            } else if (itemId == R.id.navigation_friends) {
                navController.navigate(R.id.navigation_friends);
                return true;
            }
            return false;
        });
    }
    
    
    private void checkOverdueTasks() {
        TaskService taskService = TaskService.getInstance(this);
        taskService.checkAndUpdateOverdueTasks();
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                System.out.println("Notification permission granted!");
            } else {
                System.out.println("Notification permission denied!");
            }
        }
    }
    
    private void startGuildInviteListener() {
        GuildInviteListenerService.startListening(this);
        FriendRequestListenerService.startListening(this);
        FriendshipListenerService.startListening(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        GuildInviteListenerService.stopListening();
        FriendRequestListenerService.stopListening();
        FriendshipListenerService.stopListening();
        binding = null;
    }
}
