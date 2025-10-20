package com.habitrpg.taskmanager.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.habitrpg.taskmanager.data.database.entities.SpecialMission;

import java.util.List;

@Dao
public interface SpecialMissionDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	void insert(SpecialMission mission);

	@Update
	void update(SpecialMission mission);

	@Query("SELECT * FROM special_missions WHERE guild_id = :guildId AND status = 'ACTIVE' LIMIT 1")
	SpecialMission getActiveMissionByGuild(String guildId);

	@Query("SELECT * FROM special_missions WHERE id = :missionId LIMIT 1")
	SpecialMission getById(String missionId);

	@Query("SELECT * FROM special_missions WHERE guild_id = :guildId ORDER BY start_date DESC")
	List<SpecialMission> getMissionsByGuild(String guildId);
}


