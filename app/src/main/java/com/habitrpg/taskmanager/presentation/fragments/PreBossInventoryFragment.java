package com.habitrpg.taskmanager.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Equipment;
import com.habitrpg.taskmanager.presentation.adapters.EquipmentDisplayAdapter;
import com.habitrpg.taskmanager.service.EquipmentService;

import java.util.ArrayList;
import java.util.List;

public class PreBossInventoryFragment extends Fragment {

    private RecyclerView equipmentRecyclerView;
    private TextView emptyStateText;
    private MaterialButton startBossFightButton;
    
    private EquipmentService equipmentService;
    private EquipmentDisplayAdapter equipmentAdapter;
    private List<Equipment> userEquipment = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pre_boss_inventory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        equipmentService = EquipmentService.getInstance(requireContext());
        
        initializeViews(view);
        setupRecyclerView();
        loadEquipment();
    }

    private void initializeViews(View view) {
        equipmentRecyclerView = view.findViewById(R.id.equipment_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        startBossFightButton = view.findViewById(R.id.start_boss_fight_button);
        
        startBossFightButton.setOnClickListener(v -> startBossFight());
    }

    private void setupRecyclerView() {
        equipmentAdapter = new EquipmentDisplayAdapter(userEquipment, new EquipmentDisplayAdapter.EquipmentClickListener() {
            @Override
            public void onActivateClick(Equipment equipment) {
                activateEquipment(equipment);
            }

            @Override
            public void onDeactivateClick(Equipment equipment) {
                deactivateEquipment(equipment);
            }

            @Override
            public void onUpgradeClick(Equipment equipment) {
                upgradeEquipment(equipment);
            }
        });
        
        equipmentRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        equipmentRecyclerView.setAdapter(equipmentAdapter);
    }

    private void loadEquipment() {
        equipmentService.getUserEquipment(new EquipmentService.EquipmentCallback() {
            @Override
            public void onSuccess(String message, List<Equipment> equipment) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        userEquipment.clear();
                        if (equipment != null && !equipment.isEmpty()) {
                            userEquipment.addAll(equipment);
                            equipmentRecyclerView.setVisibility(View.VISIBLE);
                            emptyStateText.setVisibility(View.GONE);
                        } else {
                            equipmentRecyclerView.setVisibility(View.GONE);
                            emptyStateText.setVisibility(View.VISIBLE);
                        }
                        equipmentAdapter.notifyDataSetChanged();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error loading equipment: " + error, Toast.LENGTH_SHORT).show();
                        equipmentRecyclerView.setVisibility(View.GONE);
                        emptyStateText.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
    }

    private void activateEquipment(Equipment equipment) {
        equipmentService.activateEquipment(equipment.getEquipmentId(), 
            new EquipmentService.EquipmentCallback() {
                @Override
                public void onSuccess(String message, List<Equipment> updatedEquipment) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            loadEquipment();
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

    private void deactivateEquipment(Equipment equipment) {
        equipmentService.deactivateEquipment(equipment.getEquipmentId(), 
            new EquipmentService.EquipmentCallback() {
                @Override
                public void onSuccess(String message, List<Equipment> updatedEquipment) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            loadEquipment();
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

    private void upgradeEquipment(Equipment equipment) {
        equipmentService.upgradeEquipment(equipment.getEquipmentId(), new EquipmentService.EquipmentCallback() {
            @Override
            public void onSuccess(String message, List<Equipment> updatedEquipment) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        loadEquipment();
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

    private void startBossFight() {
        androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.navigation_tasks, false)
            .build();
        Navigation.findNavController(requireView()).navigate(R.id.navigation_boss_fight, null, navOptions);
    }
}

