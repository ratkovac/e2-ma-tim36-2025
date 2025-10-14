package com.habitrpg.taskmanager.presentation.dialogs;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.service.CategoryService;

import java.util.List;

public class CreateCategoryDialog extends DialogFragment {
    
    private CategoryService categoryService;
    private OnCategoryCreatedListener listener;
    private String selectedColor = "#4CAF50";
    
    public interface OnCategoryCreatedListener {
        void onCategoryCreated(Category category);
    }
    
    public static CreateCategoryDialog newInstance(CategoryService categoryService, OnCategoryCreatedListener listener) {
        CreateCategoryDialog dialog = new CreateCategoryDialog();
        dialog.categoryService = categoryService;
        dialog.listener = listener;
        return dialog;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_create_category, null);
        
        EditText etCategoryName = view.findViewById(R.id.etCategoryName);
        GridLayout colorGrid = view.findViewById(R.id.colorGrid);
        
        setupColorSelection(colorGrid);
        
        builder.setView(view)
                .setTitle("Create New Category")
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etCategoryName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(getContext(), "Category name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    createCategory(name, selectedColor);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dismiss());
        
        return builder.create();
    }
    
    private void setupColorSelection(GridLayout colorGrid) {
        String[] colors = {
            "#4CAF50", "#2196F3", "#FF9800", "#9C27B0",
            "#F44336", "#00BCD4", "#8BC34A", "#795548",
            "#607D8B", "#E91E63", "#3F51B5", "#FFC107"
        };
        
        for (String colorHex : colors) {
            View colorView = new View(requireContext());
            colorView.setBackgroundColor(Color.parseColor(colorHex));
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 80;
            params.height = 80;
            params.setMargins(8, 8, 8, 8);
            colorView.setLayoutParams(params);
            
            colorView.setOnClickListener(v -> {
                selectedColor = colorHex;
                updateColorSelection(colorGrid, colorView);
            });
            
            colorGrid.addView(colorView);
        }
        
        // Select first color by default
        if (colorGrid.getChildCount() > 0) {
            updateColorSelection(colorGrid, colorGrid.getChildAt(0));
        }
    }
    
    private void updateColorSelection(GridLayout colorGrid, View selectedView) {
        for (int i = 0; i < colorGrid.getChildCount(); i++) {
            View child = colorGrid.getChildAt(i);
            if (child == selectedView) {
                child.setAlpha(1.0f);
                child.setScaleX(1.2f);
                child.setScaleY(1.2f);
            } else {
                child.setAlpha(0.7f);
                child.setScaleX(1.0f);
                child.setScaleY(1.0f);
            }
        }
    }
    
    private void createCategory(String name, String color) {
        String userId = categoryService.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Category category = new Category(userId, name, color);
        
        categoryService.createCategory(category, new CategoryService.CategoryCallback() {
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Category created successfully!", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onCategoryCreated(category);
                        }
                        dismiss();
                    });
                }
            }
            
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to create category: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            public void onCategoryRetrieved(Category category) {}
            
            public void onCategoriesRetrieved(List<Category> categories) {}
        });
    }
}
