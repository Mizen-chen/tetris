package com.example.tetris.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tetris.R;
import com.example.tetris.service.GameService;
import com.example.tetris.util.Constants;

public class GameActivity extends AppCompatActivity implements GameService.GameListener {

    private GameView gameView;
    private TextView tvScore, tvLevel, tvLines;
    private GameService service;
    private boolean bound = false;
    private String playerName;
    private Button btnPause;
    private boolean gameOverDialogShowing = false;
    private boolean gameOverPending = false;
    private int lastGameOverScore;
    private int lastGameOverLines;
    private int lastGameOverLevel;
    private AlertDialog gameOverDialog;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((GameService.GameBinder) binder).getService();
            service.setListener(GameActivity.this);
            bound = true;
            service.startGame();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
            service = null;
        }
    };

    // 动态注册的广播：接收游戏结束
    private final BroadcastReceiver gameOverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int score = intent.getIntExtra(Constants.EXTRA_SCORE, 0);
            int lines = intent.getIntExtra(Constants.EXTRA_LINES, 0);
            int level = intent.getIntExtra(Constants.EXTRA_LEVEL, 1);
            if (gameOverPending) return;
            saveScore(score, lines, level);
            lastGameOverScore = score;
            lastGameOverLines = lines;
            lastGameOverLevel = level;
            gameOverPending = true;
            showGameOverDialog(score, lines, level);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        playerName = getIntent().getStringExtra(Constants.EXTRA_PLAYER_NAME);

        gameView = findViewById(R.id.game_view);
        tvScore = findViewById(R.id.tv_score);
        tvLevel = findViewById(R.id.tv_level);
        tvLines = findViewById(R.id.tv_lines);

        Button btnLeft = findViewById(R.id.btn_left);
        Button btnRight = findViewById(R.id.btn_right);
        Button btnRotate = findViewById(R.id.btn_rotate);
        Button btnDrop = findViewById(R.id.btn_drop);
        btnPause = findViewById(R.id.btn_pause);

        Button btnGameRank = findViewById(R.id.btn_game_rank);

        // 控制按钮
        btnLeft.setOnClickListener(v -> { if (bound && service.isRunning()) service.moveLeft(); });
        btnRight.setOnClickListener(v -> { if (bound && service.isRunning()) service.moveRight(); });
        btnRotate.setOnClickListener(v -> { if (bound && service.isRunning()) service.rotate(); });
        btnDrop.setOnClickListener(v -> { if (bound && service.isRunning()) service.drop(); });

        // 暂停/继续
        btnPause.setOnClickListener(v -> {
            if (!bound) return;
            if (service.isRunning()) {
                service.pauseGame();
                btnPause.setText(R.string.resume_game);
            } else {
                service.resumeGame();
                btnPause.setText(R.string.pause_game);
            }
        });

        // 【新增】查看排行榜（先暂停游戏，防止回来就死了）
        btnGameRank.setOnClickListener(v -> {
            if (bound && service.isRunning()) {
                service.pauseGame();
                btnPause.setText(R.string.resume_game);
            }
            startActivity(new Intent(GameActivity.this, RankActivity.class));
        });

        ImageButton btnRestartGame = findViewById(R.id.btn_restart_game);
        ImageButton btnExitGame = findViewById(R.id.btn_exit_game);
        btnRestartGame.setOnClickListener(v -> confirmRestart());
        btnExitGame.setOnClickListener(v -> confirmExit());

        // 绑定 Service
        Intent si = new Intent(this, GameService.class);
        bindService(si, conn, Context.BIND_AUTO_CREATE);

        // 注册广播
        IntentFilter filter = new IntentFilter(Constants.ACTION_GAME_OVER);
        ContextCompat.registerReceiver(
                this,
                gameOverReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameOverPending && !gameOverDialogShowing) {
            showGameOverDialog(lastGameOverScore, lastGameOverLines, lastGameOverLevel);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bound) {
            service.setListener(null);
            unbindService(conn);
            bound = false;
        }
        unregisterReceiver(gameOverReceiver);
    }

    @Override
    public void onBoardChanged() {
        if (bound) gameView.update(service.getGrid(), service.getCurrent());
    }

    @Override
    public void onScoreChanged(int score, int lines, int level) {
        tvScore.setText("分数：" + score);
        tvLines.setText("消行：" + lines);
        tvLevel.setText("等级：" + level);
    }

    @Override
    public void onGameOver(int score, int lines, int level) {
        // 由广播处理
    }

    private void showGameOverDialog(int score, int lines, int level) {
        if (gameOverDialogShowing) return;
        gameOverDialogShowing = true;
        gameOverDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.game_over_title)
                .setMessage(getString(R.string.game_over_result, score, lines, level))
                .setCancelable(false)
                .setPositiveButton(R.string.continue_next_round, (dialog, which) -> {
                    gameOverDialogShowing = false;
                    gameOverPending = false;
                    if (bound && service != null) {
                        service.startGame();
                        btnPause.setText(R.string.pause_game);
                    }
                })
                .setNeutralButton(R.string.view_leaderboard, null)
                .setNegativeButton(R.string.exit_game, (dialog, which) -> {
                    gameOverDialogShowing = false;
                    gameOverPending = false;
                    finish();
                })
                .setOnDismissListener(dialog -> gameOverDialogShowing = false)
                .create();

        gameOverDialog.setOnShowListener(dialog -> {
            gameOverDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                    startActivity(new Intent(GameActivity.this, RankActivity.class))
            );
        });
        gameOverDialog.show();
    }

    private void confirmRestart() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_restart_title)
                .setMessage(R.string.confirm_restart_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.restart_game, (dialog, which) -> {
                    if (bound && service != null) {
                        service.startGame();
                        btnPause.setText(R.string.pause_game);
                    }
                })
                .show();
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_exit_title)
                .setMessage(R.string.confirm_exit_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.exit_game, (dialog, which) -> {
                    if (bound && service != null) service.pauseGame();
                    finish();
                })
                .show();
    }

    private void saveScore(int score, int lines, int level) {
        int playerId = getPlayerId(playerName);
        ContentValues cv = new ContentValues();
        cv.put("player_id", playerId);
        cv.put("score", score);
        cv.put("level", level);
        cv.put("lines", lines);
        cv.put("played_at", System.currentTimeMillis());
        Uri uri = Uri.parse("content://" + Constants.AUTHORITY + "/" + Constants.TABLE_SCORE);
        getContentResolver().insert(uri, cv);
    }

    private int getPlayerId(String name) {
        Uri uri = Uri.parse("content://" + Constants.AUTHORITY + "/" + Constants.TABLE_PLAYER);
        Cursor c = getContentResolver().query(uri, new String[]{"_id"}, "name=?", new String[]{name}, null);
        int id = -1;
        if (c != null && c.moveToFirst()) id = c.getInt(0);
        if (c != null) c.close();
        return id;
    }
}
