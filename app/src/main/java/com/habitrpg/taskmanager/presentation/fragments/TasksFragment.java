package com.habitrpg.taskmanager.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.habitrpg.taskmanager.business.tasks.TaskManager;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.databinding.FragmentTasksBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TasksFragment extends Fragment {
    
    private FragmentTasksBinding binding;
    private TaskManager taskManager;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTasksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        taskManager = TaskManager.getInstance(requireContext());
        
        setupUI();
        loadTasks();
    }
    
    private void setupUI() {
        // Set current date
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        binding.tvDate.setText(sdf.format(new Date()));
        
        // Setup RecyclerView
        binding.recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initially show loading
        showLoading(true);
    }
    
    private void loadTasks() {
        taskManager.getActiveTasks(new TaskManager.TaskListCallback() {
            @Override
            public void onTasksRetrieved(List<Task> tasks) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        
                        if (tasks == null || tasks.isEmpty()) {
                            showEmptyState(true);
                        } else {
                            showEmptyState(false);
                            // TODO: Set up adapter with tasks
                            // TaskAdapter adapter = new TaskAdapter(tasks);
                            // binding.recyclerViewTasks.setAdapter(adapter);
                        }
                    });
                }
            }
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
        // Refresh tasks when fragment becomes visible
        loadTasks();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
