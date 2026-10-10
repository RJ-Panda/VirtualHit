package com.virtualhit.app;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;

public class SettingsDialog extends Dialog {

    private final HitboxView hitboxView;
    private final SocketClient socketClient;
    private final SettingsSaveListener listener;

    public interface SettingsSaveListener {
        void onSettingsSaved(String host, int port, int socdMode, boolean haptics);
    }

    public SettingsDialog(@NonNull Context context, HitboxView hitboxView, SocketClient socketClient, SettingsSaveListener listener) {
        super(context);
        this.hitboxView = hitboxView;
        this.socketClient = socketClient;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Context ctx = getContext();

        // Create programmatic Layout for Settings Dialog
        android.widget.LinearLayout layout = new android.widget.LinearLayout(ctx);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(android.graphics.Color.parseColor("#181E29"));

        // Title
        android.widget.TextView title = new android.widget.TextView(ctx);
        title.setText("VirtualHit Settings");
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(android.graphics.Color.parseColor("#00E5FF"));
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);

        // Host IP
        android.widget.TextView lblHost = new android.widget.TextView(ctx);
        lblHost.setText("Server Host (127.0.0.1 for USB ADB Tunnel):");
        lblHost.setTextColor(android.graphics.Color.parseColor("#94A3B8"));
        layout.addView(lblHost);

        final EditText editHost = new EditText(ctx);
        editHost.setText("127.0.0.1");
        editHost.setTextColor(android.graphics.Color.WHITE);
        layout.addView(editHost);

        // Port
        android.widget.TextView lblPort = new android.widget.TextView(ctx);
        lblPort.setText("Server Port:");
        lblPort.setTextColor(android.graphics.Color.parseColor("#94A3B8"));
        layout.addView(lblPort);

        final EditText editPort = new EditText(ctx);
        editPort.setText("8888");
        editPort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        editPort.setTextColor(android.graphics.Color.WHITE);
        layout.addView(editPort);

        // SOCD Mode Spinner
        android.widget.TextView lblSocd = new android.widget.TextView(ctx);
        lblSocd.setText("SOCD Cleaning Mode:");
        lblSocd.setTextColor(android.graphics.Color.parseColor("#94A3B8"));
        lblSocd.setPadding(0, 16, 0, 8);
        layout.addView(lblSocd);

        final Spinner spinSocd = new Spinner(ctx);
        String[] socdOptions = new String[]{
                "Neutral: L+R=Neutral, D+U=Neutral (SF6 / Capcom)",
                "Up Priority: L+R=Neutral, D+U=Up (Classic)",
                "Last Input Priority"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, socdOptions);
        spinSocd.setAdapter(adapter);
        spinSocd.setSelection(hitboxView.getSocdMode());
        layout.addView(spinSocd);

        // Haptics Checkbox
        final CheckBox chkHaptics = new CheckBox(ctx);
        chkHaptics.setText("Vibration / Haptic Touch Feedback");
        chkHaptics.setTextColor(android.graphics.Color.WHITE);
        chkHaptics.setChecked(hitboxView.isHapticsEnabled());
        chkHaptics.setPadding(0, 20, 0, 20);
        layout.addView(chkHaptics);

        // Buttons Layout
        android.widget.LinearLayout btnLayout = new android.widget.LinearLayout(ctx);
        btnLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnLayout.setGravity(android.view.Gravity.END);

        Button btnCancel = new Button(ctx);
        btnCancel.setText("Cancel");
        btnCancel.setOnClickListener(v -> dismiss());

        Button btnSave = new Button(ctx);
        btnSave.setText("Save & Reconnect");
        btnSave.setTextColor(android.graphics.Color.parseColor("#00E5FF"));
        btnSave.setOnClickListener(v -> {
            String host = editHost.getText().toString().trim();
            int port = 8888;
            try {
                port = Integer.parseInt(editPort.getText().toString().trim());
            } catch (NumberFormatException ignored) {}

            int socdMode = spinSocd.getSelectedItemPosition();
            boolean haptics = chkHaptics.isChecked();

            if (listener != null) {
                listener.onSettingsSaved(host, port, socdMode, haptics);
            }
            dismiss();
        });

        btnLayout.addView(btnCancel);
        btnLayout.addView(btnSave);
        layout.addView(btnLayout);

        setContentView(layout);
    }
}
