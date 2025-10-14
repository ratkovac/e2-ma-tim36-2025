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

import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.presentation.adapters.CategoryAdapter;
import com.habitrpg.taskmanager.presentation.dialogs.CreateCategoryDialog;
import com.habitrpg.taskmanager.service.CategoryService;
import com.habitrpg.taskmanager.databinding.FragmentCategoriesBinding;

import java.util.ArrayList;
import java.util.List;

public class CategoriesFragment extends Fragment {
    
    private FragmentCategoriesBinding binding;
    private CategoryService categoryService;
    private CategoryAdapter categoryAdapter;
    private List<Category> categories;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCategoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        categoryService = CategoryService.getInstance(requireContext());
        categories = new ArrayList<>();
        
        setupClickListeners();
        setupRecyclerView();
        loadCategories();
    }
    
    private void setupClickListeners() {
        binding.btnAddCategory.setOnClickListener(v -> {
            CreateCategoryDialog dialog = CreateCategoryDialog.newInstance(
                categoryService, 
                new CreateCategoryDialog.OnCategoryCreatedListener() {
                    @Override
                    public void onCategoryCreated(Category category) {
                        loadCategories();
                    }
                }
            );
            dialog.show(getParentFragmentManager(), "CreateCategoryDialog");
        });
    }
    
    private void setupRecyclerView() {
        categoryAdapter = new CategoryAdapter(categories, new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(Category category) {
                Toast.makeText(getContext(), "Selected: " + category.getName(), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onCategoryLongClick(Category category) {
                Toast.makeText(getContext(), "Long click: " + category.getName(), Toast.LENGTH_SHORT).show();
            }
        });
        
        binding.recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewCategories.setAdapter(categoryAdapter);
        showLoading(true);
    }
    
    private void loadCategories() {
        categoryService.getAllCategories(new CategoryService.CategoryCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showEmptyState(true);
                        Toast.makeText(getContext(), "Failed to load categories: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onCategoriesRetrieved(List<Category> categories) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        
                        if (categories == null || categories.isEmpty()) {
                            showEmptyState(true);
                        } else {
                            showEmptyState(false);
                            CategoriesFragment.this.categories.clear();
                            CategoriesFragment.this.categories.addAll(categories);
                            categoryAdapter.updateCategories(categories);
                        }
                    });
                }
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
        loadCategories();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
