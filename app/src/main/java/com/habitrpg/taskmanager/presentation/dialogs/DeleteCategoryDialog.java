package com.habitrpg.taskmanager.presentation.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.service.CategoryService;

public class DeleteCategoryDialog extends DialogFragment {
    
    private CategoryService categoryService;
    private OnCategoryDeletedListener listener;
    private Category category;
    
    public interface OnCategoryDeletedListener {
        void onCategoryDeleted(Category category);
    }
    
    public static DeleteCategoryDialog newInstance(CategoryService categoryService, 
                                                  Category category,
                                                  OnCategoryDeletedListener listener) {
        DeleteCategoryDialog dialog = new DeleteCategoryDialog();
        dialog.categoryService = categoryService;
        dialog.category = category;
        dialog.listener = listener;
        return dialog;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        
        builder.setTitle("Delete Category")
                .setMessage("Are you sure you want to delete the category \"" + category.getName() + "\"?\n\n" +
                           "This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteCategory();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dismiss());
        
        return builder.create();
    }
    
    private void deleteCategory() {
        categoryService.deleteCategory(category.getId(), new CategoryService.CategoryCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Category deleted successfully!", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onCategoryDeleted(category);
                        }
                        dismiss();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to delete category: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onCategoriesRetrieved(java.util.List<Category> categories) {}
        });
    }
}
