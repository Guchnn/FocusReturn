/*
 * HyperOS3FocusRestore - Copyright (C) ImKani.
 * Modifications Copyright (C) 2026 Guchnn <https://github.com/Guchnn>.
 * This copy is a derivative work maintained by Guchnn; see MODIFICATIONS.md.
 * Licensed under the GNU General Public License v3.0 only (GPL-3.0-only).
 *
 * New file added 2026-09-18: applies custom focus notification text style
 * (size, color per light/dark scene, font family, weight, italic).
 * Modified 2026-09-19: the light/dark scene is now derived from the status bar
 * tint (the real background) instead of the system night mode, with a
 * luminance hysteresis band to ride out the ROM's tint animations.
 * See MODIFICATIONS.md.
 *
 * This file is part of a derivative work distributed under GPL-3.0-only.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package com.guchnn.focusreturn;

import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Applies the user selected focus notification text style (size, color, family,
 * weight, italic) to focus text views.
 */
final class FocusTextStyle {
    static final int FONT_FAMILY_COUNT = 6;
    static final int FONT_WEIGHT_COUNT = 8;

    private static final String[] FONT_FAMILY_NAMES = {
            "跟随系统", "黑体 Sans", "宋体 Serif", "楷体 Kai", "仿宋 FangSong", "等宽 Mono"};
    /** Generic family used when no matching font file exists on the device. */
    private static final String[] FONT_FAMILY_FALLBACKS = {
            "", "sans-serif", "serif", "serif", "serif", "monospace"};
    /** Lower case fragments matched against /system/fonts file names. */
    private static final String[][] FONT_FAMILY_FILES = {
            null,
            null,
            {"simsun", "songti", "song", "notoserifcjk", "notoserif"},
            {"kaiti", "stkaiti", "kait", "kai"},
            {"fangsong", "fangsongti", "fzfangsong", "fang"},
            null};

    private static final int[] FONT_WEIGHT_VALUES = {0, 100, 300, 400, 500, 600, 700, 900};
    private static final String[] FONT_WEIGHT_NAMES = {
            "跟随系统", "细体 100", "轻体 300", "常规 400", "中等 500", "半粗 600", "粗体 700", "特粗 900"};

    private static final int[] PRESET_COLORS = {
            FocusRestoreSettings.NO_FOCUS_TEXT_COLOR,
            0xFFFFFFFF, 0xFF000000, 0xFFFFF8E7, 0xFFB0BEC5, 0xFF4FC3F7, 0xFF1565C0, 0xFF00E5C0,
            0xFF4CAF50, 0xFFFFD54F, 0xFFFF9800, 0xFFEF5350, 0xFFF48FB1, 0xFFBA68C8};
    private static final String[] PRESET_COLOR_NAMES = {
            "跟随系统", "纯白", "纯黑", "米白", "银灰", "天蓝", "深蓝", "青绿", "翠绿",
            "暖黄", "橙", "朱红", "樱粉", "紫罗兰"};

    private static final String[] FONT_DIRS = {
            "/system/fonts", "/system/font", "/product/fonts", "/vendor/fonts"};

    private static final Map<String, Typeface> TYPEFACE_CACHE = new HashMap<>();
    private static Map<String, String> fontIndex;

    /** Status bar content color published by the active hook; unset by default. */
    private static volatile int statusBarTint = Integer.MIN_VALUE;
    private static int cachedClockId;

    /**
     * Content luminance bands used to decide the scene. Values between the two
     * are treated as an in-flight animation frame and keep the previous scene.
     */
    private static final double SCENE_CONTENT_LIGHT_MIN = 0.62;
    private static final double SCENE_CONTENT_DARK_MAX = 0.38;
    private static volatile boolean lastDarkScene;
    private static volatile boolean lastSceneKnown;

    private FocusTextStyle() {
    }

