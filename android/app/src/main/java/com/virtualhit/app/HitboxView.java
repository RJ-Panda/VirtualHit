package com.virtualhit.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HitboxView extends View {
    private final List<HitboxButton> buttons = new ArrayList<>();
    private SocketClient socketClient;
    private SOCDCleaner socdCleaner = new SOCDCleaner(SOCDCleaner.MODE_NEUTRAL);

    // Default Palette Constants
    public static final int DEFAULT_BG_COLOR = Color.parseColor("#1C1D26");

    // Paints
    private final Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTextMain = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTextSub = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintSelection = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintEdgeLight = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Vibrator vibrator;
    private boolean hapticsEnabled = true;
    private int vibrationIntensity = 12; // 1ms to 50ms intensity
    private boolean showButtonText = true;

    // Connection state
    private boolean isConnected = false;
    private String connectionStatusText = "• Disconnected";

    // Customization & Background Colors
    private int backgroundColor = DEFAULT_BG_COLOR;

    // Turbo Mode Engine
    private boolean turboEnabledGlobal = true;
    private int turboFrequency = 5; // 1 to 20 presses/sec
    private boolean turboPulseState = false;
    private final Handler turboHandler = new Handler(Looper.getMainLooper());
    private boolean isTurboTickerRunning = false;

    private final Runnable turboRunnable = new Runnable() {
        @Override
        public void run() {
            if (!hasAnyActiveTouch()) {
                isTurboTickerRunning = false;
                return;
            }
            turboPulseState = !turboPulseState;
            sendCurrentState();
            postInvalidate();
            int intervalMs = Math.max(25, 1000 / (turboFrequency * 2));
            turboHandler.postDelayed(this, intervalMs);
        }
    };

    private Bitmap transparentIconBitmap;
    private final Paint paintImage = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Edge Lights Engine
    private boolean lightsEnabled = false;
    private boolean responsiveLights = false;
    private boolean customLightColorMode = false;
    private int customLightColor = Color.parseColor("#00B099");
    private float lightsSpeed = 1.0f;
    private float hueAngle = 0.0f;

    // Layout Edit Mode State
    private boolean isEditMode = false;
    private HitboxButton selectedButton = null;
    private int draggedPointerId = -1;
    private float dragOffsetX = 0;
    private float dragOffsetY = 0;

    private OnSettingsClickListener settingsClickListener;
    private OnLayoutChangeListener layoutChangeListener;

    public interface OnSettingsClickListener {
        void onSettingsClick();
    }

    public interface OnLayoutChangeListener {
        void onSelectedButtonChanged(HitboxButton button);
        void onLayoutModified();
    }

    public HitboxView(Context context) {
        super(context);
        init(context);
    }

    public HitboxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        try {
            transparentIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.transparent_icon);
        } catch (Exception ignored) {}

        paintFill.setStyle(Paint.Style.FILL);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(4.0f);

        paintTextMain.setColor(Color.WHITE);
        paintTextMain.setTextAlign(Paint.Align.CENTER);
        paintTextMain.setFakeBoldText(true);

        paintTextSub.setColor(Color.parseColor("#94A3B8"));
        paintTextSub.setTextAlign(Paint.Align.CENTER);

        paintSelection.setStyle(Paint.Style.STROKE);
        paintSelection.setColor(Color.parseColor("#00B099"));
        paintSelection.setStrokeWidth(6.0f);
        paintSelection.setPathEffect(new DashPathEffect(new float[]{16, 12}, 0));

        paintEdgeLight.setStyle(Paint.Style.STROKE);
        paintEdgeLight.setStrokeWidth(28.0f);

        createDefaultButtons();
    }

    public boolean isTurboEnabledGlobal() {
        return turboEnabledGlobal;
    }

    public void setTurboEnabledGlobal(boolean enabled) {
        this.turboEnabledGlobal = enabled;
        postInvalidate();
    }

    public int getTurboFrequency() {
        return turboFrequency;
    }

    public void setTurboFrequency(int freq) {
        this.turboFrequency = Math.max(1, Math.min(20, freq));
    }

    public void setSocketClient(SocketClient client) {
        this.socketClient = client;
    }

    public void setSocdMode(int mode) {
        this.socdCleaner.setMode(mode);
    }

    public int getSocdMode() {
        return this.socdCleaner.getMode();
    }

    public void setHapticsEnabled(boolean enabled) {
        this.hapticsEnabled = enabled;
    }

    public boolean isHapticsEnabled() {
        return hapticsEnabled;
    }

    public int getVibrationIntensity() {
        return vibrationIntensity;
    }

    public void setVibrationIntensity(int intensity) {
        this.vibrationIntensity = Math.max(1, Math.min(50, intensity));
    }

    public boolean isShowButtonText() {
        return showButtonText;
    }

    public void setShowButtonText(boolean show) {
        this.showButtonText = show;
        postInvalidate();
    }

    public void setOnSettingsClickListener(OnSettingsClickListener listener) {
        this.settingsClickListener = listener;
    }

    public void setOnLayoutChangeListener(OnLayoutChangeListener listener) {
        this.layoutChangeListener = listener;
    }

    public void updateConnectionStatus(boolean connected, String message) {
        this.isConnected = connected;
        this.connectionStatusText = message.startsWith("•") ? message : "• " + message;
        postInvalidate();
    }

    public String getConnectionStatusText() {
        return connectionStatusText;
    }

    public boolean isConnected() {
        return isConnected;
    }

    // --- Background & Colors ---
    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        postInvalidate();
    }

    public void setBackgroundHSV(float h, float s, float v) {
        this.backgroundColor = Color.HSVToColor(new float[]{h, s, v});
        postInvalidate();
    }

    public void resetBackgroundColor() {
        this.backgroundColor = DEFAULT_BG_COLOR;
        postInvalidate();
    }

    // --- Screen Edge Lighting Engine ---
    public boolean isLightsEnabled() {
        return lightsEnabled;
    }

    public void setLightsEnabled(boolean enabled) {
        this.lightsEnabled = enabled;
        postInvalidate();
    }

    public boolean isResponsiveLights() {
        return responsiveLights;
    }

    public void setResponsiveLights(boolean responsive) {
        this.responsiveLights = responsive;
        postInvalidate();
    }

    public boolean isCustomLightColorMode() {
        return customLightColorMode;
    }

    public void setCustomLightColorMode(boolean customMode) {
        this.customLightColorMode = customMode;
        postInvalidate();
    }

    public int getCustomLightColor() {
        return customLightColor;
    }

    public void setCustomLightColor(int color) {
        this.customLightColor = color;
        postInvalidate();
    }

    public void setCustomLightHSV(float h, float s, float v) {
        this.customLightColor = Color.HSVToColor(new float[]{h, s, v});
        this.customLightColorMode = true;
        postInvalidate();
    }

    public float getLightsSpeed() {
        return lightsSpeed;
    }

    public void setLightsSpeed(float speed) {
        this.lightsSpeed = speed;
        postInvalidate();
    }

    private boolean hasAnyActiveTouch() {
        for (HitboxButton btn : buttons) {
            if (btn.isPressed()) return true;
        }
        return false;
    }

    // --- Edit Mode & Layout Controls ---
    public boolean isEditMode() {
        return isEditMode;
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (!editMode) {
            selectedButton = null;
        }
        postInvalidate();
    }

    public HitboxButton getSelectedButton() {
        return selectedButton;
    }

    public void setSelectedButton(HitboxButton btn) {
        this.selectedButton = btn;
        if (layoutChangeListener != null) {
            layoutChangeListener.onSelectedButtonChanged(selectedButton);
        }
        postInvalidate();
    }

    public List<HitboxButton> getButtons() {
        return buttons;
    }

    public void addButtonById(String btnId, String label, String subLabel, int bitFlag, int normalColor, int pressedColor) {
        HitboxButton btn = new HitboxButton(
                btnId + "_add_" + System.currentTimeMillis() % 1000,
                label,
                subLabel,
                bitFlag,
                normalColor,
                pressedColor,
                pressedColor
        );
        btn.cx = getWidth() > 0 ? getWidth() * 0.5f : 400;
        btn.cy = getHeight() > 0 ? getHeight() * 0.5f : 300;
        float baseRadius = getWidth() > 0 ? Math.min(getWidth(), getHeight()) * 0.082f : 60;
        btn.setCircleBounds(btn.cx, btn.cy, baseRadius);
        btn.isDuplicate = true;

        buttons.add(btn);
        setSelectedButton(btn);
        notifyLayoutModified();
    }

    public void addDiscButton(HitboxButton.DiscType discType) {
        boolean isDpad = discType == HitboxButton.DiscType.DPAD;
        String btnId = isDpad ? "DPAD_DISC" : "ABXY_DISC";
        String label = isDpad ? "D-Pad" : "ABXY Pad";
        int mask = isDpad ? (HitboxButton.BIT_UP | HitboxButton.BIT_DOWN | HitboxButton.BIT_LEFT | HitboxButton.BIT_RIGHT)
                          : (HitboxButton.BIT_LP | HitboxButton.BIT_MP | HitboxButton.BIT_LK | HitboxButton.BIT_MK);
        int normalColor = isDpad ? Color.parseColor("#1E293B") : Color.parseColor("#2D2436");
        int pressedColor = isDpad ? Color.parseColor("#00B099") : Color.parseColor("#00E676");

        HitboxButton btn = new HitboxButton(
                btnId + "_add_" + System.currentTimeMillis() % 1000,
                label,
                "",
                mask,
                normalColor,
                pressedColor,
                pressedColor
        );
        btn.isDisc = true;
        btn.discType = discType;
        btn.cx = getWidth() > 0 ? getWidth() * 0.5f : 400;
        btn.cy = getHeight() > 0 ? getHeight() * 0.5f : 300;
        float baseRadius = getWidth() > 0 ? Math.min(getWidth(), getHeight()) * 0.16f : 120;
        btn.setCircleBounds(btn.cx, btn.cy, baseRadius);
        btn.isDuplicate = true;

        buttons.add(btn);
        setSelectedButton(btn);
        notifyLayoutModified();
    }

    public void duplicateSelectedButton() {
        if (selectedButton == null) return;

        HitboxButton dup = new HitboxButton(
                selectedButton.id + "_dup_" + System.currentTimeMillis() % 1000,
                selectedButton.label,
                selectedButton.subLabel,
                selectedButton.bitFlag,
                selectedButton.normalColor,
                selectedButton.pressedColor,
                selectedButton.activeGlowColor
        );
        dup.cx = selectedButton.cx + 50;
        dup.cy = selectedButton.cy + 50;
        dup.radius = selectedButton.radius;
        dup.shape = selectedButton.shape;
        dup.customWidth = selectedButton.customWidth;
        dup.customHeight = selectedButton.customHeight;
        dup.isPillShape = selectedButton.isPillShape;
        dup.isDisc = selectedButton.isDisc;
        dup.discType = selectedButton.discType;
        dup.discIsSquare = selectedButton.discIsSquare;
        dup.isDuplicate = true;

        buttons.add(dup);
        setSelectedButton(dup);
        notifyLayoutModified();
    }

    public void deleteSelectedButton() {
        if (selectedButton == null) return;
        buttons.remove(selectedButton);
        setSelectedButton(null);
        notifyLayoutModified();
    }

    public void updateSelectedButtonShape(HitboxButton.Shape shape) {
        if (selectedButton == null) return;
        selectedButton.shape = shape;
        postInvalidate();
        notifyLayoutModified();
    }

    public void updateSelectedButtonDiscSquare(boolean isSquare) {
        if (selectedButton == null || !selectedButton.isDisc) return;
        selectedButton.discIsSquare = isSquare;
        postInvalidate();
        notifyLayoutModified();
    }

    public void updateSelectedButtonDimensions(float width, float height) {
        if (selectedButton == null) return;
        selectedButton.customWidth = width;
        selectedButton.customHeight = height;
        selectedButton.radius = Math.min(width, height) / 2.0f;
        postInvalidate();
        notifyLayoutModified();
    }

    public void updateSelectedButtonColor(int normalColor, int pressedColor) {
        if (selectedButton == null) return;
        selectedButton.normalColor = normalColor;
        selectedButton.pressedColor = pressedColor;
        selectedButton.activeGlowColor = pressedColor;
        postInvalidate();
        notifyLayoutModified();
    }

    private void notifyLayoutModified() {
        if (layoutChangeListener != null) {
            layoutChangeListener.onLayoutModified();
        }
    }

    // --- JSON Serialization ---
    public String exportLayoutJson() {
        try {
            JSONArray array = new JSONArray();
            for (HitboxButton btn : buttons) {
                array.put(btn.toJson());
            }
            JSONObject root = new JSONObject();
            root.put("buttons", array);
            root.put("backgroundColor", backgroundColor);
            root.put("showButtonText", showButtonText);
            root.put("lightsEnabled", lightsEnabled);
            root.put("responsiveLights", responsiveLights);
            root.put("customLightColorMode", customLightColorMode);
            root.put("customLightColor", customLightColor);
            root.put("lightsSpeed", lightsSpeed);
            root.put("vibrationIntensity", vibrationIntensity);
            root.put("turboEnabledGlobal", turboEnabledGlobal);
            root.put("turboFrequency", turboFrequency);
            return root.toString();
        } catch (JSONException e) {
            return "";
        }
    }

    public void importLayoutJson(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) return;
        try {
            JSONObject root = new JSONObject(jsonStr);
            if (root.has("backgroundColor")) {
                this.backgroundColor = root.getInt("backgroundColor");
            }
            if (root.has("showButtonText")) this.showButtonText = root.getBoolean("showButtonText");
            if (root.has("lightsEnabled")) this.lightsEnabled = root.getBoolean("lightsEnabled");
            if (root.has("responsiveLights")) this.responsiveLights = root.getBoolean("responsiveLights");
            if (root.has("customLightColorMode")) this.customLightColorMode = root.getBoolean("customLightColorMode");
            if (root.has("customLightColor")) this.customLightColor = root.getInt("customLightColor");
            if (root.has("lightsSpeed")) this.lightsSpeed = (float) root.getDouble("lightsSpeed");
            if (root.has("vibrationIntensity")) this.vibrationIntensity = root.getInt("vibrationIntensity");
            if (root.has("turboEnabledGlobal")) this.turboEnabledGlobal = root.getBoolean("turboEnabledGlobal");
            if (root.has("turboFrequency")) this.turboFrequency = root.getInt("turboFrequency");

            if (root.has("buttons")) {
                JSONArray array = root.getJSONArray("buttons");
                buttons.clear();
                for (int i = 0; i < array.length(); i++) {
                    HitboxButton btn = HitboxButton.fromJson(array.getJSONObject(i));
                    buttons.add(btn);
                }
                postInvalidate();
            }
        } catch (JSONException e) {
            createDefaultButtons();
        }
    }

    public void createDefaultButtons() {
        buttons.clear();

        int cDirNorm = Color.parseColor("#1E293B");
        int cDirPress = Color.parseColor("#00B099");

        int cJumpNorm = Color.parseColor("#334155");
        int cJumpPress = Color.parseColor("#00FF88");

        int cAuxNorm = Color.parseColor("#151D2A");
        int cAuxPress = Color.parseColor("#FFB300");

        // Official Xbox Default Action Colors
        int colA_Norm = Color.parseColor("#005F00");
        int colA_Press = Color.parseColor("#00FF00");

        int colB_Norm = Color.parseColor("#5F0000");
        int colB_Press = Color.parseColor("#FF0000");

        int colX_Norm = Color.parseColor("#00005F");
        int colX_Press = Color.parseColor("#0000FF");

        int colY_Norm = Color.parseColor("#5F5F00");
        int colY_Press = Color.parseColor("#FFFF00");

        int cBumperNorm = Color.parseColor("#2D2436");
        int cBumperPress = Color.parseColor("#FF0055");

        int cTriggerNorm = Color.parseColor("#1E2E40");
        int cTriggerPress = Color.parseColor("#00B0FF");

        // Directional Buttons with Arrow Icons (◄, ▼, ►, ▲)
        buttons.add(new HitboxButton("LEFT", "◄", "LEFT", HitboxButton.BIT_LEFT, cDirNorm, cDirPress, cDirPress));
        buttons.add(new HitboxButton("DOWN", "▼", "DOWN", HitboxButton.BIT_DOWN, cDirNorm, cDirPress, cDirPress));
        buttons.add(new HitboxButton("RIGHT", "►", "RIGHT", HitboxButton.BIT_RIGHT, cDirNorm, cDirPress, cDirPress));
        buttons.add(new HitboxButton("UP", "▲", "JUMP", HitboxButton.BIT_UP, cJumpNorm, cJumpPress, cJumpPress));

        // Action Punch / Upper Row Buttons (X, Y, RB, LB)
        buttons.add(new HitboxButton("LP", "X", "X", HitboxButton.BIT_LP, colX_Norm, colX_Press, colX_Press));
        buttons.add(new HitboxButton("MP", "Y", "Y", HitboxButton.BIT_MP, colY_Norm, colY_Press, colY_Press));
        buttons.add(new HitboxButton("HP", "RB", "RB", HitboxButton.BIT_HP, cBumperNorm, cBumperPress, cBumperPress));
        buttons.add(new HitboxButton("AP", "LB", "LB", HitboxButton.BIT_AP, cBumperNorm, cBumperPress, cBumperPress));

        // Action Kick / Lower Row Buttons (A, B, RT, LT)
        buttons.add(new HitboxButton("LK", "A", "A", HitboxButton.BIT_LK, colA_Norm, colA_Press, colA_Press));
        buttons.add(new HitboxButton("MK", "B", "B", HitboxButton.BIT_MK, colB_Norm, colB_Press, colB_Press));
        buttons.add(new HitboxButton("HK", "RT", "RT", HitboxButton.BIT_HK, cTriggerNorm, cTriggerPress, cTriggerPress));
        buttons.add(new HitboxButton("AK", "LT", "LT", HitboxButton.BIT_AK, cTriggerNorm, cTriggerPress, cTriggerPress));

        // Menu Buttons (SEL, STA, HOME, TM)
        buttons.add(new HitboxButton("SEL", "SEL", "COIN", HitboxButton.BIT_SELECT, cAuxNorm, cAuxPress, cAuxPress));
        buttons.add(new HitboxButton("STA", "STA", "OPTIONS", HitboxButton.BIT_START, cAuxNorm, cAuxPress, cAuxPress));
        buttons.add(new HitboxButton("HOM", "HOME", "HOME", HitboxButton.BIT_HOME, cAuxNorm, cAuxPress, cAuxPress));
        buttons.add(new HitboxButton("TPAD", "TM", "TURBO", HitboxButton.BIT_TOUCHPAD, cAuxNorm, cAuxPress, cAuxPress));

        if (getWidth() > 0 && getHeight() > 0) {
            layoutDefaultPositions(getWidth(), getHeight());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (buttons.isEmpty() || buttons.get(0).cx == 0) {
            layoutDefaultPositions(w, h);
        }
    }

    private void layoutDefaultPositions(int w, int h) {
        float baseRadius = Math.min(w, h) * 0.082f;
        float dirRadius = baseRadius * 1.05f;
        float jumpRadius = baseRadius * 1.35f;

        float leftX = w * 0.16f;
        float downX = w * 0.26f;
        float rightX = w * 0.36f;
        float dirY = h * 0.44f;

        for (HitboxButton btn : buttons) {
            if (btn.id.equals("LEFT"))  btn.setCircleBounds(leftX, dirY, dirRadius);
            if (btn.id.equals("DOWN"))  btn.setCircleBounds(downX, dirY + baseRadius * 0.25f, dirRadius);
            if (btn.id.equals("RIGHT")) btn.setCircleBounds(rightX, dirY, dirRadius);
        }

        float jumpX = w * 0.38f;
        float jumpY = h * 0.78f;
        for (HitboxButton btn : buttons) {
            if (btn.id.equals("UP")) btn.setCircleBounds(jumpX, jumpY, jumpRadius);
        }

        float actStartX = w * 0.54f;
        float actSpacingX = baseRadius * 2.3f;
        float punchY = h * 0.38f;
        float kickY = h * 0.68f;

        for (HitboxButton btn : buttons) {
            float arcOffset = 0f;
            if (btn.id.equals("LP")) { arcOffset = -baseRadius * 0.3f; btn.setCircleBounds(actStartX, punchY + arcOffset, baseRadius); }
            if (btn.id.equals("MP")) { arcOffset = -baseRadius * 0.5f; btn.setCircleBounds(actStartX + actSpacingX, punchY + arcOffset, baseRadius); }
            if (btn.id.equals("HP")) { arcOffset = -baseRadius * 0.3f; btn.setCircleBounds(actStartX + actSpacingX * 2, punchY + arcOffset, baseRadius); }
            if (btn.id.equals("AP")) { arcOffset = 0f;                 btn.setCircleBounds(actStartX + actSpacingX * 3, punchY + arcOffset, baseRadius); }

            if (btn.id.equals("LK")) { arcOffset = -baseRadius * 0.3f; btn.setCircleBounds(actStartX, kickY + arcOffset, baseRadius); }
            if (btn.id.equals("MK")) { arcOffset = -baseRadius * 0.5f; btn.setCircleBounds(actStartX + actSpacingX, kickY + arcOffset, baseRadius); }
            if (btn.id.equals("HK")) { arcOffset = -baseRadius * 0.3f; btn.setCircleBounds(actStartX + actSpacingX * 2, kickY + arcOffset, baseRadius); }
            if (btn.id.equals("AK")) { arcOffset = 0f;                 btn.setCircleBounds(actStartX + actSpacingX * 3, kickY + arcOffset, baseRadius); }
        }

        float auxY = h * 0.12f;
        float auxW = baseRadius * 1.6f;
        float auxH = baseRadius * 0.9f;
        float auxStartX = w * 0.36f;
        float auxSpacing = auxW * 1.2f;

        int i = 0;
        for (HitboxButton btn : buttons) {
            if (btn.id.equals("SEL") || btn.id.equals("STA") || btn.id.equals("HOM") || btn.id.equals("TPAD")) {
                btn.setRectBounds(auxStartX + i * auxSpacing, auxY, auxW, auxH);
                i++;
            }
        }
    }

    private boolean isTmHeld() {
        for (HitboxButton btn : buttons) {
            if (btn.bitFlag == HitboxButton.BIT_TOUCHPAD && btn.isPressed()) {
                return true;
            }
        }
        return false;
    }

    private void checkStartTurboTicker() {
        if (!turboEnabledGlobal) return;
        boolean hasTurboHeld = false;
        for (HitboxButton btn : buttons) {
            if (btn.isPressed() && btn.isTurboActive) {
                hasTurboHeld = true;
                break;
            }
        }
        if (hasTurboHeld && !isTurboTickerRunning) {
            isTurboTickerRunning = true;
            int intervalMs = Math.max(25, 1000 / (turboFrequency * 2));
            turboHandler.post(turboRunnable);
        }
    }

    private float dpToPx(Context ctx, float dp) {
        return dp * ctx.getResources().getDisplayMetrics().density;
    }

    // Joystick Analog Motion Tracking
    private short leftStickLx = 0;
    private short leftStickLy = 0;
    private short rightStickRx = 0;
    private short rightStickRy = 0;

    private void updateJoystickOffset(HitboxButton btn, float px, float py) {
        float dx = px - btn.cx;
        float dy = py - btn.cy;
        float maxRadius = btn.radius * 0.85f;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float clampedDx = dx;
        float clampedDy = dy;
        if (dist > maxRadius && dist > 0) {
            clampedDx = (dx / dist) * maxRadius;
            clampedDy = (dy / dist) * maxRadius;
        }
        btn.knobX = btn.cx + clampedDx;
        btn.knobY = btn.cy + clampedDy;

        float normX = clampedDx / maxRadius;
        float normY = clampedDy / maxRadius;

        short sX = (short) Math.max(-32767, Math.min(32767, Math.round(normX * 32767.0f)));
        short sY = (short) Math.max(-32767, Math.min(32767, Math.round(normY * 32767.0f)));

        if (btn.bitFlag == HitboxButton.BIT_LEFT_STICK || btn.id.startsWith("LEFT_STICK") || "LJ".equals(btn.label)) {
            leftStickLx = sX;
            leftStickLy = sY;
        } else if (btn.bitFlag == HitboxButton.BIT_RIGHT_STICK || btn.id.startsWith("RIGHT_STICK") || "RJ".equals(btn.label)) {
            rightStickRx = sX;
            rightStickRy = sY;
        }
    }

    private void resetJoystickOffset(HitboxButton btn) {
        btn.knobX = btn.cx;
        btn.knobY = btn.cy;
        if (btn.bitFlag == HitboxButton.BIT_LEFT_STICK || btn.id.startsWith("LEFT_STICK") || "LJ".equals(btn.label)) {
            leftStickLx = 0;
            leftStickLy = 0;
        } else if (btn.bitFlag == HitboxButton.BIT_RIGHT_STICK || btn.id.startsWith("RIGHT_STICK") || "RJ".equals(btn.label)) {
            rightStickRx = 0;
            rightStickRy = 0;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();
        int actionPointerId = event.getPointerId(actionIndex);

        float x = event.getX(actionIndex);
        float y = event.getY(actionIndex);

        if (action == MotionEvent.ACTION_DOWN) {
            // Top Right Settings Gear Icon
            if (x > getWidth() - 140 && y < 140) {
                if (settingsClickListener != null) {
                    settingsClickListener.onSettingsClick();
                }
                return true;
            }
            // Top Left Connection Dot
            if (x < 120 && y < 120) {
                if (socketClient != null && !socketClient.isConnected()) {
                    socketClient.connect();
                }
            }
        }

        if (isEditMode) {
            if (action == MotionEvent.ACTION_DOWN) {
                HitboxButton touched = null;
                for (HitboxButton btn : buttons) {
                    if (btn.contains(x, y)) {
                        touched = btn;
                        break;
                    }
                }
                if (touched != null) {
                    setSelectedButton(touched);
                    draggedPointerId = actionPointerId;
                    dragOffsetX = touched.cx - x;
                    dragOffsetY = touched.cy - y;
                } else {
                    setSelectedButton(null);
                }
            } else if (action == MotionEvent.ACTION_MOVE && draggedPointerId != -1) {
                for (int p = 0; p < event.getPointerCount(); p++) {
                    if (event.getPointerId(p) == draggedPointerId && selectedButton != null) {
                        selectedButton.cx = event.getX(p) + dragOffsetX;
                        selectedButton.cy = event.getY(p) + dragOffsetY;
                        selectedButton.knobX = selectedButton.cx;
                        selectedButton.knobY = selectedButton.cy;
                        postInvalidate();
                        notifyLayoutModified();
                        break;
                    }
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                draggedPointerId = -1;
            }
            return true;
        }

        // Controller Touch Processing
        boolean buttonStateChanged = false;
        boolean tmHeld = isTmHeld();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            float px = event.getX(actionIndex);
            float py = event.getY(actionIndex);

            for (HitboxButton btn : buttons) {
                if (btn.contains(px, py)) {
                    if (btn.isDisc) {
                        boolean stateChanged = btn.setDiscPointer(actionPointerId, px, py);
                        if (stateChanged) {
                            buttonStateChanged = true;
                            triggerHaptic();
                        }
                    } else {
                        boolean newlyPressed = btn.addPointer(actionPointerId);
                        if (newlyPressed) {
                            buttonStateChanged = true;
                            triggerHaptic();

                            if (tmHeld && !btn.isJoystick && btn.bitFlag != HitboxButton.BIT_TOUCHPAD) {
                                if (btn.isTurboAllowed()) {
                                    btn.isTurboActive = !btn.isTurboActive;
                                    notifyLayoutModified();
                                }
                            }
                        }

                        if (btn.isJoystick) {
                            updateJoystickOffset(btn, px, py);
                        }
                    }
                    break; // Captured by this element
                }
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            for (int p = 0; p < event.getPointerCount(); p++) {
                int pId = event.getPointerId(p);
                float px = event.getX(p);
                float py = event.getY(p);

                // Check if this pointer is locked to a joystick
                boolean isJoystickPointer = false;
                for (HitboxButton btn : buttons) {
                    if (btn.isJoystick && btn.containsPointer(pId)) {
                        updateJoystickOffset(btn, px, py);
                        buttonStateChanged = true;
                        isJoystickPointer = true;
                        break;
                    }
                }

                if (isJoystickPointer) continue;

                // Check if this pointer belongs to a disc
                boolean isDiscPointer = false;
                for (HitboxButton btn : buttons) {
                    if (btn.isDisc && btn.containsDiscPointer(pId)) {
                        boolean changed = btn.setDiscPointer(pId, px, py);
                        if (changed) buttonStateChanged = true;
                        isDiscPointer = true;
                        break;
                    }
                }

                if (isDiscPointer) continue;

                // Non-joystick, non-disc pointer: restore gliding behavior for regular buttons
                for (HitboxButton btn : buttons) {
                    if (btn.isJoystick || btn.isDisc) continue; // never glide into joysticks or discs
                    boolean inside = btn.contains(px, py);
                    boolean hadPointer = btn.containsPointer(pId);
                    if (inside && !hadPointer) {
                        btn.addPointer(pId);
                        triggerHaptic();
                        buttonStateChanged = true;
                    } else if (!inside && hadPointer) {
                        btn.removePointer(pId);
                        buttonStateChanged = true;
                    }
                }
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            for (HitboxButton btn : buttons) {
                if (btn.isDisc) {
                    boolean cleared = btn.clearDiscPointer(actionPointerId);
                    if (cleared) buttonStateChanged = true;
                } else if (btn.containsPointer(actionPointerId)) {
                    btn.removePointer(actionPointerId);
                    if (btn.isJoystick && !btn.isPressed()) {
                        resetJoystickOffset(btn);
                    }
                    buttonStateChanged = true;
                }
            }
        }

        checkStartTurboTicker();

        if (buttonStateChanged || action == MotionEvent.ACTION_MOVE) {
            sendCurrentState();
            postInvalidate();
        }

        return true;
    }

    private void sendCurrentState() {
        int rawMask = 0;
        boolean tmActive = isTmHeld();

        for (HitboxButton btn : buttons) {
            if (btn.isDisc) {
                rawMask |= btn.getActiveMask();
            } else if (btn.isPressed()) {
                if (tmActive && btn.bitFlag != HitboxButton.BIT_TOUCHPAD) {
                    continue;
                }
                if (btn.isTurboActive && turboEnabledGlobal) {
                    if (turboPulseState) {
                        rawMask |= btn.bitFlag;
                    }
                } else {
                    rawMask |= btn.bitFlag;
                }
            }
        }

        int cleanedMask = socdCleaner.clean(rawMask);

        if (socketClient != null) {
            socketClient.sendInputState(cleanedMask, leftStickLx, leftStickLy, rightStickRx, rightStickRy);
        }
    }

    private void triggerHaptic() {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(vibrationIntensity, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(vibrationIntensity);
            }
        } catch (Exception ignored) {}
    }

    private int getSegmentMask(HitboxButton.DiscType discType, int zone) {
        if (discType == HitboxButton.DiscType.DPAD) {
            switch (zone) {
                case 0: return HitboxButton.BIT_RIGHT;
                case 1: return HitboxButton.BIT_DOWN | HitboxButton.BIT_RIGHT;
                case 2: return HitboxButton.BIT_DOWN;
                case 3: return HitboxButton.BIT_DOWN | HitboxButton.BIT_LEFT;
                case 4: return HitboxButton.BIT_LEFT;
                case 5: return HitboxButton.BIT_UP | HitboxButton.BIT_LEFT;
                case 6: return HitboxButton.BIT_UP;
                case 7: return HitboxButton.BIT_UP | HitboxButton.BIT_RIGHT;
            }
        } else {
            switch (zone) {
                case 0: return HitboxButton.BIT_MK;
                case 1: return HitboxButton.BIT_LK | HitboxButton.BIT_MK;
                case 2: return HitboxButton.BIT_LK;
                case 3: return HitboxButton.BIT_LP | HitboxButton.BIT_LK;
                case 4: return HitboxButton.BIT_LP;
                case 5: return HitboxButton.BIT_MP | HitboxButton.BIT_LP;
                case 6: return HitboxButton.BIT_MP;
                case 7: return HitboxButton.BIT_MP | HitboxButton.BIT_MK;
            }
        }
        return 0;
    }

    private int getCellMask(HitboxButton.DiscType discType, int row, int col) {
        if (row == 0 && col == 0) return getSegmentMask(discType, 5); // UL
        if (row == 0 && col == 1) return getSegmentMask(discType, 6); // U
        if (row == 0 && col == 2) return getSegmentMask(discType, 7); // UR
        if (row == 1 && col == 0) return getSegmentMask(discType, 4); // L
        if (row == 1 && col == 2) return getSegmentMask(discType, 0); // R
        if (row == 2 && col == 0) return getSegmentMask(discType, 3); // DL
        if (row == 2 && col == 1) return getSegmentMask(discType, 2); // D
        if (row == 2 && col == 2) return getSegmentMask(discType, 1); // DR
        return 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw Background (#1C1D26)
        canvas.drawColor(backgroundColor);

        // 1b. Draw Non-Interactable Transparent Icon Overlay in Bottom-Left Corner
        if (transparentIconBitmap != null) {
            float iconWidth = dpToPx(getContext(), 110);
            float scale = iconWidth / transparentIconBitmap.getWidth();
            float iconHeight = transparentIconBitmap.getHeight() * scale;
            float left = dpToPx(getContext(), 16);
            float top = getHeight() - iconHeight - dpToPx(getContext(), 16);
            RectF dst = new RectF(left, top, left + iconWidth, top + iconHeight);
            canvas.drawBitmap(transparentIconBitmap, null, dst, paintImage);
        }

        // 2. Screen Edge RGB / Custom Lighting Engine
        if (lightsEnabled) {
            drawEdgeLighting(canvas);
        }

        // 3. Draw Fixed Top Bar
        drawFixedTopBar(canvas);

        // 4. Draw Hitbox Buttons
        for (HitboxButton btn : buttons) {
            boolean pressed = btn.isPressed();
            float effW = btn.getEffectiveWidth();
            float effH = btn.getEffectiveHeight();

            // Render Disc Elements (DPad / ABXY Pad)
            if (btn.isDisc) {
                int activeMask = btn.getActiveMask();

                if (btn.discIsSquare) {
                    // 3x3 Grid Mode
                    float left = btn.cx - effW / 2.0f;
                    float top = btn.cy - effH / 2.0f;
                    float cellW = effW / 3.0f;
                    float cellH = effH / 3.0f;

                    for (int r = 0; r < 3; r++) {
                        for (int c = 0; c < 3; c++) {
                            RectF cellRect = new RectF(
                                    left + c * cellW,
                                    top + r * cellH,
                                    left + (c + 1) * cellW,
                                    top + (r + 1) * cellH
                            );

                            if (r == 1 && c == 1) {
                                // Dead zone cell
                                paintFill.setColor(btn.normalColor);
                                canvas.drawRoundRect(cellRect, 6, 6, paintFill);
                                paintStroke.setColor(pressed ? Color.WHITE : Color.parseColor("#475569"));
                                canvas.drawRoundRect(cellRect, 6, 6, paintStroke);
                            } else {
                                int cellMask = getCellMask(btn.discType, r, c);
                                boolean lit = (activeMask != 0) && ((activeMask & cellMask) == cellMask);
                                paintFill.setColor(lit ? btn.pressedColor : btn.normalColor);
                                canvas.drawRoundRect(cellRect, 6, 6, paintFill);
                                paintStroke.setColor(lit ? Color.WHITE : Color.parseColor("#475569"));
                                canvas.drawRoundRect(cellRect, 6, 6, paintStroke);

                                // Labels on cardinal cells only
                                if (showButtonText) {
                                    paintTextMain.setTextSize(cellH * 0.42f);
                                    if (r == 0 && c == 1) { // Up
                                        String labelUp = (btn.discType == HitboxButton.DiscType.DPAD) ? "▲" : "Y";
                                        canvas.drawText(labelUp, cellRect.centerX(), cellRect.centerY() + cellH * 0.14f, paintTextMain);
                                    } else if (r == 2 && c == 1) { // Down
                                        String labelDown = (btn.discType == HitboxButton.DiscType.DPAD) ? "▼" : "A";
                                        canvas.drawText(labelDown, cellRect.centerX(), cellRect.centerY() + cellH * 0.14f, paintTextMain);
                                    } else if (r == 1 && c == 0) { // Left
                                        String labelLeft = (btn.discType == HitboxButton.DiscType.DPAD) ? "◄" : "X";
                                        canvas.drawText(labelLeft, cellRect.centerX(), cellRect.centerY() + cellH * 0.14f, paintTextMain);
                                    } else if (r == 1 && c == 2) { // Right
                                        String labelRight = (btn.discType == HitboxButton.DiscType.DPAD) ? "►" : "B";
                                        canvas.drawText(labelRight, cellRect.centerX(), cellRect.centerY() + cellH * 0.14f, paintTextMain);
                                    }
                                }
                            }
                        }
                    }

                    // Outer border
                    RectF gridRect = new RectF(left, top, left + effW, top + effH);
                    paintStroke.setColor(pressed ? Color.WHITE : Color.parseColor("#475569"));
                    canvas.drawRoundRect(gridRect, 10, 10, paintStroke);

                    if (isEditMode && btn == selectedButton) {
                        RectF selRect = new RectF(left - 10, top - 10, left + effW + 10, top + effH + 10);
                        canvas.drawRoundRect(selRect, 16, 16, paintSelection);
                    }
                } else {
                    // Rose Arc Circle Mode
                    RectF oval = new RectF(btn.cx - btn.radius, btn.cy - btn.radius, btn.cx + btn.radius, btn.cy + btn.radius);

                    for (int z = 0; z < 8; z++) {
                        float startAngle = -22.5f + z * 45.0f;
                        int segMask = getSegmentMask(btn.discType, z);
                        boolean lit = (activeMask != 0) && ((activeMask & segMask) == segMask);
                        paintFill.setColor(lit ? btn.pressedColor : btn.normalColor);
                        canvas.drawArc(oval, startAngle, 45.0f, true, paintFill);

                        double rad = Math.toRadians(startAngle);
                        paintStroke.setColor(Color.parseColor("#475569"));
                        canvas.drawLine(btn.cx, btn.cy, (float) (btn.cx + btn.radius * Math.cos(rad)), (float) (btn.cy + btn.radius * Math.sin(rad)), paintStroke);
                    }

                    // Outer Circle Border
                    paintStroke.setColor(pressed ? Color.WHITE : Color.parseColor("#475569"));
                    canvas.drawCircle(btn.cx, btn.cy, btn.radius, paintStroke);

                    // Center Circle (Dead Zone)
                    float centerRadius = btn.radius * 0.20f;
                    paintFill.setColor(btn.normalColor);
                    canvas.drawCircle(btn.cx, btn.cy, centerRadius, paintFill);
                    canvas.drawCircle(btn.cx, btn.cy, centerRadius, paintStroke);

                    // Labels on cardinal zones ONLY (Right, Down, Left, Up)
                    if (showButtonText) {
                        paintTextMain.setTextSize(btn.radius * 0.35f);
                        String labelUp    = (btn.discType == HitboxButton.DiscType.DPAD) ? "▲" : "Y";
                        String labelDown  = (btn.discType == HitboxButton.DiscType.DPAD) ? "▼" : "A";
                        String labelLeft  = (btn.discType == HitboxButton.DiscType.DPAD) ? "◄" : "X";
                        String labelRight = (btn.discType == HitboxButton.DiscType.DPAD) ? "►" : "B";

                        float labelDist = btn.radius * 0.62f;
                        // Right (z=0)
                        canvas.drawText(labelRight, btn.cx + labelDist, btn.cy + btn.radius * 0.12f, paintTextMain);
                        // Down (z=2)
                        canvas.drawText(labelDown, btn.cx, btn.cy + labelDist + btn.radius * 0.12f, paintTextMain);
                        // Left (z=4)
                        canvas.drawText(labelLeft, btn.cx - labelDist, btn.cy + btn.radius * 0.12f, paintTextMain);
                        // Up (z=6)
                        canvas.drawText(labelUp, btn.cx, btn.cy - labelDist + btn.radius * 0.12f, paintTextMain);
                    }

                    if (isEditMode && btn == selectedButton) {
                        canvas.drawCircle(btn.cx, btn.cy, btn.radius + 10, paintSelection);
                    }
                }
                continue;
            }

            boolean isSquare = (btn.shape == HitboxButton.Shape.SQUARE || btn.isPillShape);

            // Special Canvas Rendering for Joysticks
            if (btn.isJoystick) {
                paintFill.setColor(btn.normalColor);
                paintStroke.setColor(pressed ? Color.WHITE : Color.parseColor("#475569"));
                canvas.drawCircle(btn.cx, btn.cy, btn.radius, paintFill);
                canvas.drawCircle(btn.cx, btn.cy, btn.radius, paintStroke);

                Paint paintCross = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintCross.setStyle(Paint.Style.STROKE);
                paintCross.setColor(Color.parseColor("#334155"));
                paintCross.setStrokeWidth(2.0f);
                canvas.drawCircle(btn.cx, btn.cy, btn.radius * 0.4f, paintCross);
                canvas.drawLine(btn.cx - btn.radius * 0.7f, btn.cy, btn.cx + btn.radius * 0.7f, btn.cy, paintCross);
                canvas.drawLine(btn.cx, btn.cy - btn.radius * 0.7f, btn.cx, btn.cy + btn.radius * 0.7f, paintCross);

                if (isEditMode && btn == selectedButton) {
                    canvas.drawCircle(btn.cx, btn.cy, btn.radius + 10, paintSelection);
                }

                float kx = pressed ? btn.knobX : btn.cx;
                float ky = pressed ? btn.knobY : btn.cy;
                if (kx == 0 && ky == 0) { kx = btn.cx; ky = btn.cy; }
                float knobRadius = btn.radius * 0.48f;

                Paint paintKnob = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintKnob.setStyle(Paint.Style.FILL);
                paintKnob.setColor(pressed ? btn.pressedColor : Color.parseColor("#1E293B"));
                canvas.drawCircle(kx, ky, knobRadius, paintKnob);

                Paint paintKnobStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintKnobStroke.setStyle(Paint.Style.STROKE);
                paintKnobStroke.setStrokeWidth(4.0f);
                paintKnobStroke.setColor(pressed ? Color.WHITE : Color.parseColor("#00B099"));
                canvas.drawCircle(kx, ky, knobRadius, paintKnobStroke);

                if (showButtonText) {
                    paintTextMain.setTextSize(knobRadius * 0.65f);
                    canvas.drawText(btn.label, kx, ky + (knobRadius * 0.22f), paintTextMain);
                }
                continue;
            }

            // Touch Glow Effect
            if (pressed) {
                paintGlow.setStyle(Paint.Style.FILL);
                paintGlow.setColor(btn.activeGlowColor);
                paintGlow.setAlpha(75);
                if (isSquare) {
                    RectF glowRect = new RectF(
                            btn.cx - effW / 2.0f - 8,
                            btn.cy - effH / 2.0f - 8,
                            btn.cx + effW / 2.0f + 8,
                            btn.cy + effH / 2.0f + 8
                    );
                    canvas.drawRoundRect(glowRect, 16, 16, paintGlow);
                } else {
                    canvas.drawCircle(btn.cx, btn.cy, btn.radius * 1.18f, paintGlow);
                }
            }

            // Body
            paintFill.setColor(pressed ? btn.pressedColor : btn.normalColor);
            paintStroke.setColor(pressed ? Color.WHITE : Color.parseColor("#475569"));

            if (isSquare) {
                RectF rect = new RectF(
                        btn.cx - effW / 2.0f,
                        btn.cy - effH / 2.0f,
                        btn.cx + effW / 2.0f,
                        btn.cy + effH / 2.0f
                );
                canvas.drawRoundRect(rect, 14, 14, paintFill);
                canvas.drawRoundRect(rect, 14, 14, paintStroke);
            } else {
                canvas.drawCircle(btn.cx, btn.cy, btn.radius, paintFill);
                canvas.drawCircle(btn.cx, btn.cy, btn.radius, paintStroke);
            }

            // Selection Outline in Edit Mode
            if (isEditMode && btn == selectedButton) {
                if (isSquare) {
                    RectF selRect = new RectF(
                            btn.cx - effW / 2.0f - 10,
                            btn.cy - effH / 2.0f - 10,
                            btn.cx + effW / 2.0f + 10,
                            btn.cy + effH / 2.0f + 10
                    );
                    canvas.drawRoundRect(selRect, 16, 16, paintSelection);
                } else {
                    canvas.drawCircle(btn.cx, btn.cy, btn.radius + 10, paintSelection);
                }
            }

            // Turbo Active Badge Dot
            if (btn.isTurboActive && turboEnabledGlobal) {
                Paint paintTurboBadge = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintTurboBadge.setColor(Color.parseColor("#FF9800"));
                float badgeRadius = Math.min(effW, effH) * 0.15f;
                float badgeCx = btn.cx + effW * 0.30f;
                float badgeCy = btn.cy - effH * 0.30f;
                canvas.drawCircle(badgeCx, badgeCy, badgeRadius, paintTurboBadge);

                Paint paintTText = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintTText.setColor(Color.WHITE);
                paintTText.setTextSize(badgeRadius * 1.4f);
                paintTText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                paintTText.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("T", badgeCx, badgeCy + badgeRadius * 0.45f, paintTText);
            }

            // Text Label
            if (showButtonText) {
                paintTextMain.setTextSize(Math.min(effW, effH) * 0.38f);
                canvas.drawText(btn.label, btn.cx, btn.cy + (Math.min(effW, effH) * 0.12f), paintTextMain);
            }
        }

        // Trigger continuous frame animation if RGB cycling lights are active
        if (lightsEnabled && (!responsiveLights || hasAnyActiveTouch())) {
            hueAngle = (hueAngle + 1.8f * lightsSpeed) % 360.0f;
            postInvalidateOnAnimation();
        }
    }

    private void drawEdgeLighting(Canvas canvas) {
        if (responsiveLights && !hasAnyActiveTouch()) return;

        int w = getWidth();
        int h = getHeight();

        int lightColor;
        if (customLightColorMode) {
            lightColor = customLightColor;
        } else {
            float[] hsv = new float[]{hueAngle, 1.0f, 1.0f};
            lightColor = Color.HSVToColor(hsv);
        }

        int[] colors = new int[]{lightColor, Color.TRANSPARENT};
        float[] positions = new float[]{0.0f, 1.0f};

        LinearGradient topGrad = new LinearGradient(0, 0, 0, 36, colors, positions, Shader.TileMode.CLAMP);
        paintEdgeLight.setShader(topGrad);
        canvas.drawLine(0, 14, w, 14, paintEdgeLight);

        LinearGradient botGrad = new LinearGradient(0, h, 0, h - 36, colors, positions, Shader.TileMode.CLAMP);
        paintEdgeLight.setShader(botGrad);
        canvas.drawLine(0, h - 14, w, h - 14, paintEdgeLight);

        LinearGradient leftGrad = new LinearGradient(0, 0, 36, 0, colors, positions, Shader.TileMode.CLAMP);
        paintEdgeLight.setShader(leftGrad);
        canvas.drawLine(14, 0, 14, h, paintEdgeLight);

        LinearGradient rightGrad = new LinearGradient(w, 0, w - 36, 0, colors, positions, Shader.TileMode.CLAMP);
        paintEdgeLight.setShader(rightGrad);
        canvas.drawLine(w - 14, 0, w - 14, h, paintEdgeLight);
    }

    private void drawFixedTopBar(Canvas canvas) {
        // 1. Top-Left Connection Dot
        float dotX = 52;
        float dotY = 52;
        float dotRadius = 14;

        Paint dotGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotGlow.setColor(isConnected ? Color.parseColor("#00E676") : Color.parseColor("#FF1744"));
        dotGlow.setAlpha(70);
        canvas.drawCircle(dotX, dotY, dotRadius + 8, dotGlow);

        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(isConnected ? Color.parseColor("#00E676") : Color.parseColor("#FF1744"));
        canvas.drawCircle(dotX, dotY, dotRadius, dotPaint);

        // 2. Top-Right Settings Gear Icon
        float gearX = getWidth() - 60;
        float gearY = 52;

        Paint gearGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        gearGlow.setColor(Color.parseColor("#0F3153"));
        canvas.drawCircle(gearX, gearY, 32, gearGlow);

        Paint gearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gearPaint.setColor(isEditMode ? Color.parseColor("#00E5FF") : Color.WHITE);
        gearPaint.setStyle(Paint.Style.STROKE);
        gearPaint.setStrokeWidth(5.0f);
        canvas.drawCircle(gearX, gearY, 20, gearPaint);
        canvas.drawCircle(gearX, gearY, 8, gearPaint);
    }
}

