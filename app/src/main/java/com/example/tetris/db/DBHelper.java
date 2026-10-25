package com.example.tetris.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "tetris.db";
    public static final int DB_VERSION = 2;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE player (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "created_at INTEGER)");

        db.execSQL("CREATE TABLE score (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_id INTEGER," +
                "score INTEGER," +
                "level INTEGER," +
                "lines INTEGER," +
                "played_at INTEGER," +
                "FOREIGN KEY(player_id) REFERENCES player(_id))");

        // 个人游戏历史记录表：记录每一局游戏的完整数据
        db.execSQL("CREATE TABLE game_history (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_id INTEGER," +
                "score INTEGER," +
                "level INTEGER," +
                "lines INTEGER," +
                "duration INTEGER," +
                "pieces INTEGER," +
                "played_at INTEGER," +
                "FOREIGN KEY(player_id) REFERENCES player(_id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS game_history");
        db.execSQL("DROP TABLE IF EXISTS score");
        db.execSQL("DROP TABLE IF EXISTS player");
        onCreate(db);
    }
}