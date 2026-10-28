package com.example.myapp.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.myapp.data.local.dao.AgriDao;
import com.example.myapp.data.local.entities.AlertEntity;
import com.example.myapp.data.local.entities.DeviceEntity;
import com.example.myapp.data.local.entities.SensorDataEntity;

@Database(entities = {SensorDataEntity.class, AlertEntity.class, DeviceEntity.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract AgriDao agriDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "agri_guard_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}