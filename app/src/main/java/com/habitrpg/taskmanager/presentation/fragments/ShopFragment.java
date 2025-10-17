package com.habitrpg.taskmanager.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.databinding.FragmentShopBinding;
import com.habitrpg.taskmanager.presentation.adapters.ShopAdapter;
import com.habitrpg.taskmanager.service.EquipmentService;
import com.habitrpg.taskmanager.service.UserPreferences;
import com.habitrpg.taskmanager.data.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class ShopFragment extends Fragment {

    private FragmentShopBinding binding;
    private EquipmentService equipmentService;
    private UserRepository userRepository;
    private User currentUser;
    private List<ShopItem> shopItems = new ArrayList<>();
    private ShopAdapter shopAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentShopBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        equipmentService = EquipmentService.getInstance(requireContext());
        userRepository = UserRepository.getInstance(requireContext());
        
        setupRecyclerView();
        loadCurrentUser();
        initializeShopItems();
    }

    private void setupRecyclerView() {
        shopAdapter = new ShopAdapter(shopItems, new ShopAdapter.ShopItemClickListener() {
            @Override
            public void onItemClick(ShopItem item) {
                purchaseItem(item);
            }
        });
        binding.recyclerViewShop.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewShop.setAdapter(shopAdapter);
    }

    private void loadCurrentUser() {
        String currentUserId = UserPreferences.getInstance(requireContext()).getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error loading user: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onUserRetrieved(User user) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentUser = user;
                        updateCoinsDisplay();
                    });
                }
            }
        });
    }

    private void updateCoinsDisplay() {
        if (currentUser != null) {
            binding.textViewCoins.setText("Coins: " + currentUser.getCoins());
        }
    }

    private void initializeShopItems() {
        shopItems.clear();
        
        // Potions
        shopItems.add(new ShopItem("Strength Potion (+20% PP)", "Single-use strength potion that increases PP by 20%", 
                50, "ic_potion1", "potion", "strength", 20.0, "single_use"));
        shopItems.add(new ShopItem("Power Potion (+40% PP)", "Single-use power potion that increases PP by 40%", 
                70, "ic_potion2", "potion", "strength", 40.0, "single_use"));
        shopItems.add(new ShopItem("Permanent Strength (+5%)", "Permanent strength increase of 5%", 
                200, "ic_potion3", "potion", "strength", 5.0, "permanent"));
        shopItems.add(new ShopItem("Permanent Power (+10%)", "Permanent power increase of 10%", 
                1000, "ic_potion4", "potion", "strength", 10.0, "permanent"));
        
        // Clothing
        shopItems.add(new ShopItem("Power Gloves", "Gloves that increase strength by 10%", 
                60, "ic_gloves", "clothing", "strength", 10.0, "permanent"));
        shopItems.add(new ShopItem("Defense Shield", "Shield that increases attack success chance by 10%", 
                60, "ic_shield", "clothing", "attack_chance", 10.0, "permanent"));
        shopItems.add(new ShopItem("Speed Boots", "Boots that give 40% chance for extra attack", 
                80, "ic_boots", "clothing", "extra_attack", 40.0, "permanent"));
        
        shopAdapter.notifyDataSetChanged();
    }

    private void purchaseItem(ShopItem item) {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser.getCoins() < item.getPrice()) {
            Toast.makeText(requireContext(), "Not enough coins! You have " + currentUser.getCoins() + 
                    " coins, but need " + item.getPrice() + " coins.", Toast.LENGTH_LONG).show();
            return;
        }

        equipmentService.purchaseEquipment(item.getName(), item.getType(), item.getDescription(),
                item.getPrice(), item.getIconResource(), item.getBonusType(), item.getBonusValue(),
                item.getBonusDuration(), new EquipmentService.EquipmentCallback() {
                    @Override
                    public void onSuccess(String message, List<com.habitrpg.taskmanager.data.database.entities.Equipment> equipment) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Successfully purchased " + item.getName() + "!", Toast.LENGTH_SHORT).show();
                                loadCurrentUser(); // Refresh user data to update coins
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Purchase failed: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static class ShopItem {
        private String name;
        private String description;
        private int price;
        private String iconResource;
        private String type;
        private String bonusType;
        private double bonusValue;
        private String bonusDuration;

        public ShopItem(String name, String description, int price, String iconResource, 
                       String type, String bonusType, double bonusValue, String bonusDuration) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.iconResource = iconResource;
            this.type = type;
            this.bonusType = bonusType;
            this.bonusValue = bonusValue;
            this.bonusDuration = bonusDuration;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getPrice() { return price; }
        public String getIconResource() { return iconResource; }
        public String getType() { return type; }
        public String getBonusType() { return bonusType; }
        public double getBonusValue() { return bonusValue; }
        public String getBonusDuration() { return bonusDuration; }
    }
}
