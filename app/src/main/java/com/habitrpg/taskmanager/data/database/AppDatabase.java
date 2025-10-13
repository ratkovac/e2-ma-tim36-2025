package com.habitrpg.taskmanager.data.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.habitrpg.taskmanager.data.database.dao.CategoryDao;
import com.habitrpg.taskmanager.data.database.dao.TaskCompletionDao;
import com.habitrpg.taskmanager.data.database.dao.TaskDao;
import com.habitrpg.taskmanager.data.database.dao.UserDao;
import com.habitrpg.taskmanager.data.database.dao.UserStatisticsDao;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.data.database.entities.TaskCompletion;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.data.database.entities.UserStatistics;

@Database(
    entities = {User.class, Category.class, Task.class, TaskCompletion.class, UserStatistics.class},
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    
    private static volatile AppDatabase INSTANCE;
    
    public abstract UserDao userDao();
    public abstract CategoryDao categoryDao();
    public abstract TaskDao taskDao();
    public abstract TaskCompletionDao taskCompletionDao();
    public abstract UserStatisticsDao userStatisticsDao();
    
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "habit_rpg_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
    
    public static void destroyInstance() {
        INSTANCE = null;
    }
}
