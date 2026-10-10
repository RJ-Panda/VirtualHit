package com.virtualhit.app;

public class SOCDCleaner {
    public static final int MODE_NEUTRAL = 0;     // L+R=Neutral, D+U=Neutral (SF6/Capcom)
    public static final int MODE_UP_PRIORITY = 1; // L+R=Neutral, D+U=Up (Classic Hitbox)
    public static final int MODE_LAST_WIN = 2;    // Last input priority

    private int mode = MODE_NEUTRAL;
    private int lastLR = 0; // 1=Left, 2=Right
    private int lastUD = 0; // 1=Up, 2=Down

    public SOCDCleaner(int mode) {
        this.mode = mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }

    public int clean(int mask) {
        boolean up    = (mask & HitboxButton.BIT_UP) != 0;
        boolean down  = (mask & HitboxButton.BIT_DOWN) != 0;
        boolean left  = (mask & HitboxButton.BIT_LEFT) != 0;
        boolean right = (mask & HitboxButton.BIT_RIGHT) != 0;

        // Track last pressed directions
        if (left && !right) lastLR = 1;
        else if (right && !left) lastLR = 2;
        if (up && !down) lastUD = 1;
        else if (down && !up) lastUD = 2;

        if (mode == MODE_NEUTRAL) {
            if (left && right) {
                left = false;
                right = false;
            }
            if (up && down) {
                up = false;
                down = false;
            }
        } else if (mode == MODE_UP_PRIORITY) {
            if (left && right) {
                left = false;
                right = false;
            }
            if (up && down) {
                up = true;
                down = false;
            }
        } else if (mode == MODE_LAST_WIN) {
            if (left && right) {
                if (lastLR == 1) right = false;
                else if (lastLR == 2) left = false;
            }
            if (up && down) {
                if (lastUD == 1) down = false;
                else if (lastUD == 2) up = false;
            }
        }

        // Reconstruct mask
        int cleanedMask = mask & ~(HitboxButton.BIT_UP | HitboxButton.BIT_DOWN | HitboxButton.BIT_LEFT | HitboxButton.BIT_RIGHT);
        if (up) cleanedMask |= HitboxButton.BIT_UP;
        if (down) cleanedMask |= HitboxButton.BIT_DOWN;
        if (left) cleanedMask |= HitboxButton.BIT_LEFT;
        if (right) cleanedMask |= HitboxButton.BIT_RIGHT;

        return cleanedMask;
    }
}
