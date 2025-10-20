package com.habitrpg.taskmanager.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.repository.SpecialMissionRepository;
import com.habitrpg.taskmanager.data.preferences.UserPreferences;

public class SpecialMissionProgressFragment extends Fragment {

	private TextView tvShop;
	private TextView tvRegularHits;
	private TextView tvEasyTasks;
	private TextView tvOtherTasks;
	private TextView tvNoUnresolved;
	private TextView tvMessageDays;
	private TextView tvTotal;

	@Override
	public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_special_mission_progress, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		initializeViews(view);
		loadSummary();
	}

	private void initializeViews(View view) {
		tvShop = view.findViewById(R.id.tvShop);
		tvRegularHits = view.findViewById(R.id.tvRegularHits);
		tvEasyTasks = view.findViewById(R.id.tvEasyTasks);
		tvOtherTasks = view.findViewById(R.id.tvOtherTasks);
		tvNoUnresolved = view.findViewById(R.id.tvNoUnresolved);
		tvMessageDays = view.findViewById(R.id.tvMessageDays);
		tvTotal = view.findViewById(R.id.tvTotal);
	}

	private void loadSummary() {
		if (getContext() == null) return;
		UserPreferences prefs = UserPreferences.getInstance(getContext());
		String userId = prefs.getCurrentUserId();
		if (userId == null) return;

		SpecialMissionRepository.getInstance(getContext()).getUserProgressSummary(userId, new SpecialMissionRepository.ProgressSummaryCallback() {
			@Override
			public void onSuccess(SpecialMissionRepository.ProgressSummary s) {
				if (!isAdded()) return;
				requireActivity().runOnUiThread(() -> {
					tvShop.setText("Kupovine: " + s.shopPurchases + "/5 (" + (s.hpFromShop) + " HP)");
					tvRegularHits.setText("Boss udarci: " + s.regularBossHits + "/10 (" + (s.hpFromRegularHits) + " HP)");
					tvEasyTasks.setText("Laki zadaci: " + s.easyTasksCompleted + "/10 (" + (s.hpFromEasyTasks) + " HP)");
					tvOtherTasks.setText("Teški zadaci: " + s.otherTasksCompleted + "/6 (" + (s.hpFromOtherTasks) + " HP)");
					tvNoUnresolved.setText("Bez nerešenih: " + (s.hasNoUnresolvedTasks ? "✓" : "✗") + " (" + (s.hpFromNoUnresolved) + " HP)");
					tvMessageDays.setText("Dani sa porukama: " + s.daysWithMessagesCount + " (" + (s.hpFromMessages) + " HP)");
					tvTotal.setText("Ukupno: " + s.totalHp + " HP");
				});
			}

			@Override
			public void onNoActiveMission() {
				if (!isAdded()) return;
				requireActivity().runOnUiThread(() -> {
					tvShop.setText("Nema aktivne specijalne misije");
					tvRegularHits.setText("");
					tvEasyTasks.setText("");
					tvOtherTasks.setText("");
					tvNoUnresolved.setText("");
					tvMessageDays.setText("");
					tvTotal.setText("");
				});
			}

			@Override
			public void onError(String error) {
				// Optionally show a toast
			}
		});
	}
}


