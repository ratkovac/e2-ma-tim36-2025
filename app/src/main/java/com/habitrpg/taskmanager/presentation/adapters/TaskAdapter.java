package com.habitrpg.taskmanager.presentation.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.service.CategoryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    
    private List<Task> tasks;
    private OnTaskClickListener listener;
    private Map<Integer, Category> categories;
    private Context context;
    
    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskComplete(Task task);
        void onTaskEdit(Task task);
        void onTaskDelete(Task task);
    }
    
    public TaskAdapter(Context context, List<Task> tasks, OnTaskClickListener listener) {
        this.context = context;
        this.tasks = tasks;
        this.listener = listener;
        this.categories = new HashMap<>();
        loadCategories();
    }
    
    
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task);
    }
    
    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }
    
    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }
    
    private void loadCategories() {
        CategoryService categoryService = CategoryService.getInstance(context);
        categoryService.getAllCategories(new CategoryService.CategoryCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {}
            
            @Override
            public void onCategoriesRetrieved(List<Category> categoryList) {
                categories.clear();
                for (Category category : categoryList) {
                    categories.put(category.getId(), category);
                }
                notifyDataSetChanged();
            }
        });
    }
    
    class TaskViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTaskName;
        private TextView tvTaskDescription;
        private TextView tvTaskDifficulty;
        private TextView tvTaskImportance;
        private TextView tvTaskXP;
        private TextView tvTaskTime;
        private TextView tvTaskStatus;
        private TextView tvTaskCategory;
        private TextView tvTaskDate;
        private TextView tvRecurringIndicator;
        private View categoryColorIndicator;
        private TextView btnComplete;
        private TextView btnEdit;
        private TextView btnDelete;
        
        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvTaskDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvTaskDifficulty = itemView.findViewById(R.id.tvTaskDifficulty);
            tvTaskImportance = itemView.findViewById(R.id.tvTaskImportance);
            tvTaskXP = itemView.findViewById(R.id.tvTaskXP);
            tvTaskTime = itemView.findViewById(R.id.tvTaskTime);
            tvTaskStatus = itemView.findViewById(R.id.tvTaskStatus);
            tvTaskCategory = itemView.findViewById(R.id.tvTaskCategory);
            tvTaskDate = itemView.findViewById(R.id.tvTaskDate);
            tvRecurringIndicator = itemView.findViewById(R.id.tvRecurringIndicator);
            categoryColorIndicator = itemView.findViewById(R.id.categoryColorIndicator);
            btnComplete = itemView.findViewById(R.id.btnComplete);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onTaskClick(tasks.get(position));
                    }
                }
            });
            
            btnComplete.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onTaskComplete(tasks.get(position));
                    }
                }
            });
            
            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onTaskEdit(tasks.get(position));
                    }
                }
            });
            
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onTaskDelete(tasks.get(position));
                    }
                }
            });
        }
        
        public void bind(Task task) {
            tvTaskName.setText(task.getName());
            
            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                tvTaskDescription.setText(task.getDescription());
                tvTaskDescription.setVisibility(View.VISIBLE);
            } else {
                tvTaskDescription.setVisibility(View.GONE);
            }
            
            tvTaskDifficulty.setText(getDifficultyText(task.getDifficulty()));
            tvTaskImportance.setText(getImportanceText(task.getImportance()));
            tvTaskXP.setText("+" + task.getXpValue() + " XP");
            
            // Set date and time (combined in start_date)
            if (task.getStartDate() != null && !task.getStartDate().isEmpty()) {
                String startDateTime = task.getStartDate();
                if (startDateTime.contains(" ")) {
                    String[] parts = startDateTime.split(" ");
                    tvTaskDate.setText("Datum: " + parts[0]);
                    tvTaskTime.setText("Vreme: " + parts[1]);
                    tvTaskDate.setVisibility(View.VISIBLE);
                    tvTaskTime.setVisibility(View.VISIBLE);
                } else {
                    tvTaskDate.setText("Datum: " + startDateTime);
                    tvTaskTime.setVisibility(View.GONE);
                    tvTaskDate.setVisibility(View.VISIBLE);
                }
            } else {
                tvTaskDate.setVisibility(View.GONE);
                tvTaskTime.setVisibility(View.GONE);
            }
            
            // Set category info
            Category category = categories.get(task.getCategoryId());
            if (category != null) {
                tvTaskCategory.setText(category.getName());
                tvTaskCategory.setVisibility(View.VISIBLE);
                try {
                    int color = Color.parseColor(category.getColor());
                    categoryColorIndicator.setBackgroundColor(color);
                } catch (IllegalArgumentException e) {
                    categoryColorIndicator.setBackgroundColor(Color.GRAY);
                }
            } else {
                tvTaskCategory.setVisibility(View.GONE);
                categoryColorIndicator.setBackgroundColor(Color.GRAY);
            }
            
            // Show recurring indicator for recurring tasks
            if (task.isRecurring()) {
                tvRecurringIndicator.setVisibility(View.VISIBLE);
            } else {
                tvRecurringIndicator.setVisibility(View.GONE);
            }
            
            tvTaskStatus.setText(getStatusText(task.getStatus()));
            setStatusColor(task.getStatus());
            
            // Show/hide complete button based on status
            if ("completed".equals(task.getStatus())) {
                btnComplete.setVisibility(View.GONE);
            } else {
                btnComplete.setVisibility(View.VISIBLE);
            }
        }
        
        private String getDifficultyText(String difficulty) {
            switch (difficulty) {
                case "very_easy": return "Veoma lak";
                case "easy": return "Lak";
                case "hard": return "Težak";
                case "extreme": return "Ekstremno težak";
                default: return difficulty;
            }
        }
        
        private String getImportanceText(String importance) {
            switch (importance) {
                case "normal": return "Normalan";
                case "important": return "Važan";
                case "very_important": return "Ekstremno važan";
                case "special": return "Specijalan";
                default: return importance;
            }
        }
        
        private String getStatusText(String status) {
            switch (status) {
                case "active": return "Aktivan";
                case "completed": return "Završen";
                case "incomplete": return "Nezavršen";
                case "paused": return "Pauziran";
                case "cancelled": return "Otkazan";
                default: return status;
            }
        }
        
        private void setStatusColor(String status) {
            int color;
            switch (status) {
                case "active":
                    color = Color.parseColor("#4CAF50");
                    break;
                case "completed":
                    color = Color.parseColor("#2196F3");
                    break;
                case "incomplete":
                    color = Color.parseColor("#FF9800");
                    break;
                case "paused":
                    color = Color.parseColor("#9C27B0");
                    break;
                case "cancelled":
                    color = Color.parseColor("#F44336");
                    break;
                default:
                    color = Color.GRAY;
                    break;
            }
            tvTaskStatus.setTextColor(color);
        }
    }
}


