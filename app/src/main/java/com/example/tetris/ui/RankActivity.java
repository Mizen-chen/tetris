package com.example.tetris.ui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tetris.R;
import com.example.tetris.util.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RankActivity extends AppCompatActivity {

    private static class RankEntry {
        int rank;
        String playerName;
        int score;
        int level;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rank);

        ListView lv = findViewById(R.id.lv_rank);
        ImageButton btnExitRank = findViewById(R.id.btn_exit_rank);

        btnExitRank.setOnClickListener(v -> finish());

        List<RankEntry> data = new ArrayList<>();
        Set<Integer> rankedPlayerIds = new HashSet<>();

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

                String playerName = "未知玩家";
                Uri playerUri = Uri.parse("content://" + Constants.AUTHORITY + "/" + Constants.TABLE_PLAYER);
                Cursor pc = getContentResolver().query(playerUri, new String[]{"name"},
                        "_id=?", new String[]{String.valueOf(playerId)}, null);
                if (pc != null && pc.moveToFirst()) {
                    playerName = pc.getString(0);
                }
                if (pc != null) pc.close();

                RankEntry entry = new RankEntry();
                entry.rank = rank;
                entry.playerName = playerName;
                entry.score = score;
                entry.level = level;
                data.add(entry);
                rank++;
            }
            c.close();
        }

        lv.setAdapter(new RankAdapter(this, data));
    }

    private class RankAdapter extends BaseAdapter {
        private final Context context;
        private final List<RankEntry> entries;

        RankAdapter(Context context, List<RankEntry> entries) {
            this.context = context;
            this.entries = entries;
        }

        @Override
        public int getCount() {
            return entries.isEmpty() ? 1 : entries.size();
        }

        @Override
        public Object getItem(int position) {
            return entries.isEmpty() ? null : entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (entries.isEmpty()) {
                TextView tv = new TextView(context);
                tv.setText("暂无记录，快去玩一局吧！");
                tv.setPadding(0, 48, 0, 48);
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setTextColor(ContextCompat.getColor(context, R.color.morandi_text_light));
                tv.setTextSize(15);
                return tv;
            }

            ViewHolder holder;
            if (convertView == null || convertView instanceof TextView) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_rank, parent, false);
                holder = new ViewHolder();
                holder.badge = convertView.findViewById(R.id.tv_rank_badge);
                holder.name = convertView.findViewById(R.id.tv_rank_name);
                holder.score = convertView.findViewById(R.id.tv_rank_score);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            RankEntry entry = entries.get(position);
            holder.badge.setText(String.valueOf(entry.rank));
            holder.name.setText(entry.playerName);
            holder.score.setText("分数: " + entry.score + "  |  等级: " + entry.level);

            int badgeColor;
            switch (entry.rank) {
                case 1:
                    badgeColor = R.color.morandi_amber;
                    break;
                case 2:
                    badgeColor = R.color.morandi_neutral;
                    break;
                case 3:
                    badgeColor = R.color.morandi_primary;
                    break;
                default:
                    badgeColor = R.color.morandi_secondary;
                    break;
            }
            holder.badge.setBackgroundColor(ContextCompat.getColor(context, badgeColor));

            return convertView;
        }

        private class ViewHolder {
            TextView badge;
            TextView name;
            TextView score;
        }
    }
}
