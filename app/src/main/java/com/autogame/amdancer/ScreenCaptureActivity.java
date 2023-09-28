package com.autogame.amdancer;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;

import java.io.File;
import java.util.List;


public class ScreenCaptureActivity extends Activity {
    private static final int REQUEST_CODE = 100;
    private static final String TAG = "ScreenCaptureActivity";

    // Used to load the 'amdancer' library on application startup.
    static {
        System.loadLibrary("amdancer");
    }

    AutoAccessibilityService auto_accessibility_service = new AutoAccessibilityService();


    /****************************************** Activity Lifecycle methods ************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // start projection
        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startProjection();
//                test_click();
            }
        });

        // stop projection
        Button stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                stopProjection();
            }
        });
        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir != null) {
            String configDir = externalFilesDir.getAbsolutePath() + "/configs/";
            File storeDirectory = new File(configDir);
            if (!storeDirectory.exists()) {
                boolean success = storeDirectory.mkdirs();
                if (!success) {
                    Log.e(TAG, "Failed to create configs directory.");
                }
            }
            File buble_config_file = new File(configDir + "buble_config.ini");
            if (!buble_config_file.exists()) {
                Log.e(TAG, "Buble config file not found!");
            } else {
                Log.e(TAG, "Buble config file: " + buble_config_file.getAbsolutePath());
                boolean status = initConfig(buble_config_file.getAbsolutePath());
                if (status)
                    Log.e(TAG, "Buble config file parse success!");
                else
                    Log.e(TAG, "Buble config file parse failed!");
            }
        }
        if (!isAccessibilityServiceEnabled())
            requestAccessibilityService();
        Log.e(TAG, "request result: " + isAccessibilityServiceEnabled());
    }


    // Check if the Accessibility Service is enabled
    public boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) this.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(this.getPackageName()) && enabledServiceInfo.name.equals(AutoAccessibilityService.class.getName()))
                return true;
        }
        return false;
    }

    // Request the user to enable the Accessibility Service
    public void requestAccessibilityService() {
        Log.e(TAG, "Request for AccessibilityService");
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, 0);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                startService(com.autogame.amdancer.ScreenCaptureService.getStartIntent(this, resultCode, data));
            }
        }
    }

    /****************************************** UI Widget Callbacks *******************************/
    private void startProjection() {
        auto_accessibility_service.click(500, 500);
//        MediaProjectionManager mProjectionManager =
//                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
//        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    private void stopProjection() {
        startService(com.autogame.amdancer.ScreenCaptureService.getStopIntent(this));
    }

    public native boolean initConfig(String config_path);
}