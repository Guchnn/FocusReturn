/*
 * HyperOS3FocusRestore - Copyright (C) ImKani.
 * Modifications Copyright (C) 2026 Guchnn <https://github.com/Guchnn>.
 * This copy is a derivative work maintained by Guchnn; see MODIFICATIONS.md.
 * Licensed under the GNU General Public License v3.0 only (GPL-3.0-only).
 *
 * Modified 2026-09-18: added focus text style fields and cursor columns
 * (colors, font family, font weight, italic, font size). See MODIFICATIONS.md.
 * Modified 2026-09-19: HyperOS 3 support removed; hookMode / allowFocusClick
 * dropped, clock/icon divider and icon-text gap added. See MODIFICATIONS.md.
 * Modified 2026-09-19 (1.3.0): clock/icon divider fields dropped, the icon
 * hiding switch became hideIconsMode, and the vertical offset plus the small
 * window size mode were added. See MODIFICATIONS.md.
 * Modified 2026-09-19 (1.4.0): added double-tap triggering and hiding the
 * focus notification on the lock screen; the clock-side margin may be
 * negative. See MODIFICATIONS.md.
 *
 * This file is part of a derivative work distributed under GPL-3.0-only.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package com.guchnn.focusreturn;

import android.database.Cursor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Immutable SystemUI-side settings snapshot. */
final class HookSettings {
    final boolean limitWidth;
    final int widthDp;
    final int marqueeDelayMs;
    final boolean compatRetry;
    final boolean marqueeBounce;
    final boolean islandCompat;
    final boolean disableIslandProperty;
    final boolean disableIslandFeatureCache;
    final int hideIconsMode;
    final int focusVerticalOffsetDp;
    final int focusIconVerticalOffsetDp;
    final int focusTextVerticalOffsetDp;
    final int focusFontSizeSp;
    final int focusTextColorLight;
    final int focusTextColorDark;
    final int focusFontFamily;
    final int focusFontWeight;
    final boolean focusFontItalic;
    final boolean showAppIcon;
    final int appIconSizeDp;
    final int focusMarginStartDp;
    final int focusMarginEndDp;
    final int iconTextGapDp;
    final int clickOpenMode;
    final boolean clickDoubleTap;
    final boolean freeformFallbackDirect;
    final int freeformSizeMode;
    final boolean hideOnLockscreen;
    final boolean hideWhenAppOpen;
    final String clockDividerSymbol;
    final int clockDividerMarginStartDp;
    final int clockDividerMarginEndDp;
    final int clockDividerColorLight;
    final int clockDividerColorDark;
    final int clockDividerVerticalOffsetDp;
    final boolean focusFadeEnabled;
    final int focusFadeDurationMs;
    final String generalSeparator;
    final String sideSeparator;
    final Set<String> islandForcePackages;

