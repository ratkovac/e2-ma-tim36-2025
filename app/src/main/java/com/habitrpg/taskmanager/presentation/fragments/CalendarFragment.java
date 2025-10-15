package com.habitrpg.taskmanager.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Task;
import com.habitrpg.taskmanager.databinding.FragmentCalendarBinding;
import com.habitrpg.taskmanager.presentation.adapters.CalendarTaskAdapter;
import com.habitrpg.taskmanager.service.TaskService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CalendarFragment extends Fragment {
    
    private FragmentCalendarBinding binding;
    private TaskService taskService;
    private CalendarTaskAdapter taskAdapter;
    private List<Task> allTasks;
    private List<Task> tasksForSelectedDate;
    private String selectedDate;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        taskService = TaskService.getInstance(requireContext());
        allTasks = new ArrayList<>();
        tasksForSelectedDate = new ArrayList<>();
        
        // Set selected date to today by default
        selectedDate = getCurrentDateString();
        
        setupUI();
        loadAllTasks();
    }
    
    private void setupUI() {
        // Set current month display
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        binding.tvCurrentMonth.setText(monthFormat.format(new Date()));
        
        // Set selected date display
        updateSelectedDateDisplay();
        
        // Setup calendar listener
        binding.calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                // Format the selected date
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedDate = sdf.format(calendar.getTime());
                
                updateSelectedDateDisplay();
                filterTasksForDate();
            }
        });
        
        // Setup RecyclerView with CalendarTaskAdapter
        taskAdapter = new CalendarTaskAdapter(requireContext(), tasksForSelectedDate, new CalendarTaskAdapter.OnTaskClickListener() {
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
        
        showLoading(true);
    }
    
    private void loadAllTasks() {
        taskService.getAllTasks(new TaskService.TaskCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onTasksRetrieved(List<Task> tasks) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        
                        if (tasks != null) {
                            allTasks.clear();
                            allTasks.addAll(tasks);
                            filterTasksForDate();
                        }
                    });
                }
            }
        });
    }
    
    private void filterTasksForDate() {
        tasksForSelectedDate.clear();
        
        if (allTasks != null) {
            // Include all tasks that match the selected date
            List<Task> filtered = allTasks.stream()
                    .filter(task -> task.getStartDate() != null && 
                            task.getStartDate().startsWith(selectedDate))
                    .sorted((t1, t2) -> {
                        String time1 = "23:59";
                        String time2 = "23:59";
                        
                        // Extract time from combined start_date
                        if (t1.getStartDate() != null && t1.getStartDate().contains(" ")) {
                            String[] parts1 = t1.getStartDate().split(" ");
                            if (parts1.length > 1) {
                                time1 = parts1[1];
                            }
                        }
                        
                        if (t2.getStartDate() != null && t2.getStartDate().contains(" ")) {
                            String[] parts2 = t2.getStartDate().split(" ");
                            if (parts2.length > 1) {
                                time2 = parts2[1];
                            }
                        }
                        
                        return time1.compareTo(time2);
                    })
                    .collect(Collectors.toList());
            
            tasksForSelectedDate.addAll(filtered);
        }
        
        if (tasksForSelectedDate.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            taskAdapter.updateTasks(tasksForSelectedDate);
        }
    }
    
    private void updateSelectedDateDisplay() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
            Date date = inputFormat.parse(selectedDate);
            binding.tvSelectedDate.setText(outputFormat.format(date));
        } catch (Exception e) {
            binding.tvSelectedDate.setText(selectedDate);
        }
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
    
    private String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadAllTasks();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

