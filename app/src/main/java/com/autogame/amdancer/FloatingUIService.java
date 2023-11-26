package com.autogame.amdancer;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;


public class FloatingUIService extends Service implements View.OnClickListener {
    private static final String TAG = "AM Dancer";
    private WindowManager mWindowManager;
    private View mFloatingView;
    private View collapsedView;
    private View expandedView;
    private int width, height;

    @Override
    public void onCreate() {
        super.onCreate();

        //getting the widget layout from xml using layout inflater
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);

        //setting the layout parameters
        WindowManager.LayoutParams params;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        } else {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        //getting windows services and adding the floating view to it
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        //getting the collapsed and expanded view from the floating view
        collapsedView = mFloatingView.findViewById(R.id.layoutCollapsed);
        expandedView = mFloatingView.findViewById(R.id.layoutExpanded);

        //adding click listener to close button and expanded view
        mFloatingView.findViewById(R.id.expanded_btn).setOnClickListener(this);

        mFloatingView.findViewById(R.id.collapse_btn).setOnClickListener(this);
        mFloatingView.findViewById(R.id.startButton_4k).setOnClickListener(this);
        mFloatingView.findViewById(R.id.startButton_BB).setOnClickListener(this);
        mFloatingView.findViewById(R.id.buttonClose).setOnClickListener(this);
        mFloatingView.findViewById(R.id.settings_btn).setOnClickListener(this);
        moveToMiddleTop(params);
    }

    public void moveToMiddleTop(WindowManager.LayoutParams params) {
        int mHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        params.x = 0;
        params.y = -mHeight;
        mWindowManager.updateViewLayout(mFloatingView, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
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
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.collapse_btn) {//switching views
            collapsedView.setVisibility(View.VISIBLE);
            expandedView.setVisibility(View.GONE);
        } else if (id == R.id.expanded_btn) {
            collapsedView.setVisibility(View.GONE);
            expandedView.setVisibility(View.VISIBLE);
        } else if (id == R.id.buttonClose) {//closing the widget
            ScreenCaptureService.PLAY_MODE = ScreenCaptureService.UNK_MODE;
            stopSelf();
        } else if (id == R.id.startButton_4k) {
            if (ScreenCaptureService.PLAY_MODE != ScreenCaptureService.FK_MODE) {
                ScreenCaptureService.PLAY_MODE = ScreenCaptureService.FK_MODE;
                PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.DST_ATOP);
                mFloatingView.findViewById(R.id.startButton_4k).getBackground().setColorFilter(colorFilter);
                mFloatingView.findViewById(R.id.startButton_BB).getBackground().clearColorFilter();
            } else {
                mFloatingView.findViewById(R.id.startButton_4k).getBackground().clearColorFilter();
                ScreenCaptureService.PLAY_MODE = ScreenCaptureService.UNK_MODE;
            }
        } else if (id == R.id.startButton_BB) {
            if (ScreenCaptureService.PLAY_MODE != ScreenCaptureService.BB_MODE) {
                ScreenCaptureService.PLAY_MODE = ScreenCaptureService.BB_MODE;
                PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.DST_ATOP); //DST_ATOP
                mFloatingView.findViewById(R.id.startButton_BB).getBackground().setColorFilter(colorFilter);
                mFloatingView.findViewById(R.id.startButton_4k).getBackground().clearColorFilter();
            } else {
                mFloatingView.findViewById(R.id.startButton_BB).getBackground().clearColorFilter();
                ScreenCaptureService.PLAY_MODE = ScreenCaptureService.UNK_MODE;
            }
        } else if (id == R.id.settings_btn) {
            start_4k_settings();
        }
    }

    private void start_4k_settings() {
        Intent float_ui_intent = new Intent(getApplicationContext(), Settings.class);
        float_ui_intent.putExtra("width", width);
        float_ui_intent.putExtra("height", height);
        startService(float_ui_intent);
    }

    private void stopProjection() {
        startService(com.autogame.amdancer.ScreenCaptureService.getStopIntent(getApplicationContext()));
    }
}