    private HookSettings(boolean limitWidth, int widthDp, int marqueeDelayMs,
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
                         String generalSeparator,
                         String sideSeparator, Set<String> forcePackages,
                         int focusIconVerticalOffsetDp, int focusTextVerticalOffsetDp) {
        this.limitWidth = limitWidth;
        this.widthDp = clamp(widthDp, FocusRestoreSettings.MIN_WIDTH_DP,
                FocusRestoreSettings.MAX_WIDTH_DP);
        this.marqueeDelayMs = clamp(marqueeDelayMs, 0, 5000);
        this.compatRetry = compatRetry;
        this.marqueeBounce = marqueeBounce;
        this.islandCompat = islandCompat;
        this.disableIslandProperty = disableIslandProperty;
        this.disableIslandFeatureCache = disableIslandFeatureCache;
        this.hideIconsMode = FocusRestoreSettings.normalizeHideIconsMode(hideIconsMode);
        this.focusVerticalOffsetDp = clamp(focusVerticalOffsetDp,
                FocusRestoreSettings.MIN_FOCUS_VERTICAL_OFFSET_DP,
                FocusRestoreSettings.MAX_FOCUS_VERTICAL_OFFSET_DP);
        this.focusFontSizeSp = FocusRestoreSettings.normalizeFontSize(focusFontSizeSp);
        this.focusTextColorLight = FocusRestoreSettings.normalizeColor(focusTextColorLight);
        this.focusTextColorDark = FocusRestoreSettings.normalizeColor(focusTextColorDark);
        this.focusFontFamily = FocusRestoreSettings.normalizeFontFamily(focusFontFamily);
        this.focusFontWeight = FocusRestoreSettings.normalizeFontWeight(focusFontWeight);
        this.focusFontItalic = focusFontItalic;
        this.showAppIcon = showAppIcon;
        this.appIconSizeDp = FocusRestoreSettings.normalizeIconSize(appIconSizeDp);
        this.focusMarginStartDp = clamp(focusMarginStartDp,
                FocusRestoreSettings.MIN_FOCUS_MARGIN_START_DP,
                FocusRestoreSettings.MAX_FOCUS_MARGIN_DP);
        this.focusMarginEndDp = clamp(focusMarginEndDp,
                FocusRestoreSettings.MIN_FOCUS_MARGIN_DP,
                FocusRestoreSettings.MAX_FOCUS_MARGIN_DP);
        this.iconTextGapDp = clamp(iconTextGapDp, FocusRestoreSettings.MIN_ICON_TEXT_GAP_DP,
                FocusRestoreSettings.MAX_ICON_TEXT_GAP_DP);
        this.clickOpenMode = FocusRestoreSettings.normalizeClickMode(clickOpenMode);
        this.clickDoubleTap = clickDoubleTap;
        this.freeformFallbackDirect = freeformFallbackDirect;
        this.freeformSizeMode = FocusRestoreSettings.normalizeFreeformSizeMode(freeformSizeMode);
        this.hideOnLockscreen = hideOnLockscreen;
        this.hideWhenAppOpen = hideWhenAppOpen;
        this.clockDividerSymbol = clockDividerSymbol == null ? "" : clockDividerSymbol;
        this.clockDividerMarginStartDp = clamp(clockDividerMarginStartDp,
                FocusRestoreSettings.MIN_CLOCK_DIVIDER_MARGIN_START_DP,
                FocusRestoreSettings.MAX_CLOCK_DIVIDER_MARGIN_DP);
        this.clockDividerMarginEndDp = clamp(clockDividerMarginEndDp,
                FocusRestoreSettings.MIN_CLOCK_DIVIDER_MARGIN_DP,
                FocusRestoreSettings.MAX_CLOCK_DIVIDER_MARGIN_DP);
        this.clockDividerColorLight =
                FocusRestoreSettings.normalizeColor(clockDividerColorLight);
        this.clockDividerColorDark =
                FocusRestoreSettings.normalizeColor(clockDividerColorDark);
        this.clockDividerVerticalOffsetDp = clamp(clockDividerVerticalOffsetDp,
                FocusRestoreSettings.MIN_CLOCK_DIVIDER_VERTICAL_OFFSET_DP,
                FocusRestoreSettings.MAX_CLOCK_DIVIDER_VERTICAL_OFFSET_DP);
        this.focusFadeEnabled = focusFadeEnabled;
        this.focusFadeDurationMs = clamp(focusFadeDurationMs,
                FocusRestoreSettings.MIN_FOCUS_FADE_DURATION_MS,
                FocusRestoreSettings.MAX_FOCUS_FADE_DURATION_MS);
        this.generalSeparator = generalSeparator == null
                ? FocusRestoreSettings.DEFAULT_ISLAND_SEPARATOR : generalSeparator;
        this.sideSeparator = sideSeparator == null
                ? FocusRestoreSettings.DEFAULT_ISLAND_SEPARATOR : sideSeparator;
        this.islandForcePackages = immutablePackages(forcePackages);
        this.focusIconVerticalOffsetDp = clamp(focusIconVerticalOffsetDp,
                FocusRestoreSettings.MIN_FOCUS_ICON_VERTICAL_OFFSET_DP,
                FocusRestoreSettings.MAX_FOCUS_ICON_VERTICAL_OFFSET_DP);
        this.focusTextVerticalOffsetDp = clamp(focusTextVerticalOffsetDp,
                FocusRestoreSettings.MIN_FOCUS_TEXT_VERTICAL_OFFSET_DP,
                FocusRestoreSettings.MAX_FOCUS_TEXT_VERTICAL_OFFSET_DP);
    }

    /** Hides the whole status bar notification icon container. */
    boolean hidesAllNotificationIcons() {
        return hideIconsMode == FocusRestoreSettings.HIDE_ICONS_OTHERS;
    }

    /** Hides only the icons of the app that posted the focus notification. */
    boolean hidesFocusAppIconOnly() {
        return hideIconsMode == FocusRestoreSettings.HIDE_ICONS_FOCUS_APP;
    }

