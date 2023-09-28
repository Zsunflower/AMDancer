package com.autogame.amdancer;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class AutoAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Your accessibility event handling logic goes here.
        Log.e("AS", "onAccessibilityEvent: " + event.toString());
    }

    @Override
    public void onInterrupt() {
        // Handle interrupt, if necessary.
        Log.e("AS", "onInterrupt");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d("AS", "onServiceConnected");
    }

    public void click(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));

        GestureDescription gestureDescription = builder.build();

        // Call dispatchGesture here
        if (dispatchGesture(gestureDescription, null, null))
            Log.e("AS", "Click success");
        else
            Log.e("AS", "Click failed");
    }

    public void hold(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 200));

        GestureDescription gestureDescription = builder.build();
        if (dispatchGesture(gestureDescription, null, null))
            Log.e("AS", "Hold success");
    }

    // Perform a swipe gesture from (x1, y1) to (x2, y2)
    public void drag(int x1, int y1, int x2, int y2) {
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 20)); // Adjust duration as needed
        GestureDescription gestureDescription = gestureBuilder.build();
        // Dispatch the swipe gesture asynchronously
        if (dispatchGesture(gestureDescription, null, null))
            Log.e("AS", "Drag success");
    }
}

