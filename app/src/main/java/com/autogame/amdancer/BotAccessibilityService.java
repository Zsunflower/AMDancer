package com.autogame.amdancer;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class BotAccessibilityService extends AccessibilityService {
    private static final String TAG = "ScreenCaptureActivity";
    private static BotAccessibilityService mInstance = null;

    public static BotAccessibilityService getInstance() {
        return mInstance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Your accessibility event handling logic goes here.
    }

    @Override
    public void onInterrupt() {
        // Handle interrupt, if necessary.
        Log.e(TAG, "onInterrupt");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        mInstance = this;
        Log.d(TAG, "onServiceConnected");
    }

    public void click(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 10));

        GestureDescription gestureDescription = builder.build();

        // Call dispatchGesture here
        dispatchGesture(gestureDescription, null, null);
    }

    public void hold(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 500));

        GestureDescription gestureDescription = builder.build();
        dispatchGesture(gestureDescription, null, null);
    }

    // Perform a swipe gesture from (x1, y1) to (x2, y2)
    public void drag(int x1, int y1, int x2, int y2) {
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 400)); // Adjust duration as needed
        GestureDescription gestureDescription = gestureBuilder.build();
        // Dispatch the swipe gesture asynchronously
        dispatchGesture(gestureDescription, null, null);
    }
}

