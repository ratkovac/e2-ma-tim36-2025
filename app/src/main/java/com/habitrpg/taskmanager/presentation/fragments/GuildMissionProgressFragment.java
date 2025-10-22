package com.habitrpg.taskmanager.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.repository.SpecialMissionRepository;
import com.habitrpg.taskmanager.service.GuildService;

import java.util.List;

public class GuildMissionProgressFragment extends Fragment {

	private TextView tvBossHp;
	private TextView tvTimeLeft;
	private ProgressBar progressBar;
	private RecyclerView rvMembers;

	@Override
	public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_guild_mission_progress, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		tvBossHp = view.findViewById(R.id.tvBossHp);
		tvTimeLeft = view.findViewById(R.id.tvTimeLeft);
		progressBar = view.findViewById(R.id.progressBar);
		rvMembers = view.findViewById(R.id.rvMembers);
		rvMembers.setLayoutManager(new LinearLayoutManager(getContext()));

		loadGuildAndSummary();
	}

	private void loadGuildAndSummary() {
		if (getContext() == null) return;
		GuildService.getInstance(getContext()).getCurrentUserGuild(new GuildService.GuildCallback() {
			@Override
			public void onSuccess(String message, com.habitrpg.taskmanager.data.database.entities.Guild guild) {
				if (!isAdded()) return;
				requireActivity().runOnUiThread(() -> loadSummary(guild.getGuildId()));
			}

			@Override
			public void onError(String error) {
				// Optionally show error
			}
		});
	}

	private void loadSummary(String guildId) {
		SpecialMissionRepository.getInstance(getContext()).getGuildProgressSummary(guildId, new SpecialMissionRepository.GuildProgressSummaryCallback() {
			@Override
			public void onSuccess(SpecialMissionRepository.GuildProgressSummary s) {
				if (!isAdded()) return;
				requireActivity().runOnUiThread(() -> {
					// Check if mission has expired and finalize it
					long now = System.currentTimeMillis();
					if (now > s.endDate) {
						// Mission expired - finalize it without rewards
						SpecialMissionRepository.getInstance(getContext()).finalizeIfExpiredForGuild(guildId, new SpecialMissionRepository.FinalizeCallback() {
							@Override
							public void onCompleted(String msg) {
								if (isAdded()) {
									requireActivity().runOnUiThread(() -> {
										tvBossHp.setText("Specijalna misija je završena");
										tvTimeLeft.setText("Rok je prošao");
										progressBar.setMax(1);
										progressBar.setProgress(0);
										rvMembers.setAdapter(null);
									});
								}
							}
							@Override
							public void onNoAction() { /* no-op */ }
							@Override
							public void onError(String error) { /* no-op */ }
						});
						return;
					}
					
					int max = Math.max(1, s.initialBossHp);
					int current = Math.max(0, s.currentBossHp);
					progressBar.setMax(max);
					progressBar.setProgress(max - current); // show dealt damage
					tvBossHp.setText("Trenutni HP bosa: " + current + "/" + max);
					long msLeft = s.endDate - System.currentTimeMillis();
					long days = Math.max(0, msLeft / (24L*60L*60L*1000L));
					tvTimeLeft.setText("Preostalo vreme: " + days + " dana");
					rvMembers.setAdapter(new MembersAdapter(s.members));
				});
			}

			@Override
			public void onNoActiveMission() {
				if (!isAdded()) return;
				requireActivity().runOnUiThread(() -> {
					tvBossHp.setText("Nema aktivne specijalne misije");
					tvTimeLeft.setText("");
					progressBar.setMax(1);
					progressBar.setProgress(0);
				});
			}

			@Override
			public void onError(String error) {}
		});
	}

	private static class MembersAdapter extends RecyclerView.Adapter<MemberVH> {
		private final List<SpecialMissionRepository.GuildMemberProgress> data;
		MembersAdapter(List<SpecialMissionRepository.GuildMemberProgress> data) { this.data = data; }
		@NonNull @Override public MemberVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guild_member_progress, parent, false);
			return new MemberVH(v);
		}
		@Override public void onBindViewHolder(@NonNull MemberVH h, int i) { h.bind(data.get(i)); }
		@Override public int getItemCount() { return data != null ? data.size() : 0; }
	}

	private static class MemberVH extends RecyclerView.ViewHolder {
		private final TextView tvLine;
		MemberVH(@NonNull View itemView) { super(itemView); tvLine = itemView.findViewById(R.id.tvLine); }
		void bind(SpecialMissionRepository.GuildMemberProgress m) {
			String line = m.username + ": " + m.totalHp + " HP (" +
					"kupovine " + m.shopPurchases + ", udarci " + m.regularBossHits + ", laki " + m.easyTasksCompleted + ", teski " + m.otherTasksCompleted + ", poruke " + m.daysWithMessagesCount + (m.hasNoUnresolvedTasks ? ", bez nerešenih ✓" : ", bez nerešenih ✗") + ")";
			tvLine.setText(line);
		}
	}
}
