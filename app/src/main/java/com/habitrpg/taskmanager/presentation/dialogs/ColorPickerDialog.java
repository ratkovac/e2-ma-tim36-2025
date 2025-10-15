package com.habitrpg.taskmanager.presentation.dialogs;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.habitrpg.taskmanager.R;

public class ColorPickerDialog extends DialogFragment {
    
    private static final String ARG_SELECTED_COLOR = "selected_color";
    
    private String selectedColor;
    private OnColorSelectedListener listener;
    
    public interface OnColorSelectedListener {
        void onColorSelected(String color);
    }
    
    public static ColorPickerDialog newInstance(String currentColor, OnColorSelectedListener listener) {
        ColorPickerDialog dialog = new ColorPickerDialog();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_COLOR, currentColor);
        dialog.setArguments(args);
        dialog.listener = listener;
        return dialog;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedColor = getArguments().getString(ARG_SELECTED_COLOR);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_color_picker, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        GridLayout colorGrid = view.findViewById(R.id.colorGrid);
        setupColorSelection(colorGrid);
    }
    
    private void setupColorSelection(GridLayout colorGrid) {
        String[] colors = {
            "#4CAF50", "#2196F3", "#FF9800", "#9C27B0",
            "#F44336", "#00BCD4", "#8BC34A", "#795548",
            "#607D8B", "#E91E63", "#3F51B5", "#FFC107"
        };
        
        for (String colorHex : colors) {
            View colorView = new View(requireContext());
            colorView.setBackgroundColor(Color.parseColor(colorHex));
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 80;
            params.height = 80;
            params.setMargins(8, 8, 8, 8);
            colorView.setLayoutParams(params);
            
            colorView.setOnClickListener(v -> {
                selectedColor = colorHex;
                if (listener != null) {
                    listener.onColorSelected(colorHex);
                }
                dismiss();
            });
            
            colorGrid.addView(colorView);
        }
        
        // Highlight currently selected color
        updateColorSelection(colorGrid);
    }
    
    private void updateColorSelection(GridLayout colorGrid) {
        for (int i = 0; i < colorGrid.getChildCount(); i++) {
            View child = colorGrid.getChildAt(i);
            String childColor = getColorFromView(child);
            
            if (selectedColor != null && selectedColor.equals(childColor)) {
                child.setAlpha(1.0f);
                child.setScaleX(1.2f);
                child.setScaleY(1.2f);
            } else {
                child.setAlpha(0.7f);
                child.setScaleX(1.0f);
                child.setScaleY(1.0f);
            }
        }
    }
    
    private String getColorFromView(View view) {
        // This is a simple approach - in a real app you might want to store color info differently
        String[] colors = {
            "#4CAF50", "#2196F3", "#FF9800", "#9C27B0",
            "#F44336", "#00BCD4", "#8BC34A", "#795548",
            "#607D8B", "#E91E63", "#3F51B5", "#FFC107"
        };
        
        int index = ((GridLayout) view.getParent()).indexOfChild(view);
        if (index >= 0 && index < colors.length) {
            return colors[index];
        }
        return null;
    }
}
