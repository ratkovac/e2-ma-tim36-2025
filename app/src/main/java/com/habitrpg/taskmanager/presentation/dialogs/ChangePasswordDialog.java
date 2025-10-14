package com.habitrpg.taskmanager.presentation.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.service.AuthService;
import com.google.android.material.textfield.TextInputEditText;

public class ChangePasswordDialog extends DialogFragment {
    
    private AuthService authService;
    private OnPasswordChangedListener listener;
    
    public interface OnPasswordChangedListener {
        void onPasswordChanged();
    }
    
    public static ChangePasswordDialog newInstance(AuthService authService, OnPasswordChangedListener listener) {
        ChangePasswordDialog dialog = new ChangePasswordDialog();
        dialog.authService = authService;
        dialog.listener = listener;
        return dialog;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_change_password, null);
        
        TextInputEditText etCurrentPassword = view.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNewPassword = view.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        
        builder.setView(view)
                .setTitle("Change Password")
                .setPositiveButton("Change", (dialog, which) -> {
                    String currentPassword = etCurrentPassword.getText().toString().trim();
                    String newPassword = etNewPassword.getText().toString().trim();
                    String confirmPassword = etConfirmPassword.getText().toString().trim();
                    
                    if (TextUtils.isEmpty(currentPassword)) {
                        Toast.makeText(getContext(), "Current password is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (TextUtils.isEmpty(newPassword)) {
                        Toast.makeText(getContext(), "New password is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (newPassword.length() < 6) {
                        Toast.makeText(getContext(), "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(getContext(), "New passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    changePassword(currentPassword, newPassword);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dismiss());
        
        return builder.create();
    }
    
    private void changePassword(String currentPassword, String newPassword) {
        authService.changePassword(currentPassword, newPassword, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onPasswordChanged();
                        }
                        dismiss();
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
}
