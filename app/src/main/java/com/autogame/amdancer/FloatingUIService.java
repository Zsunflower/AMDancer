package com.autogame.amdancer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

public class FloatingUIService extends Service implements View.OnClickListener {
    private static final String TAG = "AM Dancer";
    private WindowManager mWindowManager;
    private View mFloatingView;
    private View collapsedView;
    private View expandedView;

    public FloatingUIService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("InflateParams")
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
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
            stopProjection();
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
        Intent fk_intent = new Intent(this, Settings.class);
        fk_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(fk_intent);
    }

    private void stopProjection() {
        startService(com.autogame.amdancer.ScreenCaptureService.getStopIntent(getApplicationContext()));
    }
}
