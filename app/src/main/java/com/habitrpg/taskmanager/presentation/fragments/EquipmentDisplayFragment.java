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
import com.habitrpg.taskmanager.data.database.entities.Equipment;
import com.habitrpg.taskmanager.databinding.FragmentEquipmentDisplayBinding;
import com.habitrpg.taskmanager.presentation.adapters.EquipmentDisplayAdapter;
import com.habitrpg.taskmanager.service.EquipmentService;
import com.habitrpg.taskmanager.service.UserPreferences;

import java.util.ArrayList;
import java.util.List;

public class EquipmentDisplayFragment extends Fragment {

    private FragmentEquipmentDisplayBinding binding;
    private EquipmentService equipmentService;
    private List<Equipment> userEquipment = new ArrayList<>();
    private EquipmentDisplayAdapter equipmentAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEquipmentDisplayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        equipmentService = EquipmentService.getInstance(requireContext());
        
        setupRecyclerView();
        loadUserEquipment();
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
        });
        binding.recyclerViewEquipment.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewEquipment.setAdapter(equipmentAdapter);
    }

    private void loadUserEquipment() {
        String currentUserId = UserPreferences.getInstance(requireContext()).getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        equipmentService.getUserEquipment(new EquipmentService.EquipmentCallback() {
            @Override
            public void onSuccess(String message, List<Equipment> equipment) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        userEquipment.clear();
                        userEquipment.addAll(equipment);
                        equipmentAdapter.notifyDataSetChanged();
                        
                        if (equipment.isEmpty()) {
                            binding.textViewNoEquipment.setVisibility(View.VISIBLE);
                            binding.recyclerViewEquipment.setVisibility(View.GONE);
                        } else {
                            binding.textViewNoEquipment.setVisibility(View.GONE);
                            binding.recyclerViewEquipment.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to load equipment: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void activateEquipment(Equipment equipment) {
        equipmentService.activateEquipment(equipment.getEquipmentId(), new EquipmentService.EquipmentCallback() {
            @Override
            public void onSuccess(String message, List<Equipment> equipment) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Equipment activated!", Toast.LENGTH_SHORT).show();
                        loadUserEquipment(); // Refresh the list
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to activate equipment: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void deactivateEquipment(Equipment equipment) {
        equipmentService.deactivateEquipment(equipment.getEquipmentId(), new EquipmentService.EquipmentCallback() {
            @Override
            public void onSuccess(String message, List<Equipment> equipment) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Equipment deactivated!", Toast.LENGTH_SHORT).show();
                        loadUserEquipment(); // Refresh the list
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to deactivate equipment: " + error, Toast.LENGTH_SHORT).show();
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
}
