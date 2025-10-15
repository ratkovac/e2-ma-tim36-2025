package com.habitrpg.taskmanager.presentation.dialogs;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.service.CategoryService;

public class EditCategoryDialog extends DialogFragment {
    
    private static final String ARG_CATEGORY_ID = "category_id";
    private static final String ARG_CATEGORY_NAME = "category_name";
    private static final String ARG_CATEGORY_COLOR = "category_color";
    
    private long categoryId;
    private String categoryName;
    private String categoryColor;
    private CategoryService categoryService;
    private OnCategoryUpdatedListener listener;
    
    private EditText etCategoryName;
    private View colorIndicator;
    private Button btnSelectColor;
    private Button btnSave;
    private Button btnCancel;
    
    private String selectedColor;
    
    public interface OnCategoryUpdatedListener {
        void onCategoryUpdated(Category category);
    }
    
    public static EditCategoryDialog newInstance(Category category, CategoryService categoryService, OnCategoryUpdatedListener listener) {
        EditCategoryDialog dialog = new EditCategoryDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_CATEGORY_ID, category.getId());
        args.putString(ARG_CATEGORY_NAME, category.getName());
        args.putString(ARG_CATEGORY_COLOR, category.getColor());
        dialog.setArguments(args);
        dialog.categoryService = categoryService;
        dialog.listener = listener;
        return dialog;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryId = getArguments().getLong(ARG_CATEGORY_ID);
            categoryName = getArguments().getString(ARG_CATEGORY_NAME);
            categoryColor = getArguments().getString(ARG_CATEGORY_COLOR);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_category, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        etCategoryName = view.findViewById(R.id.etCategoryName);
        colorIndicator = view.findViewById(R.id.colorIndicator);
        btnSelectColor = view.findViewById(R.id.btnSelectColor);
        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);
        
        // Populate fields
        etCategoryName.setText(categoryName);
        selectedColor = categoryColor;
        updateColorIndicator();
        
        setupClickListeners();
    }
    
    private void setupClickListeners() {
        btnSelectColor.setOnClickListener(v -> showColorPickerDialog());
        
        btnSave.setOnClickListener(v -> saveCategory());
        
        btnCancel.setOnClickListener(v -> dismiss());
    }
    
    private void showColorPickerDialog() {
        ColorPickerDialog colorDialog = ColorPickerDialog.newInstance(selectedColor, new ColorPickerDialog.OnColorSelectedListener() {
            @Override
            public void onColorSelected(String color) {
                selectedColor = color;
                updateColorIndicator();
            }
        });
        colorDialog.show(getParentFragmentManager(), "ColorPickerDialog");
    }
    
    private void updateColorIndicator() {
        try {
            int color = Color.parseColor(selectedColor);
            colorIndicator.setBackgroundColor(color);
        } catch (IllegalArgumentException e) {
            colorIndicator.setBackgroundColor(Color.GRAY);
        }
    }
    
    private void saveCategory() {
        String name = etCategoryName.getText().toString().trim();
        
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Category name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedColor == null || selectedColor.isEmpty()) {
            Toast.makeText(getContext(), "Please select a color", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create updated category
        Category updatedCategory = new Category();
        updatedCategory.setId((int) categoryId);
        updatedCategory.setName(name);
        updatedCategory.setColor(selectedColor);
        updatedCategory.setUserId(categoryService.getCurrentUserId()); // Set userId
        
        categoryService.updateCategory(updatedCategory, new CategoryService.CategoryCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Category updated successfully", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onCategoriesRetrieved(java.util.List<Category> categories) {}
        });
    }
}
