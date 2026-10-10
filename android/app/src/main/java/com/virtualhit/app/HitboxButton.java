package com.virtualhit.app;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HitboxButton {
    public enum Shape {
        CIRCLE,
        SQUARE
    }

    public enum DiscType {
        DPAD,
        ABXY
    }

    // Bitmask constants matching server protocol
    public static final int BIT_UP       = 1 << 0;
    public static final int BIT_DOWN     = 1 << 1;
    public static final int BIT_LEFT     = 1 << 2;
    public static final int BIT_RIGHT    = 1 << 3;

    public static final int BIT_LP       = 1 << 4;  // Light Punch (West / Square / X)
    public static final int BIT_MP       = 1 << 5;  // Medium Punch (North / Triangle / Y)
    public static final int BIT_HP       = 1 << 6;  // Heavy Punch (R1 / RB)
    public static final int BIT_AP       = 1 << 7;  // Extra Punch (L1 / LB)

    public static final int BIT_LK       = 1 << 8;  // Light Kick (South / Cross / A)
    public static final int BIT_MK       = 1 << 9;  // Medium Kick (East / Circle / B)
    public static final int BIT_HK       = 1 << 10; // Heavy Kick (R2 / RT)
    public static final int BIT_AK       = 1 << 11; // Extra Kick (L2 / LT)

    public static final int BIT_SELECT   = 1 << 12; // Select / Coin
    public static final int BIT_START    = 1 << 13; // Start / Options
    public static final int BIT_HOME     = 1 << 14; // Home / Guide
    public static final int BIT_TOUCHPAD = 1 << 15; // Turbo Mode / Aux
    public static final int BIT_L3       = 1 << 16; // Left Stick Press
    public static final int BIT_R3       = 1 << 17; // Right Stick Press
    public static final int BIT_LEFT_STICK  = 1 << 18; // Left Stick Analog Element
    public static final int BIT_RIGHT_STICK = 1 << 19; // Right Stick Analog Element

    public String id;
    public String label;
    public String subLabel;
    public int bitFlag;

    public float cx;
    public float cy;
    public float radius;
    public float knobX;
    public float knobY;

    // Customization Properties
    public Shape shape = Shape.CIRCLE;
    public float customWidth = 0;  // >0 if custom square width set
    public float customHeight = 0; // >0 if custom square height set
    public boolean isPillShape = false;
    public boolean isDuplicate = false;
    public boolean isTurboActive = false;
    public boolean isJoystick = false;

    // Disc Pad Properties
    public boolean isDisc = false;
    public DiscType discType = DiscType.DPAD;
    public boolean discIsSquare = false;

    private final Map<Integer, Integer> pointerZoneMasks = new HashMap<>();
    private int discLockedPointerId = -1;
    private int discActiveMask = 0;

    public int normalColor;
    public int pressedColor;
    public int activeGlowColor;

    private final Set<Integer> activePointerIds = new HashSet<>();

    public HitboxButton(String id, String label, String subLabel, int bitFlag,
                        int normalColor, int pressedColor, int activeGlowColor) {
        this.id = id;
        this.label = label;
        this.subLabel = subLabel;
        this.bitFlag = bitFlag;
        this.normalColor = normalColor;
        this.pressedColor = pressedColor;
        this.activeGlowColor = activeGlowColor;
        this.isJoystick = (bitFlag == BIT_LEFT_STICK || bitFlag == BIT_RIGHT_STICK || "LEFT_STICK".equals(id) || "RIGHT_STICK".equals(id));
    }

    public boolean isTurboAllowed() {
        if (isJoystick || isDisc) return false;
        return (bitFlag == BIT_LK || bitFlag == BIT_MK || bitFlag == BIT_LP || bitFlag == BIT_MP
             || bitFlag == BIT_AP || bitFlag == BIT_AK || bitFlag == BIT_HP || bitFlag == BIT_HK);
    }

    public void setCircleBounds(float cx, float cy, float radius) {
        this.cx = cx;
        this.cy = cy;
        this.radius = radius;
        this.isPillShape = false;
        if (customWidth <= 0) customWidth = radius * 2.0f;
        if (customHeight <= 0) customHeight = radius * 2.0f;
    }

    public void setRectBounds(float cx, float cy, float width, float height) {
        this.cx = cx;
        this.cy = cy;
        this.customWidth = width;
        this.customHeight = height;
        this.radius = Math.min(width, height) / 2.0f;
        this.isPillShape = true;
    }

    public float getEffectiveWidth() {
        if (isJoystick) return radius * 2.0f;
        if (isDisc) return radius * 2.0f;
        if (shape == Shape.SQUARE || isPillShape) {
            return customWidth > 0 ? customWidth : radius * 2.0f;
        }
        return radius * 2.0f;
    }

    public float getEffectiveHeight() {
        if (isJoystick) return radius * 2.0f;
        if (isDisc) return radius * 2.0f;
        if (shape == Shape.SQUARE || isPillShape) {
            return customHeight > 0 ? customHeight : radius * 2.0f;
        }
        return radius * 2.0f;
    }

    public boolean contains(float x, float y) {
        if (!isJoystick && (isDisc ? discIsSquare : (shape == Shape.SQUARE || isPillShape))) {
            float w = getEffectiveWidth();
            float h = getEffectiveHeight();
            float left = cx - w / 2.0f;
            float right = cx + w / 2.0f;
            float top = cy - h / 2.0f;
            float bottom = cy + h / 2.0f;
            return x >= left && x <= right && y >= top && y <= bottom;
        } else {
            float dx = x - cx;
            float dy = y - cy;
            return (dx * dx + dy * dy) <= (radius * radius * 1.35f);
        }
    }

    public boolean isPressed() {
        if (isDisc) return discActiveMask != 0 || discLockedPointerId != -1 || !pointerZoneMasks.isEmpty();
        return !activePointerIds.isEmpty();
    }

    public boolean containsPointer(int pointerId) {
        if (isDisc) return containsDiscPointer(pointerId);
        return activePointerIds.contains(pointerId);
    }

    public boolean addPointer(int pointerId) {
        boolean wasPressed = isPressed();
        activePointerIds.add(pointerId);
        return !wasPressed && isPressed();
    }

    public boolean removePointer(int pointerId) {
        boolean wasPressed = isPressed();
        activePointerIds.remove(pointerId);
        return wasPressed && !isPressed();
    }

    public void clearPointers() {
        activePointerIds.clear();
        clearAllDiscPointers();
    }

    public int getDiscZoneMask(float px, float py) {
        if (!isDisc) return 0;

        if (discIsSquare) {
            float w = getEffectiveWidth();
            float h = getEffectiveHeight();
            float left = cx - w / 2.0f;
            float top = cy - h / 2.0f;

            float cellW = w / 3.0f;
            float cellH = h / 3.0f;

            int col = (int) Math.floor((px - left) / cellW);
            int row = (int) Math.floor((py - top) / cellH);

            col = Math.max(0, Math.min(2, col));
            row = Math.max(0, Math.min(2, row));

            if (row == 1 && col == 1) return 0; // Dead zone

            if (discType == DiscType.DPAD) {
                if (row == 0 && col == 0) return BIT_UP | BIT_LEFT;
                if (row == 0 && col == 1) return BIT_UP;
                if (row == 0 && col == 2) return BIT_UP | BIT_RIGHT;
                if (row == 1 && col == 0) return BIT_LEFT;
                if (row == 1 && col == 2) return BIT_RIGHT;
                if (row == 2 && col == 0) return BIT_DOWN | BIT_LEFT;
                if (row == 2 && col == 1) return BIT_DOWN;
                if (row == 2 && col == 2) return BIT_DOWN | BIT_RIGHT;
            } else { // ABXY
                if (row == 0 && col == 0) return BIT_MP | BIT_LP; // Y+X
                if (row == 0 && col == 1) return BIT_MP;          // Y
                if (row == 0 && col == 2) return BIT_MP | BIT_MK; // Y+B
                if (row == 1 && col == 0) return BIT_LP;          // X
                if (row == 1 && col == 2) return BIT_MK;          // B
                if (row == 2 && col == 0) return BIT_LP | BIT_LK; // X+A
                if (row == 2 && col == 1) return BIT_LK;          // A
                if (row == 2 && col == 2) return BIT_LK | BIT_MK; // A+B
            }
            return 0;
        } else {
            float dx = px - cx;
            float dy = py - cy;
            float distSq = dx * dx + dy * dy;

            // 20% radius dead zone
            float deadZoneRadius = radius * 0.20f;
            if (distSq < deadZoneRadius * deadZoneRadius) {
                return 0;
            }

            double deg = Math.toDegrees(Math.atan2(dy, dx));
            if (deg < 0) deg += 360.0;

            int zone = (int) Math.floor((deg + 22.5) / 45.0) % 8;

            if (discType == DiscType.DPAD) {
                switch (zone) {
                    case 0: return BIT_RIGHT;
                    case 1: return BIT_DOWN | BIT_RIGHT;
                    case 2: return BIT_DOWN;
                    case 3: return BIT_DOWN | BIT_LEFT;
                    case 4: return BIT_LEFT;
                    case 5: return BIT_UP | BIT_LEFT;
                    case 6: return BIT_UP;
                    case 7: return BIT_UP | BIT_RIGHT;
                }
            } else { // ABXY
                switch (zone) {
                    case 0: return BIT_MK;           // Right: B
                    case 1: return BIT_LK | BIT_MK;  // Down-Right: A+B
                    case 2: return BIT_LK;           // Down: A
                    case 3: return BIT_LP | BIT_LK;  // Down-Left: X+A
                    case 4: return BIT_LP;           // Left: X
                    case 5: return BIT_MP | BIT_LP;  // Up-Left: Y+X
                    case 6: return BIT_MP;           // Up: Y
                    case 7: return BIT_MP | BIT_MK;  // Up-Right: Y+B
                }
            }
            return 0;
        }
    }

    public boolean setDiscPointer(int pId, float px, float py) {
        if (!isDisc) return false;

        int mask = getDiscZoneMask(px, py);

        if (discType == DiscType.DPAD) {
            if (discLockedPointerId == -1 || discLockedPointerId == pId) {
                discLockedPointerId = pId;
                int oldMask = discActiveMask;
                discActiveMask = mask;
                return oldMask != discActiveMask;
            }
            return false;
        } else { // ABXY
            pointerZoneMasks.put(pId, mask);
            recalculateDiscActiveMask();
            return true;
        }
    }

    public boolean clearDiscPointer(int pId) {
        if (!isDisc) return false;

        if (discType == DiscType.DPAD) {
            if (discLockedPointerId == pId) {
                discLockedPointerId = -1;
                int oldMask = discActiveMask;
                discActiveMask = 0;
                return oldMask != 0;
            }
            return false;
        } else { // ABXY
            if (pointerZoneMasks.containsKey(pId)) {
                pointerZoneMasks.remove(pId);
                recalculateDiscActiveMask();
                return true;
            }
            return false;
        }
    }

    public void clearAllDiscPointers() {
        discLockedPointerId = -1;
        pointerZoneMasks.clear();
        discActiveMask = 0;
    }

    private void recalculateDiscActiveMask() {
        int combined = 0;
        for (int mask : pointerZoneMasks.values()) {
            combined |= mask;
        }
        discActiveMask = combined;
    }

    public int getActiveMask() {
        if (isDisc) {
            return discActiveMask;
        }
        if (isPressed() && !isJoystick) {
            return bitFlag;
        }
        return 0;
    }

    public int getDiscLockedPointerId() {
        return discLockedPointerId;
    }

    public boolean containsDiscPointer(int pId) {
        if (!isDisc) return false;
        if (discType == DiscType.DPAD) {
            return discLockedPointerId == pId;
        } else {
            return pointerZoneMasks.containsKey(pId);
        }
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("label", label);
        obj.put("subLabel", subLabel != null ? subLabel : "");
        obj.put("bitFlag", bitFlag);
        obj.put("cx", cx);
        obj.put("cy", cy);
        obj.put("radius", radius);
        obj.put("shape", isJoystick ? Shape.CIRCLE.name() : shape.name());
        obj.put("customWidth", customWidth);
        obj.put("customHeight", customHeight);
        obj.put("isPillShape", isPillShape);
        obj.put("isDuplicate", isDuplicate);
        obj.put("isTurboActive", isTurboActive);
        obj.put("isJoystick", isJoystick);
        obj.put("isDisc", isDisc);
        if (isDisc) {
            obj.put("discType", discType.name());
            obj.put("discIsSquare", discIsSquare);
        }
        obj.put("normalColor", normalColor);
        obj.put("pressedColor", pressedColor);
        obj.put("activeGlowColor", activeGlowColor);
        return obj;
    }

    public static HitboxButton fromJson(JSONObject obj) throws JSONException {
        String id = obj.getString("id");
        String label = obj.getString("label");
        String subLabel = obj.optString("subLabel", "");
        int bitFlag = obj.getInt("bitFlag");
        int normalColor = obj.getInt("normalColor");
        int pressedColor = obj.getInt("pressedColor");
        int activeGlowColor = obj.optInt("activeGlowColor", pressedColor);

        HitboxButton btn = new HitboxButton(id, label, subLabel, bitFlag, normalColor, pressedColor, activeGlowColor);
        btn.cx = (float) obj.getDouble("cx");
        btn.cy = (float) obj.getDouble("cy");
        btn.radius = (float) obj.getDouble("radius");
        btn.isJoystick = obj.optBoolean("isJoystick", (bitFlag == BIT_LEFT_STICK || bitFlag == BIT_RIGHT_STICK || "LEFT_STICK".equals(id) || "RIGHT_STICK".equals(id)));
        btn.isDisc = obj.optBoolean("isDisc", false);
        if (btn.isDisc) {
            btn.discType = DiscType.valueOf(obj.optString("discType", "DPAD"));
            btn.discIsSquare = obj.optBoolean("discIsSquare", false);
        }
        btn.shape = (btn.isJoystick || btn.isDisc) ? Shape.CIRCLE : Shape.valueOf(obj.optString("shape", "CIRCLE"));
        btn.customWidth = (float) obj.optDouble("customWidth", btn.radius * 2.0f);
        btn.customHeight = (float) obj.optDouble("customHeight", btn.radius * 2.0f);
        btn.isPillShape = obj.optBoolean("isPillShape", false);
        btn.isDuplicate = obj.optBoolean("isDuplicate", false);
        btn.isTurboActive = obj.optBoolean("isTurboActive", false);
        return btn;
    }
}

