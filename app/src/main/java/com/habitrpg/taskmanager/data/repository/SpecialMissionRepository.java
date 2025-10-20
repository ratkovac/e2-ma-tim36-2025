package com.habitrpg.taskmanager.data.repository;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.dao.GuildDao;
import com.habitrpg.taskmanager.data.database.dao.SpecialMissionDao;
import com.habitrpg.taskmanager.data.database.dao.SpecialMissionProgressDao;
import com.habitrpg.taskmanager.data.database.dao.TaskDao;
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
	private final TaskDao taskDao;
	private ExecutorService executor;

	private SpecialMissionRepository(Context context) {
		AppDatabase db = AppDatabase.getDatabase(context);
		this.guildDao = db.guildDao();
		this.specialMissionDao = db.specialMissionDao();
		this.progressDao = db.specialMissionProgressDao();
		this.taskDao = db.taskDao();
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

	public void recordRegularBossHit(String userId, ProgressUpdateCallback callback) {
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
					progress = new SpecialMissionProgress();
					progress.setSpecialMissionId(mission.getId());
					progress.setUserId(userId);
				}

				if (progress.getRegularBossHits() >= 10) {
					callback.onUpdated("Regular boss hit cap reached for mission");
					return;
				}

				int damage = 2;
				int newHp = Math.max(0, mission.getCurrentBossHP() - damage);
				mission.setCurrentBossHP(newHp);
				if (newHp == 0) {
					mission.setStatus(SpecialMission.STATUS_COMPLETED);
					mission.setSuccessful(true);
					guildDao.updateGuildMissionStatus(member.getGuildId(), false);
				}
				specialMissionDao.update(mission);

				progress.setRegularBossHits(progress.getRegularBossHits() + 1);
				progress.setTotalDamageDealt(progress.getTotalDamageDealt() + damage);
				progressDao.insert(progress);

				callback.onUpdated("Special mission progress updated: regular hit -2 HP");
			} catch (Exception e) {
				callback.onError("Failed to update mission progress: " + e.getMessage());
			}
		});
	}

	public void recordTaskCompletion(String userId, String difficulty, String importance, ProgressUpdateCallback callback) {
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

				boolean isInEasySet =
					("very_easy".equals(difficulty)) ||
					("easy".equals(difficulty)) ||
					("normal".equals(importance)) ||
					("important".equals(importance));

				int totalDamage;
				SpecialMissionProgress progress = progressDao.getByMissionAndUser(mission.getId(), userId);
				if (progress == null) {
					progress = new SpecialMissionProgress();
					progress.setSpecialMissionId(mission.getId());
					progress.setUserId(userId);
				}

				if (isInEasySet) {
					int multiplier = ("easy".equals(difficulty) || "normal".equals(importance)) ? 2 : 1;
					int count = multiplier;
					if (progress.getEasyTasksCompleted() + count > 10) {
						callback.onUpdated("Easy tasks cap reached for mission");
						return;
					}
					totalDamage = 1 * multiplier;
					progress.setEasyTasksCompleted(progress.getEasyTasksCompleted() + count);
				} else {
					if (progress.getOtherTasksCompleted() >= 6) {
						callback.onUpdated("Other tasks cap reached for mission");
						return;
					}
					totalDamage = 4;
					progress.setOtherTasksCompleted(progress.getOtherTasksCompleted() + 1);
				}

				int newHp = Math.max(0, mission.getCurrentBossHP() - totalDamage);
				mission.setCurrentBossHP(newHp);
				if (newHp == 0) {
					mission.setStatus(SpecialMission.STATUS_COMPLETED);
					mission.setSuccessful(true);
					guildDao.updateGuildMissionStatus(member.getGuildId(), false);
				}
				specialMissionDao.update(mission);

				progress.setTotalDamageDealt(progress.getTotalDamageDealt() + totalDamage);
				progressDao.insert(progress);

				callback.onUpdated("Special mission progress updated by task completion");
			} catch (Exception e) {
				callback.onError("Failed to update mission progress: " + e.getMessage());
			}
		});
	}

	public void checkAndApplyNoUnresolvedTasksBonus(String userId, ProgressUpdateCallback callback) {
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
					progress = new SpecialMissionProgress();
					progress.setSpecialMissionId(mission.getId());
					progress.setUserId(userId);
				}

				if (progress.isHasNoUnresolvedTasks()) {
					callback.onUpdated("No-unresolved bonus already applied");
					return;
				}

				// Determine if user has unresolved tasks during mission window
				java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
				String missionStartDate = sdf.format(new java.util.Date(mission.getStartDate()));
				String missionEndDate = sdf.format(new java.util.Date(mission.getEndDate()));
				java.util.List<com.habitrpg.taskmanager.data.database.entities.Task> tasks = taskDao.getTasksInDateRange(userId, missionStartDate, missionEndDate);

				boolean hasUnresolved = false;
				for (com.habitrpg.taskmanager.data.database.entities.Task t : tasks) {
					String status = t.getStatus();
					if (!"completed".equals(status) && !"cancelled".equals(status)) {
						hasUnresolved = true;
						break;
					}
				}

				if (hasUnresolved) {
					callback.onUpdated("User has unresolved tasks - no bonus");
					return;
				}

				// Apply -10 HP once per mission per user
				int damage = 10;
				int newHp = Math.max(0, mission.getCurrentBossHP() - damage);
				mission.setCurrentBossHP(newHp);
				if (newHp == 0) {
					mission.setStatus(SpecialMission.STATUS_COMPLETED);
					mission.setSuccessful(true);
					guildDao.updateGuildMissionStatus(member.getGuildId(), false);
				}
				specialMissionDao.update(mission);

				progress.setHasNoUnresolvedTasks(true);
				progress.setTotalDamageDealt(progress.getTotalDamageDealt() + damage);
				progressDao.insert(progress);

				callback.onUpdated("Special mission updated: no-unresolved-tasks bonus -10 HP");
			} catch (Exception e) {
				callback.onError("Failed to apply no-unresolved-tasks bonus: " + e.getMessage());
			}
		});
	}
}


