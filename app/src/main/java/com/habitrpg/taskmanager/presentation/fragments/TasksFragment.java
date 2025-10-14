package com.habitrpg.taskmanager.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.presentation.adapters.CalendarTaskAdapter;
import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.presentation.adapters.TaskAdapter;
import com.habitrpg.taskmanager.service.TaskService;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.databinding.FragmentTasksBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TasksFragment extends Fragment {
    
    private FragmentTasksBinding binding;
    private TaskService taskService;
    private CalendarTaskAdapter taskAdapter;
    private List<Task> allTasks;
    private List<Task> filteredTasks;
    private String currentFilter = "all"; // "all", "single", "recurring"
    
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
        allTasks = new ArrayList<>();
        filteredTasks = new ArrayList<>();
        
        setupUI();
        setupClickListeners();
        loadTasks();
    }
    
    private void setupUI() {
        // Set current date
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        binding.tvDate.setText(sdf.format(new Date()));
        
        // Setup tabs
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "single";
                        break;
                    case 2:
                        currentFilter = "recurring";
                        break;
                }
                filterTasks();
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Setup RecyclerView with CalendarTaskAdapter
        taskAdapter = new CalendarTaskAdapter(requireContext(), filteredTasks, new CalendarTaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task) {
                // Navigate to task detail
                Bundle bundle = new Bundle();
                bundle.putInt("taskId", task.getId());
                Navigation.findNavController(requireView()).navigate(R.id.navigation_task_detail, bundle);
            }
        });
        
        binding.recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewTasks.setAdapter(taskAdapter);
        
        // Initially show loading
        showLoading(true);
    }
    
    private void setupClickListeners() {
        binding.fabAddTask.setOnClickListener(v -> {
            // Navigate to task creation fragment
            Navigation.findNavController(v).navigate(R.id.navigation_task_creation);
        });
    }

    private void loadTasks() {
        taskService.getAllTasks(new TaskService.TaskCallback() {
            @Override
            public void onSuccess(String message) {}

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                        showEmptyState(true);
                    });
                }
            }

            @Override
            public void onTasksRetrieved(List<Task> tasks) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        
                        if (tasks == null || tasks.isEmpty()) {
                            showEmptyState(true);
                        } else {
                            // Filter to show only current and future tasks (not past tasks)
                            // Always show recurring templates regardless of date
                            allTasks.clear();
                            String currentDate = getCurrentDateString();
                            for (Task task : tasks) {
                                // Always include recurring templates
                                if (task.isRecurring()) {
                                    allTasks.add(task);
                                }
                                // Include task instances if they are current/future or active/paused
                                else if (task.getStartDate() != null &&
                                    (task.getStartDate().compareTo(currentDate) >= 0 ||
                                     "active".equals(task.getStatus()) ||
                                     "paused".equals(task.getStatus()))) {
                                    allTasks.add(task);
                                }
                            }
                            filterTasks();
                        }
                    });
                }
            }
        });
    }

    private void filterTasks() {
        filteredTasks.clear();

        switch (currentFilter) {
            case "all":
                filteredTasks.addAll(allTasks);
                break;
            case "single":
                // Single tasks: not recurring (instances are marked as not recurring)
                filteredTasks.addAll(allTasks.stream()
                        .filter(task -> !task.isRecurring())
                        .collect(Collectors.toList()));
                break;
            case "recurring":
                // Original recurring task templates only (marked as isRecurring = true)
                filteredTasks.addAll(allTasks.stream()
                        .filter(task -> task.isRecurring())
                        .collect(Collectors.toList()));
                break;
        }

        if (filteredTasks.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            taskAdapter.updateTasks(filteredTasks);
        }
    }
    
    private String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
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
