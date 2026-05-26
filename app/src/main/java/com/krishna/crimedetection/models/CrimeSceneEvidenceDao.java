package com.krishna.crimedetection.models;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface CrimeSceneEvidenceDao {
    @Insert
    long insertEvidence(CrimeSceneEvidence evidence);

    @Query("SELECT * FROM crime_scene_evidence WHERE incidentId = :incidentId")
    CrimeSceneEvidence getEvidenceForIncident(int incidentId);
}
