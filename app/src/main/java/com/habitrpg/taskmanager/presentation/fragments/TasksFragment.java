package com.habitrpg.taskmanager.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.habitrpg.taskmanager.presentation.adapters.TaskAdapter;
import com.habitrpg.taskmanager.service.TaskService;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.databinding.FragmentTasksBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TasksFragment extends Fragment {
    
    private FragmentTasksBinding binding;
    private TaskService taskService;
    private TaskAdapter taskAdapter;
    private List<Task> tasks;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTasksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        taskService = TaskService.getInstance(requireContext());
        tasks = new ArrayList<>();
        
        setupUI();
        loadTasks();
    }
    
    private void setupUI() {
        // Set current date
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        binding.tvDate.setText(sdf.format(new Date()));
        
        // Setup RecyclerView
        taskAdapter = new TaskAdapter(tasks, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task) {
                Toast.makeText(getContext(), "Task: " + task.getName(), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onTaskComplete(Task task) {
                completeTask(task);
            }
            
            @Override
            public void onTaskEdit(Task task) {
                Toast.makeText(getContext(), "Edit: " + task.getName(), Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onTaskDelete(Task task) {
                deleteTask(task);
            }
        });
        
        binding.recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewTasks.setAdapter(taskAdapter);
        
        // Initially show loading
        showLoading(true);
    }
    
    private void loadTasks() {
        taskService.getActiveTasks(new TaskService.TaskListCallback() {
            @Override
            public void onTasksRetrieved(List<Task> tasks) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        
                        if (tasks == null || tasks.isEmpty()) {
                            showEmptyState(true);
                        } else {
                            showEmptyState(false);
                            TasksFragment.this.tasks.clear();
                            TasksFragment.this.tasks.addAll(tasks);
                            taskAdapter.updateTasks(tasks);
                        }
                    });
                }
            }
        });
    }
    
    private void completeTask(Task task) {
        taskService.completeTask(task.getId(), new TaskService.TaskCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                loadTasks();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to complete task: " + error, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
        });
    }
    
    private void deleteTask(Task task) {
        taskService.cancelTask(task.getId(), new TaskService.TaskCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), "Task deleted successfully", Toast.LENGTH_SHORT).show();
                loadTasks();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to delete task: " + error, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {}
        });
    }
    
    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recyclerViewTasks.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.layoutEmptyState.setVisibility(View.GONE);
    }
    
    private void showEmptyState(boolean show) {
        binding.layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recyclerViewTasks.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadTasks();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
