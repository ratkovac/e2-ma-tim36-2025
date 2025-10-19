package com.habitrpg.taskmanager.presentation.fragments;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Boss;
import com.habitrpg.taskmanager.data.database.entities.Equipment;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.service.AuthService;
import com.habitrpg.taskmanager.service.BossService;
import com.habitrpg.taskmanager.service.EquipmentService;

import java.util.ArrayList;
import java.util.List;

public class BossFightFragment extends Fragment {

    // UI Components
    private TextView bossHealthText;
    private ProgressBar bossHealthBar;
    private TextView playerPpText;
    private ProgressBar playerPpBar;
    private ImageView bossImage;
    private LinearLayout equipmentContainer;
    private TextView noEquipmentText;
    private TextView attackCounter;
    private TextView successChanceText;
    private MaterialButton attackButton;
    private TextView shakeInstruction;
    private TextView potentialRewardsText;
    private TextView missText;

    // Services
    private BossService bossService;
    private AuthService authService;
    private EquipmentService equipmentService;

    // Equipment bonuses
    private double strengthBonus = 0.0;
    private double attackChanceBonus = 0.0;
    private int extraAttacks = 0;

    private MediaPlayer hitSoundPlayer;

    // Shake sensor
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener shakeListener;
    private long lastShakeTime = 0;
    private static final int SHAKE_THRESHOLD = 15;
    private static final int SHAKE_TIMEOUT = 1000;

    // Boss Fight Data
    private Boss currentBoss;
    private User currentUser;
    private int remainingAttacks = 5;
    private int successChance = 0;
    private static final int MAX_ATTACKS = 5;
    private int levelTransition; // Nivo prelaska (npr. ako prelazimo iz 2 u 3, ovo je 3)

    // Reward calculation constants (matching BossService)
    private static final int BASE_COIN_REWARD = 200;
    private static final double COIN_INCREASE_RATE = 0.20; // 20% increase

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
        equipmentService = EquipmentService.getInstance(requireContext());

