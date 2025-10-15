package com.habitrpg.taskmanager.presentation.dialogs;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.service.CategoryService;

public class EditCategoryDialog extends DialogFragment {
    
    private CategoryService categoryService;
    private OnCategoryUpdatedListener listener;
    private String selectedColor;
    private Category category;
    
    public interface OnCategoryUpdatedListener {
        void onCategoryUpdated(Category category);
    }
    
    public static EditCategoryDialog newInstance(CategoryService categoryService, 
                                                Category category,
                                                OnCategoryUpdatedListener listener) {
        EditCategoryDialog dialog = new EditCategoryDialog();
        dialog.categoryService = categoryService;
        dialog.category = category;
        dialog.selectedColor = category.getColor();
        dialog.listener = listener;
        return dialog;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_category, null);
        
        TextInputEditText etCategoryName = view.findViewById(R.id.etCategoryName);
        GridLayout colorGrid = view.findViewById(R.id.colorGrid);
        
        // Pre-populate with current values
        etCategoryName.setText(category.getName());
        setupColorSelection(colorGrid);
        
        builder.setView(view)
                .setTitle("Edit Category")
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etCategoryName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(getContext(), "Category name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    updateCategory(name, selectedColor);
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
        
        // Select current color by default
        updateColorSelection(colorGrid, null);
    }
    
    private void updateColorSelection(GridLayout colorGrid, View selectedView) {
        for (int i = 0; i < colorGrid.getChildCount(); i++) {
            View child = colorGrid.getChildAt(i);
            String childColor = getColorFromView(child);
            
            if (childColor.equals(selectedColor)) {
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
    
    private String getColorFromView(View view) {
        // Convert the background color back to hex string
        int color = ((android.graphics.drawable.ColorDrawable) view.getBackground()).getColor();
        return String.format("#%06X", (0xFFFFFF & color));
    }
    
    private void updateCategory(String name, String color) {
        // Create updated category
        Category updatedCategory = new Category();
        updatedCategory.setId(category.getId());
        updatedCategory.setName(name);
        updatedCategory.setColor(color);
        updatedCategory.setUserId(categoryService.getCurrentUserId());
        
        categoryService.updateCategory(updatedCategory, new CategoryService.CategoryCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Category updated successfully!", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onCategoryUpdated(updatedCategory);
                        }
                        dismiss();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to update category: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onCategoriesRetrieved(java.util.List<Category> categories) {}
        });
    }
}
