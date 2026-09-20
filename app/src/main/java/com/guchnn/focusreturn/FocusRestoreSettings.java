/*
 * HyperOS3FocusRestore - Copyright (C) ImKani.
 * Modifications Copyright (C) 2026 Guchnn <https://github.com/Guchnn>.
 * This copy is a derivative work maintained by Guchnn; see MODIFICATIONS.md.
 * Licensed under the GNU General Public License v3.0 only (GPL-3.0-only).
 *
 * Modified 2026-09-18: added focus text style settings (font size, light/dark
 * scene colors, font family, font weight, italic). See MODIFICATIONS.md.
 * Modified 2026-09-19: HyperOS 3 support removed; focus notification is now
 * rendered only through the HyperOS 4 self-built controller. Click behaviour
 * is a two-option choice (small window / open app) with a freeform fallback,
 * and the icon-to-text gap is a dedicated slider. See MODIFICATIONS.md.
 * Modified 2026-09-19 (1.3.0): clock/icon separator options removed, a focus
 * notification vertical offset slider was added, the notification-icon hiding
 * switch became a three-state choice and the small window gained a size mode
 * (system default / measured Xiaomi default). See MODIFICATIONS.md.
 * Modified 2026-09-19 (1.4.0): the measured Xiaomi ratio became the default
 * small window size, the clock-side margin may go negative, double-tap
 * triggering and hiding the focus notification on the lock screen were added.
 * See MODIFICATIONS.md.
 *
 * This file is part of a derivative work distributed under GPL-3.0-only.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package com.guchnn.focusreturn;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Centralized persisted settings and compatibility defaults. */
public final class FocusRestoreSettings {
    public static final String PREFS_NAME = "com.guchnn.focusreturn_preferences";

    public static final String KEY_LIMIT_WIDTH = "limit_text_width";
    public static final String KEY_WIDTH_DP = "text_width_dp";
    public static final String KEY_MARQUEE_DELAY_MS = "marquee_delay_ms";
    public static final String KEY_COMPAT_RETRY = "compat_retry";
    public static final String KEY_MARQUEE_BOUNCE = "marquee_bounce";
    public static final String KEY_ISLAND_COMPAT = "island_compat";
    public static final String KEY_ISLAND_SEPARATOR = "island_separator";
    public static final String KEY_ISLAND_GENERAL_SEPARATOR = "island_general_separator";
    public static final String KEY_ISLAND_SIDE_SEPARATOR = "island_side_separator";
    public static final String KEY_ISLAND_FORCE_PACKAGES = "island_force_packages";
    public static final String KEY_ISLAND_APP_CACHE = "island_app_cache";
    public static final String KEY_DISABLE_ISLAND_PROPERTY = "disable_island_property";
    public static final String KEY_DISABLE_ISLAND_FEATURE_CACHE = "disable_island_feature_cache";
    public static final String KEY_HOOK_MODE = "hook_mode";
    /** Retired 1.2.0 key, still read once to migrate the old on/off switch. */
    public static final String KEY_LEGACY_HIDE_NOTIFICATION_ICONS = "hide_notification_icons";
    public static final String KEY_HIDE_ICONS_MODE = "hide_icons_mode";
    public static final String KEY_FOCUS_VERTICAL_OFFSET_DP = "focus_vertical_offset_dp";
    public static final String KEY_FOCUS_ICON_VERTICAL_OFFSET_DP =
            "focus_icon_vertical_offset_dp";
    public static final String KEY_FOCUS_TEXT_VERTICAL_OFFSET_DP =
            "focus_text_vertical_offset_dp";
    public static final String KEY_FOCUS_FONT_SIZE_SP = "focus_font_size_sp";
    public static final String KEY_FOCUS_TEXT_COLOR_LIGHT = "focus_text_color_light";
    public static final String KEY_FOCUS_TEXT_COLOR_DARK = "focus_text_color_dark";
    public static final String KEY_FOCUS_FONT_FAMILY = "focus_font_family";
    public static final String KEY_FOCUS_FONT_WEIGHT = "focus_font_weight";
    public static final String KEY_FOCUS_FONT_ITALIC = "focus_font_italic";
    public static final String KEY_SHOW_APP_ICON = "show_app_icon";
    public static final String KEY_APP_ICON_SIZE_DP = "app_icon_size_dp";
    public static final String KEY_FOCUS_MARGIN_START_DP = "focus_margin_start_dp";
    public static final String KEY_FOCUS_MARGIN_END_DP = "focus_margin_end_dp";
    public static final String KEY_ICON_TEXT_GAP_DP = "icon_text_gap_dp";
    public static final String KEY_CLICK_OPEN_MODE = "click_open_mode";
    public static final String KEY_CLICK_DOUBLE_TAP = "click_double_tap";
    public static final String KEY_FREEFORM_FALLBACK_DIRECT = "freeform_fallback_direct";
    public static final String KEY_FREEFORM_SIZE_MODE = "freeform_size_mode";
    public static final String KEY_HIDE_ON_LOCKSCREEN = "hide_on_lockscreen";
    public static final String KEY_HIDE_WHEN_APP_OPEN = "hide_when_app_open";
    public static final String KEY_CLOCK_DIVIDER_SYMBOL = "clock_divider_symbol";
    public static final String KEY_CLOCK_DIVIDER_MARGIN_START_DP = "clock_divider_margin_start_dp";
    public static final String KEY_CLOCK_DIVIDER_MARGIN_END_DP = "clock_divider_margin_end_dp";
    public static final String KEY_CLOCK_DIVIDER_COLOR_LIGHT = "clock_divider_color_light";
    public static final String KEY_CLOCK_DIVIDER_COLOR_DARK = "clock_divider_color_dark";
    public static final String KEY_CLOCK_DIVIDER_VERTICAL_OFFSET_DP =
            "clock_divider_vertical_offset_dp";
    public static final String KEY_FOCUS_FADE_ENABLED = "focus_fade_enabled";
    public static final String KEY_FOCUS_FADE_DURATION_MS = "focus_fade_duration_ms";
    static final String KEY_HOOK_SETTINGS_READY = "hook_settings_ready";
    public static final String PACKAGE_SET_SEPARATOR = "\u001f";

