package com.example.tetris.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tetris.db.DBHelper;
import com.example.tetris.util.Constants;

public class ScoreProvider extends ContentProvider {

    private DBHelper dbHelper;
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        MATCHER.addURI(Constants.AUTHORITY, Constants.TABLE_PLAYER, Constants.PLAYER);
        MATCHER.addURI(Constants.AUTHORITY, Constants.TABLE_SCORE, Constants.SCORE);
        MATCHER.addURI(Constants.AUTHORITY, Constants.TABLE_SCORE + "/#", Constants.SCORE_ID);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DBHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;
        int code = MATCHER.match(uri);

        switch (code) {
            case Constants.PLAYER:
                cursor = db.query(Constants.TABLE_PLAYER, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            case Constants.SCORE:
                cursor = db.query(Constants.TABLE_SCORE, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            case Constants.SCORE_ID:
                selection = "_id=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = db.query(Constants.TABLE_SCORE, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int code = MATCHER.match(uri);
        long id;
        Uri resultUri;

        switch (code) {
            case Constants.PLAYER:
                id = db.insert(Constants.TABLE_PLAYER, null, values);
                resultUri = ContentUris.withAppendedId(uri, id);
                break;
            case Constants.SCORE:
                id = db.insert(Constants.TABLE_SCORE, null, values);
                resultUri = ContentUris.withAppendedId(uri, id);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return resultUri;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.update(Constants.TABLE_SCORE, values, selection, selectionArgs);
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.delete(Constants.TABLE_SCORE, selection, selectionArgs);
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }
}