    static HookSettings defaults() {
        FocusRestoreSettings d = FocusRestoreSettings.defaults();
        return new HookSettings(d.limitWidth, d.widthDp, d.marqueeDelayMs,
                d.compatRetry, d.marqueeBounce, d.islandCompat,
                d.disableIslandProperty, d.disableIslandFeatureCache,
                d.hideIconsMode, d.focusVerticalOffsetDp, d.focusFontSizeSp,
                d.focusTextColorLight, d.focusTextColorDark,
                d.focusFontFamily, d.focusFontWeight, d.focusFontItalic,
                d.showAppIcon, d.appIconSizeDp,
                d.focusMarginStartDp, d.focusMarginEndDp, d.iconTextGapDp,
                d.clickOpenMode, d.clickDoubleTap,
                d.freeformFallbackDirect, d.freeformSizeMode, d.hideOnLockscreen,
                d.hideWhenAppOpen, d.clockDividerSymbol,
                d.clockDividerMarginStartDp, d.clockDividerMarginEndDp,
                d.clockDividerColorLight, d.clockDividerColorDark,
                d.clockDividerVerticalOffsetDp,
                d.focusFadeEnabled, d.focusFadeDurationMs,
                d.islandGeneralSeparator, d.islandSideSeparator, Collections.<String>emptySet(),
                d.focusIconVerticalOffsetDp, d.focusTextVerticalOffsetDp);
    }

