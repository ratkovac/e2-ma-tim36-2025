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
        
        // Date and time (combined in start_date)
        if (currentTask.getStartDate() != null) {
            String startDateTime = currentTask.getStartDate();
            if (startDateTime.contains(" ")) {
                String[] parts = startDateTime.split(" ");
                binding.tvStartDate.setText(parts[0]); // Date part
                binding.tvExecutionTime.setText(parts[1]); // Time part
            } else {
                binding.tvStartDate.setText(startDateTime);
                binding.tvExecutionTime.setText("Nije postavljeno");
            }
        } else {
            binding.tvStartDate.setText("Nije postavljeno");
            binding.tvExecutionTime.setText("Nije postavljeno");
        }
        
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
                    ("day".equals(currentTask.getRecurrenceUnit()) ? "dan/a" : "nedelja/e");
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
        boolean isIncomplete = status.equals("incomplete");
        boolean isCancelled = status.equals("cancelled");
        boolean isPaused = status.equals("paused");
        boolean isActive = status.equals("active");
        
        // Pravila za dugmad prema specifikaciji:
        // - Neurađen, otkazan i završen zadatak se ne mogu menjati
        // - Samo aktivan zadatak može biti označen kao urađen, otkazan ili pauziran
        // - Pauziran zadatak može samo da se aktivira (samo za ponavljajuće)
        
        // Završeno dugme - samo za aktivan zadatak
        boolean canComplete = isActive;
        binding.btnMarkCompleted.setEnabled(canComplete);
        binding.btnMarkCompleted.setAlpha(canComplete ? 1.0f : 0.5f);
        
        // Pauzirano dugme - samo za aktivan ponavljajući zadatak
        boolean canPause = isActive && currentTask.isRecurring();
        binding.btnMarkPaused.setEnabled(canPause);
        binding.btnMarkPaused.setAlpha(canPause ? 1.0f : 0.5f);
        
        // Otkazano dugme - samo za aktivan zadatak
        boolean canCancel = isActive;
        binding.btnMarkCancelled.setEnabled(canCancel);
        binding.btnMarkCancelled.setAlpha(canCancel ? 1.0f : 0.5f);
        
        // Aktivno dugme - samo za pauziran ponavljajući zadatak
        boolean canActivate = isPaused && currentTask.isRecurring();
        binding.btnMarkActive.setEnabled(canActivate);
        binding.btnMarkActive.setAlpha(canActivate ? 1.0f : 0.5f);
        
        // Edit i Delete dugmad - ne mogu se menjati neurađeni, otkazani i završeni zadaci
        boolean canModify = !isCompleted && !isIncomplete && !isCancelled;
        binding.btnEditTask.setEnabled(canModify);
        binding.btnDeleteTask.setEnabled(canModify);
        binding.btnEditTask.setAlpha(canModify ? 1.0f : 0.5f);
        binding.btnDeleteTask.setAlpha(canModify ? 1.0f : 0.5f);
        
        // Dodaj tooltip tekst za objašnjenje zašto su dugmad onemogućena
        if (!canPause && isActive) {
            binding.btnMarkPaused.setText("⏸ Pauzirano\n(Samo ponavljajući)");
        } else {
            binding.btnMarkPaused.setText("⏸ Pauzirano");
        }
        
        if (!canActivate && isPaused) {
            binding.btnMarkActive.setText("► Aktivno\n(Samo ponavljajući)");
        } else {
            binding.btnMarkActive.setText("► Aktivno");
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

