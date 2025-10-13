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
        
        setupSpinners();
        setupClickListeners();
        loadCategories();
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
        
        binding.btnStartDate.setOnClickListener(v -> showDatePickerDialog(true));
        binding.btnEndDate.setOnClickListener(v -> showDatePickerDialog(false));
        binding.btnExecutionTime.setOnClickListener(v -> showTimePickerDialog());
        
        binding.btnCreateTask.setOnClickListener(v -> createTask());
    }
    
    private void loadCategories() {
        categoryService.getAllCategories(new CategoryService.CategoryCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load categories: " + error, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onCategoriesRetrieved(List<Category> categories) {
                TaskCreationFragment.this.categories = categories;
                setupCategorySpinner();
            }
        });
    }
    
    private void setupCategorySpinner() {
        if (categories == null || categories.isEmpty()) {
            Toast.makeText(getContext(), "No categories available. Please create categories first.", Toast.LENGTH_LONG).show();
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
    
    private void showDatePickerDialog(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
            (view, year, month, dayOfMonth) -> {
                String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                if (isStartDate) {
                    binding.btnStartDate.setText(date);
                } else {
                    binding.btnEndDate.setText(date);
                }
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
        
        // Create task
        Task task = new Task();
        task.setUserId(taskService.getCurrentUserId());
        task.setCategoryId(selectedCategoryId);
        task.setName(name);
        task.setDescription(binding.editTextDescription.getText().toString().trim());
        task.setDifficulty(difficulty);
        task.setImportance(importance);
        task.setExecutionTime(binding.btnExecutionTime.getText().toString());
        
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
                task.setStartDate(binding.btnStartDate.getText().toString());
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
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                clearForm();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to create task: " + error, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
        });
    }
    
    private void clearForm() {
        binding.editTextTaskName.setText("");
        binding.editTextDescription.setText("");
        binding.editTextRecurrenceInterval.setText("");
        binding.btnStartDate.setText("Select Start Date");
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
