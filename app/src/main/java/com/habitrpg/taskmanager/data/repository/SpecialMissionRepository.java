package com.habitrpg.taskmanager.data.repository;

import android.content.Context;

import com.habitrpg.taskmanager.data.database.AppDatabase;
import com.habitrpg.taskmanager.data.database.dao.GuildDao;
import com.habitrpg.taskmanager.data.database.dao.SpecialMissionDao;
import com.habitrpg.taskmanager.data.database.dao.SpecialMissionProgressDao;
import com.habitrpg.taskmanager.data.database.dao.TaskDao;
import com.habitrpg.taskmanager.data.database.dao.UserDao;
import com.habitrpg.taskmanager.data.database.dao.EquipmentDao;
import com.habitrpg.taskmanager.data.database.dao.UserStatisticsDao;
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

	public static class ProgressSummary {
		public boolean hasActiveMission;
		public int shopPurchases;
		public int regularBossHits;
		public int easyTasksCompleted;
		public int otherTasksCompleted;
		public boolean hasNoUnresolvedTasks;
		public int daysWithMessagesCount;
		public int hpFromShop;
		public int hpFromRegularHits;
		public int hpFromEasyTasks;
		public int hpFromOtherTasks;
		public int hpFromNoUnresolved;
		public int hpFromMessages;
		public int totalHp;
	}

	public interface ProgressSummaryCallback {
		void onSuccess(ProgressSummary summary);
		void onNoActiveMission();
		void onError(String error);
	}

	public static class GuildMemberProgress {
		public String userId;
		public String username;
		public int shopPurchases;
		public int regularBossHits;
		public int easyTasksCompleted;
		public int otherTasksCompleted;
		public boolean hasNoUnresolvedTasks;
		public int daysWithMessagesCount;
		public int totalHp;
	}

	public static class GuildProgressSummary {
		public boolean hasActiveMission;
		public String missionId;
		public int initialBossHp;
		public int currentBossHp;
		public long endDate;
		public java.util.List<GuildMemberProgress> members;
	}

	public interface GuildProgressSummaryCallback {
		void onSuccess(GuildProgressSummary summary);
		void onNoActiveMission();
		void onError(String error);
	}

    public interface FinalizeCallback {
        void onCompleted(String message);
        void onNoAction();
        void onError(String error);
    }

	private static SpecialMissionRepository instance;
	private final GuildDao guildDao;
	private final SpecialMissionDao specialMissionDao;
	private final SpecialMissionProgressDao progressDao;
    private final TaskDao taskDao;
    private final UserDao userDao;
    private final EquipmentDao equipmentDao;
    private final UserStatisticsDao userStatisticsDao;
	private ExecutorService executor;

	private SpecialMissionRepository(Context context) {
		AppDatabase db = AppDatabase.getDatabase(context);
		this.guildDao = db.guildDao();
		this.specialMissionDao = db.specialMissionDao();
		this.progressDao = db.specialMissionProgressDao();
        this.taskDao = db.taskDao();
        this.userDao = db.userDao();
        this.equipmentDao = db.equipmentDao();
        this.userStatisticsDao = db.userStatisticsDao();
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
				int initialHp = 10 * Math.max(memberCount, 0);
				//TREBA 100
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
				
				// Proveri da li je boss poražen i dodeli nagrade NAKON ažuriranja
				if (newHp == 0) {
					checkBossDefeatAndAwardRewards(mission, userId, callback);
				}

				progress.setShopPurchases(progress.getShopPurchases() + 1);
				progress.setTotalDamageDealt(progress.getTotalDamageDealt() + damage);
				progressDao.insert(progress);
				System.out.println("Shop purchase recorded for userId: " + userId + " on missionId: " + mission.getId());
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
				
				// Proveri da li je boss poražen i dodeli nagrade NAKON ažuriranja
				if (newHp == 0) {
					checkBossDefeatAndAwardRewards(mission, userId, callback);
				}

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
				
				// Proveri da li je boss poražen i dodeli nagrade NAKON ažuriranja
				if (newHp == 0) {
					checkBossDefeatAndAwardRewards(mission, userId, callback);
				}

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
				
				// Proveri da li je boss poražen i dodeli nagrade NAKON ažuriranja
				if (newHp == 0) {
					checkBossDefeatAndAwardRewards(mission, userId, callback);
				}

				progress.setHasNoUnresolvedTasks(true);
				progress.setTotalDamageDealt(progress.getTotalDamageDealt() + damage);
				progressDao.insert(progress);

				callback.onUpdated("Special mission updated: no-unresolved-tasks bonus -10 HP");
			} catch (Exception e) {
				callback.onError("Failed to apply no-unresolved-tasks bonus: " + e.getMessage());
			}
		});
	}

	public void recordGuildMessageDay(String userId, ProgressUpdateCallback callback) {
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

				java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
				String todayKey = sdf.format(new java.util.Date(now));

				SpecialMissionProgress progress = progressDao.getByMissionAndUser(mission.getId(), userId);
				if (progress == null) {
					progress = new SpecialMissionProgress();
					progress.setSpecialMissionId(mission.getId());
					progress.setUserId(userId);
				}

				java.util.Set<String> days = progress.parseDaysWithMessages();
				if (days.contains(todayKey)) {
					callback.onUpdated("Guild message already counted today");
					return;
				}
				days.add(todayKey);
				progress.setDaysWithMessagesFromSet(days);

				int damage = 4;
				int newHp = Math.max(0, mission.getCurrentBossHP() - damage);
				mission.setCurrentBossHP(newHp);
				if (newHp == 0) {
					mission.setStatus(SpecialMission.STATUS_COMPLETED);
					mission.setSuccessful(true);
					guildDao.updateGuildMissionStatus(member.getGuildId(), false);
				}
				specialMissionDao.update(mission);
				
				// Proveri da li je boss poražen i dodeli nagrade NAKON ažuriranja
				if (newHp == 0) {
					checkBossDefeatAndAwardRewards(mission, userId, callback);
				}

				progress.setTotalDamageDealt(progress.getTotalDamageDealt() + damage);
				progressDao.insert(progress);

				callback.onUpdated("Special mission updated: guild message -4 HP");
			} catch (Exception e) {
				callback.onError("Failed to update guild message progress: " + e.getMessage());
			}
		});
	}

	public void getUserProgressSummary(String userId, ProgressSummaryCallback callback) {
		ensureExecutorActive();
		executor.execute(() -> {
			try {
				GuildMember member = guildDao.getGuildMemberByUserId(userId);
				if (member == null) { callback.onNoActiveMission(); return; }
				SpecialMission mission = specialMissionDao.getActiveMissionByGuild(member.getGuildId());
				if (mission == null) { callback.onNoActiveMission(); return; }

				SpecialMissionProgress progress = progressDao.getByMissionAndUser(mission.getId(), userId);
				ProgressSummary s = new ProgressSummary();
				s.hasActiveMission = true;
				if (progress != null) {
					s.shopPurchases = Math.min(5, Math.max(0, progress.getShopPurchases()));
					s.regularBossHits = Math.min(10, Math.max(0, progress.getRegularBossHits()));
					s.easyTasksCompleted = Math.min(10, Math.max(0, progress.getEasyTasksCompleted()));
					s.otherTasksCompleted = Math.min(6, Math.max(0, progress.getOtherTasksCompleted()));
					s.hasNoUnresolvedTasks = progress.isHasNoUnresolvedTasks();
					java.util.Set<String> days = progress.parseDaysWithMessages();
					s.daysWithMessagesCount = days != null ? days.size() : 0;
				} else {
					s.shopPurchases = 0;
					s.regularBossHits = 0;
					s.easyTasksCompleted = 0;
					s.otherTasksCompleted = 0;
					s.hasNoUnresolvedTasks = false;
					s.daysWithMessagesCount = 0;
				}

				s.hpFromShop = s.shopPurchases * 2;
				s.hpFromRegularHits = s.regularBossHits * 2;
				s.hpFromEasyTasks = s.easyTasksCompleted * 1;
				s.hpFromOtherTasks = s.otherTasksCompleted * 4;
				s.hpFromNoUnresolved = s.hasNoUnresolvedTasks ? 10 : 0;
				s.hpFromMessages = s.daysWithMessagesCount * 4;
				s.totalHp = s.hpFromShop + s.hpFromRegularHits + s.hpFromEasyTasks + s.hpFromOtherTasks + s.hpFromNoUnresolved + s.hpFromMessages;

				callback.onSuccess(s);
			} catch (Exception e) {
				callback.onError("Failed to load progress summary: " + e.getMessage());
			}
		});
	}

	public void getGuildProgressSummary(String guildId, GuildProgressSummaryCallback callback) {
		ensureExecutorActive();
		executor.execute(() -> {
			try {
				SpecialMission mission = specialMissionDao.getActiveMissionByGuild(guildId);
				if (mission == null) { callback.onNoActiveMission(); return; }

				java.util.List<GuildMember> members = guildDao.getGuildMembersByGuildId(guildId);
				java.util.List<SpecialMissionProgress> progresses = progressDao.getAllForMission(mission.getId());
				java.util.Map<String, SpecialMissionProgress> byUser = new java.util.HashMap<>();
				for (SpecialMissionProgress p : progresses) { byUser.put(p.getUserId(), p); }

				GuildProgressSummary g = new GuildProgressSummary();
				g.hasActiveMission = true;
				g.missionId = mission.getId();
				g.initialBossHp = mission.getInitialBossHP();
				g.currentBossHp = mission.getCurrentBossHP();
				g.endDate = mission.getEndDate();
				g.members = new java.util.ArrayList<>();

				for (GuildMember m : members) {
					GuildMemberProgress mp = new GuildMemberProgress();
					mp.userId = m.getUserId();
					mp.username = m.getUsername();
					SpecialMissionProgress p = byUser.get(m.getUserId());
					if (p != null) {
						mp.shopPurchases = Math.min(5, Math.max(0, p.getShopPurchases()));
						mp.regularBossHits = Math.min(10, Math.max(0, p.getRegularBossHits()));
						mp.easyTasksCompleted = Math.min(10, Math.max(0, p.getEasyTasksCompleted()));
						mp.otherTasksCompleted = Math.min(6, Math.max(0, p.getOtherTasksCompleted()));
						mp.hasNoUnresolvedTasks = p.isHasNoUnresolvedTasks();
						java.util.Set<String> days = p.parseDaysWithMessages();
						mp.daysWithMessagesCount = days != null ? days.size() : 0;
					} else {
						mp.shopPurchases = 0;
						mp.regularBossHits = 0;
						mp.easyTasksCompleted = 0;
						mp.otherTasksCompleted = 0;
						mp.hasNoUnresolvedTasks = false;
						mp.daysWithMessagesCount = 0;
					}

					int hpFromShop = mp.shopPurchases * 2;
					int hpFromHits = mp.regularBossHits * 2;
					int hpFromEasy = mp.easyTasksCompleted * 1;
					int hpFromOther = mp.otherTasksCompleted * 4;
					int hpFromNoUnresolved = mp.hasNoUnresolvedTasks ? 10 : 0;
					int hpFromMsgs = mp.daysWithMessagesCount * 4;
					mp.totalHp = hpFromShop + hpFromHits + hpFromEasy + hpFromOther + hpFromNoUnresolved + hpFromMsgs;
					g.members.add(mp);
				}

				callback.onSuccess(g);
			} catch (Exception e) {
				callback.onError("Failed to load guild progress: " + e.getMessage());
			}
		});
	}

	// Finalize a mission for a given guild if endDate passed; grant rewards if victory
	public void finalizeIfExpiredForGuild(String guildId, FinalizeCallback callback) {
		ensureExecutorActive();
		executor.execute(() -> {
			try {
				SpecialMission mission = specialMissionDao.getActiveMissionByGuild(guildId);
				if (mission == null) { callback.onNoAction(); return; }
				long now = System.currentTimeMillis();
				if (now < mission.getEndDate()) { callback.onNoAction(); return; }

				boolean victory = mission.getCurrentBossHP() <= 0;
				if (victory) {
					// Rewards for all guild members
					java.util.List<GuildMember> members = guildDao.getGuildMembersByGuildId(guildId);
					for (GuildMember gm : members) {
						grantPotionAndClothingSafe(gm.getUserId());
						grantHalfNextBossCoinsSafe(gm.getUserId());
						incrementUserStatsCompletedSafe(gm.getUserId());
					}
					mission.setStatus(SpecialMission.STATUS_COMPLETED);
					mission.setSuccessful(true);
				} else {
					mission.setStatus(SpecialMission.STATUS_COMPLETED);
					mission.setSuccessful(false);
				}
				specialMissionDao.update(mission);
				guildDao.updateGuildMissionStatus(guildId, false);
				callback.onCompleted("Mission finalized");
			} catch (Exception e) {
				callback.onError("Failed to finalize mission: " + e.getMessage());
			}
		});
	}

	private void checkBossDefeatAndAwardRewards(SpecialMission mission, String userId, ProgressUpdateCallback callback) {
		try {
			// Proveri da li je boss poražen koristeći prosleđeni mission objekat
			if (mission != null && mission.getCurrentBossHP() <= 0 && mission.isSuccessful()) {
				// Boss je poražen - dodeli nagrade svim članovima saveza
				java.util.List<GuildMember> members = guildDao.getGuildMembersByGuildId(mission.getGuildId());
				for (GuildMember member : members) {
					grantPotionAndClothingSafe(member.getUserId());
					grantHalfNextBossCoinsSafe(member.getUserId());
					incrementUserStatsCompletedSafe(member.getUserId());
				}
				
				// Prikaži Toast poruku korisniku koji je poslednji napao boss-a
				android.util.Log.d("SpecialMission", "Boss defeated! Rewards awarded to all guild members");
				
				// Dodaj callback za Toast poruku
				if (callback != null) {
					callback.onUpdated("🎉 Boss poražen! Nagrade dodeljene svim članovima saveza!");
				}
			}
		} catch (Exception e) {
			android.util.Log.e("SpecialMission", "Error checking boss defeat: " + e.getMessage());
		}
	}

	private void grantPotionAndClothingSafe(String userId) {
        try {
            com.habitrpg.taskmanager.data.database.entities.Equipment potion = new com.habitrpg.taskmanager.data.database.entities.Equipment();
            potion.setEquipmentId(java.util.UUID.randomUUID().toString());
            potion.setUserId(userId);
            potion.setEquipmentType("potion");
            potion.setEquipmentName("Mission Potion");
            potion.setEquipmentDescription("Special mission reward");
            potion.setPrice(0);
            potion.setIconResource("ic_potion");
            potion.setBonusType("strength");
            potion.setBonusValue(5.0);
            potion.setBonusDuration("single_use");
            potion.setActive(false);
            potion.setDurability(1);
            equipmentDao.insertEquipment(potion);

            com.habitrpg.taskmanager.data.database.entities.Equipment clothing = new com.habitrpg.taskmanager.data.database.entities.Equipment();
            clothing.setEquipmentId(java.util.UUID.randomUUID().toString());
            clothing.setUserId(userId);
            clothing.setEquipmentType("clothing");
            clothing.setEquipmentName("Mission Garment");
            clothing.setEquipmentDescription("Special mission reward");
            clothing.setPrice(0);
            clothing.setIconResource("ic_clothing");
            clothing.setBonusType("strength");
            clothing.setBonusValue(5.0);
            clothing.setBonusDuration("permanent");
            clothing.setActive(false);
            clothing.setDurability(-1);
            equipmentDao.insertEquipment(clothing);
        } catch (Exception ignored) {}
    }

    private void grantHalfNextBossCoinsSafe(String userId) {
        try {
            com.habitrpg.taskmanager.data.database.entities.User user = userDao.getUserById(userId);
            if (user == null) return;
            int currentLevel = user.getLevel();
            int nextLevel = Math.max(1, currentLevel + 1);
            int baseCoinReward = (int) (200 * Math.pow(1.20, nextLevel - 1));
            int half = (int) Math.round(baseCoinReward * 0.50);
            user.setCoins(user.getCoins() + half);
            userDao.updateUser(user);
        } catch (Exception ignored) {}
    }

    private void incrementUserStatsCompletedSafe(String userId) {
        try {
            com.habitrpg.taskmanager.data.database.entities.UserStatistics us = userStatisticsDao.getUserStatisticsByUserId(userId);
            if (us == null) {
                us = new com.habitrpg.taskmanager.data.database.entities.UserStatistics(userId);
            }
            us.setTotalSpecialMissionsCompleted(us.getTotalSpecialMissionsCompleted() + 1);
            userStatisticsDao.insertUserStatistics(us);
        } catch (Exception ignored) {}
    }
}


