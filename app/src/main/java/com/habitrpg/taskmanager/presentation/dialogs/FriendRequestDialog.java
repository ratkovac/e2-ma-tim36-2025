package com.habitrpg.taskmanager.presentation.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.habitrpg.taskmanager.data.database.entities.FriendRequest;
import com.habitrpg.taskmanager.service.FriendService;

public class FriendRequestDialog extends DialogFragment {

    private FriendRequest friendRequest;
    private boolean isAccept;
    private boolean shouldRefresh = false;

    public static FriendRequestDialog newInstance(FriendRequest request, boolean isAccept) {
        FriendRequestDialog dialog = new FriendRequestDialog();
        Bundle args = new Bundle();
        args.putSerializable("friendRequest", request);
        args.putBoolean("isAccept", isAccept);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            friendRequest = (FriendRequest) getArguments().getSerializable("friendRequest");
            isAccept = getArguments().getBoolean("isAccept", false);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String title = isAccept ? "Accept Friend Request" : "Decline Friend Request";
        String message = isAccept ?
                "Do you want to accept friend request from " + friendRequest.getFromUsername() + "?" :
                "Do you want to decline friend request from " + friendRequest.getFromUsername() + "?";
        String buttonText = isAccept ? "Accept" : "Decline";

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(buttonText, (dialogInterface, which) -> {
                    FriendService friendService = FriendService.getInstance(requireContext());

                    shouldRefresh = true;
                    
                    Bundle result = new Bundle();
                    result.putBoolean("handled", true);
                    getParentFragmentManager().setFragmentResult("friend_request_handled", result);
                    
                    dismiss();
                    
                    if (isAccept) {
                        friendService.acceptFriendRequest(friendRequest.getId(), new FriendService.FriendCallback() {
                            @Override
                            public void onSuccess(String message) {
                                if (getActivity() != null && isAdded()) {
                                    getActivity().runOnUiThread(() -> {
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onError(String error) {
                                if (getActivity() != null && isAdded()) {
                                    getActivity().runOnUiThread(() -> {
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onFriendsRetrieved(java.util.List<com.habitrpg.taskmanager.data.database.entities.Friend> friends) {}
                        });
                    } else {
                        friendService.declineFriendRequest(friendRequest.getId(), new FriendService.FriendCallback() {
                            @Override
                            public void onSuccess(String message) {
                                if (getActivity() != null && isAdded()) {
                                    getActivity().runOnUiThread(() -> {
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onError(String error) {
                                if (getActivity() != null && isAdded()) {
                                    getActivity().runOnUiThread(() -> {
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onFriendsRetrieved(java.util.List<com.habitrpg.taskmanager.data.database.entities.Friend> friends) {}
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        return dialog;
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);

        // Refresh samo ako je API poziv bio uspešan
        if (shouldRefresh) {
            Bundle result = new Bundle();
            result.putBoolean("refresh_needed", true);
            getParentFragmentManager().setFragmentResult("friend_request_dismissed", result);
        }
    }
}