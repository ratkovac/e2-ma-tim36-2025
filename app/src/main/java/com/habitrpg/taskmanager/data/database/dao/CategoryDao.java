package com.habitrpg.taskmanager.data.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.habitrpg.taskmanager.data.database.entities.Category;
import java.util.List;

@Dao
public interface CategoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertCategory(Category category);
    
    @Update
    void updateCategory(Category category);
    
    @Delete
    void deleteCategory(Category category);
    
    @Query("SELECT * FROM categories WHERE user_id = :userId ORDER BY created_at ASC")
    List<Category> getCategoriesByUserId(String userId);
    
    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    Category getCategoryById(int categoryId);
    
    @Query("SELECT * FROM categories WHERE user_id = :userId AND color = :color LIMIT 1")
    Category getCategoryByColor(String userId, String color);
    
    @Query("SELECT * FROM categories WHERE user_id = :userId AND name = :name LIMIT 1")
    Category getCategoryByName(String userId, String name);
    
    @Query("DELETE FROM categories WHERE user_id = :userId")
    void deleteAllCategoriesForUser(String userId);
}
