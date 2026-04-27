package com.example.smartscan;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NotesDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "smartscan_notes.db";
    public static final int DATABASE_VERSION = 1;

    public NotesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notes ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id TEXT NOT NULL,"
                + "title TEXT NOT NULL,"
                + "raw_text TEXT NOT NULL,"
                + "summary TEXT NOT NULL,"
                + "keywords TEXT,"
                + "created_at INTEGER NOT NULL"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS notes");
        onCreate(db);
    }
}