    static HookSettings fromCursor(Cursor cursor) {
        if (cursor == null || cursor.getColumnCount() < 3) {
            throw new IllegalArgumentException("settings cursor requires at least 3 columns");
        }
        if (cursor.isNull(0) || cursor.isNull(1) || cursor.isNull(2)) {
            throw new IllegalArgumentException("required settings column is null");
        }

        int columnCount = cursor.getColumnCount();
        boolean limitWidth = cursor.getInt(0) != 0;
        int widthDp = cursor.getInt(1);
        int marqueeDelayMs = cursor.getInt(2);
        boolean compatRetry = columnCount > 3 && !cursor.isNull(3) && cursor.getInt(3) != 0;
        boolean islandCompat = columnCount > 4 && !cursor.isNull(4) && cursor.getInt(4) != 0;
        String legacySeparator = columnCount > 5 && !cursor.isNull(5)
                ? cursor.getString(5) : FocusRestoreSettings.DEFAULT_ISLAND_SEPARATOR;
        String generalSeparator = columnCount > 6 && !cursor.isNull(6)
                ? cursor.getString(6) : legacySeparator;
        String sideSeparator = columnCount > 7 && !cursor.isNull(7)
                ? cursor.getString(7) : legacySeparator;
        Set<String> forcePackages = columnCount > 8 && !cursor.isNull(8)
                ? splitPackages(cursor.getString(8)) : Collections.<String>emptySet();
        boolean disableIslandProperty = columnCount > 9 && !cursor.isNull(9)
                ? cursor.getInt(9) != 0 : FocusRestoreSettings.DEFAULT_DISABLE_ISLAND_PROPERTY;
        boolean disableIslandFeatureCache = columnCount > 10 && !cursor.isNull(10)
                ? cursor.getInt(10) != 0 : FocusRestoreSettings.DEFAULT_DISABLE_ISLAND_FEATURE_CACHE;
        boolean marqueeBounce = columnCount > 11 && !cursor.isNull(11)
                 ? cursor.getInt(11) != 0 : FocusRestoreSettings.DEFAULT_MARQUEE_BOUNCE;
        int hideIconsMode = columnCount > 12 && !cursor.isNull(12)
                ? cursor.getInt(12) : FocusRestoreSettings.DEFAULT_HIDE_ICONS_MODE;
        int focusVerticalOffsetDp = columnCount > 13 && !cursor.isNull(13)
                ? cursor.getInt(13) : FocusRestoreSettings.DEFAULT_FOCUS_VERTICAL_OFFSET_DP;
        int focusFontSizeSp = columnCount > 14 && !cursor.isNull(14)
                ? cursor.getInt(14) : FocusRestoreSettings.DEFAULT_FOCUS_FONT_SIZE_SP;
        int focusTextColorLight = columnCount > 15 && !cursor.isNull(15)
                ? cursor.getInt(15) : FocusRestoreSettings.DEFAULT_FOCUS_TEXT_COLOR_LIGHT;
        int focusTextColorDark = columnCount > 16 && !cursor.isNull(16)
                ? cursor.getInt(16) : FocusRestoreSettings.DEFAULT_FOCUS_TEXT_COLOR_DARK;
        int focusFontFamily = columnCount > 17 && !cursor.isNull(17)
                ? cursor.getInt(17) : FocusRestoreSettings.DEFAULT_FOCUS_FONT_FAMILY;
        int focusFontWeight = columnCount > 18 && !cursor.isNull(18)
                ? cursor.getInt(18) : FocusRestoreSettings.DEFAULT_FOCUS_FONT_WEIGHT;
        boolean focusFontItalic = columnCount > 19 && !cursor.isNull(19)
                && cursor.getInt(19) != 0;
        boolean showAppIcon = columnCount > 20 && !cursor.isNull(20) && cursor.getInt(20) != 0;
        int appIconSizeDp = columnCount > 21 && !cursor.isNull(21)
                ? cursor.getInt(21) : FocusRestoreSettings.DEFAULT_APP_ICON_SIZE_DP;
        int focusMarginStartDp = columnCount > 22 && !cursor.isNull(22)
                ? cursor.getInt(22) : FocusRestoreSettings.DEFAULT_FOCUS_MARGIN_START_DP;
        int focusMarginEndDp = columnCount > 23 && !cursor.isNull(23)
                ? cursor.getInt(23) : FocusRestoreSettings.DEFAULT_FOCUS_MARGIN_END_DP;
        int iconTextGapDp = columnCount > 24 && !cursor.isNull(24)
                ? cursor.getInt(24) : FocusRestoreSettings.DEFAULT_ICON_TEXT_GAP_DP;
        int clickOpenMode = columnCount > 25 && !cursor.isNull(25)
                ? cursor.getInt(25) : FocusRestoreSettings.DEFAULT_CLICK_OPEN_MODE;
        boolean freeformFallbackDirect = columnCount > 26 && !cursor.isNull(26)
                && cursor.getInt(26) != 0;
        int freeformSizeMode = columnCount > 27 && !cursor.isNull(27)
                ? cursor.getInt(27) : FocusRestoreSettings.DEFAULT_FREEFORM_SIZE_MODE;
        boolean hideOnLockscreen = columnCount > 28 && !cursor.isNull(28)
                && cursor.getInt(28) != 0;
        boolean clickDoubleTap = columnCount > 29 && !cursor.isNull(29)
                && cursor.getInt(29) != 0;
        boolean hideWhenAppOpen = columnCount > 30 && !cursor.isNull(30)
                && cursor.getInt(30) != 0;
        String clockDividerSymbol = columnCount > 31 && !cursor.isNull(31)
                ? cursor.getString(31) : FocusRestoreSettings.DEFAULT_CLOCK_DIVIDER_SYMBOL;
        int clockDividerMarginStartDp = columnCount > 32 && !cursor.isNull(32)
                ? cursor.getInt(32) : FocusRestoreSettings.DEFAULT_CLOCK_DIVIDER_MARGIN_START_DP;
        int clockDividerMarginEndDp = columnCount > 33 && !cursor.isNull(33)
                ? cursor.getInt(33) : FocusRestoreSettings.DEFAULT_CLOCK_DIVIDER_MARGIN_END_DP;
        int clockDividerColorLight = columnCount > 34 && !cursor.isNull(34)
                ? cursor.getInt(34) : FocusRestoreSettings.DEFAULT_CLOCK_DIVIDER_COLOR_LIGHT;
        int clockDividerColorDark = columnCount > 35 && !cursor.isNull(35)
                ? cursor.getInt(35) : FocusRestoreSettings.DEFAULT_CLOCK_DIVIDER_COLOR_DARK;
        int clockDividerVerticalOffsetDp = columnCount > 36 && !cursor.isNull(36)
                ? cursor.getInt(36) : FocusRestoreSettings.DEFAULT_CLOCK_DIVIDER_VERTICAL_OFFSET_DP;
        boolean focusFadeEnabled = columnCount > 37 && !cursor.isNull(37)
                && cursor.getInt(37) != 0;
        int focusFadeDurationMs = columnCount > 38 && !cursor.isNull(38)
                ? cursor.getInt(38) : FocusRestoreSettings.DEFAULT_FOCUS_FADE_DURATION_MS;
        int focusIconVerticalOffsetDp = columnCount > 39 && !cursor.isNull(39)
                ? cursor.getInt(39)
                : FocusRestoreSettings.DEFAULT_FOCUS_ICON_VERTICAL_OFFSET_DP;
        int focusTextVerticalOffsetDp = columnCount > 40 && !cursor.isNull(40)
                ? cursor.getInt(40)
                : FocusRestoreSettings.DEFAULT_FOCUS_TEXT_VERTICAL_OFFSET_DP;

        return new HookSettings(limitWidth, widthDp, marqueeDelayMs,
                compatRetry, marqueeBounce,
                islandCompat, disableIslandProperty, disableIslandFeatureCache,
                hideIconsMode, focusVerticalOffsetDp, focusFontSizeSp,
                focusTextColorLight, focusTextColorDark,
                focusFontFamily, focusFontWeight, focusFontItalic,
                showAppIcon, appIconSizeDp,
                focusMarginStartDp, focusMarginEndDp, iconTextGapDp,
                clickOpenMode, clickDoubleTap,
                freeformFallbackDirect, freeformSizeMode, hideOnLockscreen,
                hideWhenAppOpen, clockDividerSymbol,
                clockDividerMarginStartDp, clockDividerMarginEndDp,
                clockDividerColorLight, clockDividerColorDark,
                clockDividerVerticalOffsetDp,
                focusFadeEnabled, focusFadeDurationMs,
                generalSeparator, sideSeparator, forcePackages,
                focusIconVerticalOffsetDp, focusTextVerticalOffsetDp);
    }

