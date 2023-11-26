package com.autogame.amdancer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE = 100;
    private static final String TAG = "AM Dancer";

    // Used to load the 'amdancer' library on application startup.
    static {
        System.loadLibrary("amdancer");
    }

    private Button earn;
    private int credit_count = 0;
    private RewardedAd rewardedAd;
    private boolean is_screencapture_service_started = false;
    private boolean isLoading = false;

    // Check if the Accessibility Service is enabled
    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> accessibilityService) {
        ComponentName expectedComponentName = new ComponentName(context, accessibilityService);

        String enabledServicesSetting = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
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

        Button startBot = findViewById(R.id.startButton);
        startBot.setOnClickListener(view -> {
            if (checkForOverlayPermission() && checkForAccessibilityPermission() && checkForAccessibilityConnected()) {
                if (!is_screencapture_service_started) {
                    startProjection();
                    return;
                }
                if (credit_count <= 0) {
                    Toast.makeText(this, "Click the button below to get 2 free credits.", Toast.LENGTH_SHORT).show();
                    return;
                }
                updateCredit(-1);
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int height = displayMetrics.heightPixels;
                int width = displayMetrics.widthPixels;
                Intent float_ui_intent = new Intent(MainActivity.this, FloatingUIService.class);
                float_ui_intent.putExtra("width", width);
                float_ui_intent.putExtra("height", height);
                startService(float_ui_intent);
                moveTaskToBack(false);
            }
        });

        earn = findViewById(R.id.earnCredit);
        earn.setOnClickListener(view -> earnCredit());
        loadCredit();

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
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
            }
        });
        List<String> testDeviceIds = Collections.singletonList("B2BEC6E36499DDB8C2407788BB201E8E");
        RequestConfiguration configuration =
                new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build();
        MobileAds.setRequestConfiguration(configuration);
        // load banner ads
        AdView mAdView = findViewById(R.id.adView);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                // Code to be executed when an ad request fails.
                Log.e(TAG, adError.getMessage());
            }

            @Override
            public void onAdImpression() {
                // Code to be executed when an impression is recorded
                // for an ad.
            }

            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }
        });
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        loadRewardedAd();
    }

    private void updateCreditLabel() {
        if (credit_count > 0)
            earn.setText(String.valueOf(credit_count));
        else
            earn.setText(R.string.earn_credit);
    }

    private void earnCredit() {
        if (rewardedAd == null && !isLoading) {
            loadRewardedAd();
        }
        showRewardedVideo();
    }

    private void loadCredit() {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        // Provide a default value (0 in this case) in case the key is not found
        credit_count = sharedPreferences.getInt(getString(R.string.CREDIT_KEY), 5);
        updateCreditLabel();
    }

    private void saveCredit() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.CREDIT_KEY), credit_count);
        editor.apply();
    }

    private void loadRewardedAd() {
        if (rewardedAd == null) {
            isLoading = true;
            AdRequest adRequest = new AdRequest.Builder().build();
            RewardedAd.load(
                    this,
                    "ca-app-pub-5118564015725949/8548479401",
                    adRequest,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            // Handle the error.
                            rewardedAd = null;
                            MainActivity.this.isLoading = false;
                            Log.d(TAG, loadAdError.getMessage());
                        }

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                            MainActivity.this.rewardedAd = rewardedAd;
                            MainActivity.this.isLoading = false;
                        }
                    });
        }
    }

    private void addCredit() {
        updateCredit(2);
    }

    private void updateCredit(int amount) {
        credit_count += amount;
        updateCreditLabel();
        saveCredit();
    }

    private void showRewardedVideo() {
        if (rewardedAd == null) {
            Log.e(TAG, "The rewarded ad wasn't ready yet.");
            return;
        }
        rewardedAd.setFullScreenContentCallback(
                new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Called when ad is shown.
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        // Called when ad fails to show.
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        rewardedAd = null;
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        // Called when ad is dismissed.
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        rewardedAd = null;
                        // Preload the next rewarded ad.
                        MainActivity.this.loadRewardedAd();
                    }
                });
        Activity activityContext = MainActivity.this;
        rewardedAd.show(
                activityContext,
                new OnUserEarnedRewardListener() {
                    @Override
                    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                        // Handle the reward.
                        addCredit();
                    }
                });
    }

    private void extract_configs(String outDir) {
        AssetManager assetManager = getAssets();
        InputStream in;
        FileOutputStream out;
        try {
            in = assetManager.open("4k_config.ini");
            File outFile = new File(outDir, "4k_config.ini");
            out = new FileOutputStream(outFile);
            copyFile(in, out);
            Log.e(TAG, "Copy 4k_config success");
        } catch (IOException e) {
            Log.e(TAG, "Copy 4k_config failed", e);
        }
    }

    @Override
    public void onDestroy() {
        Intent float_ui_intent = new Intent(MainActivity.this, FloatingUIService.class);
        stopService(float_ui_intent);
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
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Log.d(TAG, "Application is missing overlay permission.");

            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setMessage("Enable display over the other apss.");
            builder1.setPositiveButton(
                    "Yes",
                    (dialog, id) -> {
                        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
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
        return true;
    }

    private boolean checkForAccessibilityPermission() {
        if (isAccessibilityServiceEnabled(getApplicationContext(), BotAccessibilityService.class))
            return true;
        Log.d(TAG, "Application is missing Accessibility Service permission.");
        // Request the user to enable the Accessibility Service

        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setTitle("Accessibility Service");
        builder1.setMessage("To use the app, you must enable the \"AM Dancer\" accessibility option in your settings." + "\n" +
                "This service is required for the following features:" + "\n\n" +
                "Perform clicks on bubbles" + "\n" +
                "Perform clicks on navigator keys" + "\n\n" +
                "No data is collected or shared and will only be used for the specified purposes." + "\n" +
                "To grant permission, please click \"OK\" to open the settings screen and enable the required permission.");
        builder1.setPositiveButton(
                "Yes",
                (dialog, id) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
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