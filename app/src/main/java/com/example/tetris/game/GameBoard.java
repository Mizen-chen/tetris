package com.example.tetris.game;

import java.util.Random;

public class GameBoard {

    public static final int COLS = 10;
    public static final int ROWS = 20;

    public int[][] grid = new int[ROWS][COLS]; // 0 空，>0 表示颜色 type+1
    private Random random = new Random();

    public GameBoard() {
        reset();
    }

    public void reset() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                grid[r][c] = 0;
    }

    public Tetromino newPiece() {
        return new Tetromino(random.nextInt(7));
    }

    /** 是否可以放置 */
    public boolean canPlace(Tetromino t) {
        for (int[] cell : t.getCells()) {
            int cx = cell[0], cy = cell[1];
            if (cx < 0 || cx >= COLS || cy < 0 || cy >= ROWS) return false;
            if (grid[cy][cx] != 0) return false;
        }
        return true;
    }

    /** 固定当前方块 */
    public void lock(Tetromino t) {
        for (int[] cell : t.getCells()) {
            int cx = cell[0], cy = cell[1];
            if (cy >= 0 && cy < ROWS && cx >= 0 && cx < COLS) {
                grid[cy][cx] = t.type + 1;
            }
        }
    }

    /** 消行并返回消除的行数 */
    public int clearLines() {
        int cleared = 0;
        for (int r = ROWS - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] == 0) { full = false; break; }
            }
            if (full) {
                cleared++;
                // 上方整体下移
                for (int rr = r; rr > 0; rr--) {
                    System.arraycopy(grid[rr - 1], 0, grid[rr], 0, COLS);
                }
                for (int c = 0; c < COLS; c++) grid[0][c] = 0;
                r++; // 重新检查当前行
            }
        }
        return cleared;
    }
}