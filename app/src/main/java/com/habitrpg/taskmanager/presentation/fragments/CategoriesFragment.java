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

import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;
import com.habitrpg.taskmanager.databinding.FragmentCategoriesBinding;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoriesFragment extends Fragment {
    
    private FragmentCategoriesBinding binding;
    private AppDatabase database;
    private UserPreferences userPreferences;
    private ExecutorService executor;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCategoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        database = AppDatabase.getDatabase(requireContext());
        userPreferences = UserPreferences.getInstance(requireContext());
        executor = Executors.newFixedThreadPool(2);
        
        setupClickListeners();
        setupRecyclerView();
        loadCategories();
    }
    
    private void setupClickListeners() {
        binding.btnAddCategory.setOnClickListener(v -> {
            // TODO: Show add category dialog
            Toast.makeText(getContext(), "Add category feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void setupRecyclerView() {
        binding.recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        showLoading(true);
    }
    
    private void loadCategories() {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            showEmptyState(true);
            return;
        }
        
        executor.execute(() -> {
            List<Category> categories = database.categoryDao().getCategoriesByUserId(userId);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    
                    if (categories == null || categories.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        showEmptyState(false);
                        // TODO: Set up adapter with categories
                        // CategoryAdapter adapter = new CategoryAdapter(categories);
                        // binding.recyclerViewCategories.setAdapter(adapter);
                    }
                });
            }
        });
    }
    
    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recyclerViewCategories.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.layoutEmptyState.setVisibility(View.GONE);
    }
    
    private void showEmptyState(boolean show) {
        binding.layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recyclerViewCategories.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh categories when fragment becomes visible
        loadCategories();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
