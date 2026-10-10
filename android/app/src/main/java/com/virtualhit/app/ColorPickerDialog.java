package com.virtualhit.app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;

public class ColorPickerDialog extends Dialog {

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private final String titleText;
    private final int initialColor;
    private final OnColorSelectedListener listener;

    private final float[] hsv = new float[3];
    private View colorPreview;

    public ColorPickerDialog(@NonNull Context context, String titleText, int initialColor, OnColorSelectedListener listener) {
        super(context);
        this.titleText = titleText;
        this.initialColor = initialColor;
        this.listener = listener;
        Color.colorToHSV(initialColor, hsv);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Context ctx = getContext();

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);

        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.parseColor("#373B4B"));
        rootBg.setCornerRadius(24);
        root.setBackground(rootBg);

        // Title
        TextView title = new TextView(ctx);
        title.setText("• " + titleText);
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#00B099"));
        title.setPadding(0, 0, 0, 20);
        root.addView(title);

        // Color Preview Box
        colorPreview = new View(ctx);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 80);
        previewParams.setMargins(0, 0, 0, 24);
        colorPreview.setLayoutParams(previewParams);

        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setColor(Color.HSVToColor(hsv));
        previewBg.setCornerRadius(16);
        colorPreview.setBackground(previewBg);
        root.addView(colorPreview);

        // Hue Slider (0 - 360)
        root.addView(createSliderRow(ctx, "Hue", (int) hsv[0], 360, value -> {
            hsv[0] = value;
            updatePreview();
        }));

        // Saturation Slider (0 - 100)
        root.addView(createSliderRow(ctx, "Saturation", (int) (hsv[1] * 100), 100, value -> {
            hsv[1] = value / 100.0f;
            updatePreview();
        }));

        // Brightness Slider (0 - 100)
        root.addView(createSliderRow(ctx, "Brightness", (int) (hsv[2] * 100), 100, value -> {
            hsv[2] = value / 100.0f;
            updatePreview();
        }));

        // Preset Color Slots Palette Row
        TextView lblPresets = new TextView(ctx);
        lblPresets.setText("• Quick Color Slots:");
        lblPresets.setTextColor(Color.WHITE);
        lblPresets.setPadding(0, 12, 0, 8);
        root.addView(lblPresets);

        LinearLayout presetsLayout = new LinearLayout(ctx);
        presetsLayout.setOrientation(LinearLayout.HORIZONTAL);
        presetsLayout.setPadding(0, 8, 0, 24);
        presetsLayout.setGravity(Gravity.CENTER);

        int[] presetColors = new int[]{
                Color.parseColor("#1C1D26"), Color.parseColor("#373B4B"),
                Color.parseColor("#00B099"), Color.parseColor("#00E5FF"),
                Color.parseColor("#00FF88"), Color.parseColor("#FF0055"),
                Color.parseColor("#00B0FF"), Color.parseColor("#FFB300")
        };

        for (int preset : presetColors) {
            View patch = new View(ctx);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(54, 54);
            p.setMargins(6, 0, 6, 0);
            patch.setLayoutParams(p);

            GradientDrawable patchBg = new GradientDrawable();
            patchBg.setColor(preset);
            patchBg.setCornerRadius(12);
            patchBg.setStroke(2, Color.parseColor("#4D5A83"));
            patch.setBackground(patchBg);

            patch.setOnClickListener(v -> {
                Color.colorToHSV(preset, hsv);
                updatePreview();
            });
            presetsLayout.addView(patch);
        }
        root.addView(presetsLayout);

        // Buttons Row
        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);

        Button btnCancel = new Button(ctx);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.WHITE);
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setColor(Color.parseColor("#4D5A83"));
        cancelBg.setCornerRadius(12);
        btnCancel.setBackground(cancelBg);

        Button btnSelect = new Button(ctx);
        btnSelect.setText("Apply Color");
        btnSelect.setTextColor(Color.WHITE);
        GradientDrawable selectBg = new GradientDrawable();
        selectBg.setColor(Color.parseColor("#00B099"));
        selectBg.setCornerRadius(12);
        btnSelect.setBackground(selectBg);

        LinearLayout.LayoutParams btnP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnP.setMargins(8, 0, 8, 0);
        btnCancel.setLayoutParams(btnP);
        btnSelect.setLayoutParams(btnP);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSelect.setOnClickListener(v -> {
            if (listener != null) {
                listener.onColorSelected(Color.HSVToColor(hsv));
            }
            dismiss();
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnSelect);
        root.addView(btnRow);

        setContentView(root);
    }

    private interface OnSliderChangeListener {
        void onChange(int value);
    }

    private LinearLayout createSliderRow(Context ctx, String labelText, int initialVal, int max, OnSliderChangeListener listener) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 6, 0, 6);

        TextView lbl = new TextView(ctx);
        lbl.setText("• " + labelText + ": " + initialVal);
        lbl.setTextColor(Color.WHITE);
        lbl.setTextSize(13);

        SeekBar seekBar = new SeekBar(ctx);
        seekBar.setMax(max);
        seekBar.setProgress(initialVal);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                lbl.setText("• " + labelText + ": " + progress);
                listener.onChange(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        row.addView(lbl);
        row.addView(seekBar);
        return row;
    }

    private void updatePreview() {
        if (colorPreview != null) {
            GradientDrawable previewBg = new GradientDrawable();
            previewBg.setColor(Color.HSVToColor(hsv));
            previewBg.setCornerRadius(16);
            colorPreview.setBackground(previewBg);
        }
    }
}
