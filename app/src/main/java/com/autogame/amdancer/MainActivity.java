package com.autogame.amdancer;

import static java.lang.Integer.max;
import static java.lang.Integer.min;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;


public class MainActivity extends Activity {
    private static final int REQUEST_CODE = 100;
    private static final String TAG = "AM Dancer";

    // Used to load the 'amdancer' library on application startup.
    static {
        System.loadLibrary("amdancer");
    }

    private boolean is_screencapture_service_started = false;

    // Check if the Accessibility Service is enabled
    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> accessibilityService) {
        ComponentName expectedComponentName = new ComponentName(context, accessibilityService);

        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null)
            return false;

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);

            if (enabledService != null && enabledService.equals(expectedComponentName))
                return true;
        }

        return false;
    }

    /****************************************** Activity Lifecycle methods ************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button showWidget = findViewById(R.id.startButton);
        showWidget.setOnClickListener(view -> {
            if (checkForOverlayPermission() && checkForAccessibilityPermission() && checkForAccessibilityConnected()) {
                if (!is_screencapture_service_started) {
                    startProjection();
                    return;
                }
                Intent float_ui_intent = new Intent(MainActivity.this, FloatingUIService.class);
                startService(float_ui_intent);
                moveTaskToBack(false);
            }
        });

        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir != null) {
            String configDir = externalFilesDir.getAbsolutePath() + "/configs/";
            File storeDirectory = new File(configDir);

            if (storeDirectory.exists() || storeDirectory.mkdirs()) {
                extract_configs(configDir);
                setConfigPath(configDir);
                initConfig();
                init4kConfig();
                Log.e(TAG, "Setup config directory success!");
            } else {
                Log.e(TAG, "Failed to create the config directory!");
            }
        }
    }

    private void extract_configs(String outDir) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        String config_filename = max(width, height) + "x" + min(width, height) + ".ini";

        AssetManager assetManager = getAssets();
        String[] files;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e(TAG, "Failed to extract asset config files.", e);
            return;
        }
        for (String filename : files) {
            if (Objects.equals(filename, config_filename)) {
                Log.e(TAG, "Copy config: " + filename);
                InputStream in;
                OutputStream out;
                try {
                    in = assetManager.open(filename);
                    File outFile = new File(outDir, "buble_config_4k.ini");
                    out = Files.newOutputStream(outFile.toPath());
                    copyFile(in, out);
                    Log.e(TAG, "Copy success");
                } catch (IOException e) {
                    Log.e(TAG, "Copy failed", e);
                }
                return;
            }
        }
        Log.e(TAG, "This device don't support 4k mode");
    }

    @Override
    public void onDestroy() {
        Intent float_ui_intent = new Intent(MainActivity.this, FloatingUIService.class);
        stopService(float_ui_intent);
        Log.e(TAG, "Stop FloatingUI Service");
        super.onDestroy();
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.flush();
        out.close();
    }

    private boolean checkForAccessibilityConnected() {
        if (BotAccessibilityService.getInstance() == null) {
            Log.d(TAG, "BotAccessibilityService is not connected!.");

            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Accessibility Service");
            alert.setMessage("Accessibility Service is not connected!");
            alert.setPositiveButton("OK", null);
            alert.show();
            return false;
        }
        return true;
    }

    private boolean checkForOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Log.d(TAG, "Application is missing overlay permission.");

            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setMessage("Enable display over the other apss.");
            builder1.setPositiveButton(
                    "Yes",
                    (dialog, id) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    });

            builder1.setNegativeButton(
                    "No",
                    (dialog, id) -> dialog.cancel());

            AlertDialog alert11 = builder1.create();
            alert11.show();
            return false;
        }
        Log.d(TAG, "Application has permission to draw overlay.");
        return true;
    }

    private boolean checkForAccessibilityPermission() {
        if (isAccessibilityServiceEnabled(getApplicationContext(), BotAccessibilityService.class))
            return true;
        Log.d(TAG, "Application is missing Accessibility Service permission.");
        // Request the user to enable the Accessibility Service

        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Enable Accessibility Service to allow this app perform input touch, gesture.");
        builder1.setPositiveButton(
                "Yes",
                (dialog, id) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                });

        builder1.setNegativeButton(
                "No",
                (dialog, id) -> dialog.cancel());

        AlertDialog alert11 = builder1.create();
        alert11.show();
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                startService(com.autogame.amdancer.ScreenCaptureService.getStartIntent(this, resultCode, data));
                is_screencapture_service_started = true;
            }
        }
    }

    private void startProjection() {
        MediaProjectionManager mProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    public native void setConfigPath(String config_path);

    public native boolean initConfig();

    public native boolean init4kConfig();
}