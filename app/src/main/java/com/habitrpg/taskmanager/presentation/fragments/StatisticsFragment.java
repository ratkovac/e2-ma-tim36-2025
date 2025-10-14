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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.UserStatistics;
import com.habitrpg.taskmanager.databinding.FragmentStatisticsBinding;
import com.habitrpg.taskmanager.service.StatisticsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatisticsFragment extends Fragment {
    
    private FragmentStatisticsBinding binding;
    private StatisticsService statisticsService;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        statisticsService = StatisticsService.getInstance(requireContext());
        
        loadStatistics();
    }
    
    private void loadStatistics() {
        statisticsService.getUserStatistics(new StatisticsService.StatisticsCallback() {
            @Override
            public void onStatisticsRetrieved(UserStatistics statistics) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateStatisticsUI(statistics);
                        loadCategoryStats();
                        loadXPProgress();
                        loadDifficultyStats();
                    });
                }
            }
            
            @Override
            public void onCategoryStatsRetrieved(Map<String, Integer> categoryStats) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateCategoryChart(categoryStats);
                    });
                }
            }
            
            @Override
            public void onXPProgressRetrieved(Map<String, Integer> xpProgress) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateXPProgressChart(xpProgress);
                    });
                }
            }
            
            @Override
            public void onDifficultyStatsRetrieved(Map<String, Integer> difficultyStats) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateDifficultyChart(difficultyStats);
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void updateStatisticsUI(UserStatistics statistics) {
        binding.tvDaysActive.setText(String.valueOf(statistics.getTotalDaysActive()));
        binding.tvTasksCreated.setText(String.valueOf(statistics.getTotalTasksCreated()));
        binding.tvTasksCompleted.setText(String.valueOf(statistics.getTotalTasksCompleted()));
        binding.tvTasksPending.setText(String.valueOf(statistics.getTotalTasksPending()));
        binding.tvTasksCancelled.setText(String.valueOf(statistics.getTotalTasksCancelled()));
        binding.tvLongestStreak.setText(String.valueOf(statistics.getLongestStreak()));
        binding.tvCurrentStreak.setText(String.valueOf(statistics.getCurrentStreak()));
        binding.tvTotalXP.setText(String.valueOf(statistics.getTotalXP()));
        binding.tvSpecialMissionsStarted.setText(String.valueOf(statistics.getTotalSpecialMissionsStarted()));
        binding.tvSpecialMissionsCompleted.setText(String.valueOf(statistics.getTotalSpecialMissionsCompleted()));
        
        updateTaskStatusChart(statistics);
    }
    
    private void updateTaskStatusChart(UserStatistics statistics) {
        List<PieEntry> entries = new ArrayList<>();
        
        if (statistics.getTotalTasksCompleted() > 0) {
            entries.add(new PieEntry(statistics.getTotalTasksCompleted(), "Completed"));
        }
        if (statistics.getTotalTasksPending() > 0) {
            entries.add(new PieEntry(statistics.getTotalTasksPending(), "Pending"));
        }
        if (statistics.getTotalTasksCancelled() > 0) {
            entries.add(new PieEntry(statistics.getTotalTasksCancelled(), "Cancelled"));
        }
        
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "No Tasks"));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "Task Status");
        dataSet.setColors(Color.GREEN, Color.YELLOW, Color.RED, Color.GRAY);
        dataSet.setValueTextSize(12f);
        
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        
        binding.pieChartTaskStatus.setData(data);
        binding.pieChartTaskStatus.setDescription(null);
        binding.pieChartTaskStatus.setCenterText("Task Status");
        binding.pieChartTaskStatus.setCenterTextSize(14f);
        binding.pieChartTaskStatus.invalidate();
    }
    
    private void loadCategoryStats() {
        statisticsService.getTasksByCategory(new StatisticsService.StatisticsCallback() {
            @Override
            public void onStatisticsRetrieved(UserStatistics statistics) {}
            
            @Override
            public void onCategoryStatsRetrieved(Map<String, Integer> categoryStats) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateCategoryChart(categoryStats);
                    });
                }
            }
            
            @Override
            public void onXPProgressRetrieved(Map<String, Integer> xpProgress) {}
            
            @Override
            public void onDifficultyStatsRetrieved(Map<String, Integer> difficultyStats) {}
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void updateCategoryChart(Map<String, Integer> categoryStats) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        int index = 0;
        for (Map.Entry<String, Integer> entry : categoryStats.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }
        
        if (entries.isEmpty()) {
            entries.add(new BarEntry(0, 0));
            labels.add("No Categories");
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Tasks by Category");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextSize(12f);
        
        BarData data = new BarData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        
        binding.barChartCategories.setData(data);
        binding.barChartCategories.setDescription(null);
        binding.barChartCategories.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.barChartCategories.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.barChartCategories.getXAxis().setGranularity(1f);
        binding.barChartCategories.getXAxis().setLabelCount(labels.size());
        binding.barChartCategories.invalidate();
    }
    
    private void loadXPProgress() {
        statisticsService.getXPProgressLast7Days(new StatisticsService.StatisticsCallback() {
            @Override
            public void onStatisticsRetrieved(UserStatistics statistics) {}
            
            @Override
            public void onCategoryStatsRetrieved(Map<String, Integer> categoryStats) {}
            
            @Override
            public void onXPProgressRetrieved(Map<String, Integer> xpProgress) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateXPProgressChart(xpProgress);
                    });
                }
            }
            
            @Override
            public void onDifficultyStatsRetrieved(Map<String, Integer> difficultyStats) {}
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void updateXPProgressChart(Map<String, Integer> xpProgress) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        int index = 0;
        for (Map.Entry<String, Integer> entry : xpProgress.entrySet()) {
            entries.add(new Entry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }
        
        if (entries.isEmpty()) {
            entries.add(new Entry(0, 0));
            labels.add("No Data");
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "XP Progress (Last 7 Days)");
        dataSet.setColor(Color.MAGENTA);
        dataSet.setValueTextSize(12f);
        dataSet.setCircleColor(Color.MAGENTA);
        dataSet.setCircleRadius(4f);
        dataSet.setLineWidth(2f);
        
        LineData data = new LineData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        
        binding.lineChartXP.setData(data);
        binding.lineChartXP.setDescription(null);
        binding.lineChartXP.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.lineChartXP.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.lineChartXP.getXAxis().setGranularity(1f);
        binding.lineChartXP.getXAxis().setLabelCount(labels.size());
        binding.lineChartXP.invalidate();
    }
    
    private void loadDifficultyStats() {
        statisticsService.getAverageDifficultyXP(new StatisticsService.StatisticsCallback() {
            @Override
            public void onStatisticsRetrieved(UserStatistics statistics) {}
            
            @Override
            public void onCategoryStatsRetrieved(Map<String, Integer> categoryStats) {}
            
            @Override
            public void onXPProgressRetrieved(Map<String, Integer> xpProgress) {}
            
            @Override
            public void onDifficultyStatsRetrieved(Map<String, Integer> difficultyStats) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateDifficultyChart(difficultyStats);
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void updateDifficultyChart(Map<String, Integer> difficultyStats) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        int index = 0;
        for (Map.Entry<String, Integer> entry : difficultyStats.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }
        
        if (entries.isEmpty()) {
            entries.add(new BarEntry(0, 0));
            labels.add("No Data");
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "XP by Difficulty");
        dataSet.setColor(Color.CYAN);
        dataSet.setValueTextSize(12f);
        
        BarData data = new BarData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        
        binding.barChartDifficulty.setData(data);
        binding.barChartDifficulty.setDescription(null);
        binding.barChartDifficulty.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.barChartDifficulty.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.barChartDifficulty.getXAxis().setGranularity(1f);
        binding.barChartDifficulty.getXAxis().setLabelCount(labels.size());
        binding.barChartDifficulty.invalidate();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
