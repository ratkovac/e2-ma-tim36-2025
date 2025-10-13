package com.habitrpg.taskmanager.presentation.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Task;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    
    private List<Task> tasks;
    private OnTaskClickListener listener;
    
    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskComplete(Task task);
        void onTaskEdit(Task task);
        void onTaskDelete(Task task);
    }
    
    public TaskAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
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
    
    class TaskViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTaskName;
        private TextView tvTaskDescription;
        private TextView tvTaskDifficulty;
        private TextView tvTaskImportance;
        private TextView tvTaskXP;
        private TextView tvTaskTime;
        private TextView tvTaskStatus;
        private ImageView btnComplete;
        private ImageView btnEdit;
        private ImageView btnDelete;
        
        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvTaskDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvTaskDifficulty = itemView.findViewById(R.id.tvTaskDifficulty);
            tvTaskImportance = itemView.findViewById(R.id.tvTaskImportance);
            tvTaskXP = itemView.findViewById(R.id.tvTaskXP);
            tvTaskTime = itemView.findViewById(R.id.tvTaskTime);
            tvTaskStatus = itemView.findViewById(R.id.tvTaskStatus);
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
            
            if (task.getExecutionTime() != null && !task.getExecutionTime().isEmpty()) {
                tvTaskTime.setText("Time: " + task.getExecutionTime());
                tvTaskTime.setVisibility(View.VISIBLE);
            } else {
                tvTaskTime.setVisibility(View.GONE);
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
                case "very_easy": return "Very Easy";
                case "easy": return "Easy";
                case "hard": return "Hard";
                case "extreme": return "Extreme";
                default: return difficulty;
            }
        }
        
        private String getImportanceText(String importance) {
            switch (importance) {
                case "normal": return "Normal";
                case "important": return "Important";
                case "very_important": return "Very Important";
                case "special": return "Special";
                default: return importance;
            }
        }
        
        private String getStatusText(String status) {
            switch (status) {
                case "active": return "Active";
                case "completed": return "Completed";
                case "incomplete": return "Incomplete";
                case "paused": return "Paused";
                case "cancelled": return "Cancelled";
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

