package com.krishna.crimedetection.models;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {CrimeRecord.class, VideoUploadRecord.class, EmergencyContact.class, CrimeSceneEvidence.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract CrimeDao crimeDao();
    public abstract VideoUploadDao videoUploadDao();
    public abstract EmergencyContactDao emergencyContactDao();
    public abstract CrimeSceneEvidenceDao crimeSceneEvidenceDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "crime_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}