package com.example.tetris.game;

/** 7 种方块，用相对坐标定义每种形状的 4 个格子（4x4 网格内） */
public class Tetromino {

    public static final int[][][] SHAPES = {
            // I
            {{0,1},{1,1},{2,1},{3,1}},
            // O
            {{1,0},{2,0},{1,1},{2,1}},
            // T
            {{1,0},{0,1},{1,1},{2,1}},
            // S
            {{1,0},{2,0},{0,1},{1,1}},
            // Z
            {{0,0},{1,0},{1,1},{2,1}},
            // J
            {{0,0},{0,1},{1,1},{2,1}},
            // L
            {{2,0},{0,1},{1,1},{2,1}}
    };

    public static final int[] COLORS = {
            0xFF00FFFF, 0xFFFFFF00, 0xFF800080,
            0xFF00FF00, 0xFFFF0000, 0xFF0000FF, 0xFFFFA500
    };

    public int type;         // 0~6
    public int rotation;     // 0~3
    public int x, y;         // 位置

    public Tetromino(int type) {
        this.type = type;
        this.rotation = 0;
        this.x = 3;
        this.y = 0;
    }

    /** 返回当前旋转下的 4 个绝对坐标 */
    public int[][] getCells() {
        int[][] base = SHAPES[type];
        int[][] result = new int[4][2];
        for (int i = 0; i < 4; i++) {
            int bx = base[i][0];
            int by = base[i][1];
            int rx, ry;
            switch (rotation) {
                case 1: rx = 3 - by; ry = bx; break;
                case 2: rx = 3 - bx; ry = 3 - by; break;
                case 3: rx = by; ry = 3 - bx; break;
                default: rx = bx; ry = by;
            }
            result[i][0] = x + rx;
            result[i][1] = y + ry;
        }
        return result;
    }

    public Tetromino copy() {
        Tetromino t = new Tetromino(type);
        t.rotation = rotation;
        t.x = x;
        t.y = y;
        return t;
    }
}