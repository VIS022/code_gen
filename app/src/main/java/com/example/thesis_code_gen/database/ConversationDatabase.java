package com.example.thesis_code_gen.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {ConversationMessage.class}, version = 2)
public abstract class ConversationDatabase extends RoomDatabase {
    public abstract ConversationDao conversationDao();

    private static volatile ConversationDatabase INSTANCE;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE conversation_messages ADD COLUMN conversationId INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE conversation_messages ADD COLUMN conversationTitle TEXT");
        }
    };

    public static ConversationDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ConversationDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    ConversationDatabase.class, "conversation_database")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}