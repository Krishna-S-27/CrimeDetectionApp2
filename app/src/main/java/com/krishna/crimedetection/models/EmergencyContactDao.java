package com.krishna.crimedetection.models;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface EmergencyContactDao {
    @Insert
    long insertContact(EmergencyContact contact);

    @Update
    void updateContact(EmergencyContact contact);

    @Delete
    void deleteContact(EmergencyContact contact);

    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId AND isActive = 1")
    List<EmergencyContact> getActiveContacts(int userId);

    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId AND isPrimary = 1 LIMIT 1")
    EmergencyContact getPrimaryContact(int userId);

    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId")
    List<EmergencyContact> getAllContacts(int userId);

    @Query("DELETE FROM emergency_contacts WHERE userId = :userId AND id = :contactId")
    void deleteContactById(int userId, int contactId);
}
