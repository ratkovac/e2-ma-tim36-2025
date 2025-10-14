package com.habitrpg.taskmanager.presentation.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.presentation.fragments.LoginFragment;
import com.habitrpg.taskmanager.presentation.fragments.RegistrationFragment;

public class AuthActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        
        // Start with login fragment
        if (savedInstanceState == null) {
            showLoginFragment();
        }
    }
    
    public void showLoginFragment() {
        Fragment loginFragment = new LoginFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, loginFragment);
        transaction.commit();
    }
    
    public void showRegistrationFragment() {
        Fragment registrationFragment = new RegistrationFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, registrationFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}

