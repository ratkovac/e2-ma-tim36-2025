package com.habitrpg.taskmanager.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.habitrpg.taskmanager.data.database.entities.SpecialMissionProgress;

import java.util.List;

@Dao
public interface SpecialMissionProgressDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	void insert(SpecialMissionProgress progress);

	@Update
	void update(SpecialMissionProgress progress);

	@Query("SELECT * FROM special_mission_progress WHERE special_mission_id = :missionId AND user_id = :userId LIMIT 1")
	SpecialMissionProgress getByMissionAndUser(String missionId, String userId);

	@Query("SELECT * FROM special_mission_progress WHERE special_mission_id = :missionId")
	List<SpecialMissionProgress> getAllForMission(String missionId);

	@Query("DELETE FROM special_mission_progress WHERE special_mission_id = :missionId")
	void deleteAllForMission(String missionId);
}


