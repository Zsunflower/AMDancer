package com.autogame.amdancer;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class Settings extends Service {
    private static final Map<String, Integer> key2color = new HashMap<>();

    static {
        key2color.put("left_key", Color.GREEN);
        key2color.put("right_key", Color.BLUE);
        key2color.put("up_key", Color.YELLOW);
        key2color.put("down_key", Color.rgb(255, 128, 0));
        key2color.put("arrows_key", Color.WHITE);
        key2color.put("space_key", Color.CYAN);
        key2color.put("pointer_box", Color.RED);
        key2color.put("per_box", Color.MAGENTA);
    }

    private View mFloatingView;
    private String configPath;
    private FrameLayout container;
    private WindowManager mWindowManager;
    private int width, height;
    private int delta_per_box;

    @Override
    public void onCreate() {
        super.onCreate();
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.settings, null);

        //setting the layout parameters
        WindowManager.LayoutParams params;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        } else {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }

        //getting windows services and adding the floating view to it
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        // Get the container layout
        container = mFloatingView.findViewById(R.id.container);

        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir != null) {
            configPath = externalFilesDir.getAbsolutePath() + "/configs/";
        }
        Button save_btn = mFloatingView.findViewById(R.id.save_btn);
        save_btn.setOnClickListener(view -> {
            save_settings();
            int temp = ScreenCaptureService.PLAY_MODE;
            ScreenCaptureService.PLAY_MODE = ScreenCaptureService.UNK_MODE;
            reload4kConfig();
            ScreenCaptureService.PLAY_MODE = temp;
            stopSelf();
        });
        create_settings();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        width = intent.getIntExtra("width", 0);
        height = intent.getIntExtra("height", 0);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }

    private void save_settings() {
        File configFile = new File(configPath, "4k_config.ini");
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
            osw.write(String.format("%s=%s\n", "delta_per_box", delta_per_box));

            osw.flush();
            osw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public native void reload4kConfig();

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
                    String[] keyValuePairs = line.split("=");
                    if (keyValuePairs.length == 2) {
                        String key = keyValuePairs[0].trim();
                        String value = keyValuePairs[1].trim();
                        if (key.equals("delta_per_box")) {
                            delta_per_box = Integer.parseInt(value);
                            continue;
                        }
                        Rect r = new Rect();
                        if (parse_rect(value, r)) {
                            RectangleView bbox = new RectangleView(this, r.left, r.top, r.right, r.bottom, key2color.get(key), key);
                            container.addView(bbox);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
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