    /** True when the user picked an explicit sp value instead of the ROM default. */
    static boolean isCustom(HookSettings settings) {
        return settings != null
                && settings.focusFontSizeSp >= FocusRestoreSettings.MIN_FOCUS_FONT_SIZE_SP;
    }

    /** True when any text style option deviates from the ROM default. */
    static boolean hasCustomStyle(HookSettings settings) {
        if (settings == null) return false;
        return settings.focusFontSizeSp >= FocusRestoreSettings.MIN_FOCUS_FONT_SIZE_SP
                || settings.focusTextColorLight != FocusRestoreSettings.NO_FOCUS_TEXT_COLOR
                || settings.focusTextColorDark != FocusRestoreSettings.NO_FOCUS_TEXT_COLOR
                || settings.focusFontFamily > 0
                || settings.focusFontWeight > 0
                || settings.focusFontItalic;
    }

    static boolean apply(TextView textView, HookSettings settings) {
        return apply(textView, settings, isDarkScene(textView));
    }

    static boolean apply(TextView textView, HookSettings settings, boolean darkScene) {
        if (textView == null || !hasCustomStyle(settings)) return false;
        boolean changed = false;

        int color = colorFor(settings, darkScene);
        if (color != FocusRestoreSettings.NO_FOCUS_TEXT_COLOR
                && textView.getCurrentTextColor() != color) {
            textView.setTextColor(color);
            changed = true;
        }

        Typeface typeface = buildTypeface(settings);
        if (typeface != null && typeface != textView.getTypeface()) {
            textView.setTypeface(typeface);
            changed = true;
        }

        if (settings.focusFontSizeSp >= FocusRestoreSettings.MIN_FOCUS_FONT_SIZE_SP) {
            float sp = settings.focusFontSizeSp;
            if (!(textView.getTextSize() > 0f
                    && Math.abs(textView.getTextSize() - spToPx(textView, sp)) < 0.5f)) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
                changed = true;
            }
        }
        return changed;
    }

    /** Applies the style to a text view or to every text view inside a container. */
    static int applyTree(View root, HookSettings settings) {
        return applyTree(root, settings, isDarkScene(root));
    }

    static int applyTree(View root, HookSettings settings, boolean darkScene) {
        if (root == null || !hasCustomStyle(settings)) return 0;
        if (root instanceof TextView) {
            return apply((TextView) root, settings, darkScene) ? 1 : 0;
        }
        if (!(root instanceof ViewGroup)) return 0;
        ViewGroup group = (ViewGroup) root;
        int applied = 0;
        for (int i = 0; i < group.getChildCount(); i++) {
            applied += applyTree(group.getChildAt(i), settings, darkScene);
        }
        return applied;
    }

    /** Resolves the color for the current light/dark scene, or NO_FOCUS_TEXT_COLOR. */
    static int colorFor(View view, HookSettings settings) {
        if (settings == null) return FocusRestoreSettings.NO_FOCUS_TEXT_COLOR;
        return colorFor(settings, isDarkScene(view));
    }

    static int colorFor(HookSettings settings, boolean darkScene) {
        if (settings == null) return FocusRestoreSettings.NO_FOCUS_TEXT_COLOR;
        return darkScene ? settings.focusTextColorDark : settings.focusTextColorLight;
    }

    /**
     * Publishes the status bar content color measured by the active hook. The
     * ROM tints status bar content light over a dark background and dark over a
     * light background, so this is the authoritative "scene" signal.
     */
    static void publishStatusBarTint(int tint) {
        statusBarTint = tint;
    }

    static boolean hasStatusBarTint() {
        return statusBarTint != Integer.MIN_VALUE;
    }

    /** The last status bar content color published by the active hook, or MIN_VALUE. */
    static int getStatusBarTint() {
        return statusBarTint;
    }

    /**
     * Maps the status bar content color to the scene it implies: light content
     * means the background is dark (dark scene) and dark content means the
     * background is light (light scene).
     *
     * <p>The ROM animates the tint between states, so mid-tone frames keep the
     * previously resolved scene instead of flipping back and forth.
     */
    static boolean isDarkSceneFromTint(int tint) {
        int r = (tint >> 16) & 0xFF;
        int g = (tint >> 8) & 0xFF;
        int b = tint & 0xFF;
        double luma = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
        return resolveScene(luma);
    }

    private static synchronized boolean resolveScene(double luma) {
        if (luma >= SCENE_CONTENT_LIGHT_MIN) {
            lastDarkScene = true;   // bright content -> dark background
            lastSceneKnown = true;
            return true;
        }
        if (luma <= SCENE_CONTENT_DARK_MAX) {
            lastDarkScene = false;  // dark content -> bright background
            lastSceneKnown = true;
            return false;
        }
        // Mid-tone (animated frame): hold the last confident scene.
        return lastSceneKnown && lastDarkScene;
    }

    static String sceneLabel(boolean darkScene) {
        return darkScene ? "dark" : "light";
    }

    /**
     * Resolves the scene from the real background behind the status bar. This is
     * deliberately NOT the system dark/light theme: a focus notification can sit
     * on a light background while the system runs in dark mode (and vice versa).
     */
    static boolean isDarkScene(View view) {
        if (statusBarTint != Integer.MIN_VALUE) {
            return isDarkSceneFromTint(statusBarTint);
        }
        Integer tint = statusBarClockTint(view);
        if (tint != null) return isDarkSceneFromTint(tint);
        return isSystemNightMode(view);
    }

    /** Reads the status bar clock color, which mirrors the status bar tint. */
    private static Integer statusBarClockTint(View view) {
        if (view == null) return null;
        try {
            View root = view.getRootView();
            if (root == null || root == view) return null;
            int id = statusBarClockId(view);
            if (id == 0) return null;
            View clock = root.findViewById(id);
            if (clock instanceof TextView) {
                return ((TextView) clock).getCurrentTextColor();
            }
        } catch (Throwable ignored) {
            // Fall through to the night mode fallback.
        }
        return null;
    }

    private static int statusBarClockId(View view) {
        if (cachedClockId != 0) return cachedClockId;
        try {
            cachedClockId = view.getResources().getIdentifier(
                    "clock", "id", "com.android.systemui");
        } catch (Throwable ignored) {
            cachedClockId = 0;
        }
        return cachedClockId;
    }

    /** Last resort only; keeps behaviour from regressing when no tint is known. */
    private static boolean isSystemNightMode(View view) {
        if (view == null) return false;
        try {
            Configuration configuration = view.getResources().getConfiguration();
            return (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static Typeface buildTypeface(HookSettings settings) {
        if (settings == null) return null;
        if (settings.focusFontFamily <= 0 && settings.focusFontWeight <= 0
                && !settings.focusFontItalic) {
            return null;
        }
        String key = settings.focusFontFamily + "|" + settings.focusFontWeight + "|"
                + settings.focusFontItalic;
        synchronized (TYPEFACE_CACHE) {
            Typeface cached = TYPEFACE_CACHE.get(key);
            if (cached != null) return cached;
        }
        Typeface base = resolveBaseTypeface(settings.focusFontFamily);
        int weight = fontWeightValue(settings.focusFontWeight);
        Typeface result;
        if (weight > 0 && Build.VERSION.SDK_INT >= 28) {
            result = Typeface.create(base, weight, settings.focusFontItalic);
        } else {
            int style = settings.focusFontItalic ? Typeface.ITALIC : Typeface.NORMAL;
            if (weight >= 600) style |= Typeface.BOLD;
            result = Typeface.create(base, style);
        }
        if (result == null) return null;
        synchronized (TYPEFACE_CACHE) {
            TYPEFACE_CACHE.put(key, result);
        }
        return result;
    }

    /** Prefers a matching font file on the device, then the generic family. */
    private static Typeface resolveBaseTypeface(int index) {
        Typeface fromFile = fontFileTypeface(index);
        if (fromFile != null) return fromFile;
        String family = fontFamilyValue(index);
        if (family.length() == 0) return Typeface.DEFAULT;
        return Typeface.create(family, Typeface.NORMAL);
    }

    private static Typeface fontFileTypeface(int index) {
        String[] keywords = index > 0 && index < FONT_FAMILY_FILES.length
                ? FONT_FAMILY_FILES[index] : null;
        if (keywords == null) return null;
        Map<String, String> index2 = fontFileIndex();
        for (String keyword : keywords) {
            for (Map.Entry<String, String> entry : index2.entrySet()) {
                if (!entry.getKey().contains(keyword)) continue;
                try {
                    Typeface typeface = Typeface.createFromFile(entry.getValue());
                    if (typeface != null) return typeface;
                } catch (Throwable ignored) {
                    // Try the next candidate.
                }
            }
        }
        return null;
    }

    private static synchronized Map<String, String> fontFileIndex() {
        if (fontIndex != null) return fontIndex;
        Map<String, String> index = new HashMap<>();
        for (String dir : FONT_DIRS) {
            File[] files = new File(dir).listFiles();
            if (files == null) continue;
            for (File file : files) {
                if (!file.isFile()) continue;
                String name = file.getName().toLowerCase(Locale.US);
                if (!name.endsWith(".ttf") && !name.endsWith(".otf") && !name.endsWith(".ttc")) {
                    continue;
                }
                if (!index.containsKey(name)) index.put(name, file.getAbsolutePath());
            }
        }
        fontIndex = index;
        return fontIndex;
    }

    static String fontFamilyValue(int index) {
        if (index <= 0 || index >= FONT_FAMILY_FALLBACKS.length) return "";
        return FONT_FAMILY_FALLBACKS[index];
    }

    static String fontFamilyName(int index) {
        if (index <= 0 || index >= FONT_FAMILY_NAMES.length) return FONT_FAMILY_NAMES[0];
        return FONT_FAMILY_NAMES[index];
    }

    static int fontWeightValue(int index) {
        if (index <= 0 || index >= FONT_WEIGHT_VALUES.length) return 0;
        return FONT_WEIGHT_VALUES[index];
    }

    static String fontWeightName(int index) {
        if (index <= 0 || index >= FONT_WEIGHT_NAMES.length) return FONT_WEIGHT_NAMES[0];
        return FONT_WEIGHT_NAMES[index];
    }

    static int presetColorCount() {
        return PRESET_COLORS.length;
    }

    static int presetColor(int index) {
        if (index < 0 || index >= PRESET_COLORS.length) {
            return FocusRestoreSettings.NO_FOCUS_TEXT_COLOR;
        }
        return PRESET_COLORS[index];
    }

    static String presetColorName(int index) {
        if (index < 0 || index >= PRESET_COLOR_NAMES.length) return PRESET_COLOR_NAMES[0];
        return PRESET_COLOR_NAMES[index];
    }

    static String colorLabel(int color) {
        if (color == FocusRestoreSettings.NO_FOCUS_TEXT_COLOR) return "跟随系统";
        return String.format("#%06X", color & 0x00FFFFFF);
    }

    static String describe(HookSettings settings) {
        if (settings == null) return "settings=null";
        return "size=" + (settings.focusFontSizeSp >= FocusRestoreSettings.MIN_FOCUS_FONT_SIZE_SP
                        ? settings.focusFontSizeSp + "sp" : "system")
                + " light=" + colorLabel(settings.focusTextColorLight)
                + " dark=" + colorLabel(settings.focusTextColorDark)
                + " font=" + fontFamilyName(settings.focusFontFamily)
                + " weight=" + fontWeightName(settings.focusFontWeight)
                + " italic=" + settings.focusFontItalic;
    }

    private static float spToPx(TextView textView, float sp) {
        return sp * textView.getResources().getDisplayMetrics().scaledDensity;
    }
}
