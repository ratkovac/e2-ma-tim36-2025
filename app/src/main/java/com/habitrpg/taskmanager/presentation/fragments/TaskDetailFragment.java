package com.habitrpg.taskmanager.presentation.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.databinding.FragmentTaskDetailBinding;
import com.habitrpg.taskmanager.service.CategoryService;
import com.habitrpg.taskmanager.service.TaskService;

import java.util.List;

public class TaskDetailFragment extends Fragment {
    
    private FragmentTaskDetailBinding binding;
    private TaskService taskService;
    private CategoryService categoryService;
    private Task currentTask;
    private int taskId;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTaskDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        taskService = TaskService.getInstance(requireContext());
        categoryService = CategoryService.getInstance(requireContext());
        
        // Get task ID from arguments
        if (getArguments() != null) {
            taskId = getArguments().getInt("taskId", -1);
            if (taskId != -1) {
                loadTaskDetails();
            } else {
                Toast.makeText(getContext(), "Greška: ID zadatka nije pronađen", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(view).navigateUp();
            }
        }
        
        setupListeners();
    }
    
    private void loadTaskDetails() {
        taskService.getTaskById(taskId, new TaskService.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigateUp();
                    });
                }
            }
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {
                if (tasks != null && !tasks.isEmpty()) {
                    currentTask = tasks.get(0);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> displayTaskDetails());
                    }
                }
            }
        });
    }
    
    private void displayTaskDetails() {
        if (currentTask == null) return;
        
        // Basic info
        binding.tvTaskName.setText(currentTask.getName());
        binding.tvTaskStatus.setText(getStatusText(currentTask.getStatus()));
        setStatusColor(currentTask.getStatus());
        
        // Description
        if (currentTask.getDescription() != null && !currentTask.getDescription().isEmpty()) {
            binding.tvTaskDescription.setText(currentTask.getDescription());
        } else {
            binding.tvTaskDescription.setText("Nema opisa");
        }
        
        // Category
        loadCategory(currentTask.getCategoryId());
        
        // Dates and time
        binding.tvStartDate.setText(currentTask.getStartDate() != null ? 
                currentTask.getStartDate() : "Nije postavljeno");
        binding.tvExecutionTime.setText(currentTask.getExecutionTime() != null ? 
                currentTask.getExecutionTime() : "Nije postavljeno");
        
        if (currentTask.getEndDate() != null && !currentTask.getEndDate().isEmpty()) {
            binding.layoutEndDate.setVisibility(View.VISIBLE);
            binding.tvEndDate.setText(currentTask.getEndDate());
        } else {
            binding.layoutEndDate.setVisibility(View.GONE);
        }
        
        // Recurring info
        if (currentTask.isRecurring()) {
            binding.layoutRecurringInfo.setVisibility(View.VISIBLE);
            String recurringText = "Ponavlja se svakih " + 
                    currentTask.getRecurrenceInterval() + " " +
                    (currentTask.getRecurrenceUnit().equals("day") ? "dan/a" : "nedelja/e");
            binding.tvRecurringInfo.setText(recurringText);
        } else {
            binding.layoutRecurringInfo.setVisibility(View.GONE);
        }
        
        // Task properties
        binding.tvTaskDifficulty.setText(getDifficultyText(currentTask.getDifficulty()));
        binding.tvTaskImportance.setText(getImportanceText(currentTask.getImportance()));
        binding.tvTaskXP.setText("+" + currentTask.getXpValue() + " XP");
        
        // Update button states based on status
        updateButtonStates();
    }
    
    private void loadCategory(int categoryId) {
        categoryService.getAllCategories(new CategoryService.CategoryCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {}
            
            @Override
            public void onCategoriesRetrieved(List<Category> categories) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        for (Category category : categories) {
                            if (category.getId() == categoryId) {
                                binding.tvTaskCategory.setText(category.getName());
                                try {
                                    int color = Color.parseColor(category.getColor());
                                    binding.categoryColorIndicator.setBackgroundColor(color);
                                } catch (IllegalArgumentException e) {
                                    binding.categoryColorIndicator.setBackgroundColor(Color.GRAY);
                                }
                                break;
                            }
                        }
                    });
                }
            }
        });
    }
    
    private void setupListeners() {
        binding.btnMarkCompleted.setOnClickListener(v -> changeTaskStatus("completed"));
        binding.btnMarkPaused.setOnClickListener(v -> changeTaskStatus("paused"));
        binding.btnMarkCancelled.setOnClickListener(v -> changeTaskStatus("cancelled"));
        binding.btnMarkActive.setOnClickListener(v -> changeTaskStatus("active"));
        
        binding.btnEditTask.setOnClickListener(v -> {
            // Navigate to edit fragment
            Bundle bundle = new Bundle();
            bundle.putInt("taskId", taskId);
            bundle.putBoolean("isEdit", true);
            Navigation.findNavController(v).navigate(R.id.navigation_task_creation, bundle);
        });
        
        binding.btnDeleteTask.setOnClickListener(v -> deleteTask());
    }
    
    private void changeTaskStatus(String newStatus) {
        if (currentTask == null) return;
        
        if (newStatus.equals("completed")) {
            taskService.completeTask(taskId, new TaskService.TaskCallback() {
                @Override
                public void onSuccess(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                            loadTaskDetails(); // Reload to update UI
                        });
                    }
                }
                
                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show()
                        );
                    }
                }
                
                @Override
                public void onTasksRetrieved(List<Task> tasks) {}
            });
        } else {
            taskService.updateTaskStatus(taskId, newStatus, new TaskService.TaskCallback() {
                @Override
                public void onSuccess(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Status promenjen u: " + getStatusText(newStatus), 
                                    Toast.LENGTH_SHORT).show();
                            loadTaskDetails(); // Reload to update UI
                        });
                    }
                }
                
                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show()
                        );
                    }
                }
                
                @Override
                public void onTasksRetrieved(List<Task> tasks) {}
            });
        }
    }
    
    private void deleteTask() {
        if (currentTask == null) return;
        
        // Validacija: Ne može se obrisati završen zadatak
        if ("completed".equals(currentTask.getStatus())) {
            Toast.makeText(getContext(), "Ne možete obrisati završen zadatak", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Potvrda brisanja
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Brisanje zadatka")
                .setMessage(currentTask.isRecurring() ? 
                        "Da li ste sigurni da želite obrisati ovaj ponavljajući zadatak i sve buduće instance?" :
                        "Da li ste sigurni da želite obrisati ovaj zadatak?")
                .setPositiveButton("Obriši", (dialog, which) -> {
                    taskService.deleteTask(taskId, new TaskService.TaskCallback() {
                        @Override
                        public void onSuccess(String message) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                    Navigation.findNavController(requireView()).navigateUp();
                                });
                            }
                        }
                        
                        @Override
                        public void onError(String error) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> 
                                    Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show()
                                );
                            }
                        }
                        
                        @Override
                        public void onTasksRetrieved(List<Task> tasks) {}
                    });
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }
    
    private void updateButtonStates() {
        String status = currentTask.getStatus();
        boolean isCompleted = status.equals("completed");
        
        // Disable current status button
        binding.btnMarkCompleted.setEnabled(!isCompleted);
        binding.btnMarkPaused.setEnabled(!status.equals("paused") && !isCompleted);
        binding.btnMarkCancelled.setEnabled(!status.equals("cancelled") && !isCompleted);
        binding.btnMarkActive.setEnabled(!status.equals("active") && !isCompleted);
        
        // Gray out disabled buttons
        binding.btnMarkCompleted.setAlpha(isCompleted ? 0.5f : 1.0f);
        binding.btnMarkPaused.setAlpha(status.equals("paused") || isCompleted ? 0.5f : 1.0f);
        binding.btnMarkCancelled.setAlpha(status.equals("cancelled") || isCompleted ? 0.5f : 1.0f);
        binding.btnMarkActive.setAlpha(status.equals("active") || isCompleted ? 0.5f : 1.0f);
        
        // Disable edit and delete buttons for completed tasks
        binding.btnEditTask.setEnabled(!isCompleted);
        binding.btnDeleteTask.setEnabled(!isCompleted);
        binding.btnEditTask.setAlpha(isCompleted ? 0.5f : 1.0f);
        binding.btnDeleteTask.setAlpha(isCompleted ? 0.5f : 1.0f);
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
            case "very_important": return "Veoma važan";
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
        binding.tvTaskStatus.setTextColor(color);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

