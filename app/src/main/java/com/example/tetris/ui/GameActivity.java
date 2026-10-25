package com.example.tetris.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tetris.R;
import com.example.tetris.service.GameService;
import com.example.tetris.util.Constants;
import com.google.android.material.button.MaterialButton;

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
    private android.app.Dialog gameOverDialog;

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
            int duration = intent.getIntExtra(Constants.EXTRA_DURATION, 0);
            int pieces = intent.getIntExtra(Constants.EXTRA_PIECES, 0);
            if (gameOverPending) return;
            saveScore(score, lines, level);
            saveHistory(score, lines, level, duration, pieces);
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

        // 查看排行榜（先暂停游戏，防止回来就死了）
        btnGameRank.setOnClickListener(v -> {
            if (bound && service.isRunning()) {
                service.pauseGame();
                btnPause.setText(R.string.resume_game);
            }
            startActivity(new Intent(GameActivity.this, RankActivity.class));
        });

        // 注意：这里改为了 MaterialButton 类型以匹配新的 XML 布局
        MaterialButton btnRestartGame = findViewById(R.id.btn_restart_game);
        MaterialButton btnExitGame = findViewById(R.id.btn_exit_game);
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

        gameOverDialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_game_over, null);
        gameOverDialog.setContentView(view);
        if (gameOverDialog.getWindow() != null) {
            gameOverDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = gameOverDialog.getWindow().getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.dimAmount = 0.5f;
            gameOverDialog.getWindow().setAttributes(lp);
            gameOverDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        gameOverDialog.setCancelable(false);
        gameOverDialog.setCanceledOnTouchOutside(false);

        TextView tvScore = view.findViewById(R.id.dialog_go_score);
        TextView tvLines = view.findViewById(R.id.dialog_go_lines);
        TextView tvLevel = view.findViewById(R.id.dialog_go_level);
        MaterialButton btnContinue = view.findViewById(R.id.dialog_go_continue);
        MaterialButton btnRank = view.findViewById(R.id.dialog_go_rank);
        MaterialButton btnExit = view.findViewById(R.id.dialog_go_exit);

        tvScore.setText(String.valueOf(score));
        tvLines.setText(getString(R.string.game_over_lines, lines));
        tvLevel.setText(getString(R.string.game_over_level, level));

        btnContinue.setOnClickListener(v -> {
            gameOverDialogShowing = false;
            gameOverPending = false;
            gameOverDialog.dismiss();
            if (bound && service != null) {
                service.startGame();
                btnPause.setText(R.string.pause_game);
            }
        });

        btnRank.setOnClickListener(v ->
                startActivity(new Intent(GameActivity.this, RankActivity.class))
        );

        btnExit.setOnClickListener(v -> {
            gameOverDialogShowing = false;
            gameOverPending = false;
            gameOverDialog.dismiss();
            finish();
        });

        gameOverDialog.setOnDismissListener(dialog -> gameOverDialogShowing = false);
        gameOverDialog.show();
    }

    private void confirmRestart() {
        showConfirmDialog(
                getString(R.string.confirm_restart_title),
                getString(R.string.confirm_restart_message),
                getString(R.string.restart_game),
                ContextCompat.getColor(this, R.color.morandi_primary),
                () -> {
                    if (bound && service != null) {
                        service.startGame();
                        btnPause.setText(R.string.pause_game);
                    }
                }
        );
    }

    private void confirmExit() {
        showConfirmDialog(
                getString(R.string.confirm_exit_title),
                getString(R.string.confirm_exit_message),
                getString(R.string.exit_game),
                ContextCompat.getColor(this, R.color.morandi_warning),
                this::finish
        );
    }

    /**
     * 通用确认弹窗：弹窗前自动暂停游戏，点击取消或关闭时自动恢复游戏。
     */
    private void showConfirmDialog(String title, String message, String confirmText,
                                   int confirmBgColor, Runnable onConfirm) {
        // 弹窗前先暂停游戏，并记录原本是否在运行，便于取消时恢复
        boolean wasRunning = bound && service != null && service.isRunning();
        if (wasRunning) {
            service.pauseGame();
            btnPause.setText(R.string.resume_game);
        }

        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_confirm, null);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.dimAmount = 0.5f;
            dialog.getWindow().setAttributes(lp);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        dialog.setCancelable(true);

        TextView tvTitle = view.findViewById(R.id.dialog_title);
        TextView tvMessage = view.findViewById(R.id.dialog_message);
        MaterialButton btnCancel = view.findViewById(R.id.dialog_cancel);
        MaterialButton btnConfirm = view.findViewById(R.id.dialog_confirm);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnConfirm.setText(confirmText);
        btnConfirm.setBackgroundColor(confirmBgColor);

        Runnable resumeIfNeeded = () -> {
            if (wasRunning && bound && service != null) {
                service.resumeGame();
                btnPause.setText(R.string.pause_game);
            }
        };

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            resumeIfNeeded.run();
        });

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (onConfirm != null) onConfirm.run();
        });

        // 点击弹窗外部或返回键关闭时，同样恢复游戏
        dialog.setOnCancelListener(d -> resumeIfNeeded.run());

        dialog.show();
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

    private void saveHistory(int score, int lines, int level, int duration, int pieces) {
        int playerId = getPlayerId(playerName);
        ContentValues cv = new ContentValues();
        cv.put("player_id", playerId);
        cv.put("score", score);
        cv.put("level", level);
        cv.put("lines", lines);
        cv.put("duration", duration);
        cv.put("pieces", pieces);
        cv.put("played_at", System.currentTimeMillis());
        Uri uri = Uri.parse("content://" + Constants.AUTHORITY + "/" + Constants.TABLE_HISTORY);
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