package com.example.tetris.util;

public class Constants {
    // ContentProvider
    public static final String AUTHORITY = "com.example.tetris.provider";
    public static final String TABLE_PLAYER = "player";
    public static final String TABLE_SCORE = "score";

    public static final int PLAYER = 1;
    public static final int SCORE = 2;
    public static final int SCORE_ID = 3;

    // 广播 Action
    public static final String ACTION_GAME_OVER = "com.example.tetris.GAME_OVER";
    public static final String ACTION_SCORE_UPDATE = "com.example.tetris.SCORE_UPDATE";
    public static final String EXTRA_SCORE = "extra_score";
    public static final String EXTRA_LINES = "extra_lines";
    public static final String EXTRA_LEVEL = "extra_level";

    // Intent 参数
    public static final String EXTRA_PLAYER_NAME = "player_name";
}