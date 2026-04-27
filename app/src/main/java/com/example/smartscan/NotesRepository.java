package com.example.smartscan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class NotesRepository {

    private final NotesDbHelper dbHelper;

    public NotesRepository(Context context) {
        this.dbHelper = new NotesDbHelper(context);
    }

    public long insertNote(String userId, String title, String rawText, String summary, String keywords) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("title", title);
        values.put("raw_text", rawText);
        values.put("summary", summary);
        values.put("keywords", keywords);
        values.put("created_at", System.currentTimeMillis());
        return db.insert("notes", null, values);
    }

    public List<Note> getNotesForUser(String userId, String query) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection;
        String[] args;
        if (query == null || query.trim().isEmpty()) {
            selection = "user_id=?";
            args = new String[]{userId};
        } else {
            String like = "%" + query.trim() + "%";
            selection = "user_id=? AND ("
                    + "title LIKE ? OR "
                    + "summary LIKE ? OR "
                    + "keywords LIKE ? OR "
                    + "raw_text LIKE ?)";
            args = new String[]{userId, like, like, like, like};
        }

        String sql = "SELECT * FROM notes WHERE " + selection + " ORDER BY created_at DESC";
        Cursor cursor = db.rawQuery(sql, args);

        while (cursor.moveToNext()) {
            notes.add(cursorToNote(cursor));
        }
        cursor.close();
        return notes;
    }

    public Note getNoteById(long noteId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM notes WHERE id=?", new String[]{String.valueOf(noteId)});

        Note note = null;
        if (cursor.moveToFirst()) {
            note = cursorToNote(cursor);
        }
        cursor.close();
        return note;
    }

    public void deleteNote(long noteId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("notes", "id=?", new String[]{String.valueOf(noteId)});
    }

    private Note cursorToNote(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        String userId = cursor.getString(cursor.getColumnIndexOrThrow("user_id"));
        String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        String rawText = cursor.getString(cursor.getColumnIndexOrThrow("raw_text"));
        String summary = cursor.getString(cursor.getColumnIndexOrThrow("summary"));
        String keywords = cursor.getString(cursor.getColumnIndexOrThrow("keywords"));
        long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));

        return new Note(id, userId, title, rawText, summary, keywords, createdAt);
    }
}