    /** HyperOS 4 is the only supported mode now; the OS3 constant is gone. */
    public static final int HOOK_MODE_OS4 = 4;
    public static final int DEFAULT_HOOK_MODE = HOOK_MODE_OS4;

    /** Click behaviour when the focus notification is tapped. */
    public static final int CLICK_OPEN_NONE = 0;
    public static final int CLICK_OPEN_FREEFORM = 1;
    public static final int CLICK_OPEN_DIRECT = 2;

    /** Which status bar notification icons are hidden while a focus item shows. */
    public static final int HIDE_ICONS_NONE = 0;
    public static final int HIDE_ICONS_OTHERS = 1;
    public static final int HIDE_ICONS_FOCUS_APP = 2;

    /** How the small window bounds are derived. */
    public static final int FREEFORM_SIZE_MEASURED = 0;
    public static final int FREEFORM_SIZE_SYSTEM = 1;

    public static final boolean DEFAULT_LIMIT_WIDTH = true;
    public static final int DEFAULT_WIDTH_DP = 160;
    public static final int MIN_WIDTH_DP = 80;
    public static final int MAX_WIDTH_DP = 400;
    public static final int DEFAULT_MARQUEE_DELAY_MS = 200;
    public static final boolean DEFAULT_COMPAT_RETRY = false;
    public static final boolean DEFAULT_MARQUEE_BOUNCE = true;
    public static final boolean DEFAULT_ISLAND_COMPAT = false;
    public static final boolean DEFAULT_DISABLE_ISLAND_PROPERTY = true;
    public static final boolean DEFAULT_DISABLE_ISLAND_FEATURE_CACHE = true;
    public static final int DEFAULT_HIDE_ICONS_MODE = HIDE_ICONS_OTHERS;
    public static final String DEFAULT_ISLAND_SEPARATOR = "·";
    /** 0 keeps the ROM supplied text size; MIN..MAX overrides it in sp. */
    public static final int DEFAULT_FOCUS_FONT_SIZE_SP = 0;
    public static final int MIN_FOCUS_FONT_SIZE_SP = 8;
    public static final int MAX_FOCUS_FONT_SIZE_SP = 32;
    /** 0 keeps the ROM supplied text color, otherwise an opaque ARGB value. */
    public static final int NO_FOCUS_TEXT_COLOR = 0;
    public static final int DEFAULT_FOCUS_TEXT_COLOR_LIGHT = NO_FOCUS_TEXT_COLOR;
    public static final int DEFAULT_FOCUS_TEXT_COLOR_DARK = NO_FOCUS_TEXT_COLOR;
    /** 0 keeps the ROM supplied typeface. */
    public static final int DEFAULT_FOCUS_FONT_FAMILY = 0;
    public static final int MIN_FOCUS_FONT_FAMILY = 0;
    /** 0 keeps the ROM supplied weight, 1..7 maps to 100..900. */
    public static final int DEFAULT_FOCUS_FONT_WEIGHT = 0;
    public static final int MIN_FOCUS_FONT_WEIGHT = 0;
    public static final boolean DEFAULT_FOCUS_FONT_ITALIC = false;
    public static final boolean DEFAULT_SHOW_APP_ICON = true;
    public static final int DEFAULT_APP_ICON_SIZE_DP = 16;
    public static final int MIN_APP_ICON_SIZE_DP = 8;
    public static final int MAX_APP_ICON_SIZE_DP = 32;
    /**
     * Clock-side margin; 0 means the focus content starts right at the slot
     * edge, negative values pull it even closer over the ROM's own spacing.
     */
    public static final int DEFAULT_FOCUS_MARGIN_START_DP = 0;
    public static final int DEFAULT_FOCUS_MARGIN_END_DP = 4;
    public static final int MIN_FOCUS_MARGIN_START_DP = -24;
    public static final int MIN_FOCUS_MARGIN_DP = 0;
    public static final int MAX_FOCUS_MARGIN_DP = 24;
    /** Gap between the app icon and the focus text (no separator symbol). */
    public static final int DEFAULT_ICON_TEXT_GAP_DP = 6;
    public static final int MIN_ICON_TEXT_GAP_DP = 0;
    public static final int MAX_ICON_TEXT_GAP_DP = 32;
    public static final int DEFAULT_CLICK_OPEN_MODE = CLICK_OPEN_FREEFORM;
    public static final boolean DEFAULT_CLICK_DOUBLE_TAP = false;
    public static final boolean DEFAULT_FREEFORM_FALLBACK_DIRECT = true;
    /**
     * The platform default for a programmatic freeform launch measured only
     * ~412x732 px on the test Xiaomi (observed via ADB), so the measured
     * Xiaomi ratio is the shipped default. The numeric values are also chosen
     * so an install that still stores the 1.3.0 "system" value (0) migrates to
     * the measured mode, which is what that release actually needed.
     */
    public static final int DEFAULT_FREEFORM_SIZE_MODE = FREEFORM_SIZE_MEASURED;
    /** Vertical nudge of the whole focus notification, relative to the bar centre. */
    public static final int DEFAULT_FOCUS_VERTICAL_OFFSET_DP = 0;
    public static final int MIN_FOCUS_VERTICAL_OFFSET_DP = -10;
    public static final int MAX_FOCUS_VERTICAL_OFFSET_DP = 10;
    /** Independent vertical nudge of the app icon inside the notification. */
    public static final int DEFAULT_FOCUS_ICON_VERTICAL_OFFSET_DP = 0;
    public static final int MIN_FOCUS_ICON_VERTICAL_OFFSET_DP = -20;
    public static final int MAX_FOCUS_ICON_VERTICAL_OFFSET_DP = 20;
    /** Independent vertical nudge of the text block inside the notification. */
    public static final int DEFAULT_FOCUS_TEXT_VERTICAL_OFFSET_DP = 0;
    public static final int MIN_FOCUS_TEXT_VERTICAL_OFFSET_DP = -20;
    public static final int MAX_FOCUS_TEXT_VERTICAL_OFFSET_DP = 20;
    public static final boolean DEFAULT_HIDE_ON_LOCKSCREEN = false;
    /**
     * Hides the focus notification while the app that owns it is in the
     * foreground (full screen or freeform small window). When the notification
     * is hidden for this reason the notification icon is deliberately left
     * alone, so the user keeps a trace of it in the status bar.
     */
    public static final boolean DEFAULT_HIDE_WHEN_APP_OPEN = false;
    /** Symbol drawn between the status bar clock and the focus notification.
     * An empty value means "no divider", which is the default. */
    public static final String DEFAULT_CLOCK_DIVIDER_SYMBOL = "";
    public static final int DEFAULT_CLOCK_DIVIDER_MARGIN_START_DP = 4;
    public static final int DEFAULT_CLOCK_DIVIDER_MARGIN_END_DP = 4;
    public static final int MIN_CLOCK_DIVIDER_MARGIN_DP = 0;
    public static final int MIN_CLOCK_DIVIDER_MARGIN_START_DP = -24;
    public static final int MAX_CLOCK_DIVIDER_MARGIN_DP = 24;
    public static final int DEFAULT_CLOCK_DIVIDER_VERTICAL_OFFSET_DP = 0;
    public static final int MIN_CLOCK_DIVIDER_VERTICAL_OFFSET_DP = -10;
    public static final int MAX_CLOCK_DIVIDER_VERTICAL_OFFSET_DP = 10;
    /** Fade the focus notification in on show and out on hide. */
    public static final boolean DEFAULT_FOCUS_FADE_ENABLED = true;
    public static final int DEFAULT_FOCUS_FADE_DURATION_MS = 180;
    public static final int MIN_FOCUS_FADE_DURATION_MS = 60;
    public static final int MAX_FOCUS_FADE_DURATION_MS = 800;
    public static final int DEFAULT_CLOCK_DIVIDER_COLOR_LIGHT = NO_FOCUS_TEXT_COLOR;
    public static final int DEFAULT_CLOCK_DIVIDER_COLOR_DARK = NO_FOCUS_TEXT_COLOR;

