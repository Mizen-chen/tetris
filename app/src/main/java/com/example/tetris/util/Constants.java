package com.example.tetris.util;

public class Constants {
    // ContentProvider
    public static final String AUTHORITY = "com.example.tetris.provider";
    public static final String TABLE_PLAYER = "player";
    public static final String TABLE_SCORE = "score";
    public static final String TABLE_HISTORY = "game_history";

    public static final int PLAYER = 1;
    public static final int SCORE = 2;
    public static final int SCORE_ID = 3;
    public static final int HISTORY = 4;
    public static final int HISTORY_ID = 5;

    // 广播 Action
    public static final String ACTION_GAME_OVER = "com.example.tetris.GAME_OVER";
    public static final String EXTRA_SCORE = "extra_score";
    public static final String EXTRA_LINES = "extra_lines";
    public static final String EXTRA_LEVEL = "extra_level";
    public static final String EXTRA_DURATION = "extra_duration";
    public static final String EXTRA_PIECES = "extra_pieces";

    // Intent 参数
    public static final String EXTRA_PLAYER_NAME = "player_name";
}