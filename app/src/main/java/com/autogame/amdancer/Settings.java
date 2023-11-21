package com.autogame.amdancer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class Settings extends AppCompatActivity {

    private String configPath;
    private FrameLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        // Get the container layout
        container = findViewById(R.id.container);

        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir != null) {
            configPath = externalFilesDir.getAbsolutePath() + "/configs/";
        }
        Button save_btn = findViewById(R.id.save_btn);
        save_btn.setOnClickListener(view -> {
            save_settings();
        });
        create_settings();
    }

    private void save_settings() {
        File configFile = new File(configPath, "4k_config.ini");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        try {
            FileOutputStream fos = new FileOutputStream(configFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(String.format("%s=%s\n", "width", width));
            osw.write(String.format("%s=%s\n", "height", height));
            for (int i = 0; i < container.getChildCount(); i++) {
                View childView = container.getChildAt(i);
                if (childView instanceof RectangleView) {
                    RectangleView rect = (RectangleView) childView;
                    String line = String.format("%s=%s,%s,%s,%s\n", rect.name, rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
                    osw.write(line);
                }
            }

            osw.flush();
            osw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finish();
    }

    private boolean parse_rect(String value, Rect r) {
        String[] keyValuePairs = value.split(",");
        if (keyValuePairs.length != 4)
            return false;
        try {
            r.left = Integer.parseInt(keyValuePairs[0]);
            r.top = Integer.parseInt(keyValuePairs[1]);
            r.right = r.left + Integer.parseInt(keyValuePairs[2]);
            r.bottom = r.top + Integer.parseInt(keyValuePairs[3]);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private void create_settings() {
        File configFile = new File(configPath, "4k_config.ini");
        if (configFile.exists()) {
            try {
                FileReader fileReader = new FileReader(configFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    Log.d("File Content", line);
                    String[] keyValuePairs = line.split("=");
                    if (keyValuePairs.length == 2) {
                        String key = keyValuePairs[0].trim();
                        String value = keyValuePairs[1].trim();
                        Rect r = new Rect();
                        if (parse_rect(value, r)) {
                            RectangleView bbox = new RectangleView(this, r.left, r.top, r.right, r.bottom, Color.RED, key);
                            container.addView(bbox);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Read File", "Error reading file: " + e.getMessage());
            }
        }
    }

    private static class Rect {
        public int top, left, right, bottom;

        public Rect() {
            this.top = 0;
            this.left = 0;
            this.right = 0;
            this.bottom = 0;
        }

        public Rect(int left, int top, int right, int bottom) {
            this.top = top;
            this.left = left;
            this.right = right;
            this.bottom = bottom;
        }
    }

    // Custom view to draw a resizable rectangle
    private static class RectangleView extends View {
        private final int ANCHOR_SIZE = 10;
        private final int color;
        private final String name;
        private Paint paint;
        private Paint anchor_paint;
        private int lastTouchX;
        private int lastTouchY;
        private int left;
        private int top;
        private int right;
        private int bottom;
        private boolean isMoving = false;
        private boolean top_left_clicked = false, bottom_right_clicked = false;

        public RectangleView(Context context, int left, int top, int right, int bottom, int color, String name) {
            super(context);
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.color = color;
            this.name = name;
            init();
        }

        private void init() {
            paint = new Paint();
            paint.setColor(this.color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            anchor_paint = new Paint();
            anchor_paint.setColor(Color.WHITE);
            anchor_paint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            // Draw the rectangle on the canvas
            canvas.drawRect(left, top, right, bottom, paint);
            canvas.drawOval(left - ANCHOR_SIZE, top - ANCHOR_SIZE, left + ANCHOR_SIZE, top + ANCHOR_SIZE, anchor_paint);
            canvas.drawOval(right - ANCHOR_SIZE, bottom - ANCHOR_SIZE, right + ANCHOR_SIZE, bottom + ANCHOR_SIZE, anchor_paint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int touchX = (int) event.getX();
            int touchY = (int) event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (touchX >= left - ANCHOR_SIZE && touchX <= left + ANCHOR_SIZE &&
                            touchY >= top - ANCHOR_SIZE && touchY <= top + ANCHOR_SIZE) {
                        top_left_clicked = true;
                    } else if (touchX >= right - ANCHOR_SIZE && touchX <= right + ANCHOR_SIZE &&
                            touchY >= bottom - ANCHOR_SIZE && touchY <= bottom + ANCHOR_SIZE) {
                        bottom_right_clicked = true;
                    } else if (touchX >= left && touchX <= right &&
                            touchY >= top && touchY <= bottom) {
                        isMoving = true;
                    }
                    // Save the initial touch coordinates for moving
                    lastTouchX = touchX;
                    lastTouchY = touchY;
                    break;
                case MotionEvent.ACTION_MOVE:
                    // Calculate the distance moved
                    float dx = touchX - lastTouchX;
                    float dy = touchY - lastTouchY;
                    if (top_left_clicked) {
                        if (left + dx < right)
                            left += dx;
                        if (top + dy < bottom)
                            top += dy;
                        // Redraw the view
                        invalidate();
                    } else if (bottom_right_clicked) {
                        if (right + dx > left)
                            right += dx;
                        if (bottom + dy > top)
                            bottom += dy;
                        // Redraw the view
                        invalidate();
                    } else if (isMoving) {
                        // Update rectangle dimensions
                        left += dx;
                        right += dx;
                        top += dy;
                        bottom += dy;
                        // Redraw the view
                        invalidate();
                    }
                    // Save the current touch coordinates
                    lastTouchX = touchX;
                    lastTouchY = touchY;
                    break;
                case MotionEvent.ACTION_UP:
                    // Reset the resizing flag
                    isMoving = false;
                    top_left_clicked = false;
                    bottom_right_clicked = false;
                    break;
            }
            return isMoving || top_left_clicked || bottom_right_clicked;
        }
    }
}
