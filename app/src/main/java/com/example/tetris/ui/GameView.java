package com.example.tetris.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.tetris.game.GameBoard;
import com.example.tetris.game.Tetromino;

public class GameView extends View {

    private int[][] grid;
    private Tetromino current;
    private final Paint paint = new Paint();
    private float cell;

    public GameView(Context context) { super(context); }
    public GameView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); }

    // 参数改为 int[][]
    public void update(int[][] grid, Tetromino current) {
        this.grid = grid;
        this.current = current;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        cell = Math.min(w / (float) GameBoard.COLS, h / (float) GameBoard.ROWS);
        float offsetX = (w - cell * GameBoard.COLS) / 2f;
        float offsetY = (h - cell * GameBoard.ROWS) / 2f;

        // 背景改成浅灰偏白，不再是黑黑的
        paint.setColor(Color.parseColor("#F5F5F5"));
        canvas.drawRect(0, 0, w, h, paint);

        // 画外边框
        paint.setColor(Color.parseColor("#BDBDBD"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        canvas.drawRect(offsetX, offsetY, offsetX + cell * GameBoard.COLS, offsetY + cell * GameBoard.ROWS, paint);

        // 网格线改成很细的白色
        paint.setColor(Color.parseColor("#E0E0E0"));
        paint.setStrokeWidth(1);
        for (int r = 1; r < GameBoard.ROWS; r++) {
            float y = offsetY + r * cell;
            canvas.drawLine(offsetX, y, offsetX + cell * GameBoard.COLS, y, paint);
        }
        for (int c = 1; c < GameBoard.COLS; c++) {
            float x = offsetX + c * cell;
            canvas.drawLine(x, offsetY, x, offsetY + cell * GameBoard.ROWS, paint);
        }

        if (grid == null) return;

        // 画已固定的方块
        paint.setStyle(Paint.Style.FILL);
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                int v = grid[r][c];
                if (v > 0) {
                    paint.setColor(Tetromino.COLORS[v - 1]);
                    drawCell(canvas, c, r, offsetX, offsetY);
                }
            }
        }

        // 画当前下落的方块
        if (current != null) {
            paint.setColor(Tetromino.COLORS[current.type]);
            for (int[] cellXY : current.getCells()) {
                int c = cellXY[0], r = cellXY[1];
                if (r >= 0) drawCell(canvas, c, r, offsetX, offsetY);
            }
        }
    }

    // 改进画方块的方法，加一圈白边更美观（只保留这唯一的一个）
    // 改进画方块的方法，加一圈白边更美观
    private void drawCell(Canvas canvas, int c, int r, float ox, float oy) {
        float left = ox + c * cell + 1;
        float top = oy + r * cell + 1;
        float right = left + cell - 2;
        float bottom = top + cell - 2;

        // 1. 先记住当前方块的颜色（比如红色、绿色）
        int originalColor = paint.getColor();

        // 2. 填充颜色
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(left, top, right, bottom, paint);

        // 3. 描白边
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE); // 这里把颜色改成了白色
        paint.setStrokeWidth(2);
        canvas.drawRect(left, top, right, bottom, paint);

        // 4. 画完之后，把颜色还给原来的方块颜色！
        paint.setColor(originalColor);
        paint.setStyle(Paint.Style.FILL);
    }
}