    public final int hookMode;
    public final boolean limitWidth;
    public final int widthDp;
    public final int marqueeDelayMs;
    public final boolean compatRetry;
    public final boolean marqueeBounce;
    public final boolean islandCompat;
    public final boolean disableIslandProperty;
    public final boolean disableIslandFeatureCache;
    public final int hideIconsMode;
    public final int focusVerticalOffsetDp;
    public final int focusIconVerticalOffsetDp;
    public final int focusTextVerticalOffsetDp;
    public final int focusFontSizeSp;
    public final int focusTextColorLight;
    public final int focusTextColorDark;
    public final int focusFontFamily;
    public final int focusFontWeight;
    public final boolean focusFontItalic;
    public final boolean showAppIcon;
    public final int appIconSizeDp;
    public final int focusMarginStartDp;
    public final int focusMarginEndDp;
    public final int iconTextGapDp;
    public final int clickOpenMode;
    public final boolean clickDoubleTap;
    public final boolean freeformFallbackDirect;
    public final int freeformSizeMode;
    public final boolean hideOnLockscreen;
    public final boolean hideWhenAppOpen;
    public final String clockDividerSymbol;
    public final int clockDividerMarginStartDp;
    public final int clockDividerMarginEndDp;
    public final int clockDividerColorLight;
    public final int clockDividerColorDark;
    public final int clockDividerVerticalOffsetDp;
    public final boolean focusFadeEnabled;
    public final int focusFadeDurationMs;
    public final String islandGeneralSeparator;
    public final String islandSideSeparator;
    public final Set<String> islandForcePackages;

