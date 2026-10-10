package com.virtualhit.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SocketClient.ConnectionListener {

    private static final String PREFS_NAME = "VirtualHitPrefs";
    private static final String KEY_HOST = "server_host";
    private static final String KEY_PORT = "server_port";
    private static final String KEY_SOCD = "socd_mode";
    private static final String KEY_HAPTICS = "haptics";
    private static final String KEY_VIBRATION_INTENSITY = "vibration_intensity";
    private static final String KEY_TURBO_ENABLED = "turbo_enabled";
    private static final String KEY_TURBO_FREQ = "turbo_frequency";
    private static final String KEY_ACTIVE_LAYOUT = "active_layout_json";
    private static final String KEY_SLOT_PREFIX = "layout_slot_"; // 1, 2, 3

    private HitboxView hitboxView;
    private SettingsSidePanel sidePanel;
    private SocketClient socketClient;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Root Container Layout
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Layer 1: Hitbox View
        hitboxView = new HitboxView(this);
        hitboxView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.addView(hitboxView);

        // Load Preferences & Active Layout
        String savedHost = prefs.getString(KEY_HOST, "127.0.0.1");
        int savedPort = prefs.getInt(KEY_PORT, 8888);
        int savedSocd = prefs.getInt(KEY_SOCD, SOCDCleaner.MODE_NEUTRAL);
        boolean savedHaptics = prefs.getBoolean(KEY_HAPTICS, true);
        int savedVibrationIntensity = prefs.getInt(KEY_VIBRATION_INTENSITY, 12);
        boolean savedTurboEnabled = prefs.getBoolean(KEY_TURBO_ENABLED, true);
        int savedTurboFreq = prefs.getInt(KEY_TURBO_FREQ, 5);
        String activeLayoutJson = prefs.getString(KEY_ACTIVE_LAYOUT, null);

        hitboxView.setSocdMode(savedSocd);
        hitboxView.setHapticsEnabled(savedHaptics);
        hitboxView.setVibrationIntensity(savedVibrationIntensity);
        hitboxView.setTurboEnabledGlobal(savedTurboEnabled);
        hitboxView.setTurboFrequency(savedTurboFreq);

        if (activeLayoutJson != null && !activeLayoutJson.isEmpty()) {
            hitboxView.importLayoutJson(activeLayoutJson);
        }

        // 2. Layer 2: Socket Client
        socketClient = new SocketClient(savedHost, savedPort);
        socketClient.setConnectionListener(this);
        hitboxView.setSocketClient(socketClient);

        // 3. Layer 3: Right Slide-out Settings Side Panel
        sidePanel = new SettingsSidePanel(this, hitboxView, socketClient, new SettingsSidePanel.OnPanelEventListener() {
            @Override
            public void onSaveConnection(String host, int port) {
                prefs.edit().putString(KEY_HOST, host).putInt(KEY_PORT, port).apply();
                socketClient.setHostAndPort(host, port);
            }

            @Override
            public void onSaveLayoutSlot(int slotIndex) {
                String currentJson = hitboxView.exportLayoutJson();
                prefs.edit().putString(KEY_SLOT_PREFIX + slotIndex, currentJson).apply();
                saveActiveLayoutPrefs();
                Toast.makeText(MainActivity.this, "Saved to Custom " + slotIndex, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadLayoutSlot(int slotIndex) {
                String slotJson = prefs.getString(KEY_SLOT_PREFIX + slotIndex, null);
                if (slotJson == null || slotJson.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Custom " + slotIndex + " is empty.", Toast.LENGTH_SHORT).show();
                } else {
                    hitboxView.importLayoutJson(slotJson);
                    saveActiveLayoutPrefs();
                    Toast.makeText(MainActivity.this, "Loaded Custom " + slotIndex, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLayoutChanged() {
                saveActiveLayoutPrefs();
            }
        });

        sidePanel.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.addView(sidePanel);

        setContentView(rootLayout);

        hitboxView.setOnSettingsClickListener(() -> {
            if (sidePanel.isOpen()) {
                sidePanel.closePanel();
            } else {
                sidePanel.openPanel();
            }
        });

        socketClient.connect();
    }

    private void saveActiveLayoutPrefs() {
        if (prefs == null || hitboxView == null) return;
        String json = hitboxView.exportLayoutJson();
        prefs.edit()
                .putString(KEY_ACTIVE_LAYOUT, json)
                .putInt(KEY_SOCD, hitboxView.getSocdMode())
                .putBoolean(KEY_HAPTICS, hitboxView.isHapticsEnabled())
                .putInt(KEY_VIBRATION_INTENSITY, hitboxView.getVibrationIntensity())
                .putBoolean(KEY_TURBO_ENABLED, hitboxView.isTurboEnabledGlobal())
                .putInt(KEY_TURBO_FREQ, hitboxView.getTurboFrequency())
                .apply();
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    public void onBackPressed() {
        if (sidePanel != null && sidePanel.isOpen()) {
            sidePanel.closePanel();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConnectionStateChanged(final boolean connected, final String message) {
        runOnUiThread(() -> {
            if (hitboxView != null) {
                hitboxView.updateConnectionStatus(connected, message);
            }
            if (sidePanel != null) {
                sidePanel.updateConnectionStatusPill();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveActiveLayoutPrefs();
        if (socketClient != null) {
            socketClient.disconnect();
        }
    }
}