        initializeViews(view);
        setupClickListeners();
        initializeShakeSensor();
        loadBossFightData();
    }

    private void initializeViews(View view) {
        bossHealthText = view.findViewById(R.id.boss_health_text);
        bossHealthBar = view.findViewById(R.id.boss_health_bar);
        playerPpText = view.findViewById(R.id.player_pp_text);
        playerPpBar = view.findViewById(R.id.player_pp_bar);
        bossImage = view.findViewById(R.id.boss_image);
        equipmentContainer = view.findViewById(R.id.equipment_container);
        noEquipmentText = view.findViewById(R.id.no_equipment_text);
        attackCounter = view.findViewById(R.id.attack_counter);
        successChanceText = view.findViewById(R.id.success_chance_text);
        attackButton = view.findViewById(R.id.attack_button);
        shakeInstruction = view.findViewById(R.id.shake_instruction);
        potentialRewardsText = view.findViewById(R.id.potential_rewards_text);
        missText = view.findViewById(R.id.miss_text);
        hitSoundPlayer = MediaPlayer.create(requireContext(), R.raw.hit);
    }

    private void setupClickListeners() {
        attackButton.setOnClickListener(v -> {
            // TODO: Implement attack logic
            performAttack();
        });
    }
    
    private void initializeShakeSensor() {
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        shakeListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                
                float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
                
                if (Math.abs(acceleration) > SHAKE_THRESHOLD) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastShakeTime > SHAKE_TIMEOUT) {
                        lastShakeTime = currentTime;
                        onShakeDetected();
                    }
                }
            }
            
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
    }
    
    private void onShakeDetected() {
        // Perform attack when shake is detected
        performAttack();
    }

    private void loadBossFightData() {
        // Load current user
        authService.getCurrentUser(new AuthService.UserCallback() {
            @Override
            public void onUserRetrieved(User user) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentUser = user;
                        levelTransition = user.getLevel(); // Trenutni nivo korisnika

                        loadActiveEquipment();
                    });
                }
            }
        });
    }

    private void loadActiveEquipment() {
        equipmentService.getActiveEquipment(new EquipmentService.EquipmentCallback() {
            @Override
            public void onSuccess(String message, List<Equipment> equipment) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        calculateEquipmentBonuses(equipment);
                        updateEquipmentDisplay();
                        loadCurrentBoss();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Continue without equipment bonuses
                        loadCurrentBoss();
                    });
                }
            }
        });
    }
    
    private void calculateEquipmentBonuses(List<Equipment> equipment) {
        // Reset bonuses
        strengthBonus = 0.0;
        attackChanceBonus = 0.0;
        extraAttacks = 0;

        // Calculate bonuses from active equipment
        for (Equipment item : equipment) {
            if (item.isActive()) {
                switch (item.getBonusType()) {
                    case "strength":
                        strengthBonus += item.getBonusValue();
                        break;
                    case "attack_chance":
                        attackChanceBonus += item.getBonusValue();
                        break;
                    case "extra_attack":
                        // Calculate percentage of base attacks (40% of 5 = 2 extra attacks)
                        int extraAttacksFromItem = (int) Math.round(MAX_ATTACKS * (item.getBonusValue() / 100.0));
                        extraAttacks += extraAttacksFromItem;
                        break;
                }
            }
        }

        // Update max attacks based on extra attacks bonus
        remainingAttacks = MAX_ATTACKS + extraAttacks;
    }

    private void loadCurrentBoss() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "User data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the new method that knows about level transition
        bossService.getBossForLevelTransition(currentUser.getLevel(), new BossService.BossCallback() {
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

        // Update player PP (with equipment bonuses)
        int bonusPP = (int) (currentUser.getPowerPoints() * (strengthBonus / 100.0));
        int totalPP = currentUser.getPowerPoints() + bonusPP;
        playerPpText.setText(totalPP + " PP" + (bonusPP > 0 ? " (+" + bonusPP + ")" : ""));
        playerPpBar.setMax(totalPP + 100); // Set max to current PP + buffer
        playerPpBar.setProgress(totalPP);

        // Update attack counter (with extra attacks)
        int maxAttacks = MAX_ATTACKS + extraAttacks;
        attackCounter.setText(remainingAttacks + " / " + maxAttacks);

        // Update success chance (with attack chance bonus)
        int bonusSuccessChance = (int) attackChanceBonus;
        int totalSuccessChance = successChance + bonusSuccessChance;
        if (totalSuccessChance > 100) {
            totalSuccessChance = 100;
        }
        successChanceText.setText(totalSuccessChance + "%" + (bonusSuccessChance > 0 ? " (+" + bonusSuccessChance + "%)" : ""));

        // Update equipment display with active equipment icons
        updateEquipmentDisplay();

        // Update potential rewards display
        updatePotentialRewards();
    }

    /**
     * Calculate and display potential coin rewards based on boss level
     */
    private void updatePotentialRewards() {
        if (currentBoss == null) {
            return;
        }

        // Calculate potential coin reward using the same formula as BossService
        int bossLevel = currentBoss.getLevel();
        int potentialCoinReward = (int) (BASE_COIN_REWARD * Math.pow(1 + COIN_INCREASE_RATE, bossLevel - 1));

        // Update the rewards text
        String rewardsText = "Potencijalne nagrade:\n+" + potentialCoinReward + " novčića";
        potentialRewardsText.setText(rewardsText);
    }

    /**
     * Update equipment display with active equipment icons
     */
    private void updateEquipmentDisplay() {
        if (equipmentContainer == null || noEquipmentText == null) {
            return;
        }

        // Clear existing equipment icons (but keep the noEquipmentText)
        // Remove all views except noEquipmentText
        for (int i = equipmentContainer.getChildCount() - 1; i >= 0; i--) {
            if (equipmentContainer.getChildAt(i) != noEquipmentText) {
                equipmentContainer.removeViewAt(i);
            }
        }

        // Hide no equipment text initially
        noEquipmentText.setVisibility(View.GONE);

        // Get active equipment and display their icons
        equipmentService.getActiveEquipment(new EquipmentService.EquipmentCallback() {
            @Override
            public void onSuccess(String message, List<Equipment> equipment) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        boolean hasActiveEquipment = false;
                        
                        for (Equipment item : equipment) {
                            if (item.isActive()) {
                                hasActiveEquipment = true;
                                
                                ImageView equipmentIcon = new ImageView(requireContext());
                                equipmentIcon.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ));
                                
                                // Set icon size
                                int iconSize = (int) (32 * getResources().getDisplayMetrics().density);
                                equipmentIcon.getLayoutParams().width = iconSize;
                                equipmentIcon.getLayoutParams().height = iconSize;
                                
                                // Add margin between icons
                                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) equipmentIcon.getLayoutParams();
                                params.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
                                equipmentIcon.setLayoutParams(params);
                                
                                // Set the icon resource
                                int iconResource = getIconResource(item.getIconResource());
                                equipmentIcon.setImageResource(iconResource);
                                equipmentIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                equipmentIcon.setContentDescription(item.getEquipmentName());
                                
                                equipmentContainer.addView(equipmentIcon);
                            }
                        }
                        
                        // Show "Nema opreme" text if no active equipment
                        if (!hasActiveEquipment) {
                            noEquipmentText.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                // Show "Nema opreme" text if there's an error loading equipment
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        noEquipmentText.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
    }

    /**
     * Convert iconResource string to drawable resource ID
     */
    private int getIconResource(String iconResourceName) {
        switch (iconResourceName) {
            case "ic_potion1":
                return R.drawable.ic_potion1;
            case "ic_potion2":
                return R.drawable.ic_potion2;
            case "ic_potion3":
                return R.drawable.ic_potion3;
            case "ic_potion4":
                return R.drawable.ic_potion4;
            case "ic_gloves":
                return R.drawable.ic_gloves;
            case "ic_shield":
                return R.drawable.ic_shield;
            case "ic_boots":
                return R.drawable.ic_boots;
            case "ic_sword":
                return R.drawable.ic_sword;
            case "ic_bow":
                return R.drawable.ic_bow;
            default:
                return R.drawable.ic_potion1; // Default icon
        }
    }

    private void performAttack() {
        if (remainingAttacks <= 0 || currentBoss == null || currentUser == null) {
            return;
        }
        
        remainingAttacks--;
        // Update UI to show decreased attack count
        updateUI();
        // Calculate total power points with equipment bonuses
        int bonusPP = (int) (currentUser.getPowerPoints() * (strengthBonus / 100.0));
        int totalPP = currentUser.getPowerPoints() + bonusPP;

        // Perform actual boss attack with bonus power points
        bossService.performBossAttack(currentBoss.getLevel(), totalPP, new BossService.BossCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        
                        // Check if attack was successful or missed
                        if (message.contains("successful")) {
                            // Play hit sound when attack is successful
                            playHitSound();
                        } else if (message.contains("missed")) {
                            // Play miss sound when attack misses
                            playMissSound();
                        }

                        // Check if boss is defeated or attacks exhausted
                        if (currentBoss != null && currentBoss.isDefeated()) {
                            // Boss defeated - end fight with victory
                            int maxAttacks = MAX_ATTACKS + extraAttacks;
                            bossService.endBossFight(currentBoss, maxAttacks - remainingAttacks, new BossService.BossCallback() {
                                @Override
                                public void onSuccess(String message) {}
                                
                                @Override
                                public void onError(String error) {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "Error ending fight: " + error, Toast.LENGTH_SHORT).show();
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
                        } else if (remainingAttacks <= 0) {
                            // Attacks exhausted - end fight
                            int maxAttacks = MAX_ATTACKS + extraAttacks;
                            bossService.endBossFight(currentBoss, maxAttacks, new BossService.BossCallback() {
                                @Override
                                public void onSuccess(String message) {}
                                
                                @Override
                                public void onError(String error) {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "Error ending fight: " + error, Toast.LENGTH_SHORT).show();
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
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Attack failed: " + error, Toast.LENGTH_SHORT).show();
                        // Play miss sound and animation
                        playMissSound();
                    });
                }
            }
            
            @Override
            public void onBossRetrieved(Boss boss) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentBoss = boss;
                        updateUI();
                    });
                }
            }
            
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
    
    private void playHitSound() {
        if (hitSoundPlayer != null) {
            try {
                // Reset to beginning if already playing
                hitSoundPlayer.seekTo(0);
                hitSoundPlayer.start();
            } catch (Exception e) {
                // Log error but don't crash the app
                e.printStackTrace();
            }
        }
        
        // Animate boss hit effect
        animateBossHit();
    }
    
    private void playMissSound() {
        // For now, just show miss animation
        // TODO: Add miss sound if available
        animateBossMiss();
        animateMissText();
    }

    private void animateBossHit() {
        if (bossImage == null) return;
        
        // Create a red tint effect using ColorMatrix
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0); // Make it grayscale first
        colorMatrix.set(new float[]{
            1.5f, 0f, 0f, 0f, 50f,  // Red channel increased
            0f, 0.3f, 0f, 0f, 0f,   // Green channel reduced
            0f, 0f, 0.3f, 0f, 0f,   // Blue channel reduced
            0f, 0f, 0f, 1f, 0f      // Alpha unchanged
        });
        
        ColorMatrixColorFilter redFilter = new ColorMatrixColorFilter(colorMatrix);
        
        // Create animation that fades the red effect in and out
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f, 0f);
        animator.setDuration(300); // 300ms animation
        
        animator.addUpdateListener(animation -> {
            float progress = (Float) animation.getAnimatedValue();
            
            if (progress > 0) {
                // Apply red tint based on progress
                ColorMatrix currentMatrix = new ColorMatrix();
                currentMatrix.setSaturation(1f - progress * 0.7f); // Reduce saturation
                
                // Add red tint
                float redIntensity = progress * 0.8f;
                currentMatrix.set(new float[]{
                    1f + redIntensity, 0f, 0f, 0f, redIntensity * 50f,
                    0f, 1f - redIntensity * 0.3f, 0f, 0f, 0f,
                    0f, 0f, 1f - redIntensity * 0.3f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                });

                bossImage.setColorFilter(new ColorMatrixColorFilter(currentMatrix));
            } else {
                // Remove color filter when animation ends
                bossImage.clearColorFilter();
            }
        });

        animator.start();
    }

    private void animateBossMiss() {
        if (bossImage == null) return;

        // Create a blue tint effect for miss (different from hit)
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f, 0f);
        animator.setDuration(300); // 300ms animation

        animator.addUpdateListener(animation -> {
            float progress = (Float) animation.getAnimatedValue();

            if (progress > 0) {
                // Apply blue tint based on progress
                ColorMatrix currentMatrix = new ColorMatrix();
                currentMatrix.setSaturation(1f - progress * 0.5f); // Reduce saturation slightly

                // Add blue tint for miss
                float blueIntensity = progress * 0.6f;
                currentMatrix.set(new float[]{
                    1f - blueIntensity * 0.2f, 0f, 0f, 0f, 0f,
                    0f, 1f - blueIntensity * 0.2f, 0f, 0f, 0f,
                    0f, 0f, 1f + blueIntensity, 0f, blueIntensity * 30f,
                    0f, 0f, 0f, 1f, 0f
                });

                bossImage.setColorFilter(new ColorMatrixColorFilter(currentMatrix));
            } else {
                // Remove color filter when animation ends
                bossImage.clearColorFilter();
            }
        });
        
        animator.start();
    }

    private void animateMissText() {
        if (missText == null) return;

        // Make text visible and start animation
        missText.setVisibility(View.VISIBLE);
        missText.setAlpha(0f);

        // Create animation that fades in and out
        ObjectAnimator fadeInAnimator = ObjectAnimator.ofFloat(missText, "alpha", 0f, 1f);
        fadeInAnimator.setDuration(200); // Quick fade in

        ObjectAnimator fadeOutAnimator = ObjectAnimator.ofFloat(missText, "alpha", 1f, 0f);
        fadeOutAnimator.setDuration(800); // Slower fade out
        fadeOutAnimator.setStartDelay(300); // Stay visible for 300ms

        // Chain animations
        fadeInAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                fadeOutAnimator.start();
            }
        });

        fadeOutAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                missText.setVisibility(View.GONE);
            }
        });

        fadeInAnimator.start();
    }

    private void reduceEquipmentDurability() {
        // Get active equipment and reduce durability
        equipmentService.getActiveEquipment(new EquipmentService.EquipmentCallback() {
            @Override
            public void onSuccess(String message, List<Equipment> equipment) {
                if (!equipment.isEmpty()) {
                    // Filter out equipment with durability -1 (forever)
                    List<Equipment> equipmentToReduce = new ArrayList<>();
                    for (Equipment item : equipment) {
                        if (item.getDurability() != -1) {
                            equipmentToReduce.add(item);
                        }
                    }

                    if (!equipmentToReduce.isEmpty()) {
                        equipmentService.reduceEquipmentDurability(equipmentToReduce, new EquipmentService.EquipmentCallback() {
                            @Override
                            public void onSuccess(String message, List<Equipment> updatedEquipment) {
                                // Equipment durability updated successfully
                            }

                            @Override
                            public void onError(String error) {
                                // Log error but don't show to user
                            }
                        });
                    }
                }
            }

            @Override
            public void onError(String error) {
                // Log error but don't show to user
            }
        });
    }

    private void showBossFightResult(BossService.BossFightResult result) {
        // Reduce equipment durability after boss fight
        reduceEquipmentDurability();
        // Show treasure dialog only for complete victory
        if (result.isVictory()) {
            showTreasureDialog(result);
        } else {
            // Show defeat dialog for both defeat and partial victory
            showDefeatDialog(result);
        }
        
        // Disable attack button
        attackButton.setEnabled(false);
        if (result.isVictory()) {
            attackButton.setText("🎉 POBEDA!");
        } else if (result.isPartialVictory()) {
            attackButton.setText("💀 PORAZ (50% nagrada)");
        } else {
            attackButton.setText("💀 PORAZ");
        }
    }
    
    private void showTreasureDialog(BossService.BossFightResult result) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_treasure);
        dialog.setCancelable(false);
        
        ImageView treasureChest = dialog.findViewById(R.id.treasure_chest);
        TextView coinsText = dialog.findViewById(R.id.coins_reward);
        TextView equipmentText = dialog.findViewById(R.id.equipment_reward);
        MaterialButton shakeButton = dialog.findViewById(R.id.shake_button);
        MaterialButton exitButton = dialog.findViewById(R.id.exit_button);
        TextView shakeInstruction = dialog.findViewById(R.id.shake_instruction);
        
        // Set initial closed chest image
        treasureChest.setImageResource(R.drawable.treasure);
        
        // Set rewards text
        coinsText.setText("+" + result.getCoinsEarned() + " novčića");

        // Use equipment drop result from BossService
        if (result.isEquipmentDropped()) {
            equipmentText.setText("Oprema: ??? ✨");
            equipmentText.setVisibility(View.VISIBLE);
        } else {
            equipmentText.setVisibility(View.GONE);
        }
        
        // Hide rewards initially
        coinsText.setVisibility(View.GONE);
        if (result.isEquipmentDropped()) {
            equipmentText.setVisibility(View.GONE);
        }
        
        // Shake button for testing on emulator
        shakeButton.setOnClickListener(v -> openTreasureChest(treasureChest, coinsText, equipmentText, exitButton, dialog));
        
        // Exit button to go back to tasks
        exitButton.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToTasks();
        });
        
        // Start listening for shake
        sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        
        // Store dialog reference for shake detection
        final Dialog finalDialog = dialog;
        final ImageView finalTreasureChest = treasureChest;
        final TextView finalCoinsText = coinsText;
        final TextView finalEquipmentText = equipmentText;
        final MaterialButton finalExitButton = exitButton;
        
        // Override onShakeDetected for this dialog
        shakeListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                
                float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
                
                if (Math.abs(acceleration) > SHAKE_THRESHOLD) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastShakeTime > SHAKE_TIMEOUT) {
                        lastShakeTime = currentTime;
                        openTreasureChest(finalTreasureChest, finalCoinsText, finalEquipmentText, finalExitButton, finalDialog);
                    }
                }
            }
            
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        
        dialog.show();
    }
    
    private void openTreasureChest(ImageView treasureChest, TextView coinsText, TextView equipmentText, MaterialButton exitButton, Dialog dialog) {
        // Stop listening for shake
        sensorManager.unregisterListener(shakeListener);
        
        // Change to open chest image
        treasureChest.setImageResource(R.drawable.treasurechest);
        
        // Show rewards with animation
        coinsText.setVisibility(View.VISIBLE);

        // Add equipment if equipment was dropped
        if (equipmentText.getText().toString().contains("???")) {
            equipmentService.addTreasureEquipment(new EquipmentService.EquipmentCallback() {
                @Override
                public void onSuccess(String message, List<Equipment> equipment) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            equipmentText.setText(message + " ✨");
                            equipmentText.setVisibility(View.VISIBLE);
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            equipmentText.setText("Equipment not found");
                            equipmentText.setVisibility(View.VISIBLE);
                        });
                    }
                }
            });
        }
        
        // Show exit button
        exitButton.setVisibility(View.VISIBLE);
        
        // Animate the rewards appearing
        ObjectAnimator coinsAnimator = ObjectAnimator.ofFloat(coinsText, "alpha", 0f, 1f);
        coinsAnimator.setDuration(500);
        coinsAnimator.start();
        
        if (equipmentText.getVisibility() == View.VISIBLE) {
            ObjectAnimator equipmentAnimator = ObjectAnimator.ofFloat(equipmentText, "alpha", 0f, 1f);
            equipmentAnimator.setDuration(500);
            equipmentAnimator.setStartDelay(200);
            equipmentAnimator.start();
        }
        
        // Animate exit button appearing
        ObjectAnimator exitButtonAnimator = ObjectAnimator.ofFloat(exitButton, "alpha", 0f, 1f);
        exitButtonAnimator.setDuration(500);
        exitButtonAnimator.setStartDelay(400);
        exitButtonAnimator.start();
    }
    
    private void showDefeatDialog(BossService.BossFightResult result) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_defeat);
        dialog.setCancelable(false);
        
        TextView defeatMessage = dialog.findViewById(R.id.defeat_message);
        LinearLayout rewardsContainer = dialog.findViewById(R.id.rewards_container);
        TextView partialCoinsReward = dialog.findViewById(R.id.partial_coins_reward);
        TextView partialEquipmentReward = dialog.findViewById(R.id.partial_equipment_reward);
        TextView damageInfo = dialog.findViewById(R.id.damage_info);
        MaterialButton backButton = dialog.findViewById(R.id.back_button);
        
        // Set defeat message
        defeatMessage.setText("Unfortunately, you have lost!");
        
        // Set damage info
        damageInfo.setText("You dealt " + String.format("%.1f", result.getHpDamagePercentage()) + "% damage to the boss.\nBetter luck next time!");
        
        // Check if user gets partial rewards (50% or more damage)
        if (result.getHpDamagePercentage() >= 50.0f) {
            // Show partial rewards
            rewardsContainer.setVisibility(View.VISIBLE);
            partialCoinsReward.setText("+" + result.getCoinsEarned() + " coins");
            
            if (result.isEquipmentDropped()) {
                partialEquipmentReward.setText("Equipment: " + result.getEquipmentEarned() + " ✨");
                partialEquipmentReward.setVisibility(View.VISIBLE);
            } else {
                partialEquipmentReward.setVisibility(View.GONE);
            }
        } else {
            // No rewards for less than 50% damage
            rewardsContainer.setVisibility(View.GONE);
        }
        
        // Back button to return to tasks
        backButton.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToTasks();
        });
        
        dialog.show();
    }
    
    private void navigateToTasks() {
        if (currentUser == null) {
            // No user data - return to tasks
            if (getActivity() != null && getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
            return;
        }

        int expectedBossLevel = currentUser.getLevel() - 1; // Boss level for this transition

        // Check if there are more bosses to fight
        bossService.getBossForLevelTransition(currentUser.getLevel(), new BossService.BossCallback() {
            @Override
            public void onSuccess(String message) {}

            @Override
            public void onError(String error) {
                // No more bosses - return to tasks
                if (getActivity() != null && getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }
            }

            @Override
            public void onBossRetrieved(Boss boss) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    // Check if this is a different boss than the one we just fought
                    if (currentBoss != null && boss.getId() != currentBoss.getId()) {
                        // There's another boss to fight
                        currentBoss = boss;
                        remainingAttacks = MAX_ATTACKS;

                        // Re-enable attack button
                        attackButton.setEnabled(true);
                        attackButton.setText("⚔");

                        // Update UI with new boss data
                        updateUI();

                        // Reload success rate for the new boss
                        loadSuccessRate();

                        String message = boss.getLevel() < expectedBossLevel ?
                            "Neporaženi boss! Level " + boss.getLevel() :
                            "Novi boss se pojavio! Level " + boss.getLevel();
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    } else {
                        // Same boss or no more bosses - return to tasks
                        if (getParentFragmentManager() != null) {
                            getParentFragmentManager().popBackStack();
                        }
                    }
                });
            }

            @Override
            public void onBossFightResult(BossService.BossFightResult result) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register shake sensor listener
        if (sensorManager != null && shakeListener != null && accelerometer != null) {
            sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister shake sensor listener
        if (sensorManager != null && shakeListener != null) {
            sensorManager.unregisterListener(shakeListener);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (hitSoundPlayer != null) {
            hitSoundPlayer.release();
            hitSoundPlayer = null;
        }
        
        // Unregister shake sensor
        if (sensorManager != null && shakeListener != null) {
            sensorManager.unregisterListener(shakeListener);
        }
    }


}
