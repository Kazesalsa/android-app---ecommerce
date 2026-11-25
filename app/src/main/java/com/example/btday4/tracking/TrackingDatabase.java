package com.example.btday4.tracking;

import android.content.Context;
import android.util.Log;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Standalone Room database ("my_tracking_room.db"), completely isolated from AppDatabase.
 */
@Database(entities = {UserEventEntity.class}, version = 2, exportSchema = false)
public abstract class TrackingDatabase extends RoomDatabase {

    public static final String DB_NAME = "my_tracking_room.db";

    private static volatile TrackingDatabase instance;
    private static final ExecutorService TRACKING_EXECUTOR = Executors.newSingleThreadExecutor();

    public abstract UserEventDao userEventDao();

    public static TrackingDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (TrackingDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    TrackingDatabase.class,
                                    DB_NAME
                            )
                            .fallbackToDestructiveMigration(true)
                            .build();
                }
            }
        }
        return instance;
    }

    public static void runAsync(Context context, TrackingDbTask task) {
        Context appContext = context.getApplicationContext();
        TRACKING_EXECUTOR.execute(() -> {
            try {
                task.run(getInstance(appContext));
            } catch (Exception e) {
                Log.e("TrackingDatabase", "Tracking DB operation failed", e);
            }
        });
    }

    public interface TrackingDbTask {
        void run(TrackingDatabase db);
    }
}