    private FocusRestoreSettings(int hookMode, boolean limitWidth, int widthDp, int marqueeDelayMs,
                                 boolean compatRetry, boolean marqueeBounce, boolean islandCompat,
                                 boolean disableIslandProperty, boolean disableIslandFeatureCache,
                                 int hideIconsMode, int focusVerticalOffsetDp,
                                 int focusFontSizeSp,
                                 int focusTextColorLight, int focusTextColorDark,
                                 int focusFontFamily, int focusFontWeight, boolean focusFontItalic,
                                 boolean showAppIcon, int appIconSizeDp,
                                 int focusMarginStartDp, int focusMarginEndDp, int iconTextGapDp,
                                 int clickOpenMode, boolean clickDoubleTap,
                                 boolean freeformFallbackDirect, int freeformSizeMode,
                                 boolean hideOnLockscreen,
                                 boolean hideWhenAppOpen,
                                 String clockDividerSymbol,
                                 int clockDividerMarginStartDp,
                                 int clockDividerMarginEndDp,
                                 int clockDividerColorLight,
                                 int clockDividerColorDark,
                                 int clockDividerVerticalOffsetDp,
                                 boolean focusFadeEnabled,
                                 int focusFadeDurationMs,
                                 String islandGeneralSeparator,
                                 String islandSideSeparator,
                                 Set<String> islandForcePackages,
                                 int focusIconVerticalOffsetDp,
                                 int focusTextVerticalOffsetDp) {
        this.hookMode = normalizeHookMode(hookMode);
        this.limitWidth = limitWidth;
        this.widthDp = clamp(widthDp, MIN_WIDTH_DP, MAX_WIDTH_DP);
        this.marqueeDelayMs = clamp(marqueeDelayMs, 0, 5000);
        this.compatRetry = compatRetry;
        this.marqueeBounce = marqueeBounce;
        this.islandCompat = islandCompat;
        this.disableIslandProperty = disableIslandProperty;
        this.disableIslandFeatureCache = disableIslandFeatureCache;
        this.hideIconsMode = normalizeHideIconsMode(hideIconsMode);
        this.focusVerticalOffsetDp = clamp(focusVerticalOffsetDp,
                MIN_FOCUS_VERTICAL_OFFSET_DP, MAX_FOCUS_VERTICAL_OFFSET_DP);
        this.focusFontSizeSp = normalizeFontSize(focusFontSizeSp);
        this.focusTextColorLight = normalizeColor(focusTextColorLight);
        this.focusTextColorDark = normalizeColor(focusTextColorDark);
        this.focusFontFamily = normalizeFontFamily(focusFontFamily);
        this.focusFontWeight = normalizeFontWeight(focusFontWeight);
        this.focusFontItalic = focusFontItalic;
        this.showAppIcon = showAppIcon;
        this.appIconSizeDp = clamp(appIconSizeDp, MIN_APP_ICON_SIZE_DP, MAX_APP_ICON_SIZE_DP);
        this.focusMarginStartDp = clamp(focusMarginStartDp,
                MIN_FOCUS_MARGIN_START_DP, MAX_FOCUS_MARGIN_DP);
        this.focusMarginEndDp = clamp(focusMarginEndDp, MIN_FOCUS_MARGIN_DP, MAX_FOCUS_MARGIN_DP);
        this.iconTextGapDp = clamp(iconTextGapDp, MIN_ICON_TEXT_GAP_DP, MAX_ICON_TEXT_GAP_DP);
        this.clickOpenMode = normalizeClickMode(clickOpenMode);
        this.clickDoubleTap = clickDoubleTap;
        this.freeformFallbackDirect = freeformFallbackDirect;
        this.freeformSizeMode = normalizeFreeformSizeMode(freeformSizeMode);
        this.hideOnLockscreen = hideOnLockscreen;
        this.hideWhenAppOpen = hideWhenAppOpen;
        this.clockDividerSymbol = clockDividerSymbol == null ? "" : clockDividerSymbol;
        this.clockDividerMarginStartDp = clamp(clockDividerMarginStartDp,
                MIN_CLOCK_DIVIDER_MARGIN_START_DP, MAX_CLOCK_DIVIDER_MARGIN_DP);
        this.clockDividerMarginEndDp = clamp(clockDividerMarginEndDp,
                MIN_CLOCK_DIVIDER_MARGIN_DP, MAX_CLOCK_DIVIDER_MARGIN_DP);
        this.clockDividerColorLight = normalizeColor(clockDividerColorLight);
        this.clockDividerColorDark = normalizeColor(clockDividerColorDark);
        this.clockDividerVerticalOffsetDp = clamp(clockDividerVerticalOffsetDp,
                MIN_CLOCK_DIVIDER_VERTICAL_OFFSET_DP, MAX_CLOCK_DIVIDER_VERTICAL_OFFSET_DP);
        this.focusFadeEnabled = focusFadeEnabled;
        this.focusFadeDurationMs = clamp(focusFadeDurationMs,
                MIN_FOCUS_FADE_DURATION_MS, MAX_FOCUS_FADE_DURATION_MS);
        this.islandGeneralSeparator = valueOrDefault(islandGeneralSeparator);
        this.islandSideSeparator = valueOrDefault(islandSideSeparator);
        this.islandForcePackages = immutablePackages(islandForcePackages);
        this.focusIconVerticalOffsetDp = clamp(focusIconVerticalOffsetDp,
                MIN_FOCUS_ICON_VERTICAL_OFFSET_DP, MAX_FOCUS_ICON_VERTICAL_OFFSET_DP);
        this.focusTextVerticalOffsetDp = clamp(focusTextVerticalOffsetDp,
                MIN_FOCUS_TEXT_VERTICAL_OFFSET_DP, MAX_FOCUS_TEXT_VERTICAL_OFFSET_DP);
    }

