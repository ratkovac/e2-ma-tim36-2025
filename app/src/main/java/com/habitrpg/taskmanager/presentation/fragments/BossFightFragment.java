package com.habitrpg.taskmanager.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Boss;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.service.AuthService;
import com.habitrpg.taskmanager.service.BossService;

public class BossFightFragment extends Fragment {

    // UI Components
    private TextView bossHealthText;
    private ProgressBar bossHealthBar;
    private TextView playerPpText;
    private ProgressBar playerPpBar;
    private ImageView bossImage;
    private ImageView equipmentIcon;
    private TextView equipmentName;
    private TextView attackCounter;
    private TextView successChanceText;
    private MaterialButton attackButton;
    private TextView shakeInstruction;

    // Services
    private BossService bossService;
    private AuthService authService;
    
    // Boss Fight Data
    private Boss currentBoss;
    private User currentUser;
    private int remainingAttacks = 5;
    private int successChance = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boss_fight, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize services
        bossService = BossService.getInstance(requireContext());
        authService = AuthService.getInstance(requireContext());
        
        initializeViews(view);
        setupClickListeners();
        loadBossFightData();
    }

    private void initializeViews(View view) {
        bossHealthText = view.findViewById(R.id.boss_health_text);
        bossHealthBar = view.findViewById(R.id.boss_health_bar);
        playerPpText = view.findViewById(R.id.player_pp_text);
        playerPpBar = view.findViewById(R.id.player_pp_bar);
        bossImage = view.findViewById(R.id.boss_image);
        equipmentIcon = view.findViewById(R.id.equipment_icon);
        equipmentName = view.findViewById(R.id.equipment_name);
        attackCounter = view.findViewById(R.id.attack_counter);
        successChanceText = view.findViewById(R.id.success_chance_text);
        attackButton = view.findViewById(R.id.attack_button);
        shakeInstruction = view.findViewById(R.id.shake_instruction);
    }

    private void setupClickListeners() {
        attackButton.setOnClickListener(v -> {
            // TODO: Implement attack logic
            performAttack();
        });
    }

    private void loadBossFightData() {
        // Load current user
        authService.getCurrentUser(new AuthService.UserCallback() {
            @Override
            public void onUserRetrieved(User user) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentUser = user;
                        loadCurrentBoss();
                    });
                }
            }
        });
    }
    
    private void loadCurrentBoss() {
        bossService.getCurrentBoss(new BossService.BossCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error loading boss: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onBossRetrieved(Boss boss) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentBoss = boss;
                        loadSuccessRate();
                    });
                }
            }
            
            @Override
            public void onBossFightResult(BossService.BossFightResult result) {}
        });
    }
    
    private void loadSuccessRate() {
        bossService.calculateTaskSuccessRate(new BossService.BossCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        successChance = Integer.parseInt(message);
                        updateUI();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        successChance = 0;
                        updateUI();
                    });
                }
            }
            
            @Override
            public void onBossRetrieved(Boss boss) {}
            
            @Override
            public void onBossFightResult(BossService.BossFightResult result) {}
        });
    }

    private void updateUI() {
        if (currentBoss == null || currentUser == null) {
            return;
        }
        
        // Update boss health
        bossHealthText.setText(currentBoss.getCurrentHp() + " HP");
        bossHealthBar.setMax(currentBoss.getMaxHp());
        bossHealthBar.setProgress(currentBoss.getCurrentHp());

        // Update player PP
        playerPpText.setText(currentUser.getPowerPoints() + " PP");
        playerPpBar.setMax(currentUser.getPowerPoints() + 100); // Set max to current PP + buffer
        playerPpBar.setProgress(currentUser.getPowerPoints());

        // Update attack counter
        attackCounter.setText(remainingAttacks + " / 5");

        // Update success chance
        successChanceText.setText(successChance + "%");

        // Update equipment display (placeholder for now)
        equipmentName.setText("Magic Sword");
        equipmentIcon.setVisibility(View.VISIBLE);
    }

    private void performAttack() {
        if (remainingAttacks <= 0 || currentBoss == null || currentUser == null) {
            return;
        }
        
        remainingAttacks--;
        
        // Perform actual boss attack
        bossService.performBossAttack(currentBoss.getLevel(), currentUser.getPowerPoints(), new BossService.BossCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        updateUI();
                        
                        // Check if boss is defeated or attacks exhausted
                        if (currentBoss.isDefeated()) {
                            showVictoryMessage();
                        } else if (remainingAttacks <= 0) {
                            showDefeatMessage();
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Attack failed: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onBossRetrieved(Boss boss) {}
            
            @Override
            public void onBossFightResult(BossService.BossFightResult result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showBossFightResult(result);
                    });
                }
            }
        });
    }

    private void showVictoryMessage() {
        attackButton.setText("POBEDA!");
        attackButton.setEnabled(false);
    }

    private void showDefeatMessage() {
        attackButton.setText("PORAZ!");
        attackButton.setEnabled(false);
    }
    
    private void showBossFightResult(BossService.BossFightResult result) {
        String message = "Boss Fight Result:\n";
        if (result.isVictory()) {
            message += "Victory! You earned " + result.getCoinsEarned() + " coins!";
            if (result.isEquipmentDropped()) {
                message += "\nEquipment dropped: " + result.getEquipmentEarned();
            }
        } else {
            message += "Defeat! Better luck next time.";
        }
        
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        
        // Disable attack button
        attackButton.setEnabled(false);
        attackButton.setText(result.isVictory() ? "POBEDA!" : "PORAZ!");
    }

}
