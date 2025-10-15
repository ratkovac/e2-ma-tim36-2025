package com.habitrpg.taskmanager.presentation.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.service.CategoryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarTaskAdapter extends RecyclerView.Adapter<CalendarTaskAdapter.CalendarTaskViewHolder> {
    
    private List<Task> tasks;
    private OnTaskClickListener listener;
    private Map<Integer, Category> categories;
    private Context context;
    
    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }
    
    public CalendarTaskAdapter(Context context, List<Task> tasks, OnTaskClickListener listener) {
        this.context = context;
        this.tasks = tasks;
        this.listener = listener;
        this.categories = new HashMap<>();
        loadCategories();
    }
    
    @NonNull
    @Override
    public CalendarTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_task, parent, false);
        return new CalendarTaskViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CalendarTaskViewHolder holder, int position) {
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
    
    class CalendarTaskViewHolder extends RecyclerView.ViewHolder {
        private CardView taskCard;
        private LinearLayout taskContainer;
        private TextView tvTimeSlot;
        private TextView tvTaskName;
        private TextView tvCategoryName;
        private TextView tvTaskStatus;
        private TextView tvXPValue;
        
        public CalendarTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskCard = itemView.findViewById(R.id.taskCard);
            taskContainer = itemView.findViewById(R.id.taskContainer);
            tvTimeSlot = itemView.findViewById(R.id.tvTimeSlot);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvTaskStatus = itemView.findViewById(R.id.tvTaskStatus);
            tvXPValue = itemView.findViewById(R.id.tvXPValue);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onTaskClick(tasks.get(position));
                    }
                }
            });
        }
        
        public void bind(Task task) {
            // Set task name
            tvTaskName.setText(task.getName());
            
            // Set time slot from combined start_date
            if (task.getStartDate() != null && !task.getStartDate().isEmpty()) {
                String startDateTime = task.getStartDate();
                if (startDateTime.contains(" ")) {
                    String[] parts = startDateTime.split(" ");
                    tvTimeSlot.setText(parts[1]); // Time part
                } else {
                    tvTimeSlot.setText("--:--");
                }
            } else {
                tvTimeSlot.setText("--:--");
            }
            
            // Set XP value
            tvXPValue.setText("+" + task.getXpValue());
            
            // Set status
            tvTaskStatus.setText(getStatusText(task.getStatus()));
            
            // Set category and background color
            Category category = categories.get(task.getCategoryId());
            if (category != null) {
                tvCategoryName.setText(category.getName());
                
                // Set background color of entire container to category color
                try {
                    int color = Color.parseColor(category.getColor());
                    taskContainer.setBackgroundColor(color);
                    
                    // Adjust alpha for better visibility
                    taskCard.setCardBackgroundColor(color);
                    
                } catch (IllegalArgumentException e) {
                    // Default color if parsing fails
                    taskContainer.setBackgroundColor(Color.parseColor("#757575"));
                    taskCard.setCardBackgroundColor(Color.parseColor("#757575"));
                }
            } else {
                tvCategoryName.setText("Bez kategorije");
                taskContainer.setBackgroundColor(Color.parseColor("#757575"));
                taskCard.setCardBackgroundColor(Color.parseColor("#757575"));
            }
            
            // Add status overlay effect
            addStatusOverlay(task.getStatus());
        }
        
        private void addStatusOverlay(String status) {
            // Add subtle overlay based on status
            float alpha = 1.0f;
            switch (status) {
                case "completed":
                    alpha = 0.7f; // Slightly transparent for completed
                    break;
                case "paused":
                    alpha = 0.8f;
                    break;
                case "cancelled":
                    alpha = 0.6f;
                    break;
                case "active":
                default:
                    alpha = 1.0f;
                    break;
            }
            taskContainer.setAlpha(alpha);
        }
        
        private String getStatusText(String status) {
            switch (status) {
                case "active": return "● Aktivan";
                case "completed": return "✓ Završen";
                case "incomplete": return "○ Nezavršen";
                case "paused": return "⏸ Pauziran";
                case "cancelled": return "✗ Otkazan";
                default: return status;
            }
        }
    }
}

