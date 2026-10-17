package com.example.tetris.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.tetris.game.GameBoard;
import com.example.tetris.game.Tetromino;
import com.example.tetris.util.Constants;

public class GameService extends Service {

    public interface GameListener {
        void onBoardChanged();
        void onScoreChanged(int score, int lines, int level);
        void onGameOver(int score, int lines, int level);
    }

    private final IBinder binder = new GameBinder();
    private GameListener listener;

    private GameBoard board;
    private Tetromino current;
    private int score = 0;
    private int lines = 0;
    private int level = 1;
    private boolean running = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tick;

    public class GameBinder extends Binder {
        public GameService getService() { return GameService.this; }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setListener(GameListener l) { this.listener = l; }

    public void startGame() {
        board = new GameBoard();
        score = 0; lines = 0; level = 1;
        current = board.newPiece();
        running = true;
        scheduleTick();
        notifyBoard();
        notifyScore();
    }

    public void pauseGame() {
        running = false;
        if (tick != null) handler.removeCallbacks(tick);
    }

    public void resumeGame() {
        if (board == null || current == null) return;
        if (!isGameOver()) {
            running = true;
            scheduleTick();
        }
    }

    private void scheduleTick() {
        if (tick != null) handler.removeCallbacks(tick);
        tick = () -> {
            if (!running) return;
            moveDown();
            if (running) scheduleTick();
        };
        int interval = Math.max(120, 600 - (level - 1) * 60);
        handler.postDelayed(tick, interval);
    }

    public void moveLeft()  { move(-1, 0); }
    public void moveRight() { move(1, 0); }

    private void move(int dx, int dy) {
        if (current == null) return;
        current.x += dx;
        current.y += dy;
        if (!board.canPlace(current)) {
            current.x -= dx;
            current.y -= dy;
        }
        notifyBoard();
    }

    public void rotate() {
        if (current == null) return;
        int old = current.rotation;
        current.rotation = (current.rotation + 1) % 4;
        if (!board.canPlace(current)) current.rotation = old;
        notifyBoard();
    }

    public void drop() {
        while (true) {
            current.y++;
            if (!board.canPlace(current)) {
                current.y--;
                break;
            }
        }
        moveDown();
    }

    private void moveDown() {
        if (current == null) return;
        current.y++;
        if (!board.canPlace(current)) {
            current.y--;
            lockPiece();
        }
        notifyBoard();
    }

    private void lockPiece() {
        board.lock(current);

        // 【修改点1】每次锁定方块，基础分 +1
        score += 1;

        int cleared = board.clearLines();
        if (cleared > 0) {
            lines += cleared;
            // 【修改点2】消除行给额外奖励分
            score += cleared * 100 * level;
            level = 1 + lines / 5;
        }
        notifyScore(); // 通知UI更新分数

        current = board.newPiece();
        if (!board.canPlace(current)) {
            gameOver();
        }
    }

    private boolean isGameOver() {
        return current == null || !board.canPlace(current);
    }

    private void gameOver() {
        running = false;
        if (tick != null) handler.removeCallbacks(tick);

        if (listener != null) listener.onGameOver(score, lines, level);

        // 用广播通知（BroadcastReceiver 登场）
        Intent intent = new Intent(Constants.ACTION_GAME_OVER);
        intent.putExtra(Constants.EXTRA_SCORE, score);
        intent.putExtra(Constants.EXTRA_LINES, lines);
        intent.putExtra(Constants.EXTRA_LEVEL, level);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void notifyBoard() {
        if (listener != null) listener.onBoardChanged();
    }

    private void notifyScore() {
        if (listener != null) listener.onScoreChanged(score, lines, level);
        Intent intent = new Intent(Constants.ACTION_SCORE_UPDATE);
        intent.putExtra(Constants.EXTRA_SCORE, score);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        running = false;
        if (tick != null) handler.removeCallbacks(tick);
        super.onDestroy();
    }

    // 供 Activity 绘制读取
    public int[][] getGrid() { return board == null ? new int[GameBoard.ROWS][GameBoard.COLS] : board.grid; }
    public Tetromino getCurrent() { return current; }
    public int getScore() { return score; }
    public int getLines() { return lines; }
    public int getLevel() { return level; }
    public boolean isRunning() { return running; }
}