    public static FocusRestoreSettings defaults() {
        return new FocusRestoreSettings(DEFAULT_HOOK_MODE, DEFAULT_LIMIT_WIDTH, DEFAULT_WIDTH_DP,
                DEFAULT_MARQUEE_DELAY_MS, DEFAULT_COMPAT_RETRY, DEFAULT_MARQUEE_BOUNCE,
                DEFAULT_ISLAND_COMPAT,
                DEFAULT_DISABLE_ISLAND_PROPERTY, DEFAULT_DISABLE_ISLAND_FEATURE_CACHE,
                DEFAULT_HIDE_ICONS_MODE, DEFAULT_FOCUS_VERTICAL_OFFSET_DP,
                DEFAULT_FOCUS_FONT_SIZE_SP,
                DEFAULT_FOCUS_TEXT_COLOR_LIGHT, DEFAULT_FOCUS_TEXT_COLOR_DARK,
                DEFAULT_FOCUS_FONT_FAMILY, DEFAULT_FOCUS_FONT_WEIGHT, DEFAULT_FOCUS_FONT_ITALIC,
                DEFAULT_SHOW_APP_ICON, DEFAULT_APP_ICON_SIZE_DP,
                DEFAULT_FOCUS_MARGIN_START_DP, DEFAULT_FOCUS_MARGIN_END_DP, DEFAULT_ICON_TEXT_GAP_DP,
                DEFAULT_CLICK_OPEN_MODE, DEFAULT_CLICK_DOUBLE_TAP,
                DEFAULT_FREEFORM_FALLBACK_DIRECT, DEFAULT_FREEFORM_SIZE_MODE,
                DEFAULT_HIDE_ON_LOCKSCREEN,
                DEFAULT_HIDE_WHEN_APP_OPEN,
                DEFAULT_CLOCK_DIVIDER_SYMBOL,
                DEFAULT_CLOCK_DIVIDER_MARGIN_START_DP,
                DEFAULT_CLOCK_DIVIDER_MARGIN_END_DP,
                DEFAULT_CLOCK_DIVIDER_COLOR_LIGHT,
                DEFAULT_CLOCK_DIVIDER_COLOR_DARK,
                DEFAULT_CLOCK_DIVIDER_VERTICAL_OFFSET_DP,
                DEFAULT_FOCUS_FADE_ENABLED,
                DEFAULT_FOCUS_FADE_DURATION_MS,
                DEFAULT_ISLAND_SEPARATOR,
                DEFAULT_ISLAND_SEPARATOR,
                Collections.<String>emptySet(),
                DEFAULT_FOCUS_ICON_VERTICAL_OFFSET_DP,
                DEFAULT_FOCUS_TEXT_VERTICAL_OFFSET_DP);
    }

    public static FocusRestoreSettings withValues(int hookMode, boolean limitWidth, int widthDp,
                                                  int marqueeDelayMs,
                                                  boolean compatRetry, boolean marqueeBounce, boolean islandCompat,
                                                  boolean disableIslandProperty, boolean disableIslandFeatureCache,
                                                  int hideIconsMode, int focusVerticalOffsetDp,
                                                  int focusFontSizeSp,
                                                  int focusTextColorLight, int focusTextColorDark,
                                                  int focusFontFamily, int focusFontWeight,
                                                  boolean focusFontItalic, boolean showAppIcon, int appIconSizeDp,
                                                  int focusMarginStartDp,
                                                  int focusMarginEndDp, int iconTextGapDp,
                                                  int clickOpenMode, boolean clickDoubleTap,
                                                  boolean freeformFallbackDirect, int freeformSizeMode,
                                                  boolean hideOnLockscreen,
                                                  boolean hideWhenAppOpen,
                                                  String clockDividerSymbol,
                                                  int clockDividerMarginStartDp,
                                                  int clockDividerMarginEndDp,
                                                  int clockDividerColorLight,
                                                  int clockDividerColorDark,
                                                  int clockDividerVerticalOffsetDp,
                                                  boolean focusFadeEnabled,
                                                  int focusFadeDurationMs,
                                                  String islandGeneralSeparator,
                                                  String islandSideSeparator,
                                                  Set<String> islandForcePackages,
                                                  int focusIconVerticalOffsetDp,
                                                  int focusTextVerticalOffsetDp) {
        return new FocusRestoreSettings(hookMode, limitWidth, widthDp, marqueeDelayMs,
                compatRetry, marqueeBounce,
                islandCompat, disableIslandProperty, disableIslandFeatureCache,
                hideIconsMode, focusVerticalOffsetDp, focusFontSizeSp,
                focusTextColorLight, focusTextColorDark,
                focusFontFamily, focusFontWeight, focusFontItalic, showAppIcon, appIconSizeDp,
                focusMarginStartDp, focusMarginEndDp, iconTextGapDp,
                clickOpenMode, clickDoubleTap, freeformFallbackDirect, freeformSizeMode,
                hideOnLockscreen, hideWhenAppOpen, clockDividerSymbol,
                clockDividerMarginStartDp, clockDividerMarginEndDp,
                clockDividerColorLight, clockDividerColorDark, clockDividerVerticalOffsetDp,
                focusFadeEnabled, focusFadeDurationMs,
                islandGeneralSeparator, islandSideSeparator, islandForcePackages,
                focusIconVerticalOffsetDp, focusTextVerticalOffsetDp);
    }

