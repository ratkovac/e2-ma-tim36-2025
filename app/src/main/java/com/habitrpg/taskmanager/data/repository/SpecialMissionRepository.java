package com.habitrpg.taskmanager.data.repository;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.dao.GuildDao;
import com.habitrpg.taskmanager.data.database.dao.SpecialMissionDao;
import com.habitrpg.taskmanager.data.database.dao.SpecialMissionProgressDao;
import com.habitrpg.taskmanager.data.database.entities.Guild;
import com.habitrpg.taskmanager.data.database.entities.GuildMember;
import com.habitrpg.taskmanager.data.database.entities.SpecialMission;
import com.habitrpg.taskmanager.data.database.entities.SpecialMissionProgress;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpecialMissionRepository {

	public interface StartMissionCallback {
		void onSuccess(String message, SpecialMission mission);
		void onError(String error);
	}

	public interface ProgressUpdateCallback {
		void onUpdated(String message);
		void onNoActiveMission();
		void onError(String error);
	}

	private static SpecialMissionRepository instance;
	private final GuildDao guildDao;
	private final SpecialMissionDao specialMissionDao;
	private final SpecialMissionProgressDao progressDao;
	private ExecutorService executor;

	private SpecialMissionRepository(Context context) {
		AppDatabase db = AppDatabase.getDatabase(context);
		this.guildDao = db.guildDao();
		this.specialMissionDao = db.specialMissionDao();
		this.progressDao = db.specialMissionProgressDao();
		this.executor = Executors.newSingleThreadExecutor();
	}

	public static synchronized SpecialMissionRepository getInstance(Context context) {
		if (instance == null) {
			instance = new SpecialMissionRepository(context);
		}
		return instance;
	}

	private void ensureExecutorActive() {
		if (executor.isShutdown()) {
			executor = Executors.newSingleThreadExecutor();
		}
	}

	public void startMissionForGuild(String guildId, String requesterUserId, StartMissionCallback callback) {
		ensureExecutorActive();
		executor.execute(() -> {
			try {
				Guild guild = guildDao.getGuildById(guildId);
				if (guild == null || !guild.isActive()) {
					callback.onError("Guild not found or inactive");
					return;
				}

				if (!requesterUserId.equals(guild.getLeaderId())) {
					callback.onError("Only the guild leader can start a mission");
					return;
				}

				SpecialMission existing = specialMissionDao.getActiveMissionByGuild(guildId);
				if (existing != null) {
					callback.onError("An active mission already exists");
					return;
				}

				int memberCount = guildDao.getGuildMemberCount(guildId);
				int initialHp = 100 * Math.max(memberCount, 0);

				long start = System.currentTimeMillis();
				long end = start + 14L * 24L * 60L * 60L * 1000L;

				SpecialMission mission = new SpecialMission(UUID.randomUUID().toString(), guildId, start, end, initialHp, initialHp, SpecialMission.STATUS_ACTIVE, false);
				specialMissionDao.insert(mission);

				List<GuildMember> members = guildDao.getGuildMembersByGuildId(guildId);
				for (GuildMember m : members) {
					SpecialMissionProgress p = new SpecialMissionProgress();
					p.setSpecialMissionId(mission.getId());
					p.setUserId(m.getUserId());
					progressDao.insert(p);
				}

				guildDao.updateGuildMissionStatus(guildId, true);

				callback.onSuccess("Special mission started", mission);
			} catch (Exception e) {
				callback.onError("Failed to start mission: " + e.getMessage());
			}
		});
	}

	public void recordShopPurchase(String userId, ProgressUpdateCallback callback) {
		ensureExecutorActive();
		executor.execute(() -> {
			try {
				GuildMember member = guildDao.getGuildMemberByUserId(userId);
				if (member == null) {
					callback.onNoActiveMission();
					return;
				}

				SpecialMission mission = specialMissionDao.getActiveMissionByGuild(member.getGuildId());
				if (mission == null) {
					callback.onNoActiveMission();
					return;
				}

				long now = System.currentTimeMillis();
				if (now > mission.getEndDate()) {
					callback.onNoActiveMission();
					return;
				}

				SpecialMissionProgress progress = progressDao.getByMissionAndUser(mission.getId(), userId);
				if (progress == null) {
					// initialize if missing
					progress = new SpecialMissionProgress();
					progress.setSpecialMissionId(mission.getId());
					progress.setUserId(userId);
				}

				if (progress.getShopPurchases() >= 5) {
					callback.onUpdated("Shop purchase cap reached for mission");
					return;
				}

				// apply damage and increment
				int damage = 2;
				int newHp = Math.max(0, mission.getCurrentBossHP() - damage);
				mission.setCurrentBossHP(newHp);
				if (newHp == 0) {
					mission.setStatus(SpecialMission.STATUS_COMPLETED);
					mission.setSuccessful(true);
					guildDao.updateGuildMissionStatus(member.getGuildId(), false);
				}
				specialMissionDao.update(mission);

				progress.setShopPurchases(progress.getShopPurchases() + 1);
				progress.setTotalDamageDealt(progress.getTotalDamageDealt() + damage);
				progressDao.insert(progress);

				callback.onUpdated("Special mission progress updated: -2 HP");
			} catch (Exception e) {
				callback.onError("Failed to update mission progress: " + e.getMessage());
			}
		});
	}
}