    String describe() {
        return "limit=" + limitWidth + " widthDp=" + widthDp
                + " delayMs=" + marqueeDelayMs + " compatRetry=" + compatRetry
                 + " marqueeBounce=" + marqueeBounce
                + " islandCompat=" + islandCompat
                 + " disableIslandProperty=" + disableIslandProperty
                 + " disableIslandFeatureCache=" + disableIslandFeatureCache
                 + " hideIcons=" + FocusRestoreSettings.describeHideIcons(hideIconsMode)
                + " verticalOffsetDp=" + focusVerticalOffsetDp
                + " iconVertical=" + focusIconVerticalOffsetDp
                + " textVertical=" + focusTextVerticalOffsetDp
                + " fontSizeSp=" + (focusFontSizeSp >= FocusRestoreSettings.MIN_FOCUS_FONT_SIZE_SP
                        ? String.valueOf(focusFontSizeSp) : "system")
                + " colorLight=" + describeColor(focusTextColorLight)
                + " colorDark=" + describeColor(focusTextColorDark)
                + " fontFamily=" + focusFontFamily
                + " fontWeight=" + describeWeight(focusFontWeight)
                + " italic=" + focusFontItalic
                + " showAppIcon=" + showAppIcon + " appIconSizeDp=" + appIconSizeDp
                + " marginStartDp=" + focusMarginStartDp + " marginEndDp=" + focusMarginEndDp
                + " iconTextGapDp=" + iconTextGapDp
                + " clickOpenMode=" + FocusRestoreSettings.describeClick(clickOpenMode)
                + " doubleTap=" + clickDoubleTap
                + " freeformFallback=" + freeformFallbackDirect
                + " freeformSize=" + FocusRestoreSettings.describeFreeformSize(freeformSizeMode)
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
                + " islandSeparator=" + displaySeparator(generalSeparator)
                + " islandSideSeparator=" + displaySeparator(sideSeparator);
    }

    private static Set<String> splitPackages(String value) {
        if (value == null || value.length() == 0) return Collections.emptySet();
        HashSet<String> result = new HashSet<>();
        String[] parts = value.split(java.util.regex.Pattern.quote(
                FocusRestoreSettings.PACKAGE_SET_SEPARATOR));
        for (String part : parts) {
            if (part != null && part.trim().length() > 0) result.add(part.trim());
        }
        return result;
    }

    private static Set<String> immutablePackages(Set<String> packages) {
        if (packages == null || packages.isEmpty()) return Collections.emptySet();
        HashSet<String> copy = new HashSet<>();
        for (String value : packages) {
            if (value != null && value.trim().length() > 0) copy.add(value.trim());
        }
        return Collections.unmodifiableSet(copy);
    }

    private static String displaySeparator(String value) {
        return value.length() == 0 ? "<empty>" : value;
    }

    private static String describeColor(int value) {
        return value == FocusRestoreSettings.NO_FOCUS_TEXT_COLOR ? "system"
                : String.format("#%06X", value & 0x00FFFFFF);
    }

    private static String describeWeight(int value) {
        return value <= 0 ? "system" : String.valueOf(FocusTextStyle.fontWeightValue(value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