    public static SharedPreferences hookPreferences(Context context) {
        Context storage = context.createDeviceProtectedStorageContext();
        return storage.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    static boolean hasHookSettings(SharedPreferences preferences) {
        return preferences.getBoolean(KEY_HOOK_SETTINGS_READY, false);
    }

    public static FocusRestoreSettings fromPreferences(SharedPreferences preferences) {
        String legacy = preferences.getString(KEY_ISLAND_SEPARATOR, DEFAULT_ISLAND_SEPARATOR);
        // 1.2.0 stored the icon hiding as an on/off switch; migrate it once.
        int legacyHideIcons = preferences.getBoolean(
                KEY_LEGACY_HIDE_NOTIFICATION_ICONS, true) ? HIDE_ICONS_OTHERS : HIDE_ICONS_NONE;
        return new FocusRestoreSettings(
                preferences.getInt(KEY_HOOK_MODE, DEFAULT_HOOK_MODE),
                preferences.getBoolean(KEY_LIMIT_WIDTH, DEFAULT_LIMIT_WIDTH),
                preferences.getInt(KEY_WIDTH_DP, DEFAULT_WIDTH_DP),
                preferences.getInt(KEY_MARQUEE_DELAY_MS, DEFAULT_MARQUEE_DELAY_MS),
                preferences.getBoolean(KEY_COMPAT_RETRY, DEFAULT_COMPAT_RETRY),
                preferences.getBoolean(KEY_MARQUEE_BOUNCE, DEFAULT_MARQUEE_BOUNCE),
                 preferences.getBoolean(KEY_ISLAND_COMPAT, DEFAULT_ISLAND_COMPAT),
                preferences.getBoolean(KEY_DISABLE_ISLAND_PROPERTY, DEFAULT_DISABLE_ISLAND_PROPERTY),
                preferences.getBoolean(KEY_DISABLE_ISLAND_FEATURE_CACHE, DEFAULT_DISABLE_ISLAND_FEATURE_CACHE),
                preferences.getInt(KEY_HIDE_ICONS_MODE, legacyHideIcons),
                preferences.getInt(KEY_FOCUS_VERTICAL_OFFSET_DP, DEFAULT_FOCUS_VERTICAL_OFFSET_DP),
                preferences.getInt(KEY_FOCUS_FONT_SIZE_SP, DEFAULT_FOCUS_FONT_SIZE_SP),
                preferences.getInt(KEY_FOCUS_TEXT_COLOR_LIGHT, DEFAULT_FOCUS_TEXT_COLOR_LIGHT),
                preferences.getInt(KEY_FOCUS_TEXT_COLOR_DARK, DEFAULT_FOCUS_TEXT_COLOR_DARK),
                preferences.getInt(KEY_FOCUS_FONT_FAMILY, DEFAULT_FOCUS_FONT_FAMILY),
                preferences.getInt(KEY_FOCUS_FONT_WEIGHT, DEFAULT_FOCUS_FONT_WEIGHT),
                preferences.getBoolean(KEY_FOCUS_FONT_ITALIC, DEFAULT_FOCUS_FONT_ITALIC),
                preferences.getBoolean(KEY_SHOW_APP_ICON, DEFAULT_SHOW_APP_ICON),
                preferences.getInt(KEY_APP_ICON_SIZE_DP, DEFAULT_APP_ICON_SIZE_DP),
                preferences.getInt(KEY_FOCUS_MARGIN_START_DP, DEFAULT_FOCUS_MARGIN_START_DP),
                preferences.getInt(KEY_FOCUS_MARGIN_END_DP, DEFAULT_FOCUS_MARGIN_END_DP),
                preferences.getInt(KEY_ICON_TEXT_GAP_DP, DEFAULT_ICON_TEXT_GAP_DP),
                preferences.getInt(KEY_CLICK_OPEN_MODE, DEFAULT_CLICK_OPEN_MODE),
                preferences.getBoolean(KEY_CLICK_DOUBLE_TAP, DEFAULT_CLICK_DOUBLE_TAP),
                preferences.getBoolean(KEY_FREEFORM_FALLBACK_DIRECT, DEFAULT_FREEFORM_FALLBACK_DIRECT),
                preferences.getInt(KEY_FREEFORM_SIZE_MODE, DEFAULT_FREEFORM_SIZE_MODE),
                preferences.getBoolean(KEY_HIDE_ON_LOCKSCREEN, DEFAULT_HIDE_ON_LOCKSCREEN),
                preferences.getBoolean(KEY_HIDE_WHEN_APP_OPEN, DEFAULT_HIDE_WHEN_APP_OPEN),
                preferences.getString(KEY_CLOCK_DIVIDER_SYMBOL, DEFAULT_CLOCK_DIVIDER_SYMBOL),
                preferences.getInt(KEY_CLOCK_DIVIDER_MARGIN_START_DP,
                        DEFAULT_CLOCK_DIVIDER_MARGIN_START_DP),
                preferences.getInt(KEY_CLOCK_DIVIDER_MARGIN_END_DP,
                        DEFAULT_CLOCK_DIVIDER_MARGIN_END_DP),
                preferences.getInt(KEY_CLOCK_DIVIDER_COLOR_LIGHT,
                        DEFAULT_CLOCK_DIVIDER_COLOR_LIGHT),
                preferences.getInt(KEY_CLOCK_DIVIDER_COLOR_DARK,
                        DEFAULT_CLOCK_DIVIDER_COLOR_DARK),
                preferences.getInt(KEY_CLOCK_DIVIDER_VERTICAL_OFFSET_DP,
                        DEFAULT_CLOCK_DIVIDER_VERTICAL_OFFSET_DP),
                preferences.getBoolean(KEY_FOCUS_FADE_ENABLED, DEFAULT_FOCUS_FADE_ENABLED),
                preferences.getInt(KEY_FOCUS_FADE_DURATION_MS, DEFAULT_FOCUS_FADE_DURATION_MS),
                preferences.getString(KEY_ISLAND_GENERAL_SEPARATOR, legacy),
                preferences.getString(KEY_ISLAND_SIDE_SEPARATOR, legacy),
                preferences.getStringSet(KEY_ISLAND_FORCE_PACKAGES, Collections.<String>emptySet()),
                preferences.getInt(KEY_FOCUS_ICON_VERTICAL_OFFSET_DP,
                        DEFAULT_FOCUS_ICON_VERTICAL_OFFSET_DP),
                preferences.getInt(KEY_FOCUS_TEXT_VERTICAL_OFFSET_DP,
                        DEFAULT_FOCUS_TEXT_VERTICAL_OFFSET_DP));
    }

    String describe() {
        return "limit=" + limitWidth + " widthDp=" + widthDp
                + " delayMs=" + marqueeDelayMs + " compatRetry=" + compatRetry
                + " marqueeBounce=" + marqueeBounce + " islandCompat=" + islandCompat
                + " disableIslandProperty=" + disableIslandProperty
                + " disableIslandFeatureCache=" + disableIslandFeatureCache
                + " hideIcons=" + describeHideIcons(hideIconsMode)
                + " verticalOffsetDp=" + focusVerticalOffsetDp
                + " iconVertical=" + focusIconVerticalOffsetDp
                + " textVertical=" + focusTextVerticalOffsetDp
                + " fontSizeSp=" + (focusFontSizeSp >= MIN_FOCUS_FONT_SIZE_SP
                        ? String.valueOf(focusFontSizeSp) : "system")
                + " colorLight=" + describeColor(focusTextColorLight)
                + " colorDark=" + describeColor(focusTextColorDark)
                + " fontFamily=" + focusFontFamily
                + " fontWeight=" + describeWeight(focusFontWeight)
                + " italic=" + focusFontItalic
                + " showAppIcon=" + showAppIcon + " appIconSizeDp=" + appIconSizeDp
                + " marginStartDp=" + focusMarginStartDp + " marginEndDp=" + focusMarginEndDp
                + " iconTextGapDp=" + iconTextGapDp
                + " clickOpenMode=" + describeClick(clickOpenMode)
                + " doubleTap=" + clickDoubleTap
                + " freeformFallback=" + freeformFallbackDirect
                + " freeformSize=" + describeFreeformSize(freeformSizeMode)
                + " hideOnLockscreen=" + hideOnLockscreen
                + " hideWhenAppOpen=" + hideWhenAppOpen
                + " clockDivider=" + displaySeparator(clockDividerSymbol)
                + " clockDividerStart=" + clockDividerMarginStartDp
                + " clockDividerEnd=" + clockDividerMarginEndDp
                + " clockDividerColorLight=" + describeColor(clockDividerColorLight)
                + " clockDividerColorDark=" + describeColor(clockDividerColorDark)
                + " clockDividerVertical=" + clockDividerVerticalOffsetDp
                + " fade=" + (focusFadeEnabled ? focusFadeDurationMs + "ms" : "off")
                + " forcePackages=" + islandForcePackages
                + " islandSeparator=" + displaySeparator(islandGeneralSeparator)
                + " islandSideSeparator=" + displaySeparator(islandSideSeparator);
    }

    public boolean save(SharedPreferences preferences) {
        return preferences.edit()
                .putInt(KEY_HOOK_MODE, hookMode)
                .putBoolean(KEY_LIMIT_WIDTH, limitWidth)
                .putInt(KEY_WIDTH_DP, widthDp)
                .putInt(KEY_MARQUEE_DELAY_MS, marqueeDelayMs)
                .putBoolean(KEY_COMPAT_RETRY, compatRetry)
                .putBoolean(KEY_MARQUEE_BOUNCE, marqueeBounce)
                .putBoolean(KEY_ISLAND_COMPAT, islandCompat)
                .putBoolean(KEY_DISABLE_ISLAND_PROPERTY, disableIslandProperty)
                .putBoolean(KEY_DISABLE_ISLAND_FEATURE_CACHE, disableIslandFeatureCache)
                .putInt(KEY_HIDE_ICONS_MODE, hideIconsMode)
                .putInt(KEY_FOCUS_VERTICAL_OFFSET_DP, focusVerticalOffsetDp)
                .putInt(KEY_FOCUS_FONT_SIZE_SP, focusFontSizeSp)
                .putInt(KEY_FOCUS_TEXT_COLOR_LIGHT, focusTextColorLight)
                .putInt(KEY_FOCUS_TEXT_COLOR_DARK, focusTextColorDark)
                .putInt(KEY_FOCUS_FONT_FAMILY, focusFontFamily)
                .putInt(KEY_FOCUS_FONT_WEIGHT, focusFontWeight)
                .putBoolean(KEY_FOCUS_FONT_ITALIC, focusFontItalic)
                .putBoolean(KEY_SHOW_APP_ICON, showAppIcon)
                .putInt(KEY_APP_ICON_SIZE_DP, appIconSizeDp)
                .putInt(KEY_FOCUS_MARGIN_START_DP, focusMarginStartDp)
                .putInt(KEY_FOCUS_MARGIN_END_DP, focusMarginEndDp)
                .putInt(KEY_ICON_TEXT_GAP_DP, iconTextGapDp)
                .putInt(KEY_CLICK_OPEN_MODE, clickOpenMode)
                .putBoolean(KEY_CLICK_DOUBLE_TAP, clickDoubleTap)
                .putBoolean(KEY_FREEFORM_FALLBACK_DIRECT, freeformFallbackDirect)
                .putInt(KEY_FREEFORM_SIZE_MODE, freeformSizeMode)
                .putBoolean(KEY_HIDE_ON_LOCKSCREEN, hideOnLockscreen)
                .putBoolean(KEY_HIDE_WHEN_APP_OPEN, hideWhenAppOpen)
                .putString(KEY_CLOCK_DIVIDER_SYMBOL, clockDividerSymbol)
                .putInt(KEY_CLOCK_DIVIDER_MARGIN_START_DP, clockDividerMarginStartDp)
                .putInt(KEY_CLOCK_DIVIDER_MARGIN_END_DP, clockDividerMarginEndDp)
                .putInt(KEY_CLOCK_DIVIDER_COLOR_LIGHT, clockDividerColorLight)
                .putInt(KEY_CLOCK_DIVIDER_COLOR_DARK, clockDividerColorDark)
                .putInt(KEY_CLOCK_DIVIDER_VERTICAL_OFFSET_DP, clockDividerVerticalOffsetDp)
                .putBoolean(KEY_FOCUS_FADE_ENABLED, focusFadeEnabled)
                .putInt(KEY_FOCUS_FADE_DURATION_MS, focusFadeDurationMs)
                .putString(KEY_ISLAND_GENERAL_SEPARATOR, islandGeneralSeparator)
                .putString(KEY_ISLAND_SIDE_SEPARATOR, islandSideSeparator)
                .putString(KEY_ISLAND_SEPARATOR, islandGeneralSeparator)
                .putStringSet(KEY_ISLAND_FORCE_PACKAGES, islandForcePackages)
                .putInt(KEY_FOCUS_ICON_VERTICAL_OFFSET_DP, focusIconVerticalOffsetDp)
                .putInt(KEY_FOCUS_TEXT_VERTICAL_OFFSET_DP, focusTextVerticalOffsetDp)
                .putBoolean(KEY_HOOK_SETTINGS_READY, true)
                .commit();
    }

    private static Set<String> immutablePackages(Set<String> packages) {
        if (packages == null || packages.isEmpty()) return Collections.emptySet();
        HashSet<String> copy = new HashSet<>();
        for (String value : packages) {
            if (value != null && value.trim().length() > 0) copy.add(value.trim());
        }
        return Collections.unmodifiableSet(copy);
    }

    private static String valueOrDefault(String value) {
        return value == null ? DEFAULT_ISLAND_SEPARATOR : value;
    }

    private static String displaySeparator(String value) {
        return value.length() == 0 ? "<empty>" : value;
    }

    static int normalizeHookMode(int value) {
        return HOOK_MODE_OS4;
    }

    static int normalizeClickMode(int value) {
        if (value == CLICK_OPEN_FREEFORM || value == CLICK_OPEN_DIRECT) return value;
        return CLICK_OPEN_NONE;
    }

    static int normalizeHideIconsMode(int value) {
        if (value == HIDE_ICONS_NONE || value == HIDE_ICONS_FOCUS_APP) return value;
        return HIDE_ICONS_OTHERS;
    }

    static int normalizeFreeformSizeMode(int value) {
        return value == FREEFORM_SIZE_SYSTEM ? FREEFORM_SIZE_SYSTEM : FREEFORM_SIZE_MEASURED;
    }

    /** Values below the minimum fall back to the ROM supplied text size. */
    static int normalizeFontSize(int value) {
        if (value < MIN_FOCUS_FONT_SIZE_SP) return DEFAULT_FOCUS_FONT_SIZE_SP;
        return clamp(value, MIN_FOCUS_FONT_SIZE_SP, MAX_FOCUS_FONT_SIZE_SP);
    }

    /** Zero or a fully transparent value keeps the ROM supplied text color. */
    static int normalizeColor(int value) {
        if ((value >>> 24) == 0) return NO_FOCUS_TEXT_COLOR;
        return 0xFF000000 | (value & 0x00FFFFFF);
    }

    static int normalizeFontFamily(int value) {
        if (value <= MIN_FOCUS_FONT_FAMILY) return DEFAULT_FOCUS_FONT_FAMILY;
        return clamp(value, MIN_FOCUS_FONT_FAMILY, FocusTextStyle.FONT_FAMILY_COUNT - 1);
    }

    static int normalizeFontWeight(int value) {
        if (value <= MIN_FOCUS_FONT_WEIGHT) return DEFAULT_FOCUS_FONT_WEIGHT;
        return clamp(value, MIN_FOCUS_FONT_WEIGHT, FocusTextStyle.FONT_WEIGHT_COUNT - 1);
    }

    static int normalizeIconSize(int value) {
        if (value <= 0) return DEFAULT_APP_ICON_SIZE_DP;
        return clamp(value, MIN_APP_ICON_SIZE_DP, MAX_APP_ICON_SIZE_DP);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String describeColor(int value) {
        return value == NO_FOCUS_TEXT_COLOR ? "system"
                : String.format("#%06X", value & 0x00FFFFFF);
    }

    private static String describeWeight(int value) {
        if (value <= MIN_FOCUS_FONT_WEIGHT) return "system";
        return String.valueOf(FocusTextStyle.fontWeightValue(value));
    }

    static String describeClick(int value) {
        if (value == CLICK_OPEN_FREEFORM) return "freeform";
        if (value == CLICK_OPEN_DIRECT) return "direct";
        return "none";
    }

    static String describeHideIcons(int value) {
        if (value == HIDE_ICONS_NONE) return "none";
        if (value == HIDE_ICONS_FOCUS_APP) return "focusApp";
        return "others";
    }

    static String describeFreeformSize(int value) {
        return value == FREEFORM_SIZE_SYSTEM ? "system" : "measured";
    }
}
