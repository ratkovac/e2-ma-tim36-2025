package com.habitrpg.taskmanager.data.database.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity(tableName = "special_mission_progress")
public class SpecialMissionProgress {

	@PrimaryKey
	@NonNull
	@ColumnInfo(name = "id")
	private String id;

	@ColumnInfo(name = "special_mission_id")
	private String specialMissionId;

	@ColumnInfo(name = "user_id")
	private String userId;

	@ColumnInfo(name = "shop_purchases")
	private int shopPurchases; // 0-5

	@ColumnInfo(name = "regular_boss_hits")
	private int regularBossHits; // 0-10

	@ColumnInfo(name = "easy_tasks_completed")
	private int easyTasksCompleted; // 0-10 (weights applied elsewhere)

	@ColumnInfo(name = "other_tasks_completed")
	private int otherTasksCompleted; // 0-6

	@ColumnInfo(name = "has_no_unresolved_tasks")
	private boolean hasNoUnresolvedTasks;

	@ColumnInfo(name = "days_with_messages")
	private String daysWithMessages; // stored as comma-separated epoch-day strings

	@ColumnInfo(name = "total_damage_dealt")
	private int totalDamageDealt;

	public SpecialMissionProgress() {
		this.id = UUID.randomUUID().toString();
		this.specialMissionId = "";
		this.userId = "";
		this.shopPurchases = 0;
		this.regularBossHits = 0;
		this.easyTasksCompleted = 0;
		this.otherTasksCompleted = 0;
		this.hasNoUnresolvedTasks = false;
		this.daysWithMessages = "";
		this.totalDamageDealt = 0;
	}

	@Ignore
	public SpecialMissionProgress(@NonNull String id, String specialMissionId, String userId,
									int shopPurchases, int regularBossHits, int easyTasksCompleted,
									int otherTasksCompleted, boolean hasNoUnresolvedTasks,
									String daysWithMessages, int totalDamageDealt) {
		this.id = id;
		this.specialMissionId = specialMissionId;
		this.userId = userId;
		this.shopPurchases = shopPurchases;
		this.regularBossHits = regularBossHits;
		this.easyTasksCompleted = easyTasksCompleted;
		this.otherTasksCompleted = otherTasksCompleted;
		this.hasNoUnresolvedTasks = hasNoUnresolvedTasks;
		this.daysWithMessages = daysWithMessages;
		this.totalDamageDealt = totalDamageDealt;
	}

	@NonNull
	public String getId() {
		return id;
	}

	public void setId(@NonNull String id) {
		this.id = id;
	}

	public String getSpecialMissionId() {
		return specialMissionId;
	}

	public void setSpecialMissionId(String specialMissionId) {
		this.specialMissionId = specialMissionId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public int getShopPurchases() {
		return shopPurchases;
	}

	public void setShopPurchases(int shopPurchases) {
		this.shopPurchases = shopPurchases;
	}

	public int getRegularBossHits() {
		return regularBossHits;
	}

	public void setRegularBossHits(int regularBossHits) {
		this.regularBossHits = regularBossHits;
	}

	public int getEasyTasksCompleted() {
		return easyTasksCompleted;
	}

	public void setEasyTasksCompleted(int easyTasksCompleted) {
		this.easyTasksCompleted = easyTasksCompleted;
	}

	public int getOtherTasksCompleted() {
		return otherTasksCompleted;
	}

	public void setOtherTasksCompleted(int otherTasksCompleted) {
		this.otherTasksCompleted = otherTasksCompleted;
	}

	public boolean isHasNoUnresolvedTasks() {
		return hasNoUnresolvedTasks;
	}

	public void setHasNoUnresolvedTasks(boolean hasNoUnresolvedTasks) {
		this.hasNoUnresolvedTasks = hasNoUnresolvedTasks;
	}

	public String getDaysWithMessages() {
		return daysWithMessages;
	}

	public void setDaysWithMessages(String daysWithMessages) {
		this.daysWithMessages = daysWithMessages;
	}

	public int getTotalDamageDealt() {
		return totalDamageDealt;
	}

	public void setTotalDamageDealt(int totalDamageDealt) {
		this.totalDamageDealt = totalDamageDealt;
	}

	// Helpers to work with the stored comma-separated days (epoch-day strings)
	public Set<String> parseDaysWithMessages() {
		Set<String> days = new HashSet<>();
		if (daysWithMessages == null || daysWithMessages.isEmpty()) {
			return days;
		}
		String[] parts = daysWithMessages.split(",");
		for (String p : parts) {
			String trimmed = p.trim();
			if (!trimmed.isEmpty()) {
				days.add(trimmed);
			}
		}
		return days;
	}

	public void setDaysWithMessagesFromSet(Set<String> days) {
		if (days == null || days.isEmpty()) {
			this.daysWithMessages = "";
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (String d : days) {
			if (sb.length() > 0) sb.append(',');
			sb.append(d);
		}
		this.daysWithMessages = sb.toString();
	}
}


