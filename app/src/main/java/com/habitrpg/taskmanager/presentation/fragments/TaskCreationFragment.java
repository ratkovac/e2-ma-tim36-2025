package com.habitrpg.taskmanager.presentation.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Category;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.databinding.FragmentTaskCreationBinding;
import com.habitrpg.taskmanager.service.CategoryService;
import com.habitrpg.taskmanager.service.TaskService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskCreationFragment extends Fragment {
    
    private FragmentTaskCreationBinding binding;
    private TaskService taskService;
    private CategoryService categoryService;
    private List<Category> categories;
    private int selectedCategoryId = -1;
    
    // Edit mode variables
    private boolean isEditMode = false;
    private int editTaskId = -1;
    private Task currentTask = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskCreationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        taskService = TaskService.getInstance(requireContext());
        categoryService = CategoryService.getInstance(requireContext());
        
        // Check if edit mode
        if (getArguments() != null) {
            isEditMode = getArguments().getBoolean("isEdit", false);
            editTaskId = getArguments().getInt("taskId", -1);
        }

        // Update button text based on mode
        if (isEditMode) {
            binding.btnCreateTask.setText("Edit Task");
        } else {
            binding.btnCreateTask.setText("Create Task");
        }

        setupToolbar();
        setupSpinners();
        setupClickListeners();
        loadCategories();

        // Load task data if edit mode
        if (isEditMode && editTaskId != -1) {
            loadTaskForEdit();
        }
    }
    
    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });
    }

    private void setupSpinners() {
        // Difficulty spinner
        String[] difficulties = {"Veoma lak", "Lak", "Težak", "Ekstremno težak"};
        String[] difficultyValues = {"very_easy", "easy", "hard", "extreme"};
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(requireContext(), 
            android.R.layout.simple_spinner_item, difficulties);
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDifficulty.setAdapter(difficultyAdapter);
        
        // Importance spinner
        String[] importances = {"Normalan", "Važan", "Ekstremno važan", "Specijalan"};
        String[] importanceValues = {"normal", "important", "very_important", "special"};
        ArrayAdapter<String> importanceAdapter = new ArrayAdapter<>(requireContext(), 
            android.R.layout.simple_spinner_item, importances);
        importanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerImportance.setAdapter(importanceAdapter);
        
        // Recurrence unit spinner
        String[] recurrenceUnits = {"Dan", "Nedelja"};
        String[] recurrenceUnitValues = {"day", "week"};
        ArrayAdapter<String> recurrenceUnitAdapter = new ArrayAdapter<>(requireContext(), 
            android.R.layout.simple_spinner_item, recurrenceUnits);
        recurrenceUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerRecurrenceUnit.setAdapter(recurrenceUnitAdapter);
        
        // Store values for later use
        binding.spinnerDifficulty.setTag(difficultyValues);
        binding.spinnerImportance.setTag(importanceValues);
        binding.spinnerRecurrenceUnit.setTag(recurrenceUnitValues);
    }
    
    private void setupClickListeners() {
        binding.checkboxRecurring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.layoutRecurringOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        
        binding.btnStartDate.setOnClickListener(v -> showDatePickerDialog());
        binding.btnEndDate.setOnClickListener(v -> showEndDatePickerDialog());
        binding.btnExecutionTime.setOnClickListener(v -> showTimePickerDialog());
        
        binding.btnCreateTask.setOnClickListener(v -> createTask());
    }
    
    private void loadCategories() {
        categoryService.getAllCategories(new CategoryService.CategoryCallback() {
            @Override
            public void onSuccess(String message) {}

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Failed to load categories: " + error, Toast.LENGTH_SHORT).show();
                });
            }


            @Override
            public void onCategoriesRetrieved(List<Category> categories) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        TaskCreationFragment.this.categories = categories;
                        setupCategorySpinner();

                        // If edit mode, populate the form after categories are loaded
                        if (isEditMode && currentTask != null) {
                            populateFormWithTask();
                        }
                    });
                }
            }
        });
    }

    private void loadTaskForEdit() {
        taskService.getTaskById(editTaskId, new TaskService.TaskCallback() {
            @Override
            public void onSuccess(String message) {}

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                        requireActivity().onBackPressed();
                    });
                }
            }

            @Override
            public void onTasksRetrieved(List<Task> tasks) {
                if (getActivity() != null && tasks != null && !tasks.isEmpty()) {
                    currentTask = tasks.get(0);

                    // Check if task is completed
                    if ("completed".equals(currentTask.getStatus())) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Ne možete menjati završen zadatak", Toast.LENGTH_SHORT).show();
                            requireActivity().onBackPressed();
                        });
                        return;
                    }

                    // Populate form will be called after categories load
                }
            }
        });
    }

    private void populateFormWithTask() {
        if (currentTask == null || categories == null) return;

        // Update title and button text
        binding.editTextTaskName.setText(currentTask.getName());

        if (currentTask.getDescription() != null) {
            binding.editTextDescription.setText(currentTask.getDescription());
        }

        // Set category
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == currentTask.getCategoryId()) {
                binding.spinnerCategory.setSelection(i);
                selectedCategoryId = currentTask.getCategoryId();
                break;
            }
        }

        // Set difficulty
        String[] difficultyValues = {"very_easy", "easy", "hard", "extreme"};
        for (int i = 0; i < difficultyValues.length; i++) {
            if (difficultyValues[i].equals(currentTask.getDifficulty())) {
                binding.spinnerDifficulty.setSelection(i);
                break;
            }
        }

        // Set importance
        String[] importanceValues = {"normal", "important", "very_important", "special"};
        for (int i = 0; i < importanceValues.length; i++) {
            if (importanceValues[i].equals(currentTask.getImportance())) {
                binding.spinnerImportance.setSelection(i);
                break;
            }
        }

        // Set dates and time - parse combined start_date
        if (currentTask.getStartDate() != null) {
            String startDateTime = currentTask.getStartDate();
            if (startDateTime.contains(" ")) {
                String[] parts = startDateTime.split(" ");
                binding.btnStartDate.setText(parts[0]); // Date part
                if (parts.length > 1) {
                    binding.btnExecutionTime.setText(parts[1]); // Time part
                }
            } else {
                binding.btnStartDate.setText(startDateTime);
            }
        }

        // Disable recurring options for edit (can't change recurring settings)
        binding.checkboxRecurring.setEnabled(false);
        binding.checkboxRecurring.setChecked(currentTask.isRecurring());

        if (currentTask.isRecurring()) {
            binding.editTextRecurrenceInterval.setText(String.valueOf(currentTask.getRecurrenceInterval()));
            if (currentTask.getEndDate() != null) {
                binding.btnEndDate.setText(currentTask.getEndDate());
            }
            String[] recurrenceUnitValues = {"day", "week"};
            for (int i = 0; i < recurrenceUnitValues.length; i++) {
                if (recurrenceUnitValues[i].equals(currentTask.getRecurrenceUnit())) {
                    binding.spinnerRecurrenceUnit.setSelection(i);
                    break;
                }
            }
        }
    }

    private void setupCategorySpinner() {
        if (categories == null || categories.isEmpty()) {
            Toast.makeText(getContext(), "Prvo napravite kategoriju u sekciji Categories!", Toast.LENGTH_LONG).show();
            requireActivity().runOnUiThread(() -> {
                Navigation.findNavController(requireView()).navigateUp();
            });
            return;
        }
        
        List<String> categoryNames = new ArrayList<>();
        for (Category category : categories) {
            categoryNames.add(category.getName());
        }
        
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), 
            android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(categoryAdapter);
        
        binding.spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategoryId = categories.get(position).getId();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCategoryId = -1;
            }
        });
    }
    
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
            (view, year, month, dayOfMonth) -> {
                String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                binding.btnStartDate.setText(date);
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void showEndDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
            (view, year, month, dayOfMonth) -> {
                String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                binding.btnEndDate.setText(date);
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
            (view, hourOfDay, minute) -> {
                String time = String.format("%02d:%02d", hourOfDay, minute);
                binding.btnExecutionTime.setText(time);
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        );
        timePickerDialog.show();
    }
    
    private void createTask() {
        // Validate inputs
        String name = binding.editTextTaskName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            binding.editTextTaskName.setError("Task name is required");
            return;
        }
        
        if (selectedCategoryId == -1) {
            Toast.makeText(getContext(), "Please create categories first before creating tasks", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Get selected values
        String[] difficultyValues = (String[]) binding.spinnerDifficulty.getTag();
        String[] importanceValues = (String[]) binding.spinnerImportance.getTag();
        String[] recurrenceUnitValues = (String[]) binding.spinnerRecurrenceUnit.getTag();
        
        String difficulty = difficultyValues[binding.spinnerDifficulty.getSelectedItemPosition()];
        String importance = importanceValues[binding.spinnerImportance.getSelectedItemPosition()];
        
        if (isEditMode) {
            // Update existing task
            updateTask(name, difficulty, importance);
        } else {
            // Create new task
            createNewTask(name, difficulty, importance);
        }
    }

    private void updateTask(String name, String difficulty, String importance) {
        if (currentTask == null) return;

        // Update only allowed fields: name, description, start date and time, difficulty, importance
        currentTask.setName(name);
        currentTask.setDescription(binding.editTextDescription.getText().toString().trim());
        currentTask.setDifficulty(difficulty);
        currentTask.setImportance(importance);
        
        // Combine date and time for start_date
        String startDate = binding.btnStartDate.getText().toString();
        String executionTime = binding.btnExecutionTime.getText().toString();
        if (!startDate.equals("Select Date") && !executionTime.equals("Select Time")) {
            currentTask.setStartDate(startDate + " " + executionTime);
        }

        taskService.updateTask(currentTask, new TaskService.TaskCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Zadatak ažuriran", Toast.LENGTH_SHORT).show();
                        requireActivity().onBackPressed();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
        });
    }

    private void createNewTask(String name, String difficulty, String importance) {
        String[] recurrenceUnitValues = (String[]) binding.spinnerRecurrenceUnit.getTag();

        // Create task
        Task task = new Task();
        task.setUserId(taskService.getCurrentUserId());
        task.setCategoryId(selectedCategoryId);
        task.setName(name);
        task.setDescription(binding.editTextDescription.getText().toString().trim());
        task.setDifficulty(difficulty);
        task.setImportance(importance);
        // Combine date and time for start_date
        String startDate = binding.btnStartDate.getText().toString();
        String executionTime = binding.btnExecutionTime.getText().toString();
        if (!startDate.equals("Select Date") && !executionTime.equals("Select Time")) {
            task.setStartDate(startDate + " " + executionTime);
        } else {
            // Default to current date and time if not set
            task.setStartDate(com.habitrpg.taskmanager.util.DateUtils.getCurrentDateTimeString());
        }

        // Handle recurring task
        boolean isRecurring = binding.checkboxRecurring.isChecked();
        task.setRecurring(isRecurring);
        
        if (isRecurring) {
            String intervalText = binding.editTextRecurrenceInterval.getText().toString().trim();
            if (TextUtils.isEmpty(intervalText)) {
                binding.editTextRecurrenceInterval.setError("Recurrence interval is required");
                return;
            }
            
            try {
                int interval = Integer.parseInt(intervalText);
                if (interval < 1) {
                    binding.editTextRecurrenceInterval.setError("Interval must be at least 1");
                    return;
                }
                
                task.setRecurrenceInterval(interval);
                task.setRecurrenceUnit(recurrenceUnitValues[binding.spinnerRecurrenceUnit.getSelectedItemPosition()]);
                // For recurring tasks, combine date and time like regular tasks
                String recurringStartDate = binding.btnStartDate.getText().toString();
                String recurringExecutionTime = binding.btnExecutionTime.getText().toString();
                if (!recurringStartDate.equals("Select Date") && !recurringExecutionTime.equals("Select Time")) {
                    task.setStartDate(recurringStartDate + " " + recurringExecutionTime);
                }
                task.setEndDate(binding.btnEndDate.getText().toString());
                
            } catch (NumberFormatException e) {
                binding.editTextRecurrenceInterval.setError("Invalid interval format");
                return;
            }
        }
        
        // Create task
        taskService.createTask(task, new TaskService.TaskCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        // Navigate back to tasks fragment
                        Navigation.findNavController(requireView()).navigateUp();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to create task: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
        });
    }
    
    private void clearForm() {
        binding.editTextTaskName.setText("");
        binding.editTextDescription.setText("");
        binding.editTextRecurrenceInterval.setText("");
        binding.btnStartDate.setText("Select Date");
        binding.btnEndDate.setText("Select End Date");
        binding.btnExecutionTime.setText("Select Time");
        binding.checkboxRecurring.setChecked(false);
        binding.layoutRecurringOptions.setVisibility(View.GONE);
        binding.spinnerDifficulty.setSelection(0);
        binding.spinnerImportance.setSelection(0);
        binding.spinnerRecurrenceUnit.setSelection(0);
        if (binding.spinnerCategory.getAdapter() != null) {
            binding.spinnerCategory.setSelection(0);
        }
    }
}
