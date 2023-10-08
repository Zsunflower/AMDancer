package com.autogame.amdancer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class FloatingUIService extends Service implements View.OnClickListener {
    private static final String TAG = "AM Dancer";
    private int cap_result_code = -1;
    private Intent cap_intent = null;
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
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // Requires SYSTEM_ALERT_WINDOW permission
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        //getting windows services and adding the floating view to it
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        //getting the collapsed and expanded view from the floating view
        collapsedView = mFloatingView.findViewById(R.id.layoutCollapsed);
        expandedView = mFloatingView.findViewById(R.id.layoutExpanded);

        //adding click listener to close button and expanded view
        mFloatingView.findViewById(R.id.buttonClose).setOnClickListener(this);
        expandedView.setOnClickListener(this);

        mFloatingView.findViewById(R.id.startButton_4k).setOnClickListener(this);
        mFloatingView.findViewById(R.id.startButton_BB).setOnClickListener(this);
        //adding an touchlistener to make drag movement of the floating widget
        mFloatingView.findViewById(R.id.relativeLayoutParent).setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        return true;

                    case MotionEvent.ACTION_UP:
                        //when the drag is ended switching the state of the widget
                        collapsedView.setVisibility(View.GONE);
                        expandedView.setVisibility(View.VISIBLE);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        //this code is helping the widget to move around the screen with fingers
                        return true;
                }
                return false;
            }
        });
        moveToMiddleTop(params);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        cap_result_code = intent.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED);
        cap_intent = intent.getParcelableExtra("DATA");
        Log.e(TAG, "Recive code: " + cap_result_code);
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
        if (id == R.id.layoutExpanded) {//switching views
            collapsedView.setVisibility(View.VISIBLE);
            expandedView.setVisibility(View.GONE);
        } else if (id == R.id.buttonClose) {//closing the widget
            stopSelf();
        } else if (id == R.id.startButton_4k) {
            Log.e(TAG, "start 4k mode");
            ScreenCaptureService.PLAY_MODE = ScreenCaptureService.FK_MODE;
            startProjection();
        } else if (id == R.id.startButton_BB) {
            Log.e(TAG, "start BB mode");
            ScreenCaptureService.PLAY_MODE = ScreenCaptureService.BB_MODE;
            startProjection();
        }
    }

    private void startProjection() {
        startService(com.autogame.amdancer.ScreenCaptureService.getStartIntent(getApplicationContext(), cap_result_code, cap_intent));
    }

    private void stopProjection() {
        startService(com.autogame.amdancer.ScreenCaptureService.getStopIntent(getApplicationContext()));
    }
}
