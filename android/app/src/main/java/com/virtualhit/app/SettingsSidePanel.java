package com.virtualhit.app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsSidePanel extends FrameLayout {

    // Palette Constants
    public static final int COLOR_DRAWER_BG = Color.parseColor("#373B4B");
    public static final int COLOR_SECTION_HEADER = Color.parseColor("#4D5A83");
    public static final int COLOR_SECTION_BG = Color.parseColor("#353759");
    public static final int COLOR_BUTTON = Color.parseColor("#0F3153");
    public static final int COLOR_ACCENT = Color.parseColor("#00B099");

    private final HitboxView hitboxView;
    private final SocketClient socketClient;
    private final OnPanelEventListener eventListener;

    private LinearLayout panelContainer;
    private TextView txtConnectionPill;
    private View dotConnection;

    // Connection Controls
    private EditText editHost;
    private EditText editPort;

    // Appearance Inline Sliders
    private float[] bgHsv = new float[3];
    private float[] lightHsv = new float[3];

    private SeekBar sbBgHue, sbBgSat, sbBgVal;
    private TextView lblBgHue, lblBgSat, lblBgVal;

    private Switch swLights;
    private SeekBar sbLightsSpeed;
    private Switch swResponsiveLights;
    private Switch swCustomLightColor;
    private LinearLayout customLightControls;
    private SeekBar sbLightHue, sbLightSat, sbLightVal;
    private TextView lblLightHue, lblLightSat, lblLightVal;

    // Layout Controls
    private Spinner spinSlot;
    private Switch swShowText;
    private Switch swEditMode;
    private LinearLayout layoutEditorControls;
    private TextView txtSelectedButtonLabel;
    private RadioGroup rgShape;
    private RadioButton rbRound, rbSquare;
    private LinearLayout squareControls;
    private SeekBar sbWidth, sbHeight, sbRadius;
    private TextView lblWidthVal, lblHeightVal;
    private Button btnButtonColorNorm, btnButtonColorPress;

    // Haptics Intensity
    private Switch swHaptics;
    private LinearLayout hapticsIntensityLayout;
    private SeekBar sbHapticIntensity;
    private TextView lblHapticVal;

    private boolean isOpen = false;

    public interface OnPanelEventListener {
        void onSaveConnection(String host, int port);
        void onSaveLayoutSlot(int slotIndex);
        void onLoadLayoutSlot(int slotIndex);
        void onLayoutChanged();
    }

    public SettingsSidePanel(Context context, HitboxView hitboxView, SocketClient socketClient, OnPanelEventListener eventListener) {
        super(context);
        this.hitboxView = hitboxView;
        this.socketClient = socketClient;
        this.eventListener = eventListener;

        Color.colorToHSV(hitboxView.getBackgroundColor(), bgHsv);
        Color.colorToHSV(hitboxView.getCustomLightColor(), lightHsv);

        init(context);
    }

    private void init(Context context) {
        setBackgroundColor(Color.parseColor("#AA000000"));
        setVisibility(GONE);

        setOnClickListener(v -> closePanel());

        // Traditional Pop-up Menu Modal Container (#373B4B)
        panelContainer = new LinearLayout(context);
        panelContainer.setOrientation(LinearLayout.VERTICAL);
        
        GradientDrawable dialogBg = new GradientDrawable();
        dialogBg.setColor(COLOR_DRAWER_BG);
        dialogBg.setCornerRadius(dpToPx(context, 16));
        panelContainer.setBackground(dialogBg);

        panelContainer.setPadding(24, 20, 24, 20);
        panelContainer.setClickable(true);

        // Centered Pop-up Modal Dimensions
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int maxWidth = Math.min(dpToPx(context, 540), dm.widthPixels - dpToPx(context, 32));
        int maxHeight = Math.min(dpToPx(context, 680), dm.heightPixels - dpToPx(context, 24));

        LayoutParams params = new LayoutParams(maxWidth, maxHeight);
        params.gravity = Gravity.CENTER;
        panelContainer.setLayoutParams(params);

        // Modal Header Title Bar
        LinearLayout headerBar = new LinearLayout(context);
        headerBar.setOrientation(LinearLayout.HORIZONTAL);
        headerBar.setGravity(Gravity.CENTER_VERTICAL);
        headerBar.setPadding(8, 4, 8, 12);

        TextView titleText = new TextView(context);
        titleText.setText("VirtualHit Settings");
        titleText.setTextColor(COLOR_ACCENT);
        titleText.setTextSize(18);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnCloseTop = new Button(context);
        btnCloseTop.setText("✕");
        btnCloseTop.setTextColor(Color.WHITE);
        btnCloseTop.setTextSize(16);
        btnCloseTop.setBackground(createRoundedDrawable(COLOR_SECTION_HEADER, 8));
        LinearLayout.LayoutParams btnCloseParams = new LinearLayout.LayoutParams(dpToPx(context, 36), dpToPx(context, 36));
        btnCloseTop.setLayoutParams(btnCloseParams);
        btnCloseTop.setOnClickListener(v -> closePanel());

        headerBar.addView(titleText);
        headerBar.addView(btnCloseTop);
        panelContainer.addView(headerBar);

        // ScrollView stretching 100% of remaining vertical space in popup
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollParams.setMargins(0, 4, 0, 0);
        scrollView.setLayoutParams(scrollParams);

        LinearLayout scrollContent = new LinearLayout(context);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        scrollContent.setLayoutParams(contentParams);

        // --- 1. TOP HEADER: CONNECTION PILL ---
        scrollContent.addView(createConnectionHeader(context));

        // --- 2. SECTION 1: CONNECTION SETTINGS ---
        scrollContent.addView(createSectionCard(context, "Connection Settings", createConnectionContent(context)));

        // --- 3. SECTION 2: APPEARANCE AND BACKGROUND ---
        scrollContent.addView(createSectionCard(context, "Appearance and Background", createAppearanceContent(context)));

        // --- 4. SECTION 3: LAYOUT SLOTS & CUSTOMIZATION ---
        scrollContent.addView(createSectionCard(context, "Layout Customization Engine", createLayoutEditorContent(context)));

        // --- 5. SECTION 4: TURBO MODE SETTINGS ---
        scrollContent.addView(createSectionCard(context, "Turbo Mode Settings", createTurboSettingsContent(context)));

        // --- 6. SECTION 5: SOCD RULES AND HAPTICS ---
        scrollContent.addView(createSectionCard(context, "SOCD Rules and Haptics", createSocdContent(context)));

        // --- 7. FOOTER: RJ PANDA VERSION STRING ---
        scrollContent.addView(createFooterLabel(context));

        scrollView.addView(scrollContent);
        panelContainer.addView(scrollView);

        addView(panelContainer);

        hitboxView.setOnLayoutChangeListener(new HitboxView.OnLayoutChangeListener() {
            @Override
            public void onSelectedButtonChanged(HitboxButton button) {
                updateSelectedButtonUI(button);
            }

            @Override
            public void onLayoutModified() {
                if (eventListener != null) {
                    eventListener.onLayoutChanged();
                }
            }
        });
    }

    public void openPanel() {
        if (isOpen) return;
        isOpen = true;
        updateConnectionStatusPill();
        setVisibility(VISIBLE);

        panelContainer.setAlpha(0f);
        panelContainer.setScaleX(0.85f);
        panelContainer.setScaleY(0.85f);
        panelContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .start();
    }

    public void closePanel() {
        if (!isOpen) return;
        isOpen = false;

        panelContainer.animate()
                .alpha(0f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(150)
                .withEndAction(() -> setVisibility(GONE))
                .start();
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void updateConnectionStatusPill() {
        if (txtConnectionPill != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                boolean connected = socketClient.isConnected();
                dotConnection.setBackground(createRoundedDrawable(
                        connected ? Color.parseColor("#00E676") : Color.parseColor("#FF1744"), 10));
                txtConnectionPill.setText(hitboxView.getConnectionStatusText());
            });
        }
    }

    // --- Connection Header Pill ---
    private View createConnectionHeader(Context ctx) {
        LinearLayout pillLayout = new LinearLayout(ctx);
        pillLayout.setOrientation(LinearLayout.HORIZONTAL);
        pillLayout.setGravity(Gravity.CENTER_VERTICAL);
        pillLayout.setPadding(20, 10, 20, 10);
        pillLayout.setBackground(createRoundedDrawable(COLOR_SECTION_HEADER, 12));

        dotConnection = new View(ctx);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(18, 18);
        dotParams.setMargins(0, 0, 14, 0);
        dotConnection.setLayoutParams(dotParams);
        dotConnection.setBackground(createRoundedDrawable(Color.parseColor("#FF1744"), 9));

        txtConnectionPill = new TextView(ctx);
        txtConnectionPill.setText("• Disconnected");
        txtConnectionPill.setTextColor(Color.WHITE);
        txtConnectionPill.setTextSize(13);
        txtConnectionPill.setTypeface(null, Typeface.BOLD);
        txtConnectionPill.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnReconnect = new Button(ctx);
        btnReconnect.setText("Reconnect");
        btnReconnect.setTextColor(Color.WHITE);
        btnReconnect.setTextSize(11);
        btnReconnect.setBackground(createRoundedDrawable(COLOR_BUTTON, 8));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(12, 0, 0, 0);
        btnReconnect.setLayoutParams(btnParams);
        btnReconnect.setPadding(dpToPx(ctx, 14), dpToPx(ctx, 6), dpToPx(ctx, 14), dpToPx(ctx, 6));

        btnReconnect.setOnClickListener(v -> {
            if (socketClient != null) {
                socketClient.connect();
                updateConnectionStatusPill();
            }
        });

        pillLayout.addView(dotConnection);
        pillLayout.addView(txtConnectionPill);
        pillLayout.addView(btnReconnect);

        LinearLayout wrapper = new LinearLayout(ctx);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, 0, 0, 14);
        wrapper.addView(pillLayout);
        return wrapper;
    }

    // --- Section Card Container Helper ---
    private View createSectionCard(Context ctx, String titleText, View contentBody) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(0, 0, 0, 20);

        LinearLayout header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(24, 16, 24, 16);
        header.setBackground(createRoundedDrawable(COLOR_SECTION_HEADER, 12));
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(ctx);
        title.setText("• " + titleText);
        title.setTextColor(Color.WHITE);
        title.setTextSize(14);
        title.setTypeface(null, Typeface.BOLD);

        header.addView(title);

        contentBody.setPadding(24, 24, 24, 24);
        contentBody.setBackground(createRoundedDrawable(COLOR_SECTION_BG, 12));

        card.addView(header);
        card.addView(contentBody);
        return card;
    }

    // --- Content 1: Connection Settings ---
    private View createConnectionContent(Context ctx) {
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView lblHost = new TextView(ctx);
        lblHost.setText("• Host IP:");
        lblHost.setTextColor(Color.WHITE);
        editHost = new EditText(ctx);
        editHost.setText("127.0.0.1");
        editHost.setTextColor(Color.WHITE);
        editHost.setBackground(createRoundedDrawable(Color.parseColor("#2C303E"), 8));
        editHost.setPadding(20, 16, 20, 16);

        TextView lblPort = new TextView(ctx);
        lblPort.setText("• Port:");
        lblPort.setTextColor(Color.WHITE);
        lblPort.setPadding(0, 12, 0, 0);
        editPort = new EditText(ctx);
        editPort.setText("8888");
        editPort.setTextColor(Color.WHITE);
        editPort.setBackground(createRoundedDrawable(Color.parseColor("#2C303E"), 8));
        editPort.setPadding(20, 16, 20, 16);

        Button btnSaveConn = createStyledFullButton(ctx, "Save & Connect");
        btnSaveConn.setOnClickListener(v -> {
            String h = editHost.getText().toString().trim();
            int p = 8888;
            try { p = Integer.parseInt(editPort.getText().toString().trim()); } catch (Exception ignored) {}
            if (eventListener != null) {
                eventListener.onSaveConnection(h, p);
            }
            updateConnectionStatusPill();
        });

        layout.addView(lblHost);
        layout.addView(editHost);
        layout.addView(lblPort);
        layout.addView(editPort);
        layout.addView(btnSaveConn);
        return layout;
    }

    // --- Content 2: Appearance and Background ---
    private View createAppearanceContent(Context ctx) {
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Background Color Picker Title
        TextView lblBgTitle = new TextView(ctx);
        lblBgTitle.setText("• Background Color Picker:");
        lblBgTitle.setTextColor(Color.WHITE);
        lblBgTitle.setTypeface(null, Typeface.BOLD);
        layout.addView(lblBgTitle);

        // Background HSV Sliders
        Color.colorToHSV(hitboxView.getBackgroundColor(), bgHsv);

        lblBgHue = new TextView(ctx);
        lblBgHue.setText("• Hue: " + (int) bgHsv[0]);
        lblBgHue.setTextColor(Color.WHITE);
        sbBgHue = new SeekBar(ctx);
        sbBgHue.setMax(360);
        sbBgHue.setProgress((int) bgHsv[0]);

        lblBgSat = new TextView(ctx);
        lblBgSat.setText("• Saturation: " + (int) (bgHsv[1] * 100) + "%");
        lblBgSat.setTextColor(Color.WHITE);
        sbBgSat = new SeekBar(ctx);
        sbBgSat.setMax(100);
        sbBgSat.setProgress((int) (bgHsv[1] * 100));

        lblBgVal = new TextView(ctx);
        lblBgVal.setText("• Brightness: " + (int) (bgHsv[2] * 100) + "%");
        lblBgVal.setTextColor(Color.WHITE);
        sbBgVal = new SeekBar(ctx);
        sbBgVal.setMax(100);
        sbBgVal.setProgress((int) (bgHsv[2] * 100));

        SeekBar.OnSeekBarChangeListener bgHsvListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    bgHsv[0] = sbBgHue.getProgress();
                    bgHsv[1] = sbBgSat.getProgress() / 100.0f;
                    bgHsv[2] = sbBgVal.getProgress() / 100.0f;

                    lblBgHue.setText("• Hue: " + (int) bgHsv[0]);
                    lblBgSat.setText("• Saturation: " + (int) (bgHsv[1] * 100) + "%");
                    lblBgVal.setText("• Brightness: " + (int) (bgHsv[2] * 100) + "%");

                    hitboxView.setBackgroundHSV(bgHsv[0], bgHsv[1], bgHsv[2]);
                    if (eventListener != null) eventListener.onLayoutChanged();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        };

        sbBgHue.setOnSeekBarChangeListener(bgHsvListener);
        sbBgSat.setOnSeekBarChangeListener(bgHsvListener);
        sbBgVal.setOnSeekBarChangeListener(bgHsvListener);

        layout.addView(lblBgHue);
        layout.addView(sbBgHue);
        layout.addView(lblBgSat);
        layout.addView(sbBgSat);
        layout.addView(lblBgVal);
        layout.addView(sbBgVal);

        Button btnResetBg = createStyledFullButton(ctx, "Reset Background Color");
        btnResetBg.setOnClickListener(v -> {
            hitboxView.resetBackgroundColor();
            Color.colorToHSV(HitboxView.DEFAULT_BG_COLOR, bgHsv);
            sbBgHue.setProgress((int) bgHsv[0]);
            sbBgSat.setProgress((int) (bgHsv[1] * 100));
            sbBgVal.setProgress((int) (bgHsv[2] * 100));
            lblBgHue.setText("• Hue: " + (int) bgHsv[0]);
            lblBgSat.setText("• Saturation: " + (int) (bgHsv[1] * 100) + "%");
            lblBgVal.setText("• Brightness: " + (int) (bgHsv[2] * 100) + "%");
            if (eventListener != null) eventListener.onLayoutChanged();
        });
        layout.addView(btnResetBg);

        // Light Effect Controls
        TextView lblLightTitle = new TextView(ctx);
        lblLightTitle.setText("• Light Effect Controls:");
        lblLightTitle.setTextColor(Color.WHITE);
        lblLightTitle.setTypeface(null, Typeface.BOLD);
        lblLightTitle.setPadding(0, 20, 0, 8);
        layout.addView(lblLightTitle);

        swLights = createStyledSwitch(ctx, "Edge Glow");
        swLights.setChecked(hitboxView.isLightsEnabled());
        swLights.setOnCheckedChangeListener((bv, isChecked) -> {
            hitboxView.setLightsEnabled(isChecked);
            if (eventListener != null) eventListener.onLayoutChanged();
        });
        layout.addView(swLights);

        TextView lblSpeed = new TextView(ctx);
        lblSpeed.setText("• Light Cycle Speed:");
        lblSpeed.setTextColor(Color.WHITE);
        sbLightsSpeed = new SeekBar(ctx);
        sbLightsSpeed.setMax(300);
        sbLightsSpeed.setProgress((int) (hitboxView.getLightsSpeed() * 100));
        sbLightsSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                float spd = Math.max(0.2f, progress / 100.0f);
                hitboxView.setLightsSpeed(spd);
                if (eventListener != null) eventListener.onLayoutChanged();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        layout.addView(lblSpeed);
        layout.addView(sbLightsSpeed);

        swResponsiveLights = createStyledSwitch(ctx, "Responsive (Touch Glow Only)");
        swResponsiveLights.setChecked(hitboxView.isResponsiveLights());
        swResponsiveLights.setOnCheckedChangeListener((bv, isChecked) -> {
            hitboxView.setResponsiveLights(isChecked);
            if (eventListener != null) eventListener.onLayoutChanged();
        });
        layout.addView(swResponsiveLights);

        swCustomLightColor = createStyledSwitch(ctx, "Custom Light Color");
        swCustomLightColor.setChecked(hitboxView.isCustomLightColorMode());
        swCustomLightColor.setOnCheckedChangeListener((bv, isChecked) -> {
            hitboxView.setCustomLightColorMode(isChecked);
            customLightControls.setVisibility(isChecked ? VISIBLE : GONE);
            if (eventListener != null) eventListener.onLayoutChanged();
        });
        layout.addView(swCustomLightColor);

        // Custom Light HSV Sliders
        customLightControls = new LinearLayout(ctx);
        customLightControls.setOrientation(LinearLayout.VERTICAL);
        customLightControls.setVisibility(hitboxView.isCustomLightColorMode() ? VISIBLE : GONE);

        Color.colorToHSV(hitboxView.getCustomLightColor(), lightHsv);

        lblLightHue = new TextView(ctx);
        lblLightHue.setText("• Light Hue: " + (int) lightHsv[0]);
        lblLightHue.setTextColor(Color.WHITE);
        sbLightHue = new SeekBar(ctx);
        sbLightHue.setMax(360);
        sbLightHue.setProgress((int) lightHsv[0]);

        lblLightSat = new TextView(ctx);
        lblLightSat.setText("• Light Saturation: " + (int) (lightHsv[1] * 100) + "%");
        lblLightSat.setTextColor(Color.WHITE);
        sbLightSat = new SeekBar(ctx);
        sbLightSat.setMax(100);
        sbLightSat.setProgress((int) (lightHsv[1] * 100));

        lblLightVal = new TextView(ctx);
        lblLightVal.setText("• Light Brightness: " + (int) (lightHsv[2] * 100) + "%");
        lblLightVal.setTextColor(Color.WHITE);
        sbLightVal = new SeekBar(ctx);
        sbLightVal.setMax(100);
        sbLightVal.setProgress((int) (lightHsv[2] * 100));

        SeekBar.OnSeekBarChangeListener lightHsvListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    lightHsv[0] = sbLightHue.getProgress();
                    lightHsv[1] = sbLightSat.getProgress() / 100.0f;
                    lightHsv[2] = sbLightVal.getProgress() / 100.0f;

                    lblLightHue.setText("• Light Hue: " + (int) lightHsv[0]);
                    lblLightSat.setText("• Light Saturation: " + (int) (lightHsv[1] * 100) + "%");
                    lblLightVal.setText("• Light Brightness: " + (int) (lightHsv[2] * 100) + "%");

                    hitboxView.setCustomLightHSV(lightHsv[0], lightHsv[1], lightHsv[2]);
                    if (eventListener != null) eventListener.onLayoutChanged();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        };

        sbLightHue.setOnSeekBarChangeListener(lightHsvListener);
        sbLightSat.setOnSeekBarChangeListener(lightHsvListener);
        sbLightVal.setOnSeekBarChangeListener(lightHsvListener);

        customLightControls.addView(lblLightHue);
        customLightControls.addView(sbLightHue);
        customLightControls.addView(lblLightSat);
        customLightControls.addView(sbLightSat);
        customLightControls.addView(lblLightVal);
        customLightControls.addView(sbLightVal);

        layout.addView(customLightControls);
        return layout;
    }

    // --- Content 3: Layout Slots & Customization Engine ---
    private View createLayoutEditorContent(Context ctx) {
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Layout Slots Spinner (Custom 1, Custom 2, Custom 3 - NO DEFAULT SLOT)
        TextView lblSlot = new TextView(ctx);
        lblSlot.setText("• Select Layout Slot:");
        lblSlot.setTextColor(Color.WHITE);

        spinSlot = new Spinner(ctx);
        spinSlot.setBackground(createRoundedDrawable(Color.parseColor("#2C303E"), 8));
        String[] slots = new String[]{"Custom 1", "Custom 2", "Custom 3"};
        WhiteTextSpinnerAdapter slotAdapter = new WhiteTextSpinnerAdapter(ctx, slots);
        spinSlot.setAdapter(slotAdapter);

        LinearLayout slotBtnRow = new LinearLayout(ctx);
        slotBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        slotBtnRow.setPadding(0, 8, 0, 16);

        Button btnSaveSlot = createStyledRowButton(ctx, "Save Slot");
        btnSaveSlot.setOnClickListener(v -> {
            int pos = spinSlot.getSelectedItemPosition() + 1; // 1-based indexing for Custom 1, 2, 3
            if (eventListener != null) eventListener.onSaveLayoutSlot(pos);
        });

        Button btnLoadSlot = createStyledRowButton(ctx, "Load Slot");
        btnLoadSlot.setOnClickListener(v -> {
            int pos = spinSlot.getSelectedItemPosition() + 1; // 1-based indexing for Custom 1, 2, 3
            if (eventListener != null) eventListener.onLoadLayoutSlot(pos);
        });

        slotBtnRow.addView(btnSaveSlot);
        slotBtnRow.addView(btnLoadSlot);

        layout.addView(lblSlot);
        layout.addView(spinSlot);
        layout.addView(slotBtnRow);

        // Show/Hide Button Text Labels Toggle
        swShowText = createStyledSwitch(ctx, "Show Button Text Labels");
        swShowText.setChecked(hitboxView.isShowButtonText());
        swShowText.setOnCheckedChangeListener((bv, isChecked) -> {
            hitboxView.setShowButtonText(isChecked);
            if (eventListener != null) eventListener.onLayoutChanged();
        });
        layout.addView(swShowText);

        // Edit Mode Switch
        swEditMode = createStyledSwitch(ctx, "Interactive Layout Edit Mode");
        swEditMode.setChecked(hitboxView.isEditMode());
        swEditMode.setOnCheckedChangeListener((bv, isChecked) -> {
            hitboxView.setEditMode(isChecked);
            layoutEditorControls.setVisibility(isChecked ? VISIBLE : GONE);
        });
        layout.addView(swEditMode);

        layoutEditorControls = new LinearLayout(ctx);
        layoutEditorControls.setOrientation(LinearLayout.VERTICAL);
        layoutEditorControls.setVisibility(hitboxView.isEditMode() ? VISIBLE : GONE);
        layoutEditorControls.setPadding(0, 16, 0, 0);

        // Action Row: Add, Delete, Reset Layout
        LinearLayout actionRow = new LinearLayout(ctx);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnAdd = createStyledRowButton(ctx, "Add");
        btnAdd.setOnClickListener(v -> showAddButtonPicker(ctx));

        Button btnDel = createStyledRowButton(ctx, "Delete");
        btnDel.setOnClickListener(v -> hitboxView.deleteSelectedButton());

        Button btnReset = createStyledRowButton(ctx, "Reset Layout");
        btnReset.setOnClickListener(v -> {
            int pos = spinSlot.getSelectedItemPosition() + 1;
            if (eventListener != null) eventListener.onLoadLayoutSlot(pos);
        });

        actionRow.addView(btnAdd);
        actionRow.addView(btnDel);
        actionRow.addView(btnReset);
        layoutEditorControls.addView(actionRow);

        txtSelectedButtonLabel = new TextView(ctx);
        txtSelectedButtonLabel.setText("• Selected Button: None (Tap button to edit)");
        txtSelectedButtonLabel.setTextColor(Color.WHITE);
        txtSelectedButtonLabel.setPadding(0, 16, 0, 8);
        layoutEditorControls.addView(txtSelectedButtonLabel);

        // Button Shape Selector
        TextView lblShape = new TextView(ctx);
        lblShape.setText("• Button Shape:");
        lblShape.setTextColor(Color.WHITE);
        layoutEditorControls.addView(lblShape);

        rgShape = new RadioGroup(ctx);
        rgShape.setOrientation(RadioGroup.HORIZONTAL);

        rbRound = new RadioButton(ctx);
        rbRound.setText("Round");
        rbRound.setTextColor(Color.WHITE);

        rbSquare = new RadioButton(ctx);
        rbSquare.setText("Square / Rect");
        rbSquare.setTextColor(Color.WHITE);

        rgShape.addView(rbRound);
        rgShape.addView(rbSquare);
        rgShape.setOnCheckedChangeListener((group, checkedId) -> {
            HitboxButton sel = hitboxView.getSelectedButton();
            if (sel != null) {
                boolean isSq = (checkedId == rbSquare.getId());
                if (sel.isDisc) {
                    hitboxView.updateSelectedButtonDiscSquare(isSq);
                    squareControls.setVisibility(GONE);
                } else {
                    hitboxView.updateSelectedButtonShape(isSq ? HitboxButton.Shape.SQUARE : HitboxButton.Shape.CIRCLE);
                    squareControls.setVisibility(isSq ? VISIBLE : GONE);
                }
            }
        });
        layoutEditorControls.addView(rgShape);

        // Overall Size Slider
        TextView lblRadius = new TextView(ctx);
        lblRadius.setText("• Overall Size:");
        lblRadius.setTextColor(Color.WHITE);
        sbRadius = new SeekBar(ctx);
        sbRadius.setMax(200);
        sbRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && hitboxView.getSelectedButton() != null) {
                    float size = Math.max(20, progress);
                    hitboxView.updateSelectedButtonDimensions(size * 2, size * 2);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        layoutEditorControls.addView(lblRadius);
        layoutEditorControls.addView(sbRadius);

        // Square Dimensions Sliders: Width & Height up to 300%
        squareControls = new LinearLayout(ctx);
        squareControls.setOrientation(LinearLayout.VERTICAL);
        squareControls.setVisibility(GONE);

        lblWidthVal = new TextView(ctx);
        lblWidthVal.setText("• Width (up to 300%):");
        lblWidthVal.setTextColor(Color.WHITE);
        sbWidth = new SeekBar(ctx);
        sbWidth.setMax(300);

        lblHeightVal = new TextView(ctx);
        lblHeightVal.setText("• Height (up to 300%):");
        lblHeightVal.setTextColor(Color.WHITE);
        sbHeight = new SeekBar(ctx);
        sbHeight.setMax(300);

        SeekBar.OnSeekBarChangeListener dimListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && hitboxView.getSelectedButton() != null) {
                    float w = Math.max(30, sbWidth.getProgress());
                    float h = Math.max(30, sbHeight.getProgress());
                    lblWidthVal.setText("• Width: " + (int) w + "px");
                    lblHeightVal.setText("• Height: " + (int) h + "px");
                    hitboxView.updateSelectedButtonDimensions(w, h);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        };
        sbWidth.setOnSeekBarChangeListener(dimListener);
        sbHeight.setOnSeekBarChangeListener(dimListener);

        squareControls.addView(lblWidthVal);
        squareControls.addView(sbWidth);
        squareControls.addView(lblHeightVal);
        squareControls.addView(sbHeight);
        layoutEditorControls.addView(squareControls);

        // Normal & Pressed Color Pickers
        LinearLayout colorsRow = new LinearLayout(ctx);
        colorsRow.setOrientation(LinearLayout.HORIZONTAL);
        colorsRow.setPadding(0, 16, 0, 0);

        btnButtonColorNorm = createStyledRowButton(ctx, "Normal Color");
        btnButtonColorNorm.setOnClickListener(v -> {
            HitboxButton sel = hitboxView.getSelectedButton();
            if (sel != null) {
                ColorPickerDialog dlg = new ColorPickerDialog(ctx, "Normal Color", sel.normalColor, color -> {
                    hitboxView.updateSelectedButtonColor(color, sel.pressedColor);
                });
                dlg.show();
            }
        });

        btnButtonColorPress = createStyledRowButton(ctx, "Pressed Color");
        btnButtonColorPress.setOnClickListener(v -> {
            HitboxButton sel = hitboxView.getSelectedButton();
            if (sel != null) {
                ColorPickerDialog dlg = new ColorPickerDialog(ctx, "Pressed Highlight Color", sel.pressedColor, color -> {
                    hitboxView.updateSelectedButtonColor(sel.normalColor, color);
                });
                dlg.show();
            }
        });

        colorsRow.addView(btnButtonColorNorm);
        colorsRow.addView(btnButtonColorPress);
        layoutEditorControls.addView(colorsRow);

        layout.addView(layoutEditorControls);
        return layout;
    }

    private View createTurboSettingsContent(Context ctx) {
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView lblFreq = new TextView(ctx);
        lblFreq.setText("• Rapid-Fire Speed: " + hitboxView.getTurboFrequency() + " presses/sec");
        lblFreq.setTextColor(Color.WHITE);

        SeekBar sbTurboFreq = new SeekBar(ctx);
        sbTurboFreq.setMax(19); // 0..19 -> maps to 1..20
        sbTurboFreq.setProgress(hitboxView.getTurboFrequency() - 1);
        sbTurboFreq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int freq = progress + 1;
                hitboxView.setTurboFrequency(freq);
                lblFreq.setText("• Rapid-Fire Speed: " + freq + " presses/sec");
                if (eventListener != null) eventListener.onLayoutChanged();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        Switch swTurboGlobal = createStyledSwitch(ctx, "Enable Turbo Mode Functionality");
        swTurboGlobal.setChecked(hitboxView.isTurboEnabledGlobal());
        swTurboGlobal.setOnCheckedChangeListener((bv, isChecked) -> {
            hitboxView.setTurboEnabledGlobal(isChecked);
            if (eventListener != null) eventListener.onLayoutChanged();
        });

        layout.addView(lblFreq);
        layout.addView(sbTurboFreq);
        layout.addView(swTurboGlobal);
        return layout;
    }

    private void showAddButtonPicker(Context ctx) {
        Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        int maxWidth = Math.min(dpToPx(ctx, 540), dm.widthPixels - dpToPx(ctx, 32));
        int maxHeight = Math.min(dpToPx(ctx, 680), dm.heightPixels - dpToPx(ctx, 24));

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 20, 24, 20);

        GradientDrawable dialogBg = new GradientDrawable();
        dialogBg.setColor(COLOR_DRAWER_BG);
        dialogBg.setCornerRadius(dpToPx(ctx, 16));
        root.setBackground(dialogBg);

        // Dialog Header Bar
        LinearLayout headerBar = new LinearLayout(ctx);
        headerBar.setOrientation(LinearLayout.HORIZONTAL);
        headerBar.setGravity(Gravity.CENTER_VERTICAL);
        headerBar.setPadding(8, 4, 8, 12);

        TextView title = new TextView(ctx);
        title.setText("• Add Controller Button");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(COLOR_ACCENT);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnCloseDlg = new Button(ctx);
        btnCloseDlg.setText("✕");
        btnCloseDlg.setTextColor(Color.WHITE);
        btnCloseDlg.setTextSize(16);
        btnCloseDlg.setBackground(createRoundedDrawable(COLOR_SECTION_HEADER, 8));
        LinearLayout.LayoutParams btnCloseParams = new LinearLayout.LayoutParams(dpToPx(ctx, 36), dpToPx(ctx, 36));
        btnCloseDlg.setLayoutParams(btnCloseParams);
        btnCloseDlg.setOnClickListener(v -> dialog.dismiss());

        headerBar.addView(title);
        headerBar.addView(btnCloseDlg);
        root.addView(headerBar);

        // Scrollable List of Available Buttons
        ScrollView scrollView = new ScrollView(ctx);
        scrollView.setFillViewport(true);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollParams.setMargins(0, 8, 0, 0);
        scrollView.setLayoutParams(scrollParams);

        LinearLayout listContent = new LinearLayout(ctx);
        listContent.setOrientation(LinearLayout.VERTICAL);

        String[] buttonNames = new String[]{
                "DPad Disc (Directional Rose)", "ABXY Pad (Action Disc)",
                "A (Light Kick)", "X (Light Punch)", "B (Medium Kick)", "Y (Medium Punch)",
                "RB (Heavy Punch)", "RT (Heavy Kick)", "LB (Extra Punch)", "LT (Extra Kick)",
                "Left Stick (LJ)", "Right Stick (RJ)",
                "L3 (Press Left Stick)", "R3 (Press Right Stick)",
                "SELECT (Coin)", "START (Options)", "HOME (PS / Guide)", "TM (Turbo Mode)",
                "LEFT (◄)", "DOWN (▼)", "RIGHT (►)", "UP (▲)"
        };

        String[] buttonIds = new String[]{
                "DPAD_DISC", "ABXY_DISC",
                "LK", "LP", "MK", "MP",
                "HP", "HK", "AP", "AK",
                "LEFT_STICK", "RIGHT_STICK",
                "L3", "R3",
                "SEL", "STA", "HOM", "TPAD",
                "LEFT", "DOWN", "RIGHT", "UP"
        };

        String[] labels = new String[]{
                "D-Pad", "ABXY Pad",
                "A", "X", "B", "Y",
                "RB", "RT", "LB", "LT",
                "LJ", "RJ",
                "L3", "R3",
                "SEL", "STA", "HOME", "TM",
                "◄", "▼", "►", "▲"
        };

        int[] bitFlags = new int[]{
                0, 0,
                HitboxButton.BIT_LK, HitboxButton.BIT_LP, HitboxButton.BIT_MK, HitboxButton.BIT_MP,
                HitboxButton.BIT_HP, HitboxButton.BIT_HK, HitboxButton.BIT_AP, HitboxButton.BIT_AK,
                HitboxButton.BIT_LEFT_STICK, HitboxButton.BIT_RIGHT_STICK,
                HitboxButton.BIT_L3, HitboxButton.BIT_R3,
                HitboxButton.BIT_SELECT, HitboxButton.BIT_START, HitboxButton.BIT_HOME, HitboxButton.BIT_TOUCHPAD,
                HitboxButton.BIT_LEFT, HitboxButton.BIT_DOWN, HitboxButton.BIT_RIGHT, HitboxButton.BIT_UP
        };

        for (int i = 0; i < buttonNames.length; i++) {
            final int index = i;
            TextView itemText = new TextView(ctx);
            itemText.setText("• " + buttonNames[i]);
            itemText.setTextColor(Color.WHITE);
            itemText.setTextSize(15);
            itemText.setTypeface(null, Typeface.BOLD);
            itemText.setPadding(24, 20, 24, 20);

            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            itemParams.setMargins(0, 4, 0, 4);
            itemText.setLayoutParams(itemParams);

            GradientDrawable itemBg = new GradientDrawable();
            itemBg.setColor(COLOR_SECTION_BG);
            itemBg.setCornerRadius(dpToPx(ctx, 10));
            itemText.setBackground(itemBg);

            itemText.setOnClickListener(v -> {
                String id = buttonIds[index];
                if ("DPAD_DISC".equals(id)) {
                    hitboxView.addDiscButton(HitboxButton.DiscType.DPAD);
                } else if ("ABXY_DISC".equals(id)) {
                    hitboxView.addDiscButton(HitboxButton.DiscType.ABXY);
                } else {
                    String label = labels[index];
                    int flag = bitFlags[index];
                    int normCol = Color.parseColor("#1E293B");
                    int pressCol = COLOR_ACCENT;
                    hitboxView.addButtonById(id, label, "", flag, normCol, pressCol);
                }
                dialog.dismiss();
            });

            listContent.addView(itemText);
        }

        scrollView.addView(listContent);
        root.addView(scrollView);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(maxWidth, maxHeight);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    public static class WhiteTextSpinnerAdapter extends ArrayAdapter<String> {
        public WhiteTextSpinnerAdapter(Context context, String[] objects) {
            super(context, android.R.layout.simple_spinner_item, objects);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(13);
                tv.setTypeface(null, Typeface.BOLD);
                tv.setPadding(16, 12, 16, 12);
            }
            return v;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v = super.getDropDownView(position, convertView, parent);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setTextColor(Color.WHITE);
                tv.setBackgroundColor(Color.parseColor("#2C303E"));
                tv.setTextSize(13);
                tv.setPadding(24, 18, 24, 18);
            }
            return v;
        }
    }

    private void updateSelectedButtonUI(HitboxButton btn) {
        if (btn == null) {
            txtSelectedButtonLabel.setText("• Selected Button: None (Tap button to edit)");
            squareControls.setVisibility(GONE);
            rbRound.setText("Round");
            rbSquare.setText("Square / Rect");
            rbRound.setEnabled(true);
            rbSquare.setEnabled(true);
            return;
        }
        txtSelectedButtonLabel.setText("• Selected Button: " + btn.label + " (" + btn.id + ")");

        if (btn.isDisc) {
            rbRound.setText("Circle (Rose)");
            rbSquare.setText("Square (3x3 Grid)");
            rbRound.setEnabled(true);
            rbSquare.setEnabled(true);
            if (btn.discIsSquare) {
                rbSquare.setChecked(true);
            } else {
                rbRound.setChecked(true);
            }
            squareControls.setVisibility(GONE);
        } else if (btn.isJoystick) {
            rbRound.setText("Round");
            rbSquare.setText("Square / Rect");
            rbRound.setChecked(true);
            rbRound.setEnabled(false);
            rbSquare.setEnabled(false);
            squareControls.setVisibility(GONE);
        } else {
            rbRound.setText("Round");
            rbSquare.setText("Square / Rect");
            rbRound.setEnabled(true);
            rbSquare.setEnabled(true);
            if (btn.shape == HitboxButton.Shape.SQUARE || btn.isPillShape) {
                rbSquare.setChecked(true);
                squareControls.setVisibility(VISIBLE);
            } else {
                rbRound.setChecked(true);
                squareControls.setVisibility(GONE);
            }
        }

        sbWidth.setProgress((int) btn.getEffectiveWidth());
        sbHeight.setProgress((int) btn.getEffectiveHeight());
        sbRadius.setProgress((int) btn.radius);
        lblWidthVal.setText("• Width: " + (int) btn.getEffectiveWidth() + "px");
        lblHeightVal.setText("• Height: " + (int) btn.getEffectiveHeight() + "px");
    }

    // --- Content 4: SOCD Rules and Haptics ---
    private View createSocdContent(Context ctx) {
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView lblSocd = new TextView(ctx);
        lblSocd.setText("• SOCD Cleaning Rule:");
        lblSocd.setTextColor(Color.WHITE);

        Spinner spinSocd = new Spinner(ctx);
        spinSocd.setBackground(createRoundedDrawable(Color.parseColor("#2C303E"), 8));
        String[] socdOptions = new String[]{
                "Neutral: L+R=Neutral, D+U=Neutral (Capcom / SF6)",
                "Up Priority: L+R=Neutral, D+U=Up (Classic)",
                "Last Input Priority"
        };
        WhiteTextSpinnerAdapter adapter = new WhiteTextSpinnerAdapter(ctx, socdOptions);
        spinSocd.setAdapter(adapter);
        spinSocd.setSelection(hitboxView.getSocdMode());
        spinSocd.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                hitboxView.setSocdMode(position);
                if (socketClient != null && socketClient.isConnected()) {
                    socketClient.sendSocdModeChange(position);
                }
                if (eventListener != null) eventListener.onLayoutChanged();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        layout.addView(lblSocd);
        layout.addView(spinSocd);

        swHaptics = createStyledSwitch(ctx, "Vibration Haptic Touch Feedback");
        swHaptics.setChecked(hitboxView.isHapticsEnabled());
        swHaptics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hitboxView.setHapticsEnabled(isChecked);
            hapticsIntensityLayout.setVisibility(isChecked ? VISIBLE : GONE);
            if (eventListener != null) eventListener.onLayoutChanged();
        });
        layout.addView(swHaptics);

        // Vibration Intensity Slider (1ms - 50ms)
        hapticsIntensityLayout = new LinearLayout(ctx);
        hapticsIntensityLayout.setOrientation(LinearLayout.VERTICAL);
        hapticsIntensityLayout.setVisibility(hitboxView.isHapticsEnabled() ? VISIBLE : GONE);

        lblHapticVal = new TextView(ctx);
        lblHapticVal.setText("• Vibration Intensity: " + hitboxView.getVibrationIntensity() + "ms");
        lblHapticVal.setTextColor(Color.WHITE);

        sbHapticIntensity = new SeekBar(ctx);
        sbHapticIntensity.setMax(50);
        sbHapticIntensity.setProgress(hitboxView.getVibrationIntensity());
        sbHapticIntensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int val = Math.max(1, progress);
                hitboxView.setVibrationIntensity(val);
                lblHapticVal.setText("• Vibration Intensity: " + val + "ms");
                if (eventListener != null) eventListener.onLayoutChanged();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        hapticsIntensityLayout.addView(lblHapticVal);
        hapticsIntensityLayout.addView(sbHapticIntensity);
        layout.addView(hapticsIntensityLayout);

        return layout;
    }

    // --- Footer Label ---
    private View createFooterLabel(Context ctx) {
        TextView footer = new TextView(ctx);
        footer.setText("VirtualHit v1.4.0 developed by RJ Panda");
        footer.setTextColor(Color.parseColor("#94A3B8"));
        footer.setTextSize(12);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 24, 0, 16);
        return footer;
    }


    // --- Styling Helpers ---
    private GradientDrawable createRoundedDrawable(int fillColor, float cornerRadiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(dpToPx(getContext(), (int) cornerRadiusDp));
        return gd;
    }

    private Button createStyledFullButton(Context ctx, String text) {
        Button btn = new Button(ctx);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setBackground(createRoundedDrawable(COLOR_BUTTON, 10));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(6, 12, 6, 6);
        btn.setLayoutParams(p);
        return btn;
    }

    private Button createStyledRowButton(Context ctx, String text) {
        Button btn = new Button(ctx);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setBackground(createRoundedDrawable(COLOR_BUTTON, 10));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        p.setMargins(6, 6, 6, 6);
        btn.setLayoutParams(p);
        return btn;
    }

    private Button createStyledButton(Context ctx, String text) {
        return createStyledFullButton(ctx, text);
    }

    private Switch createStyledSwitch(Context ctx, String text) {
        Switch sw = new Switch(ctx);
        sw.setText("• " + text);
        sw.setTextColor(Color.WHITE);
        sw.setPadding(0, 10, 0, 10);
        return sw;
    }

    private int dpToPx(Context ctx, int dp) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density);
    }
}
