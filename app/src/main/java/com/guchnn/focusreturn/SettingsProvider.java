/*
 * HyperOS3FocusRestore - Copyright (C) ImKani.
 * Modifications Copyright (C) 2026 Guchnn <https://github.com/Guchnn>.
 * This copy is a derivative work maintained by Guchnn; see MODIFICATIONS.md.
 * Licensed under the GNU General Public License v3.0 only (GPL-3.0-only).
 *
 * Modified 2026-09-18: exposed the new focus text style settings as extra
 * cursor columns (appended at the end). See MODIFICATIONS.md.
 * Modified 2026-09-19: HyperOS 3 support removed; cursor no longer carries
 * hook_mode or allow_focus_click. See MODIFICATIONS.md.
 * Modified 2026-09-19 (1.3.0): clock/icon divider columns dropped; added
 * hide_icons_mode, focus_vertical_offset_dp and freeform_size_mode. See
 * MODIFICATIONS.md.
 *
 * This file is part of a derivative work distributed under GPL-3.0-only.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package com.guchnn.focusreturn;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public final class SettingsProvider extends ContentProvider {
    private static final String TAG = "FocusReturn";
    static final String AUTHORITY = "com.guchnn.focusreturn.settings";
    static final Uri URI = Uri.parse("content://" + AUTHORITY + "/config");
    /**
     * Cursor layout. Every index here must stay in sync with
     * {@link HookSettings#fromCursor} - both sides are ordered exactly alike.
     */
    static final String[] COLUMNS = {
            "limit_text_width", "text_width_dp", "marquee_delay_ms", "compat_retry",
            "island_compat", "island_separator", "island_general_separator", "island_side_separator",
            "island_force_packages", "disable_island_property", "disable_island_feature_cache",
            "marquee_bounce", "hide_icons_mode", "focus_vertical_offset_dp",
            "focus_font_size_sp", "focus_text_color_light", "focus_text_color_dark",
            "focus_font_family", "focus_font_weight", "focus_font_italic",
            "show_app_icon", "app_icon_size_dp",
            "focus_margin_start_dp", "focus_margin_end_dp", "icon_text_gap_dp",
            "click_open_mode", "freeform_fallback_direct", "freeform_size_mode",
            "hide_on_lockscreen", "click_double_tap",
            "hide_when_app_open", "clock_divider_symbol",
            "clock_divider_margin_start_dp", "clock_divider_margin_end_dp",
            "clock_divider_color_light", "clock_divider_color_dark",
            "clock_divider_vertical_offset_dp",
            "focus_fade_enabled", "focus_fade_duration_ms",
            "focus_icon_vertical_offset_dp", "focus_text_vertical_offset_dp"};
    static final String KEY_MARQUEE_DELAY_MS = FocusRestoreSettings.KEY_MARQUEE_DELAY_MS;
    static final int DEFAULT_MARQUEE_DELAY_MS = FocusRestoreSettings.DEFAULT_MARQUEE_DELAY_MS;
    private String lastDiagnostic;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (!URI.equals(uri) || getContext() == null) return null;
        Context context = getContext();
        SharedPreferences preferences = FocusRestoreSettings.hookPreferences(context);
        if (!FocusRestoreSettings.hasHookSettings(preferences)) {
            logDiagnostic("provider settings unavailable storage=deviceProtected ready=false "
                    + "columns=" + COLUMNS.length);
            return null;
        }
        FocusRestoreSettings settings = FocusRestoreSettings.fromPreferences(preferences);
        logDiagnostic("provider settings storage=deviceProtected ready=true columns="
                + COLUMNS.length + " " + settings.describe());
        String legacySeparator = preferences.getString(FocusRestoreSettings.KEY_ISLAND_SEPARATOR,
                FocusRestoreSettings.DEFAULT_ISLAND_SEPARATOR);
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        cursor.addRow(new Object[]{
                settings.limitWidth ? 1 : 0, settings.widthDp,
                settings.marqueeDelayMs, settings.compatRetry ? 1 : 0,
                settings.islandCompat ? 1 : 0, legacySeparator,
                settings.islandGeneralSeparator,
                settings.islandSideSeparator, joinPackages(settings.islandForcePackages),
                 settings.disableIslandProperty ? 1 : 0,
                 settings.disableIslandFeatureCache ? 1 : 0,
                  settings.marqueeBounce ? 1 : 0,
                   settings.hideIconsMode,
                   settings.focusVerticalOffsetDp,
                   settings.focusFontSizeSp,
                   settings.focusTextColorLight,
                   settings.focusTextColorDark,
                   settings.focusFontFamily,
                   settings.focusFontWeight,
                   settings.focusFontItalic ? 1 : 0,
                   settings.showAppIcon ? 1 : 0,
                   settings.appIconSizeDp,
                   settings.focusMarginStartDp,
                   settings.focusMarginEndDp,
                   settings.iconTextGapDp,
                   settings.clickOpenMode,
                   settings.freeformFallbackDirect ? 1 : 0,
                   settings.freeformSizeMode,
                   settings.hideOnLockscreen ? 1 : 0,
                   settings.clickDoubleTap ? 1 : 0,
                   settings.hideWhenAppOpen ? 1 : 0,
                   settings.clockDividerSymbol,
                   settings.clockDividerMarginStartDp,
                   settings.clockDividerMarginEndDp,
                   settings.clockDividerColorLight,
                   settings.clockDividerColorDark,
                   settings.clockDividerVerticalOffsetDp,
                   settings.focusFadeEnabled ? 1 : 0,
                   settings.focusFadeDurationMs,
                   settings.focusIconVerticalOffsetDp,
                   settings.focusTextVerticalOffsetDp});
        return cursor;
    }

    private void logDiagnostic(String diagnostic) {
        if (diagnostic.equals(lastDiagnostic)) return;
        lastDiagnostic = diagnostic;
        Log.i(TAG, diagnostic);
    }

    private static String joinPackages(java.util.Set<String> packages) {
        if (packages == null || packages.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        for (String value : packages) {
            if (result.length() > 0) result.append(FocusRestoreSettings.PACKAGE_SET_SEPARATOR);
            result.append(value);
        }
        return result.toString();
    }

    @Override public String getType(Uri uri) {
        return URI.equals(uri) ? "vnd.android.cursor.item/vnd.hyperos3.settings" : null;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { throw new UnsupportedOperationException(); }
}
