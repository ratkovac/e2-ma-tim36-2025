package com.habitrpg.taskmanager.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    
    public static String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    public static String[] getCurrentWeekDates() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        String weekStart = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        
        cal.add(Calendar.DAY_OF_WEEK, 6);
        String weekEnd = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        
        return new String[]{weekStart, weekEnd};
    }
    
    public static String[] getCurrentMonthDates() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String monthStart = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        String monthEnd = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        
        return new String[]{monthStart, monthEnd};
    }
    
    public static boolean isDateInRange(long timestamp, String startDate, String endDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date taskDate = new Date(timestamp);
            
            String taskDateStr = sdf.format(taskDate);
            return taskDateStr.compareTo(startDate) >= 0 && taskDateStr.compareTo(endDate) <= 0;
        } catch (Exception e) {
            return false;
        }
    }
}

