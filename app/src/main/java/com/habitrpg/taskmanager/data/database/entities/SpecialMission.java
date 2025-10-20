package com.habitrpg.taskmanager.data.database.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "special_missions")
public class SpecialMission {

	public static final String STATUS_ACTIVE = "ACTIVE";
	public static final String STATUS_COMPLETED = "COMPLETED";
	public static final String STATUS_FAILED = "FAILED";

	@PrimaryKey
	@NonNull
	@ColumnInfo(name = "id")
	private String id;

	@ColumnInfo(name = "guild_id")
	private String guildId;

	@ColumnInfo(name = "start_date")
	private long startDate;

	@ColumnInfo(name = "end_date")
	private long endDate;

	@ColumnInfo(name = "initial_boss_hp")
	private int initialBossHP;

	@ColumnInfo(name = "current_boss_hp")
	private int currentBossHP;

	@ColumnInfo(name = "status")
	private String status;

	@ColumnInfo(name = "is_successful")
	private boolean isSuccessful;

	public SpecialMission() {
		this.id = UUID.randomUUID().toString();
		this.guildId = "";
		this.startDate = System.currentTimeMillis();
		long twoWeeksMs = 14L * 24L * 60L * 60L * 1000L;
		this.endDate = this.startDate + twoWeeksMs;
		this.initialBossHP = 0;
		this.currentBossHP = 0;
		this.status = STATUS_ACTIVE;
		this.isSuccessful = false;
	}

	@Ignore
	public SpecialMission(@NonNull String id, String guildId, long startDate, long endDate,
							int initialBossHP, int currentBossHP, String status, boolean isSuccessful) {
		this.id = id;
		this.guildId = guildId;
		this.startDate = startDate;
		this.endDate = endDate;
		this.initialBossHP = initialBossHP;
		this.currentBossHP = currentBossHP;
		this.status = status;
		this.isSuccessful = isSuccessful;
	}

	@NonNull
	public String getId() {
		return id;
	}

	public void setId(@NonNull String id) {
		this.id = id;
	}

	public String getGuildId() {
		return guildId;
	}

	public void setGuildId(String guildId) {
		this.guildId = guildId;
	}

	public long getStartDate() {
		return startDate;
	}

	public void setStartDate(long startDate) {
		this.startDate = startDate;
	}

	public long getEndDate() {
		return endDate;
	}

	public void setEndDate(long endDate) {
		this.endDate = endDate;
	}

	public int getInitialBossHP() {
		return initialBossHP;
	}

	public void setInitialBossHP(int initialBossHP) {
		this.initialBossHP = initialBossHP;
	}

	public int getCurrentBossHP() {
		return currentBossHP;
	}

	public void setCurrentBossHP(int currentBossHP) {
		this.currentBossHP = currentBossHP;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean isSuccessful() {
		return isSuccessful;
	}

	public void setSuccessful(boolean successful) {
		isSuccessful = successful;
	}
}


