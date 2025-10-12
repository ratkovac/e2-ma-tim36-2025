package com.habitrpg.taskmanager.data.repository;

import android.content.Context;
import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryRepository {
    
    private static CategoryRepository instance;
    private AppDatabase database;
    private UserPreferences userPreferences;
    private ExecutorService executor;
    
    private CategoryRepository(Context context) {
        database = AppDatabase.getDatabase(context);
        userPreferences = UserPreferences.getInstance(context);
        executor = Executors.newFixedThreadPool(2);
    }
    
    public static synchronized CategoryRepository getInstance(Context context) {
        if (instance == null) {
            instance = new CategoryRepository(context.getApplicationContext());
        }
        return instance;
    }
    
    public void insertCategory(Category category, CategoryCallback callback) {
        executor.execute(() -> {
            try {
                database.categoryDao().insertCategory(category);
                callback.onSuccess("Category created successfully");
            } catch (Exception e) {
                callback.onError("Failed to create category: " + e.getMessage());
            }
        });
    }
    
    public void updateCategory(Category category, CategoryCallback callback) {
        executor.execute(() -> {
            try {
                database.categoryDao().updateCategory(category);
                callback.onSuccess("Category updated successfully");
            } catch (Exception e) {
                callback.onError("Failed to update category: " + e.getMessage());
            }
        });
    }
    
    public void deleteCategory(long categoryId, CategoryCallback callback) {
        executor.execute(() -> {
            try {
                Category category = database.categoryDao().getCategoryById((int) categoryId);
                if (category != null) {
                    database.categoryDao().deleteCategory(category);
                    callback.onSuccess("Category deleted successfully");
                } else {
                    callback.onError("Category not found");
                }
            } catch (Exception e) {
                callback.onError("Failed to delete category: " + e.getMessage());
            }
        });
    }
    
    public void getCategoryById(long categoryId, CategoryCallback callback) {
        executor.execute(() -> {
            try {
                Category category = database.categoryDao().getCategoryById((int) categoryId);
                callback.onCategoryRetrieved(category);
            } catch (Exception e) {
                callback.onError("Failed to get category: " + e.getMessage());
            }
        });
    }
    
    public void getCategoriesByUserId(String userId, CategoryCallback callback) {
        executor.execute(() -> {
            try {
                List<Category> categories = database.categoryDao().getCategoriesByUserId(userId);
                callback.onCategoriesRetrieved(categories);
            } catch (Exception e) {
                callback.onError("Failed to get categories: " + e.getMessage());
            }
        });
    }
    
    public interface CategoryCallback {
        void onSuccess(String message);
        void onError(String error);
        void onCategoryRetrieved(Category category);
        void onCategoriesRetrieved(List<Category> categories);
    }
}
