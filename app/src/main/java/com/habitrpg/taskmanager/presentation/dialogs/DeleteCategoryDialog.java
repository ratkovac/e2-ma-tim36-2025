package com.habitrpg.taskmanager.presentation.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.service.CategoryService;

public class DeleteCategoryDialog extends DialogFragment {
    
    private static final String ARG_CATEGORY_ID = "category_id";
    private static final String ARG_CATEGORY_NAME = "category_name";
    
    private long categoryId;
    private String categoryName;
    private CategoryService categoryService;
    private OnCategoryDeletedListener listener;
    
    private TextView tvCategoryName;
    private TextView tvWarningMessage;
    private Button btnDelete;
    private Button btnCancel;
    
    public interface OnCategoryDeletedListener {
        void onCategoryDeleted(Category category);
    }
    
    public static DeleteCategoryDialog newInstance(Category category, CategoryService categoryService, OnCategoryDeletedListener listener) {
        DeleteCategoryDialog dialog = new DeleteCategoryDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_CATEGORY_ID, category.getId());
        args.putString(ARG_CATEGORY_NAME, category.getName());
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
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_delete_category, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        tvCategoryName = view.findViewById(R.id.tvCategoryName);
        tvWarningMessage = view.findViewById(R.id.tvWarningMessage);
        btnDelete = view.findViewById(R.id.btnDelete);
        btnCancel = view.findViewById(R.id.btnCancel);
        
        // Populate fields
        tvCategoryName.setText(categoryName);
        tvWarningMessage.setText("Are you sure you want to delete this category? This action cannot be undone.");
        
        setupClickListeners();
    }
    
    private void setupClickListeners() {
        btnDelete.setOnClickListener(v -> deleteCategory());
        
        btnCancel.setOnClickListener(v -> dismiss());
    }
    
    private void deleteCategory() {
        categoryService.deleteCategory(categoryId, new CategoryService.CategoryCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Category deleted successfully", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            // Create a temporary category object for the callback
                            Category tempCategory = new Category();
                            tempCategory.setId((int) categoryId);
                            tempCategory.setName(categoryName);
                            listener.onCategoryDeleted(tempCategory);
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
