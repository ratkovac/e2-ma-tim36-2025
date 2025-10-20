package com.habitrpg.taskmanager.service;

import android.content.Context;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;
import com.habitrpg.taskmanager.data.repository.CategoryRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryService {

    private static CategoryService instance;
    private CategoryRepository categoryRepository;
    private UserPreferences userPreferences;
    private ExecutorService executor;

    private CategoryService(Context context) {
        categoryRepository = CategoryRepository.getInstance(context);
        userPreferences = UserPreferences.getInstance(context);
        executor = Executors.newFixedThreadPool(2);
    }

    public static synchronized CategoryService getInstance(Context context) {
        if (instance == null) {
            instance = new CategoryService(context.getApplicationContext());
        }
        return instance;
    }

    public void getAllCategories(CategoryCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        categoryRepository.getCategoriesByUserId(userId, new CategoryRepository.CategoryCallback() {
            @Override
            public void onSuccess(String message) {}

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onCategoryRetrieved(Category category) {}

            @Override
            public void onCategoriesRetrieved(List<Category> categories) {
                callback.onCategoriesRetrieved(categories);
            }
        });
    }

    public String getCurrentUserId() {
        return userPreferences.getCurrentUserId();
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    public void createCategory(Category category, CategoryCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        categoryRepository.insertCategory(category, new CategoryRepository.CategoryCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onCategoryRetrieved(Category category) {}

            @Override
            public void onCategoriesRetrieved(List<Category> categories) {}
        });
    }

    public void updateCategory(Category category, CategoryCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        if (!category.getUserId().equals(userId)) {
            callback.onError("Unauthorized to update this category");
            return;
        }

        // Enforce unique color per user on update (allow same category to keep its color)
        categoryRepository.getCategoryByColor(userId, category.getColor(), new CategoryRepository.CategoryCallback() {
            @Override
            public void onSuccess(String message) {}

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onCategoryRetrieved(Category existingByColor) {
                if (existingByColor != null && existingByColor.getId() != category.getId()) {
                    callback.onError("Color already in use by another category");
                    return;
                }

                categoryRepository.updateCategory(category, new CategoryRepository.CategoryCallback() {
                    @Override
                    public void onSuccess(String message) {
                        callback.onSuccess(message);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }

                    @Override
                    public void onCategoryRetrieved(Category category) {}

                    @Override
                    public void onCategoriesRetrieved(List<Category> categories) {}
                });
            }

            @Override
            public void onCategoriesRetrieved(List<Category> categories) {}
        });
    }

    public void deleteCategory(long categoryId, CategoryCallback callback) {
        String userId = userPreferences.getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        categoryRepository.deleteCategory(categoryId, new CategoryRepository.CategoryCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }

            @Override
            public void onCategoryRetrieved(Category category) {}

            @Override
            public void onCategoriesRetrieved(List<Category> categories) {}
        });
    }

    public interface CategoryCallback {
        void onSuccess(String message);
        void onError(String error);
        void onCategoriesRetrieved(List<Category> categories);
    }
}


