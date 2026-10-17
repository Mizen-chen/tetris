package com.example.tetris.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tetris.R;
import com.example.tetris.util.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RankActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rank);

        ListView lv = findViewById(R.id.lv_rank);
        ImageButton btnExitRank = findViewById(R.id.btn_exit_rank);
        List<String> data = new ArrayList<>();
        Set<Integer> rankedPlayerIds = new HashSet<>();

        btnExitRank.setOnClickListener(v -> finish());

        // 查分数表
        Uri uri = Uri.parse("content://" + Constants.AUTHORITY + "/" + Constants.TABLE_SCORE);
        Cursor c = getContentResolver().query(
                uri,
                null,
                null,
                null,
                "score DESC, level DESC, lines DESC, played_at DESC"
        );

        if (c != null) {
            int rank = 1;
            while (c.moveToNext()) {
                int playerId = c.getInt(c.getColumnIndexOrThrow("player_id"));
                if (!rankedPlayerIds.add(playerId)) {
                    continue;
                }

                int score = c.getInt(c.getColumnIndexOrThrow("score"));
                int level = c.getInt(c.getColumnIndexOrThrow("level"));

                // 去玩家表里把名字查出来
                String playerName = "未知玩家";
                Uri playerUri = Uri.parse("content://" + Constants.AUTHORITY + "/" + Constants.TABLE_PLAYER);
                Cursor pc = getContentResolver().query(playerUri, new String[]{"name"},
                        "_id=?", new String[]{String.valueOf(playerId)}, null);
                if (pc != null && pc.moveToFirst()) {
                    playerName = pc.getString(0);
                }
                if (pc != null) pc.close();

                // 拼装显示的文字
                String medal = rank == 1 ? "🥇" : (rank == 2 ? "🥈" : (rank == 3 ? "🥉" : rank + "."));
                data.add(medal + " " + playerName + "\n分数: " + score + " | 等级: " + level);
                rank++;
            }
            c.close();
        }

        if (data.isEmpty()) {
            data.add("暂无记录，快去玩一局吧！");
        }

        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, data));
    }
}
