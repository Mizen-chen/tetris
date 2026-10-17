package com.example.tetris.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tetris.R;
import com.example.tetris.util.Constants;

public class MainActivity extends AppCompatActivity {

    private EditText etName;
    private Button btnStart, btnRank, btnLogout;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etName = findViewById(R.id.et_name);
        btnStart = findViewById(R.id.btn_start);
        btnRank = findViewById(R.id.btn_rank);
        btnLogout = findViewById(R.id.btn_logout);

        sp = getSharedPreferences("user_info", MODE_PRIVATE);

        // 开始游戏按钮逻辑
        btnStart.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "请先输入昵称！", Toast.LENGTH_SHORT).show();
                return;
            }

            // 记住登录状态
            sp.edit().putString("player_name", name).apply();
            ensurePlayer(name);

            // 跳转游戏界面
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra(Constants.EXTRA_PLAYER_NAME, name);
            startActivity(intent);
        });

        // 排行榜按钮逻辑
        btnRank.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RankActivity.class));
        });

        // 退出登录按钮逻辑
        btnLogout.setOnClickListener(v -> {
            sp.edit().clear().apply(); // 清空保存的账号
            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
            updateUI(); // 刷新界面状态
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI(); // 每次回到主界面，刷新一下UI状态
    }

    // 根据是否有保存的账号，来切换界面的显示状态
    private void updateUI() {
        String savedName = sp.getString("player_name", "");
        if (!savedName.isEmpty()) {
            // 已登录状态
            etName.setText(savedName);
            etName.setEnabled(false); // 禁止修改
            btnStart.setText("继续游戏");
            btnLogout.setVisibility(View.VISIBLE);
        } else {
            // 未登录状态
            etName.setText("");
            etName.setEnabled(true); // 允许输入
            btnStart.setText("开始游戏");
            btnLogout.setVisibility(View.GONE);
        }
    }

    // 确保玩家存在于数据库中（通过 ContentProvider）
    private void ensurePlayer(String name) {
        Uri uri = Uri.parse("content://" + Constants.AUTHORITY + "/" + Constants.TABLE_PLAYER);
        Cursor c = getContentResolver().query(uri, null, "name=?", new String[]{name}, null);
        if (c != null && c.getCount() > 0) {
            c.close();
            return;
        }
        if (c != null) c.close();

        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("created_at", System.currentTimeMillis());
        getContentResolver().insert(uri, cv);
    }
}