package com.habitrpg.taskmanager.presentation.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Friend;
import com.habitrpg.taskmanager.data.database.entities.FriendRequest;
import com.habitrpg.taskmanager.data.database.entities.User;
import com.habitrpg.taskmanager.databinding.FragmentFriendsBinding;
import com.habitrpg.taskmanager.presentation.adapters.FriendAdapter;
import com.habitrpg.taskmanager.presentation.adapters.FriendRequestAdapter;
import com.habitrpg.taskmanager.presentation.adapters.UserSearchAdapter;
import com.habitrpg.taskmanager.presentation.dialogs.FriendRequestDialog;
import com.habitrpg.taskmanager.service.FriendService;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {
    
    private FragmentFriendsBinding binding;
    private FriendService friendService;
    
    private List<Friend> friends = new ArrayList<>();
    private List<FriendRequest> pendingRequests = new ArrayList<>();
    private List<User> searchResults = new ArrayList<>();
    
    private FriendAdapter friendAdapter;
    private FriendRequestAdapter requestAdapter;
    private UserSearchAdapter searchAdapter;
    
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFriendsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        friendService = FriendService.getInstance(requireContext());
        
        setupRecyclerViews();
        setupClickListeners();
        setupFragmentResultListeners();
        loadData();
    }
    
    private void setupFragmentResultListeners() {
        getParentFragmentManager().setFragmentResultListener("friend_request_handled", getViewLifecycleOwner(), (requestKey, bundle) -> {
            loadData();
        });
    }
    
    private void setupRecyclerViews() {
        // Friends RecyclerView
        friendAdapter = new FriendAdapter(friends, this::onFriendClick, this::onRemoveFriend);
        binding.recyclerViewFriends.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewFriends.setAdapter(friendAdapter);
        
        // Requests RecyclerView
        requestAdapter = new FriendRequestAdapter(pendingRequests, this::onAcceptRequest, this::onDeclineRequest);
        binding.recyclerViewRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewRequests.setAdapter(requestAdapter);
        
        // Search RecyclerView
        searchAdapter = new UserSearchAdapter(searchResults, this::onUserClick);
        binding.recyclerViewSearch.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewSearch.setAdapter(searchAdapter);
    }
    
    private void setupClickListeners() {
        binding.fabScanQR.setOnClickListener(v -> scanQRCode());
        binding.btnGuild.setOnClickListener(v -> navigateToGuild());
        
        binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchUsers(query);
                return true;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    showFriendsView();
                }
                return true;
            }
        });
    }
    
    private void loadData() {
        loadFriends();
        loadPendingRequests();
    }
    
    private void loadFriends() {
        android.util.Log.d("FriendsFragment", "loadFriends() called");
        friendService.getFriends(new FriendService.FriendCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onFriendsRetrieved(List<Friend> friendsList) {
                android.util.Log.d("FriendsFragment", "onFriendsRetrieved: " + friendsList.size() + " friends");
                requireActivity().runOnUiThread(() -> {
                    friends.clear();
                    friends.addAll(friendsList);
                    friendAdapter.notifyDataSetChanged();
                    updateEmptyStates();
                });
            }
        });
    }
    
    private void loadPendingRequests() {
        android.util.Log.d("FriendsFragment", "loadPendingRequests() called");
        friendService.getPendingRequests(new FriendService.FriendRequestCallback() {
            @Override
            public void onSuccess(String message) {}
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onFriendRequestsRetrieved(List<FriendRequest> requests) {
                android.util.Log.d("FriendsFragment", "onFriendRequestsRetrieved: " + requests.size() + " requests");
                requireActivity().runOnUiThread(() -> {
                    pendingRequests.clear();
                    pendingRequests.addAll(requests);
                    requestAdapter.notifyDataSetChanged();
                    updateEmptyStates();
                });
            }
        });
    }
    
    private void searchUsers(String query) {
        if (query.trim().isEmpty()) {
            showFriendsView();
            return;
        }
        
        showSearchView();
        
        friendService.searchUsers(query, new FriendService.UserSearchCallback() {
            @Override
            public void onUsersFound(List<User> users) {
                requireActivity().runOnUiThread(() -> {
                    searchResults.clear();
                    searchResults.addAll(users);
                    searchAdapter.notifyDataSetChanged();
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void scanQRCode() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), 
                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return;
        }
        
        // Start QR code scanner
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan QR code to add friend");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }
    
    private void navigateToGuild() {
        Navigation.findNavController(requireView()).navigate(R.id.navigation_guild);
    }
    
    private void onFriendClick(Friend friend) {
        // TODO: Implement friend profile navigation
        Toast.makeText(requireContext(), "Friend profile: " + friend.getFriendUsername(), Toast.LENGTH_SHORT).show();
    }
    
    private void onRemoveFriend(Friend friend) {
        friendService.removeFriend(friend.getFriendUserId(), new FriendService.FriendCallback() {
            @Override
            public void onSuccess(String message) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    loadFriends();
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onFriendsRetrieved(List<Friend> friends) {}
        });
    }
    
    private void onAcceptRequest(FriendRequest request) {
        FriendRequestDialog dialog = FriendRequestDialog.newInstance(request, true);
        dialog.show(getParentFragmentManager(), "AcceptFriendRequest");
        
        // Set up a listener to refresh when dialog is dismissed
        getParentFragmentManager().setFragmentResultListener("friend_request_dismissed", this, (requestKey, bundle) -> {
            refreshData();
        });
    }
    
    private void onDeclineRequest(FriendRequest request) {
        FriendRequestDialog dialog = FriendRequestDialog.newInstance(request, false);
        dialog.show(getParentFragmentManager(), "DeclineFriendRequest");
        
        // Set up a listener to refresh when dialog is dismissed
        getParentFragmentManager().setFragmentResultListener("friend_request_dismissed", this, (requestKey, bundle) -> {
            refreshData();
        });
    }
    
    private void onUserClick(User user) {
        // Navigate to user profile
        Bundle bundle = new Bundle();
        bundle.putString("userId", user.getId());
        bundle.putString("username", user.getUsername());
        bundle.putString("email", user.getEmail());
        bundle.putInt("avatarId", user.getAvatarId());
        Navigation.findNavController(requireView()).navigate(R.id.navigation_user_profile, bundle);
    }
    
    private void showFriendsView() {
        binding.layoutFriends.setVisibility(View.VISIBLE);
        binding.layoutSearch.setVisibility(View.GONE);
    }
    
    private void showSearchView() {
        binding.layoutFriends.setVisibility(View.GONE);
        binding.layoutSearch.setVisibility(View.VISIBLE);
    }
    
    private void updateEmptyStates() {
        binding.textViewEmptyFriends.setVisibility(friends.isEmpty() ? View.VISIBLE : View.GONE);
        binding.textViewEmptyRequests.setVisibility(pendingRequests.isEmpty() ? View.VISIBLE : View.GONE);
    }
    
    public void refreshData() {
        android.util.Log.d("FriendsFragment", "refreshData() called");
        loadData();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(requireContext(), "QR scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                // Process scanned QR code
                String qrContent = result.getContents();
                processQRCode(qrContent);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    private void processQRCode(String qrContent) {
        try {
            // QR code should contain user ID
            friendService.addFriendByQRCode(qrContent, new FriendService.FriendCallback() {
                @Override
                public void onSuccess(String message) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        refreshData(); // Refresh friends list
                    });
                }
                
                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Failed to add friend: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onFriendsRetrieved(List<Friend> friends) {
                    // Not used here
                }
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Invalid QR code format", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanQRCode();
            } else {
                Toast.makeText(requireContext(), "Camera permission required for QR scanning", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (friendService != null) {
            friendService.shutdown();
        }
    }
}
