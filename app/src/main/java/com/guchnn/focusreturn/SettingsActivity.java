/*
 * HyperOS3FocusRestore - Copyright (C) ImKani.
 * Modifications Copyright (C) 2026 Guchnn <https://github.com/Guchnn>.
 * This copy is a derivative work maintained by Guchnn; see MODIFICATIONS.md.
 * Licensed under the GNU General Public License v3.0 only (GPL-3.0-only).
 *
 * Modified 2026-09-18: rebuilt the settings screen as a MIUI style card list
 * and added the font / weight / italic / scene color controls.
 * Modified 2026-09-18 (2nd): full UI redesign - single page hierarchy with
 * grouped cards, conditional sub-blocks, snackbar status bar, dark theme
 * support and a fixed top bar inset clipping bug.
 * Modified 2026-09-18 (3rd): restructured into 3 top level tabs (显示 / 文字 /
 * 更多) with dedicated second level sub pages so no single screen is crowded.
 * Modified 2026-09-19: corrected the scene colour wording - the light/dark
 * choice follows the status bar background, not the system theme.
 * Modified 2026-09-19 (2nd): compact header bar (app name + save/restart icon
 * actions with a root powered SystemUI restart), merged the former 文字 tab
 * into the 显示 tab entries, moved 转换超级岛内容 / 强制转换白名单 into 高级
 * and dropped the 超级岛 page together with its separator inputs.
 * Modified 2026-09-19 (3rd): true edge to edge layout (the top bar and the
 * bottom bar now own the system bar insets instead of the root view, which
 * removes the doubled top padding and the visible seam under the status bar),
 * every control now auto saves silently after a short debounce (the header
 * save action and the snackbar are gone, the outcome of a SystemUI restart is
 * reported in a dialog) and a new launcher icon.
 * Modified 2026-09-19 (4th): fixed a launch crash (NullPointerException) on
 * HyperOS 4 - the system bar setup ran before setContentView and MIUI's
 * PhoneWindow.getInsetsController() dereferences the not yet created DecorView.
 * The setup now runs after setContentView and takes the controller from the
 * decor view itself, with a guard for ROMs that behave differently.
 * See MODIFICATIONS.md.
 *
 * This file is part of a derivative work distributed under GPL-3.0-only.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package com.guchnn.focusreturn;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import android.text.Editable;
import android.text.TextWatcher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class SettingsActivity extends Activity {
    private static final String TAG = "FocusReturn";
    // Deprecated aliases retained for the existing Hook source/API surface.
    static final String PREFS_NAME = FocusRestoreSettings.PREFS_NAME;
    static final String KEY_LIMIT_WIDTH = FocusRestoreSettings.KEY_LIMIT_WIDTH;
    static final String KEY_WIDTH_DP = FocusRestoreSettings.KEY_WIDTH_DP;
    static final String KEY_MARQUEE_DELAY_MS = FocusRestoreSettings.KEY_MARQUEE_DELAY_MS;
    static final String KEY_COMPAT_RETRY = FocusRestoreSettings.KEY_COMPAT_RETRY;
    static final String KEY_ISLAND_COMPAT = FocusRestoreSettings.KEY_ISLAND_COMPAT;
    static final String KEY_ISLAND_SEPARATOR = FocusRestoreSettings.KEY_ISLAND_SEPARATOR;
    static final String KEY_ISLAND_GENERAL_SEPARATOR = FocusRestoreSettings.KEY_ISLAND_GENERAL_SEPARATOR;
    static final String KEY_ISLAND_SIDE_SEPARATOR = FocusRestoreSettings.KEY_ISLAND_SIDE_SEPARATOR;
    static final String DEFAULT_ISLAND_SEPARATOR = FocusRestoreSettings.DEFAULT_ISLAND_SEPARATOR;
    static final int DEFAULT_WIDTH_DP = FocusRestoreSettings.DEFAULT_WIDTH_DP;
    static final int MIN_WIDTH_DP = FocusRestoreSettings.MIN_WIDTH_DP;
    static final int MAX_WIDTH_DP = FocusRestoreSettings.MAX_WIDTH_DP;
    static final int DEFAULT_MARQUEE_DELAY_MS = FocusRestoreSettings.DEFAULT_MARQUEE_DELAY_MS;
    static final int DEFAULT_FOCUS_FONT_SIZE_SP = FocusRestoreSettings.DEFAULT_FOCUS_FONT_SIZE_SP;
    static final int MIN_FOCUS_FONT_SIZE_SP = FocusRestoreSettings.MIN_FOCUS_FONT_SIZE_SP;
    static final int MAX_FOCUS_FONT_SIZE_SP = FocusRestoreSettings.MAX_FOCUS_FONT_SIZE_SP;

    private static final String APP_NAME = "焦点回归";

    private static final int TAB_FOCUS = 0;
    private static final int TAB_STYLE = 1;
    private static final int TAB_ADVANCED = 2;
    private static final int TAB_ABOUT = 3;

    /** Root commands tried in order until one succeeds (see performSystemUiRestart). */
    private static final String[] RESTART_SYSTEMUI_COMMANDS = {
            "pkill -f \"[c]om.android.systemui\"",
            "killall com.android.systemui",
            "kill $(pidof com.android.systemui)"
    };
    private static final long ROOT_PROBE_TIMEOUT_MS = 60000L;
    private static final long ROOT_COMMAND_TIMEOUT_MS = 15000L;
    /** Quiet period after the last control change before the silent auto save runs. */
    private static final long AUTO_SAVE_DELAY_MS = 350L;

    // Palette (instance fields so dark / light theme can swap them in onCreate).
    private int colorPrimary;
    private int colorPrimaryLight;
    private int colorBackground;
    private int colorCard;
    private int colorTextPrimary;
    private int colorTextSecondary;
    private int colorDivider;
    private int colorInputBackground;
    private int colorTrack;
    // Miuix tokens that have no Android equivalent.
    private int colorGroupTitle;      // onBackgroundVariant
    private int colorAction;          // onSurfaceVariantActions (40%)
    private int colorChipBackground;  // tertiaryContainer
    private int colorChipText;        // onTertiaryContainer
    private int colorSliderTrack;     // sliderBackground
    private boolean darkTheme;

    private SharedPreferences preferences;
    private FocusRestoreSettings settings;
    private ImageView restartButton;
    private TextView topTitle;
    private LinearLayout contentHost;
    private ScrollView contentScroll;
    private Button[] navButtons;

    /** Coalesces rapid control changes (slider drags) into one silent write. */
    private final Handler saveHandler = new Handler(Looper.getMainLooper());
    private final Runnable saveTask = new Runnable() {
        @Override
        public void run() {
            persistNow();
        }
    };
    private boolean unsavedChanges;

    private int currentTab = TAB_FOCUS;
    private Runnable whitelistUpdater;
    private Runnable clickModeUpdater;

    private Switch disableIslandPropertySwitch;
    private Switch disableIslandFeatureCacheSwitch;

    private boolean pendingManual, pendingCompatRetry, pendingMarqueeBounce, pendingIslandCompat,
            pendingDisableIslandProperty, pendingDisableIslandFeatureCache,
            pendingShowAppIcon, pendingFreeformFallbackDirect,
            pendingClickDoubleTap, pendingHideOnLockscreen, pendingHideWhenAppOpen,
            pendingFocusFadeEnabled;
    private int pendingWidthDp, pendingDelayMs, pendingFontSizeSp;
    private int pendingTextColorLight, pendingTextColorDark;
    private int pendingFontFamily, pendingFontWeight;
    private int pendingAppIconSizeDp, pendingFocusMarginStartDp, pendingFocusMarginEndDp;
    private int pendingIconTextGapDp, pendingClickOpenMode;
    private int pendingHideIconsMode, pendingFocusVerticalOffsetDp, pendingFreeformSizeMode;
    private int pendingFocusFadeDurationMs = FocusRestoreSettings.DEFAULT_FOCUS_FADE_DURATION_MS;
    private boolean pendingFontItalic;
    private boolean lightColorExpanded, darkColorExpanded;
    private boolean dividerLightColorExpanded, dividerDarkColorExpanded;
    // Clock divider (1.5.0): the symbol plus its two gaps and scene colours.
    private String pendingClockDividerSymbol = FocusRestoreSettings.DEFAULT_CLOCK_DIVIDER_SYMBOL;
    private int pendingClockDividerMarginStartDp =
            FocusRestoreSettings.DEFAULT_CLOCK_DIVIDER_MARGIN_START_DP;
    private int pendingClockDividerMarginEndDp =
            FocusRestoreSettings.DEFAULT_CLOCK_DIVIDER_MARGIN_END_DP;
    private int pendingClockDividerColorLight =
            FocusRestoreSettings.DEFAULT_CLOCK_DIVIDER_COLOR_LIGHT;
    private int pendingClockDividerColorDark =
            FocusRestoreSettings.DEFAULT_CLOCK_DIVIDER_COLOR_DARK;
    private int pendingClockDividerVerticalOffsetDp =
            FocusRestoreSettings.DEFAULT_CLOCK_DIVIDER_VERTICAL_OFFSET_DP;
    private String pendingGeneralSeparator, pendingSideSeparator;
    private Set<String> pendingForcePackages = new HashSet<>();
    private List<ApplicationInfo> dialogAllApps = new ArrayList<>();
    private List<ApplicationInfo> dialogVisibleApps = new ArrayList<>();
    private Set<String> dialogSelectedPackages;
    private ListView dialogListView;
    private ForcePackageAdapter dialogAdapter;

    /** Choice button groups, each bound to its own pending value field. */
    private static final int CHOICE_CLICK = 0;
    private static final int CHOICE_ICONS = 1;
    private static final int CHOICE_SIZE = 2;

    /** Colour block targets, so one block implementation serves all of them. */
    private static final int COLOR_TEXT_LIGHT = 0;
    private static final int COLOR_TEXT_DARK = 1;
    private static final int COLOR_DIVIDER_LIGHT = 2;
    private static final int COLOR_DIVIDER_DARK = 3;

    private int colorValue(int which) {
        switch (which) {
            case COLOR_TEXT_LIGHT: return pendingTextColorLight;
            case COLOR_TEXT_DARK: return pendingTextColorDark;
            case COLOR_DIVIDER_LIGHT: return pendingClockDividerColorLight;
            default: return pendingClockDividerColorDark;
        }
    }

    private void setColorValue(int which, int value) {
        switch (which) {
            case COLOR_TEXT_LIGHT: pendingTextColorLight = value; break;
            case COLOR_TEXT_DARK: pendingTextColorDark = value; break;
            case COLOR_DIVIDER_LIGHT: pendingClockDividerColorLight = value; break;
            default: pendingClockDividerColorDark = value; break;
        }
    }

    private boolean colorExpanded(int which) {
        switch (which) {
            case COLOR_TEXT_LIGHT: return lightColorExpanded;
            case COLOR_TEXT_DARK: return darkColorExpanded;
            case COLOR_DIVIDER_LIGHT: return dividerLightColorExpanded;
            default: return dividerDarkColorExpanded;
        }
    }

    private void setColorExpanded(int which, boolean expanded) {
        switch (which) {
            case COLOR_TEXT_LIGHT: lightColorExpanded = expanded; break;
            case COLOR_TEXT_DARK: darkColorExpanded = expanded; break;
            case COLOR_DIVIDER_LIGHT: dividerLightColorExpanded = expanded; break;
            default: dividerDarkColorExpanded = expanded; break;
        }
    }

    private int choiceValue(int which) {
        switch (which) {
            case CHOICE_CLICK: return pendingClickOpenMode;
            case CHOICE_ICONS: return pendingHideIconsMode;
            default: return pendingFreeformSizeMode;
        }
    }

    private void setChoiceValue(int which, int value) {
        switch (which) {
            case CHOICE_CLICK: pendingClickOpenMode = value; break;
            case CHOICE_ICONS: pendingHideIconsMode = value; break;
            default: pendingFreeformSizeMode = value; break;
        }
    }
    private EditText dialogSearchInput;
    private Switch dialogShowSystemSwitch;
    private TextView dialogEmptyView;
    private boolean dialogAppsLoaded;
    private static final String APP_CACHE_SEPARATOR = "\u001e";
    private final Map<String, String> appLabels = new java.util.HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyPalette();
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences hookPreferences = FocusRestoreSettings.hookPreferences(this);
        if (!FocusRestoreSettings.hasHookSettings(hookPreferences)) {
            FocusRestoreSettings initialSettings = FocusRestoreSettings.fromPreferences(preferences);
            boolean migrated = initialSettings.save(hookPreferences);
            android.util.Log.i(TAG, "hook settings migration storage=deviceProtected saved="
                    + migrated + " " + initialSettings.describe());
        }
        loadSettings();
        setContentView(createContent());
        // Must run after the content is attached: on HyperOS 4 (MIUI)
        // PhoneWindow#getInsetsController() dereferences the DecorView without
        // creating it, so calling it before setContentView throws a
        // NullPointerException and kills the activity on launch.
        configureSystemBars(getWindow());
        render();
    }

    @Override
    public void onBackPressed() {
        // Back means "return to the first tab" before it means "leave".
        if (currentTab != TAB_FOCUS) {
            currentTab = TAB_FOCUS;
            render();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Flush a pending debounced write so nothing is lost when leaving the screen.
        persistNow();
    }

    /**
     * Miuix (HyperOS) colour tokens, taken verbatim from the library docs so the
     * re-implementation matches the real component library in a Views project.
     */
    private void applyPalette() {
        int night = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        darkTheme = night == Configuration.UI_MODE_NIGHT_YES;
        if (darkTheme) {
            colorPrimary = 0xFF277AF7;            // primary
            colorPrimaryLight = 0xFF2B3B54;       // tertiaryContainer
            colorBackground = 0xFF000000;         // surface
            colorCard = 0xFF242424;               // surfaceVariant
            colorTextPrimary = 0xFFF2F2F2;        // onSurface
            colorTextSecondary = 0x80FFFFFF;      // onSurfaceVariantSummary 50%
            colorDivider = 0xFF393939;            // dividerLine
            colorInputBackground = 0xFF434343;    // secondaryVariant
            colorTrack = 0xFF505050;              // secondary (switch track off)
            colorGroupTitle = 0xFF787E96;         // onBackgroundVariant
            colorAction = 0x66FFFFFF;             // onSurfaceVariantActions 40%
            colorChipBackground = 0xFF2B3B54;     // tertiaryContainer
            colorChipText = 0xFF4788FF;           // onTertiaryContainer
            colorSliderTrack = 0x26FFFFFF;        // sliderBackground 15%
        } else {
            colorPrimary = 0xFF3482FF;
            colorPrimaryLight = 0xFFEAF2FF;
            colorBackground = 0xFFF7F7F7;
            colorCard = 0xFFFFFFFF;
            colorTextPrimary = 0xFF000000;
            colorTextSecondary = 0x99000000;
            colorDivider = 0xFFE0E0E0;
            colorInputBackground = 0xFFF0F0F0;
            colorTrack = 0xFFE6E6E6;
            colorGroupTitle = 0xFF8C93B0;
            colorAction = 0x66000000;
            colorChipBackground = 0xFFEAF2FF;
            colorChipText = 0xFF3482FF;
            colorSliderTrack = 0x0F000000;
        }
    }

    private View createContent() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(colorBackground);
        // The insets are deliberately NOT consumed here: the top bar and the
        // bottom bar paint their own card surface under the system bars, which
        // is what makes the layout edge to edge without a seam.

        outer.addView(createTopBar(), new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        contentScroll = scroll;
        scroll.setFillViewport(true);
        contentHost = new LinearLayout(this);
        contentHost.setOrientation(LinearLayout.VERTICAL);
        contentHost.setPadding(dp(12), dp(4), dp(12), dp(12));
        scroll.addView(contentHost);
        outer.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        View navDivider = new View(this);
        navDivider.setBackgroundColor(colorDivider);
        outer.addView(navDivider, new LinearLayout.LayoutParams(-1, Math.max(1, dp(1))));
        outer.addView(createBottomNavigation(), new LinearLayout.LayoutParams(-1, -2));
        return outer;
    }

    /**
     * Compact header: back chevron (sub pages only) + title, then the SystemUI
     * restart icon action. Deliberately short (44dp) so the content keeps the
     * screen estate, and it owns the status bar inset so its card surface is
     * painted edge to edge.
     */
    private View createTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(colorBackground);
        bar.setMinimumHeight(dp(56));
        bar.setPadding(dp(16), dp(4), dp(16), dp(4));
        applyTopInsets(bar);


        topTitle = text(APP_NAME, 20, colorTextPrimary);
        topTitle.setTypeface(topTitle.getTypeface(), 1);
        topTitle.setSingleLine(true);
        topTitle.setGravity(Gravity.START);
        topTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        bar.addView(topTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        restartButton = headerIconAction(R.drawable.ic_restart, "重启系统界面",
                v -> showRestartSystemUiDialog());
        bar.addView(restartButton, new LinearLayout.LayoutParams(dp(38), dp(38)));
        return bar;
    }

    private ImageView headerIconAction(int drawableRes, String description,
                                       View.OnClickListener listener) {
        ImageView action = new ImageView(this);
        action.setImageResource(drawableRes);
        action.setColorFilter(colorTextPrimary);
        action.setScaleType(ImageView.ScaleType.FIT_CENTER);
        action.setContentDescription(description);
        action.setPadding(dp(9), dp(9), dp(9), dp(9));
        action.setBackground(rippleBackground());
        action.setOnClickListener(listener);
        return action;
    }

    private Drawable rippleBackground() {
        int ripple = darkTheme ? 0x33FFFFFF : 0x1F000000;
        return new RippleDrawable(ColorStateList.valueOf(ripple), null, null);
    }

    private View createBottomNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setBackgroundColor(colorCard);
        nav.setElevation(dp(2));
        nav.setPadding(dp(8), dp(6), dp(8), dp(6));
        // Fixed bar: it lives outside the ScrollView and owns the navigation bar
        // inset, so it stays pinned and never gets overlapped by the gesture bar.
        applyBottomInsets(nav);
        String[] names = {"焦点", "样式", "高级", "关于"};
        navButtons = new Button[names.length];
        for (int i = 0; i < names.length; i++) {
            final int tab = i;
            Button item = new Button(this);
            item.setText(names[i]);
            item.setTextSize(12);
            item.setAllCaps(false);
            item.setMinHeight(0);
            item.setMinWidth(0);
            item.setPadding(0, 0, 0, 0);
            item.setOnClickListener(v -> {
                if (currentTab == tab) return;
                currentTab = tab;
                render();
            });
            navButtons[i] = item;
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(40), 1f));
        }
        return nav;
    }

    private void render() {
        if (contentHost == null) return;
        contentHost.removeAllViews();
        updateNavButtons(currentTab);
        if (topTitle != null) topTitle.setText(APP_NAME);
        if (contentScroll != null) contentScroll.scrollTo(0, 0);
        switch (currentTab) {
            case TAB_STYLE: buildStylePage(contentHost); break;
            case TAB_ADVANCED: buildAdvancedPage(contentHost); break;
            case TAB_ABOUT: buildAboutPage(contentHost); break;
            default: buildFocusPage(contentHost); break;
        }
    }

    // ---------- tab 1: 焦点 ----------

    private void buildFocusPage(LinearLayout root) {
        // Card 1: how the notice appears and disappears.
        LinearLayout baseCard = card();
        final LinearLayout fadeArea = new LinearLayout(this);
        fadeArea.setOrientation(LinearLayout.VERTICAL);
        final TextView fadeValue = text(pendingFocusFadeDurationMs + " ms", 14, colorPrimary);
        final SeekBar fadeBar = new SeekBar(this);
        styleSeekBar(fadeBar);
        fadeBar.setMax(FocusRestoreSettings.MAX_FOCUS_FADE_DURATION_MS
                - FocusRestoreSettings.MIN_FOCUS_FADE_DURATION_MS);
        fadeBar.setProgress(pendingFocusFadeDurationMs
                - FocusRestoreSettings.MIN_FOCUS_FADE_DURATION_MS);
        fadeBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int v = FocusRestoreSettings.MIN_FOCUS_FADE_DURATION_MS + p;
                fadeValue.setText(v + " ms");
                if (user) { pendingFocusFadeDurationMs = v; markPending(); }
            }
        });
        addSwitchItem(baseCard, "渐隐渐显动画",
                "焦点通知出现时淡入、消失时淡出，不再硬切", pendingFocusFadeEnabled, (b, c) -> {
                    pendingFocusFadeEnabled = c;
                    fadeArea.setVisibility(c ? View.VISIBLE : View.GONE);
                    markPending();
                });
        fadeArea.setVisibility(pendingFocusFadeEnabled ? View.VISIBLE : View.GONE);
        addSliderContent(fadeArea, "动画时长", fadeValue, fadeBar,
                FocusRestoreSettings.MIN_FOCUS_FADE_DURATION_MS + " ms",
                FocusRestoreSettings.MAX_FOCUS_FADE_DURATION_MS + " ms");
        baseCard.addView(fadeArea);
        divider(baseCard);

        final TextView offsetValue = text(describeOffset(pendingFocusVerticalOffsetDp),
                14, colorPrimary);
        final SeekBar offsetBar = new SeekBar(this);
        styleSeekBar(offsetBar);
        offsetBar.setMax(FocusRestoreSettings.MAX_FOCUS_VERTICAL_OFFSET_DP
                - FocusRestoreSettings.MIN_FOCUS_VERTICAL_OFFSET_DP);
        offsetBar.setProgress(pendingFocusVerticalOffsetDp
                - FocusRestoreSettings.MIN_FOCUS_VERTICAL_OFFSET_DP);
        offsetBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int v = FocusRestoreSettings.MIN_FOCUS_VERTICAL_OFFSET_DP + p;
                offsetValue.setText(describeOffset(v));
                if (user) { pendingFocusVerticalOffsetDp = v; markPending(); }
            }
        });
        addSliderContent(baseCard, "焦点通知上下位置", offsetValue, offsetBar,
                describeOffset(FocusRestoreSettings.MIN_FOCUS_VERTICAL_OFFSET_DP),
                describeOffset(FocusRestoreSettings.MAX_FOCUS_VERTICAL_OFFSET_DP));
        addSection(root, "焦点通知基础设置", baseCard);

        // Card 2: content width and the scrolling behaviour it triggers.
        LinearLayout scrollCard = card();
        manualWidth(scrollCard);
        divider(scrollCard);

        final TextView delayValue = text(
                String.format(Locale.US, "%.1f 秒", pendingDelayMs / 1000f), 14, colorPrimary);
        final SeekBar delayBar = new SeekBar(this);
        styleSeekBar(delayBar);
        delayBar.setMax(50);
        delayBar.setProgress(pendingDelayMs / 100);
        delayBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int d = p * 100;
                delayValue.setText(String.format(Locale.US, "%.1f 秒", d / 1000f));
                if (user) { pendingDelayMs = d; markPending(); }
            }
        });
        addSliderContent(scrollCard, "滚动启动延迟", delayValue, delayBar, "0 秒", "5 秒");
        divider(scrollCard);
        addSwitchItem(scrollCard, "往返滚动", "内容滚动到末端后反向滚回",
                pendingMarqueeBounce, (b, c) -> { pendingMarqueeBounce = c; markPending(); });
        divider(scrollCard);
        addSwitchItem(scrollCard, "兼容重试", "偶尔不滚动时多尝试一次启动",
                pendingCompatRetry, (b, c) -> { pendingCompatRetry = c; markPending(); });
        addSection(root, "通知滚动设置", scrollCard);

        // Card 3: status bar notification icons.
        LinearLayout iconCard = card();
        LinearLayout iconsHeader = itemRow();
        iconsHeader.addView(titleBox("隐藏通知图标",
                "焦点通知显示期间如何处理状态栏上的其他通知图标"),
                new LinearLayout.LayoutParams(0, -2, 1f));
        iconCard.addView(iconsHeader);
        final Button[] iconModes = new Button[3];
        LinearLayout iconsSeg = new LinearLayout(this);
        iconsSeg.setOrientation(LinearLayout.HORIZONTAL);
        iconsSeg.setPadding(dp(12), 0, dp(12), dp(10));
        iconModes[0] = makeChoiceButton("不隐藏", FocusRestoreSettings.HIDE_ICONS_NONE,
                iconModes, CHOICE_ICONS);
        iconModes[1] = makeChoiceButton("隐藏其他", FocusRestoreSettings.HIDE_ICONS_OTHERS,
                iconModes, CHOICE_ICONS);
        iconModes[2] = makeChoiceButton("仅隐藏本应用", FocusRestoreSettings.HIDE_ICONS_FOCUS_APP,
                iconModes, CHOICE_ICONS);
        for (int index = 0; index < iconModes.length; index++) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(34), 1f);
            if (index > 0) params.leftMargin = dp(6);
            iconsSeg.addView(iconModes[index], params);
        }
        iconCard.addView(iconsSeg);
        updateChoiceButtons(iconModes, CHOICE_ICONS);
        addSection(root, "状态栏图标", iconCard);

        // Card 4: when the notice is hidden.
        LinearLayout timeCard = card();
        addSwitchItem(timeCard, "锁屏时隐藏焦点通知",
                "息屏或锁屏界面上不显示焦点通知，解锁后自动恢复",
                pendingHideOnLockscreen, (b, c) -> { pendingHideOnLockscreen = c; markPending(); });
        divider(timeCard);
        addSwitchItem(timeCard, "应用打开时隐藏焦点通知",
                "发出焦点通知的应用正在前台运行时（全屏或小窗）隐藏焦点通知；"
                        + "此时不会隐藏该应用的通知图标",
                pendingHideWhenAppOpen, (b, c) -> { pendingHideWhenAppOpen = c; markPending(); });
        addSection(root, "显示时机", timeCard);

        // Card 5: opening the app in a small window.
        LinearLayout windowCard = card();
        LinearLayout clickHeader = itemRow();
        clickHeader.addView(titleBox("点击焦点通知",
                "点击后以小窗打开或直接使用应用"), new LinearLayout.LayoutParams(0, -2, 1f));
        windowCard.addView(clickHeader);

        final Button[] clickModes = new Button[2];
        LinearLayout clickSeg = new LinearLayout(this);
        clickSeg.setOrientation(LinearLayout.HORIZONTAL);
        clickSeg.setPadding(dp(12), 0, dp(12), dp(8));
        clickModes[0] = makeChoiceButton("以小窗打开", FocusRestoreSettings.CLICK_OPEN_FREEFORM,
                clickModes, CHOICE_CLICK);
        clickModes[1] = makeChoiceButton("直接打开应用", FocusRestoreSettings.CLICK_OPEN_DIRECT,
                clickModes, CHOICE_CLICK);
        LinearLayout.LayoutParams cLeft = new LinearLayout.LayoutParams(0, dp(34), 1f);
        clickSeg.addView(clickModes[0], cLeft);
        LinearLayout.LayoutParams cRight = new LinearLayout.LayoutParams(0, dp(34), 1f);
        cRight.leftMargin = dp(8);
        clickSeg.addView(clickModes[1], cRight);
        windowCard.addView(clickSeg);
        updateChoiceButtons(clickModes, CHOICE_CLICK);
        divider(windowCard);

        final Switch doubleTapSwitch = addSwitchItem(windowCard,
                "双击呼出",
                "开启后需要双击焦点通知才触发上面的点击行为，避免误触",
                pendingClickDoubleTap, (b, c) -> { pendingClickDoubleTap = c; markPending(); });
        final Switch freeformFallbackSwitch = addSwitchItem(windowCard,
                "小窗启动失败时直接打开应用",
                "选中「以小窗打开」时生效：若小窗模式不可用，退而直接打开应用",
                pendingFreeformFallbackDirect, (b, c) -> { pendingFreeformFallbackDirect = c; markPending(); });
        clickModeUpdater = () -> {
            boolean clickable = pendingClickOpenMode != FocusRestoreSettings.CLICK_OPEN_NONE;
            doubleTapSwitch.setEnabled(clickable);
            doubleTapSwitch.setAlpha(clickable ? 1f : 0.4f);
            boolean free = pendingClickOpenMode == FocusRestoreSettings.CLICK_OPEN_FREEFORM;
            freeformFallbackSwitch.setEnabled(free);
            freeformFallbackSwitch.setAlpha(free ? 1f : 0.4f);
        };
        clickModeUpdater.run();
        divider(windowCard);

        LinearLayout sizeHeader = itemRow();
        sizeHeader.addView(titleBox("小窗尺寸", "选择「以小窗打开」时小窗使用的尺寸"),
                new LinearLayout.LayoutParams(0, -2, 1f));
        windowCard.addView(sizeHeader);
        final Button[] sizeModes = new Button[2];
        LinearLayout sizeSeg = new LinearLayout(this);
        sizeSeg.setOrientation(LinearLayout.HORIZONTAL);
        sizeSeg.setPadding(dp(12), 0, dp(12), dp(10));
        sizeModes[0] = makeChoiceButton("系统默认", FocusRestoreSettings.FREEFORM_SIZE_SYSTEM,
                sizeModes, CHOICE_SIZE);
        sizeModes[1] = makeChoiceButton("小米默认比例", FocusRestoreSettings.FREEFORM_SIZE_MEASURED,
                sizeModes, CHOICE_SIZE);
        LinearLayout.LayoutParams sLeft = new LinearLayout.LayoutParams(0, dp(34), 1f);
        sizeSeg.addView(sizeModes[0], sLeft);
        LinearLayout.LayoutParams sRight = new LinearLayout.LayoutParams(0, dp(34), 1f);
        sRight.leftMargin = dp(8);
        sizeSeg.addView(sizeModes[1], sRight);
        windowCard.addView(sizeSeg);
        updateChoiceButtons(sizeModes, CHOICE_SIZE);
        addSection(root, "小窗打开", windowCard);
    }

    private void manualWidth(LinearLayout displayCard) {
        final LinearLayout widthArea = new LinearLayout(this);
        widthArea.setOrientation(LinearLayout.VERTICAL);
        final TextView widthValue = text(pendingWidthDp + " dp", 14, colorPrimary);
        final SeekBar widthBar = new SeekBar(this);
        styleSeekBar(widthBar);
        widthBar.setMax(MAX_WIDTH_DP - MIN_WIDTH_DP);
        widthBar.setProgress(pendingWidthDp - MIN_WIDTH_DP);
        widthBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int w = MIN_WIDTH_DP + p;
                widthValue.setText(w + " dp");
                if (user) { pendingWidthDp = w; markPending(); }
            }
        });
        addSwitchItem(displayCard, "限制焦点通知宽度",
                "超出设定上限的内容自动滚动显示", pendingManual, (b, c) -> {
                    pendingManual = c;
                    widthArea.setVisibility(c ? View.VISIBLE : View.GONE);
                    markPending();
                });
        widthArea.setVisibility(pendingManual ? View.VISIBLE : View.GONE);
        addSliderContent(widthArea, "最大宽度", widthValue, widthBar,
                MIN_WIDTH_DP + " dp", MAX_WIDTH_DP + " dp");
        displayCard.addView(widthArea);
    }

    // ---------- tab 2: 样式 ----------

    private void buildStylePage(LinearLayout root) {
        LinearLayout styleCard = card();
        final TextView sizeValue = text(fontSizeLabel(pendingFontSizeSp), 14, colorPrimary);
        final SeekBar sizeBar = new SeekBar(this);
        styleSeekBar(sizeBar);
        sizeBar.setMax(MAX_FOCUS_FONT_SIZE_SP - MIN_FOCUS_FONT_SIZE_SP + 1);
        sizeBar.setProgress(fontSizeSpToProgress(pendingFontSizeSp));
        sizeBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int sp = progressToFontSizeSp(p);
                sizeValue.setText(fontSizeLabel(sp));
                if (user) { pendingFontSizeSp = sp; markPending(); }
            }
        });
        addSliderContent(styleCard, "字体大小", sizeValue, sizeBar,
                "跟随系统", MAX_FOCUS_FONT_SIZE_SP + " sp");
        divider(styleCard);

        LinearLayout familyHeader = itemRow();
        familyHeader.setPadding(dp(16), dp(12), dp(16), dp(10));
        familyHeader.addView(text("字体", 15, colorTextPrimary),
                new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView familyValue = text(FocusTextStyle.fontFamilyName(pendingFontFamily),
                14, colorPrimary);
        familyHeader.addView(familyValue);
        styleCard.addView(familyHeader);

        final Button[] chips = new Button[FocusTextStyle.FONT_FAMILY_COUNT];
        LinearLayout familyGrid = new LinearLayout(this);
        familyGrid.setOrientation(LinearLayout.HORIZONTAL);
        familyGrid.setGravity(Gravity.CENTER);
        familyGrid.setPadding(dp(12), 0, dp(12), dp(14));
        for (int index = 0; index < FocusTextStyle.FONT_FAMILY_COUNT; index++) {
            final int family = index;
            Button chip = new Button(this);
            chip.setText(FocusTextStyle.fontFamilyName(index));
            chip.setTextSize(13);
            chip.setAllCaps(false);
            chip.setMinHeight(0);
            chip.setMinWidth(0);
            chip.setPadding(0, 0, 0, 0);
            chip.setOnClickListener(v -> {
                pendingFontFamily = family;
                updateFontChips(chips, familyValue);
                markPending();
            });
            chips[index] = chip;
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(0, dp(34), 1f);
            if (index > 0) chipParams.leftMargin = dp(6);
            familyGrid.addView(chip, chipParams);
        }
        styleCard.addView(familyGrid);
        updateFontChips(chips, familyValue);
        divider(styleCard);

        final TextView weightValue = text(FocusTextStyle.fontWeightName(pendingFontWeight),
                14, colorPrimary);
        final SeekBar weightBar = new SeekBar(this);
        styleSeekBar(weightBar);
        weightBar.setMax(FocusTextStyle.FONT_WEIGHT_COUNT - 1);
        weightBar.setProgress(pendingFontWeight);
        weightBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                weightValue.setText(FocusTextStyle.fontWeightName(p));
                if (user) { pendingFontWeight = p; markPending(); }
            }
        });
        addSliderContent(styleCard, "字重", weightValue, weightBar,
                FocusTextStyle.fontWeightName(0),
                FocusTextStyle.fontWeightName(FocusTextStyle.FONT_WEIGHT_COUNT - 1));
        divider(styleCard);
        addSwitchItem(styleCard, "斜体", "以斜体样式显示焦点通知文字",
                pendingFontItalic, (b, c) -> { pendingFontItalic = c; markPending(); });
        addSection(root, "文字基础样式", styleCard);

        LinearLayout colorCard = card();
        final ColorUi light = new ColorUi();
        addColorBlock(colorCard, "亮色场景", "状态栏底色为亮色时使用（自动识别）", light,
                COLOR_TEXT_LIGHT);
        divider(colorCard);
        final ColorUi dark = new ColorUi();
        addColorBlock(colorCard, "暗色场景", "状态栏底色为暗色时使用（自动识别）", dark,
                COLOR_TEXT_DARK);
        addSection(root, "文字颜色", colorCard);
        installColorListeners(light, COLOR_TEXT_LIGHT);
        installColorListeners(dark, COLOR_TEXT_DARK);
        applyColorToUi(light, colorValue(COLOR_TEXT_LIGHT));
        applyColorToUi(dark, colorValue(COLOR_TEXT_DARK));

        LinearLayout appIconCard = card();
        final LinearLayout iconSizeArea = new LinearLayout(this);
        iconSizeArea.setOrientation(LinearLayout.VERTICAL);
        final TextView iconSizeValue = text(pendingAppIconSizeDp + " dp", 14, colorPrimary);
        final SeekBar iconSizeBar = new SeekBar(this);
        styleSeekBar(iconSizeBar);
        iconSizeBar.setMax(FocusRestoreSettings.MAX_APP_ICON_SIZE_DP
                - FocusRestoreSettings.MIN_APP_ICON_SIZE_DP);
        iconSizeBar.setProgress(pendingAppIconSizeDp
                - FocusRestoreSettings.MIN_APP_ICON_SIZE_DP);
        iconSizeBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int v = FocusRestoreSettings.MIN_APP_ICON_SIZE_DP + p;
                iconSizeValue.setText(v + " dp");
                if (user) { pendingAppIconSizeDp = v; markPending(); }
            }
        });
        addSwitchItem(appIconCard, "显示应用图标",
                "在焦点通知左侧显示发出应用自己的彩色图标",
                pendingShowAppIcon, (b, c) -> {
                    pendingShowAppIcon = c;
                    iconSizeArea.setVisibility(c ? View.VISIBLE : View.GONE);
                    markPending();
                });
        iconSizeArea.setVisibility(pendingShowAppIcon ? View.VISIBLE : View.GONE);
        addSliderContent(iconSizeArea, "图标大小", iconSizeValue, iconSizeBar,
                FocusRestoreSettings.MIN_APP_ICON_SIZE_DP + " dp",
                FocusRestoreSettings.MAX_APP_ICON_SIZE_DP + " dp");
        appIconCard.addView(iconSizeArea);
        divider(appIconCard);

        final TextView gapValue = text(pendingIconTextGapDp + " dp", 14, colorPrimary);
        final SeekBar gapBar = new SeekBar(this);
        styleSeekBar(gapBar);
        gapBar.setMax(FocusRestoreSettings.MAX_ICON_TEXT_GAP_DP
                - FocusRestoreSettings.MIN_ICON_TEXT_GAP_DP);
        gapBar.setProgress(pendingIconTextGapDp
                - FocusRestoreSettings.MIN_ICON_TEXT_GAP_DP);
        gapBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int v = FocusRestoreSettings.MIN_ICON_TEXT_GAP_DP + p;
                gapValue.setText(v + " dp");
                if (user) { pendingIconTextGapDp = v; markPending(); }
            }
        });
        addSliderContent(appIconCard, "图标与文字间距", gapValue, gapBar,
                FocusRestoreSettings.MIN_ICON_TEXT_GAP_DP + " dp",
                FocusRestoreSettings.MAX_ICON_TEXT_GAP_DP + " dp");
        addSection(root, "应用图标", appIconCard);
    }

    private void updateFontChips(Button[] chips, TextView familyValue) {
        for (int i = 0; i < chips.length; i++) {
            boolean selected = i == pendingFontFamily;
            chips[i].setBackground(roundedBg(selected ? colorPrimaryLight : colorInputBackground, 10));
            chips[i].setTextColor(selected ? colorPrimary : colorTextSecondary);
            chips[i].setTypeface(chips[i].getTypeface(), selected ? 1 : 0);
        }
        if (familyValue != null) {
            familyValue.setText(FocusTextStyle.fontFamilyName(pendingFontFamily));
        }
    }

    // ---------- tab 3: 高级 ----------

    private void buildAdvancedPage(LinearLayout root) {
        // Card 1: the symbol between the status bar clock and the notice.
        LinearLayout dividerCard = card();
        final LinearLayout dividerGaps = new LinearLayout(this);
        dividerGaps.setOrientation(LinearLayout.VERTICAL);

        LinearLayout dividerHeader = itemRow();
        dividerHeader.addView(titleBox("时钟与焦点通知分隔符",
                "留空则不显示分隔符；输入的内容按原样绘制"),
                new LinearLayout.LayoutParams(0, -2, 1f));
        dividerCard.addView(dividerHeader);
        final EditText dividerInput = input("例如 ·  或  |");
        dividerInput.setText(pendingClockDividerSymbol);
        dividerInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                pendingClockDividerSymbol = s.toString().trim();
                dividerGaps.setVisibility(pendingClockDividerSymbol.length() == 0
                        ? View.GONE : View.VISIBLE);
                markPending();
            }
        });
        addInput(dividerCard, dividerInput);

        final TextView dividerStartValue =
                text(pendingClockDividerMarginStartDp + " dp", 14, colorPrimary);
        final SeekBar dividerStartBar = new SeekBar(this);
        styleSeekBar(dividerStartBar);
        dividerStartBar.setMax(FocusRestoreSettings.MAX_CLOCK_DIVIDER_MARGIN_DP
                - FocusRestoreSettings.MIN_CLOCK_DIVIDER_MARGIN_START_DP);
        dividerStartBar.setProgress(pendingClockDividerMarginStartDp
                - FocusRestoreSettings.MIN_CLOCK_DIVIDER_MARGIN_START_DP);
        dividerStartBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int v = FocusRestoreSettings.MIN_CLOCK_DIVIDER_MARGIN_START_DP + p;
                dividerStartValue.setText(v + " dp");
                if (user) { pendingClockDividerMarginStartDp = v; markPending(); }
            }
        });
        addSliderContent(dividerGaps, "分隔符与时钟间距", dividerStartValue, dividerStartBar,
                FocusRestoreSettings.MIN_CLOCK_DIVIDER_MARGIN_START_DP + " dp",
                FocusRestoreSettings.MAX_CLOCK_DIVIDER_MARGIN_DP + " dp");

        final TextView dividerEndValue =
                text(pendingClockDividerMarginEndDp + " dp", 14, colorPrimary);
        final SeekBar dividerEndBar = new SeekBar(this);
        styleSeekBar(dividerEndBar);
        dividerEndBar.setMax(FocusRestoreSettings.MAX_CLOCK_DIVIDER_MARGIN_DP
                - FocusRestoreSettings.MIN_CLOCK_DIVIDER_MARGIN_DP);
        dividerEndBar.setProgress(pendingClockDividerMarginEndDp
                - FocusRestoreSettings.MIN_CLOCK_DIVIDER_MARGIN_DP);
        dividerEndBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int v = FocusRestoreSettings.MIN_CLOCK_DIVIDER_MARGIN_DP + p;
                dividerEndValue.setText(v + " dp");
                if (user) { pendingClockDividerMarginEndDp = v; markPending(); }
            }
        });
        addSliderContent(dividerGaps, "分隔符与焦点通知间距", dividerEndValue, dividerEndBar,
                FocusRestoreSettings.MIN_CLOCK_DIVIDER_MARGIN_DP + " dp",
                FocusRestoreSettings.MAX_CLOCK_DIVIDER_MARGIN_DP + " dp");

        final TextView dividerOffsetValue = text(
                describeOffset(pendingClockDividerVerticalOffsetDp), 14, colorPrimary);
        final SeekBar dividerOffsetBar = new SeekBar(this);
        styleSeekBar(dividerOffsetBar);
        dividerOffsetBar.setMax(FocusRestoreSettings.MAX_CLOCK_DIVIDER_VERTICAL_OFFSET_DP
                - FocusRestoreSettings.MIN_CLOCK_DIVIDER_VERTICAL_OFFSET_DP);
        dividerOffsetBar.setProgress(pendingClockDividerVerticalOffsetDp
                - FocusRestoreSettings.MIN_CLOCK_DIVIDER_VERTICAL_OFFSET_DP);
        dividerOffsetBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int v = FocusRestoreSettings.MIN_CLOCK_DIVIDER_VERTICAL_OFFSET_DP + p;
                dividerOffsetValue.setText(describeOffset(v));
                if (user) { pendingClockDividerVerticalOffsetDp = v; markPending(); }
            }
        });
        addSliderContent(dividerGaps, "分隔符上下位置", dividerOffsetValue, dividerOffsetBar,
                describeOffset(FocusRestoreSettings.MIN_CLOCK_DIVIDER_VERTICAL_OFFSET_DP),
                describeOffset(FocusRestoreSettings.MAX_CLOCK_DIVIDER_VERTICAL_OFFSET_DP));
        dividerGaps.setVisibility(pendingClockDividerSymbol.length() == 0
                ? View.GONE : View.VISIBLE);
        dividerCard.addView(dividerGaps);
        divider(dividerCard);

        LinearLayout dividerColorHeader = itemRow();
        dividerColorHeader.addView(titleBox("分隔符颜色",
                "留空则跟随状态栏原生反色（自动识别亮暗）"),
                new LinearLayout.LayoutParams(0, -2, 1f));
        dividerCard.addView(dividerColorHeader);
        final ColorUi dividerLightUi = new ColorUi();
        addColorBlock(dividerCard, "亮色场景", "状态栏底色为亮色时使用",
                dividerLightUi, COLOR_DIVIDER_LIGHT);
        divider(dividerCard);
        final ColorUi dividerDarkUi = new ColorUi();
        addColorBlock(dividerCard, "暗色场景", "状态栏底色为暗色时使用",
                dividerDarkUi, COLOR_DIVIDER_DARK);
        installColorListeners(dividerLightUi, COLOR_DIVIDER_LIGHT);
        installColorListeners(dividerDarkUi, COLOR_DIVIDER_DARK);
        applyColorToUi(dividerLightUi, colorValue(COLOR_DIVIDER_LIGHT));
        applyColorToUi(dividerDarkUi, colorValue(COLOR_DIVIDER_DARK));
        addSection(root, "分隔符设置", dividerCard);

        // Card 2: the clock side spacing.
        LinearLayout clockCard = card();
        final TextView startValue = text(describeClockMargin(pendingFocusMarginStartDp),
                14, colorPrimary);
        final SeekBar startBar = new SeekBar(this);
        styleSeekBar(startBar);
        startBar.setMax(FocusRestoreSettings.MAX_FOCUS_MARGIN_DP
                - FocusRestoreSettings.MIN_FOCUS_MARGIN_START_DP);
        startBar.setProgress(pendingFocusMarginStartDp
                - FocusRestoreSettings.MIN_FOCUS_MARGIN_START_DP);
        startBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int v = FocusRestoreSettings.MIN_FOCUS_MARGIN_START_DP + p;
                startValue.setText(describeClockMargin(v));
                if (user) { pendingFocusMarginStartDp = v; markPending(); }
            }
        });
        addSliderContent(clockCard, "距左侧时钟间距", startValue, startBar,
                describeClockMargin(FocusRestoreSettings.MIN_FOCUS_MARGIN_START_DP),
                describeClockMargin(FocusRestoreSettings.MAX_FOCUS_MARGIN_DP));
        addSection(root, "时钟关联", clockCard);

        // Card 3: super island (超级岛) content conversion.
        LinearLayout advCard = card();
        addSwitchItem(advCard, "转换超级岛内容",
                "把超级岛协议内容转换为焦点通知显示", pendingIslandCompat, (b, c) -> {
                    pendingIslandCompat = c;
                    if (whitelistUpdater != null) whitelistUpdater.run();
                    markPending();
                });
        divider(advCard);

        final LinearLayout whitelistRow = itemRow();
        final TextView whitelistValue = text("", 14, colorPrimary);
        whitelistUpdater = () -> {
            boolean enabled = pendingIslandCompat;
            whitelistRow.setEnabled(enabled);
            whitelistRow.setAlpha(enabled ? 1f : 0.4f);
            whitelistValue.setText(pendingForcePackages.isEmpty()
                    ? "未选择" : "已选 " + pendingForcePackages.size() + " 个");
        };
        whitelistRow.setOnClickListener(v -> {
            if (pendingIslandCompat) showForcePackagesDialog();
        });
        whitelistRow.addView(titleBox("强制转换白名单", "仅对选中的应用强制转换"),
                new LinearLayout.LayoutParams(0, -2, 1f));
        whitelistRow.addView(whitelistValue);
        advCard.addView(whitelistRow);
        whitelistUpdater.run();
        divider(advCard);

        final EditText contentSepEdit = input("内容连接符号，如 ： : ·");
        contentSepEdit.setText(pendingGeneralSeparator);
        contentSepEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                pendingGeneralSeparator = s == null ? "" : s.toString();
                markPending();
            }
            @Override public void afterTextChanged(Editable e) { }
        });
        addInput(advCard, contentSepEdit);
        addSection(root, "超级内容转换", advCard);

        if (com.guchnn.focusreturn.BuildConfig.DEBUG) {
            LinearLayout debugCard = card();
            disableIslandPropertySwitch = addSwitchItem(debugCard, "覆盖 feature.island.debug=false",
                    null, pendingDisableIslandProperty,
                    (b, c) -> { pendingDisableIslandProperty = c; markPending(); });
            divider(debugCard);
            disableIslandFeatureCacheSwitch = addSwitchItem(debugCard, "禁用 FEATURE_DYNAMIC_ISLAND",
                    null, pendingDisableIslandFeatureCache,
                    (b, c) -> { pendingDisableIslandFeatureCache = c; markPending(); });
            addSection(root, "调试选项", debugCard);
        }
    }

    private void buildAboutPage(LinearLayout root) {
        LinearLayout infoCard = card();
        LinearLayout appRow = itemRow();
        LinearLayout appBox = new LinearLayout(this);
        appBox.setOrientation(LinearLayout.VERTICAL);
        TextView name = text("焦点回归", 18, colorTextPrimary);
        name.setTypeface(name.getTypeface(), 1);
        appBox.addView(name);
        TextView version = text("FocusReturn · 版本 "
                + com.guchnn.focusreturn.BuildConfig.VERSION_NAME
                + " · 衍生修改版（上游 ImKani 0.13.6）", 12, colorTextSecondary);
        version.setPadding(0, dp(3), 0, 0);
        appBox.addView(version);
        appRow.addView(appBox);
        infoCard.addView(appRow);
        divider(infoCard);
        infoCard.addView(infoRow("适配系统", "HyperOS 4"));
        addSection(root, "模块信息", infoCard);

        LinearLayout openCard = card();
        openCard.addView(infoRow("作者", "Guchnn"));
        divider(openCard);
        openCard.addView(infoRow("底层设置基于", "ImKani"));
        divider(openCard);
        openCard.addView(infoRow("开源许可证", "GPL-3.0-only"));
        divider(openCard);
        openCard.addView(infoRow("上游基线", "HyperOS3FocusRestore 0.13.6"));
        addSection(root, "开源信息", openCard);

        LinearLayout linkCard = card();
        linkCard.addView(linkRow("模块作者 GitHub", "https://github.com/Guchnn"));
        divider(linkCard);
        linkCard.addView(linkRow("上游 GitHub 项目主页",
                "https://github.com/ImKani/HyperOS3FocusRestore"));
        divider(linkCard);
        linkCard.addView(linkRow("上游作者酷安主页", "https://www.coolapk.com/u/1205658"));
        addSection(root, "外部链接", linkCard);

        LinearLayout licenseCard = card();
        LinearLayout licRow = itemRow();
        licRow.addView(text("Copyright (C) ImKani（上游原始代码）\n"
                        + "Copyright (C) 2026 Guchnn（二创修改部分）\n\n"
                        + "本程序是自由软件：你可以依据自由软件基金会发布的 GNU 通用公共许可证"
                        + "第 3 版（GPL-3.0-only）的条款重新发布或修改它。\n\n"
                        + "本程序是基于“希望它有用”的目的发布的，但不提供任何担保；"
                        + "亦不保证其适销性或针对特定用途的适用性。详见 GPL-3.0 正文。",
                12, colorTextSecondary));
        licenseCard.addView(licRow);
        addSection(root, "版权与许可证", licenseCard);

        LinearLayout riskCard = card();
        LinearLayout riskRow = itemRow();
        riskRow.addView(text("本模块通过 LSPosed Hook 介入系统界面，存在 ROM 版本差异、"
                + "系统崩溃、状态栏显示异常、功能失效等不可控风险。使用前请自行备份，"
                + "并自行承担使用风险。修改设置后需重启 SystemUI 或设备生效。",
                13, colorTextSecondary));
        riskCard.addView(riskRow);
        addSection(root, "说明", riskCard);
    }

    // ---------- building blocks ----------

    /** Miuix SmallTitle: PaddingValues(28dp, 8dp) relative to the screen edge. */
    private void addSection(LinearLayout root, String title, LinearLayout cardView) {
        if (title != null) {
            TextView label = text(title, 13, colorGroupTitle);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
            labelParams.leftMargin = dp(16);
            labelParams.bottomMargin = dp(8);
            labelParams.topMargin = dp(8);
            root.addView(label, labelParams);
        }
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(12);
        root.addView(cardView, cardParams);
    }

    private LinearLayout infoRow(String title, String value) {
        LinearLayout row = itemRow();
        row.addView(text(title, 15, colorTextPrimary), new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(text(value, 14, colorTextSecondary));
        return row;
    }

    private LinearLayout linkRow(final String title, final String url) {
        LinearLayout row = itemRow();
        row.setOnClickListener(v -> openExternalLink(url));
        row.addView(text(title, 15, colorPrimary), new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(text("›", 18, colorTextSecondary));
        return row;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(roundedBg(colorCard, 16));
        return card;
    }

    private void divider(LinearLayout card) {
        View line = new View(this);
        line.setBackgroundColor(colorDivider);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, Math.max(1, dp(1)));
        params.leftMargin = dp(16);
        card.addView(line, params);
    }

    private LinearLayout itemRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));
        row.setMinimumHeight(dp(56));
        return row;
    }

    /**
     * Row label. The summary is deliberately NOT rendered any more: the approved
     * design has no per-option helper text (the parameter is kept so the call
     * sites stay readable as documentation of what each row does).
     */
    private LinearLayout titleBox(String title, String summary) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(text(title, 16, colorTextPrimary));
        return box;
    }

    private Switch addSwitchItem(LinearLayout card, String title, String summary,
                                 boolean checked, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = itemRow();
        row.addView(titleBox(title, summary), new LinearLayout.LayoutParams(0, -2, 1f));
        Switch control = new Switch(this);
        control.setChecked(checked);
        styleSwitch(control);
        if (listener != null) control.setOnCheckedChangeListener(listener);
        row.addView(control);
        card.addView(row);
        return control;
    }

    /** Slider block appended into the given container (no wrapping card). */
    private void addSliderContent(LinearLayout container, String title, TextView valueView,
                                  SeekBar bar, String minLabel, String maxLabel) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(16), dp(12), dp(16), dp(4));
        head.addView(text(title, 15, colorTextPrimary), new LinearLayout.LayoutParams(0, -2, 1f));
        valueView.setTypeface(valueView.getTypeface(), 1);
        head.addView(valueView);
        container.addView(head);

        LinearLayout barRow = new LinearLayout(this);
        barRow.setPadding(dp(12), 0, dp(12), 0);
        barRow.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        container.addView(barRow);

        LinearLayout range = new LinearLayout(this);
        range.setPadding(dp(16), 0, dp(16), dp(12));
        range.addView(text(minLabel, 11, colorTextSecondary),
                new LinearLayout.LayoutParams(0, -2, 1f));
        TextView max = text(maxLabel, 11, colorTextSecondary);
        max.setGravity(Gravity.END);
        range.addView(max, new LinearLayout.LayoutParams(0, -2, 1f));
        container.addView(range);
    }

    private void addInput(LinearLayout container, EditText editText) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.leftMargin = dp(16);
        params.rightMargin = dp(16);
        params.bottomMargin = dp(12);
        container.addView(editText, params);
    }

    private void addNote(LinearLayout root, String message) {
        TextView note = text(message, 12, colorTextSecondary);
        note.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.leftMargin = dp(6);
        params.rightMargin = dp(6);
        root.addView(note, params);
    }

    private Button makeChoiceButton(final String label, final int mode, final Button[] group,
                                    final int which) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(0, 0, 0, 0);
        button.setTag(Integer.valueOf(mode));
        button.setOnClickListener(v -> {
            if (choiceValue(which) == mode) return;
            setChoiceValue(which, mode);
            updateChoiceButtons(group, which);
            if (which == CHOICE_CLICK && clickModeUpdater != null) clickModeUpdater.run();
            markPending();
        });
        return button;
    }

    private void updateChoiceButtons(Button[] group, int which) {
        int current = choiceValue(which);
        for (int i = 0; i < group.length; i++) {
            Object tag = group[i].getTag();
            boolean selected = tag instanceof Integer
                    && ((Integer) tag).intValue() == current;
            group[i].setTextColor(selected ? colorChipText : colorAction);
            group[i].setTypeface(group[i].getTypeface(), selected ? 1 : 0);
            group[i].setBackground(roundedBg(
                    selected ? colorChipBackground : colorInputBackground, 8));
        }
    }

    /** Human readable label for the focus notification vertical offset. */
    private static String describeOffset(int value) {
        if (value == 0) return "居中";
        return (value > 0 ? "下移 " : "上移 ") + Math.abs(value) + " dp";
    }

    /** Human readable label for the clock-side margin (may be negative). */
    private static String describeClockMargin(int value) {
        if (value == 0) return "0 dp（贴住时钟）";
        return value + " dp" + (value < 0 ? "（挤向时钟）" : "");
    }

    private void updateNavButtons(int selected) {
        if (navButtons == null) return;
        for (int i = 0; i < navButtons.length; i++) {
            Button button = navButtons[i];
            boolean active = i == selected;
            button.setBackground(roundedBg(Color.TRANSPARENT, 20));
            button.setTextColor(active ? colorPrimary : colorAction);
            button.setTypeface(button.getTypeface(), active ? 1 : 0);
        }
    }

    private abstract static class SimpleSeek implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar s) { }
        @Override public void onStopTrackingTouch(SeekBar s) { }
    }

    private static int progressToFontSizeSp(int progress) {
        return progress <= 0 ? 0 : MIN_FOCUS_FONT_SIZE_SP - 1 + progress;
    }

    private static int fontSizeSpToProgress(int fontSizeSp) {
        return fontSizeSp < MIN_FOCUS_FONT_SIZE_SP ? 0 : fontSizeSp - MIN_FOCUS_FONT_SIZE_SP + 1;
    }

    private static String fontSizeLabel(int fontSizeSp) {
        return fontSizeSp < MIN_FOCUS_FONT_SIZE_SP ? "跟随系统" : fontSizeSp + " sp";
    }

    // ---------- color blocks ----------

    private static final class ColorUi {
        View swatch;
        TextView expandHint;
        LinearLayout details;
        SeekBar[] channels = new SeekBar[3];
        TextView[] channelValues = new TextView[3];
        Button[] presets;
        int color = FocusRestoreSettings.NO_FOCUS_TEXT_COLOR;
        boolean expanded;
    }

    private void addColorBlock(LinearLayout card, String title, String summary,
                               final ColorUi ui, final int which) {
        ui.expanded = colorExpanded(which);
        final LinearLayout header = itemRow();
        header.addView(titleBox(title, summary), new LinearLayout.LayoutParams(0, -2, 1f));
        ui.swatch = new View(this);
        ui.swatch.setBackground(borderedSwatch());
        header.addView(ui.swatch, new LinearLayout.LayoutParams(dp(22), dp(22)));
        ui.expandHint = text("›", 20, colorTextSecondary);
        ui.expandHint.setPadding(dp(10), 0, 0, 0);
        header.addView(ui.expandHint);
        header.setOnClickListener(v -> {
            ui.expanded = !ui.expanded;
            setColorExpanded(which, ui.expanded);
            ui.details.setVisibility(ui.expanded ? View.VISIBLE : View.GONE);
            ui.expandHint.setRotation(ui.expanded ? 90f : 0f);
        });
        card.addView(header);

        ui.details = new LinearLayout(this);
        ui.details.setOrientation(LinearLayout.VERTICAL);
        ui.details.setPadding(dp(16), dp(2), dp(16), dp(14));
        ui.details.setVisibility(ui.expanded ? View.VISIBLE : View.GONE);
        ui.expandHint.setRotation(ui.expanded ? 90f : 0f);

        LinearLayout presetGrid = new LinearLayout(this);
        presetGrid.setOrientation(LinearLayout.VERTICAL);
        int count = FocusTextStyle.presetColorCount();
        ui.presets = new Button[count];
        int perRow = 7;
        for (int start = 0; start < count; start += perRow) {
            LinearLayout gridRow = new LinearLayout(this);
            gridRow.setOrientation(LinearLayout.HORIZONTAL);
            gridRow.setGravity(Gravity.CENTER);
            for (int index = start; index < Math.min(start + perRow, count); index++) {
                Button swatch = createPresetButton(ui, which, index);
                ui.presets[index] = swatch;
                LinearLayout.LayoutParams swatchParams =
                        new LinearLayout.LayoutParams(0, dp(34), 1f);
                if (index > start) swatchParams.leftMargin = dp(6);
                gridRow.addView(swatch, swatchParams);
            }
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(6);
            presetGrid.addView(gridRow, rowParams);
        }
        ui.details.addView(presetGrid);

        String[] channelLabels = {"R", "G", "B"};
        for (int i = 0; i < 3; i++) {
            LinearLayout channelRow = new LinearLayout(this);
            channelRow.setOrientation(LinearLayout.HORIZONTAL);
            channelRow.setGravity(Gravity.CENTER_VERTICAL);
            channelRow.setPadding(0, dp(2), 0, dp(2));
            ui.channelValues[i] = text("0", 13, colorPrimary);
            ui.channelValues[i].setTypeface(ui.channelValues[i].getTypeface(), 1);
            ui.channelValues[i].setGravity(Gravity.END);
            ui.channelValues[i].setPadding(0, 0, dp(8), 0);
            channelRow.addView(ui.channelValues[i], new LinearLayout.LayoutParams(dp(34), -2));
            channelRow.addView(text(channelLabels[i], 12, colorTextSecondary),
                    new LinearLayout.LayoutParams(dp(16), -2));
            SeekBar channel = new SeekBar(this);
            styleSeekBar(channel);
            channel.setMax(255);
            ui.channels[i] = channel;
            channelRow.addView(channel, new LinearLayout.LayoutParams(0, -2, 1f));
            ui.details.addView(channelRow);
        }
        card.addView(ui.details);
    }

    private Button createPresetButton(final ColorUi ui, final int which, final int index) {
        Button swatch = new Button(this);
        swatch.setTextSize(11);
        swatch.setAllCaps(false);
        swatch.setMinHeight(0);
        swatch.setMinWidth(0);
        swatch.setPadding(0, 0, 0, 0);
        swatch.setOnClickListener(v -> {
            int color = FocusTextStyle.presetColor(index);
            setColorValue(which, color);
            applyColorToUi(ui, color);
            markPending();
        });
        return swatch;
    }

    private void installColorListeners(final ColorUi ui, final int which) {
        if (ui == null) return;
        for (int i = 0; i < ui.channels.length; i++) {
            if (ui.channels[i] == null) continue;
            ui.channels[i].setOnSeekBarChangeListener(new SimpleSeek() {
                @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                    if (!user) return;
                    int color = 0xFF000000 | readChannels(ui);
                    setColorValue(which, color);
                    applyColorToUi(ui, color);
                    markPending();
                }
            });
        }
    }

    private void applyColorToUi(ColorUi ui, int color) {
        if (ui == null) return;
        ui.color = color;
        int rgb = color == FocusRestoreSettings.NO_FOCUS_TEXT_COLOR ? 0 : (color & 0x00FFFFFF);
        for (int i = 0; i < 3; i++) {
            int value = (rgb >> (16 - i * 8)) & 0xFF;
            if (ui.channels[i] != null) ui.channels[i].setProgress(value);
            if (ui.channelValues[i] != null) ui.channelValues[i].setText(String.valueOf(value));
        }
        if (ui.swatch != null) {
            ui.swatch.setBackground(color == FocusRestoreSettings.NO_FOCUS_TEXT_COLOR
                    ? borderedSwatch() : roundedBg(color, 11));
        }
        updatePresetStates(ui, color);
    }

    private Drawable borderedSwatch() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.TRANSPARENT);
        drawable.setCornerRadius(dp(11));
        drawable.setStroke(Math.max(1, dp(1)), colorTrack);
        int cross = dp(22);
        drawable.setSize(cross, cross);
        return drawable;
    }

    private void updatePresetStates(ColorUi ui, int color) {
        if (ui.presets == null) return;
        for (int i = 0; i < ui.presets.length; i++) {
            Button swatch = ui.presets[i];
            if (swatch == null) continue;
            int preset = FocusTextStyle.presetColor(i);
            boolean selected = preset == color;
            if (preset == FocusRestoreSettings.NO_FOCUS_TEXT_COLOR) {
                swatch.setTextSize(9);
                swatch.setText("跟随");
                swatch.setTextColor(selected ? colorPrimary : colorTextSecondary);
                swatch.setBackground(roundedBg(selected ? colorPrimaryLight
                        : colorInputBackground, 17));
            } else {
                swatch.setText(selected ? "✓" : "");
                swatch.setTextColor(contrastText(preset));
                swatch.setBackground(roundedBg(preset, 17));
            }
        }
    }

    private int readChannels(ColorUi ui) {
        int red = ui.channels[0] != null ? ui.channels[0].getProgress() : 0;
        int green = ui.channels[1] != null ? ui.channels[1].getProgress() : 0;
        int blue = ui.channels[2] != null ? ui.channels[2].getProgress() : 0;
        return (red << 16) | (green << 8) | blue;
    }

    private static int contrastText(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0;
        return luminance > 0.6 ? Color.rgb(32, 33, 36) : Color.WHITE;
    }

    // ---------- whitelist ----------

    private void showForcePackagesDialog() {
        dialogAllApps = readCachedApps();
        dialogVisibleApps.clear();
        dialogSelectedPackages = new HashSet<>(pendingForcePackages);
        dialogAppsLoaded = !dialogAllApps.isEmpty();

        final Dialog dialog = new Dialog(this);
        dialog.setOnDismissListener(d -> clearDialogState());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(createWhitelistDialogView(dialog));
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.82f));
        }
        dialog.show();
        filterDialogApps();
    }

    private View createWhitelistDialogView(final Dialog dialog) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(roundedBg(colorCard, 16));
        root.setPadding(dp(20), dp(20), dp(20), dp(10));
        TextView title = text("强制转换应用", 18, colorTextPrimary);
        title.setTypeface(title.getTypeface(), 1);
        root.addView(title, matchWrap(dp(12)));

        dialogSearchInput = input("搜索应用名称或包名");
        dialogSearchInput.setTextSize(14);
        dialogSearchInput.setMinHeight(dp(48));
        dialogSearchInput.setBackground(roundedBg(colorInputBackground, 12));
        dialogSearchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { filterDialogApps(); }
            public void afterTextChanged(Editable s) { }
        });
        root.addView(dialogSearchInput, matchWrap(dp(6)));

        LinearLayout options = new LinearLayout(this);
        options.setGravity(Gravity.CENTER_VERTICAL);
        dialogShowSystemSwitch = new Switch(this);
        dialogShowSystemSwitch.setText("显示系统应用");
        dialogShowSystemSwitch.setTextSize(14);
        styleSwitch(dialogShowSystemSwitch);
        dialogShowSystemSwitch.setOnCheckedChangeListener((button, checked) -> filterDialogApps());
        options.addView(dialogShowSystemSwitch, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button refresh = new Button(this);
        refresh.setText("加载应用");
        refresh.setAllCaps(false);
        refresh.setTextColor(colorPrimary);
        refresh.setBackground(roundedBg(colorPrimaryLight, 12));
        refresh.setMinHeight(dp(40));
        refresh.setOnClickListener(v -> loadDialogApps());
        options.addView(refresh, new LinearLayout.LayoutParams(dp(88), dp(40)));
        root.addView(options, matchWrap(dp(4)));

        dialogListView = new ListView(this);
        dialogListView.setDivider(null);
        dialogListView.setDividerHeight(0);
        dialogAdapter = new ForcePackageAdapter();
        dialogListView.setAdapter(dialogAdapter);
        dialogListView.setVisibility(View.GONE);
        dialogListView.setOnItemClickListener((parent, view, position, id) -> {
            ApplicationInfo app = dialogVisibleApps.get(position);
            if (!dialogSelectedPackages.add(app.packageName)) dialogSelectedPackages.remove(app.packageName);
            filterDialogApps();
        });
        root.addView(dialogListView, new LinearLayout.LayoutParams(-1, 0, 1f));

        dialogEmptyView = text("尚未加载应用，请点击“加载应用”", 14, colorTextSecondary);
        dialogEmptyView.setGravity(Gravity.CENTER);
        root.addView(dialogEmptyView, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        Button cancel = new Button(this);
        cancel.setText("取消"); cancel.setAllCaps(false); cancel.setTextColor(colorTextSecondary);
        cancel.setBackgroundColor(Color.TRANSPARENT); cancel.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(cancel, new LinearLayout.LayoutParams(dp(76), dp(48)));
        Button done = new Button(this);
        done.setText("完成"); done.setAllCaps(false); done.setTextColor(Color.WHITE);
        done.setBackground(roundedBg(colorPrimary, 12));
        done.setOnClickListener(v -> {
            pendingForcePackages = new HashSet<>(dialogSelectedPackages);
            if (whitelistUpdater != null) whitelistUpdater.run();
            markPending();
            dialog.dismiss();
        });
        buttons.addView(done, new LinearLayout.LayoutParams(dp(76), dp(48)));
        root.addView(buttons, matchWrap(0));
        return root;
    }

    private void clearDialogState() {
        dialogAllApps = new ArrayList<>();
        dialogVisibleApps = new ArrayList<>();
        dialogSelectedPackages = null;
        dialogListView = null;
        dialogAdapter = null;
        dialogSearchInput = null;
        dialogShowSystemSwitch = null;
        dialogEmptyView = null;
        dialogAppsLoaded = false;
    }

    private void loadDialogApps() {
        List<ApplicationInfo> refreshed = new ArrayList<>(getPackageManager().getInstalledApplications(0));
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(FocusRestoreSettings.KEY_ISLAND_APP_CACHE, encodeAppCache(refreshed))
                .apply();
        dialogAllApps = refreshed;
        appLabels.clear();
        for (ApplicationInfo app : refreshed) {
            if (app != null && app.packageName != null) {
                appLabels.put(app.packageName, String.valueOf(app.loadLabel(getPackageManager())));
            }
        }
        dialogAppsLoaded = true;
        filterDialogApps();
    }

    private List<ApplicationInfo> readCachedApps() {
        appLabels.clear();
        String encoded = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(FocusRestoreSettings.KEY_ISLAND_APP_CACHE, "");
        List<ApplicationInfo> result = new ArrayList<>();
        if (encoded.length() == 0) return result;
        for (String record : encoded.split("\\n")) {
            String[] fields = record.split(java.util.regex.Pattern.quote(APP_CACHE_SEPARATOR), -1);
            if (fields.length < 3) continue;
            try {
                ApplicationInfo app = new ApplicationInfo();
                app.packageName = fields[0];
                app.name = fields[1];
                app.flags = Integer.parseInt(fields[2]);
                appLabels.put(app.packageName, fields[1]);
                result.add(app);
            } catch (Throwable ignored) {
            }
        }
        return result;
    }

    private String encodeAppCache(List<ApplicationInfo> apps) {
        StringBuilder result = new StringBuilder();
        for (ApplicationInfo app : apps) {
            if (app == null || app.packageName == null) continue;
            String label = String.valueOf(app.loadLabel(getPackageManager()))
                    .replace(APP_CACHE_SEPARATOR, " ").replace('\n', ' ').replace('\r', ' ');
            if (result.length() > 0) result.append('\n');
            result.append(app.packageName).append(APP_CACHE_SEPARATOR)
                    .append(label).append(APP_CACHE_SEPARATOR).append(app.flags);
        }
        return result.toString();
    }

    private void filterDialogApps() {
        if (!dialogAppsLoaded || dialogAdapter == null) return;
        String query = dialogSearchInput == null ? "" : dialogSearchInput.getText().toString().toLowerCase(Locale.ROOT).trim();
        boolean includeSystem = dialogShowSystemSwitch != null && dialogShowSystemSwitch.isChecked();
        dialogVisibleApps = new ArrayList<>();
        for (ApplicationInfo app : dialogAllApps) {
            boolean system = (app.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            if (!includeSystem && system && !dialogSelectedPackages.contains(app.packageName)) continue;
            String label = appLabels.get(app.packageName);
            if (label == null) {
                label = String.valueOf(app.loadLabel(getPackageManager()));
                appLabels.put(app.packageName, label);
            }
            if (query.length() > 0 && !label.toLowerCase(Locale.ROOT).contains(query)
                    && !app.packageName.toLowerCase(Locale.ROOT).contains(query)) continue;
            dialogVisibleApps.add(app);
        }
        Collections.sort(dialogVisibleApps, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo a, ApplicationInfo b) {
                boolean as = dialogSelectedPackages.contains(a.packageName);
                boolean bs = dialogSelectedPackages.contains(b.packageName);
                if (as != bs) return as ? -1 : 1;
                String aLabel = appLabels.get(a.packageName);
                String bLabel = appLabels.get(b.packageName);
                if (aLabel == null) aLabel = String.valueOf(a.loadLabel(getPackageManager()));
                if (bLabel == null) bLabel = String.valueOf(b.loadLabel(getPackageManager()));
                return aLabel.compareToIgnoreCase(bLabel);
            }
        });
        dialogAdapter.notifyDataSetChanged();
        boolean empty = dialogVisibleApps.isEmpty();
        dialogListView.setVisibility(empty ? View.GONE : View.VISIBLE);
        dialogEmptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) dialogEmptyView.setText(dialogAppsLoaded ? "没有找到匹配的应用" : "尚未加载应用，请点击“加载应用”");
    }

    private final class ForcePackageAdapter extends BaseAdapter {
        public int getCount() { return dialogVisibleApps.size(); }
        public ApplicationInfo getItem(int position) { return dialogVisibleApps.get(position); }
        public long getItemId(int position) { return position; }
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            LinearLayout row;
            TextView name;
            TextView packageName;
            View accent;
            if (convertView instanceof LinearLayout && ((LinearLayout) convertView).getChildCount() == 2) {
                row = (LinearLayout) convertView;
                accent = row.getChildAt(0);
                LinearLayout textBox = (LinearLayout) row.getChildAt(1);
                name = (TextView) textBox.getChildAt(0);
                packageName = (TextView) textBox.getChildAt(1);
            } else {
                row = new LinearLayout(SettingsActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                accent = new View(SettingsActivity.this);
                row.addView(accent, new LinearLayout.LayoutParams(dp(4), -1));
                LinearLayout textBox = new LinearLayout(SettingsActivity.this);
                textBox.setOrientation(LinearLayout.VERTICAL);
                textBox.setPadding(dp(14), dp(8), dp(12), dp(8));
                name = text("", 15, colorTextPrimary);
                name.setTypeface(name.getTypeface(), 1);
                packageName = text("", 12, colorTextSecondary);
                textBox.addView(name, matchWrap(1));
                textBox.addView(packageName, matchWrap(0));
                row.addView(textBox, new LinearLayout.LayoutParams(0, -2, 1f));
            }
            ApplicationInfo app = getItem(position);
            String label = appLabels.get(app.packageName);
            if (label == null) {
                label = String.valueOf(app.loadLabel(getPackageManager()));
                appLabels.put(app.packageName, label);
            }
            name.setText(label);
            packageName.setText(app.packageName);
            boolean selected = dialogSelectedPackages.contains(app.packageName);
            row.setBackground(roundedBg(selected ? colorPrimaryLight : colorCard, 12));
            accent.setBackgroundColor(selected ? colorPrimary : Color.TRANSPARENT);
            return row;
        }
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setMinHeight(dp(48));
        e.setTextSize(15);
        e.setHint(hint);
        e.setTextColor(colorTextPrimary);
        e.setHintTextColor(colorTextSecondary);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackground(roundedBg(colorInputBackground, 12));
        e.setOnFocusChangeListener((v, focus) -> { if (!focus) markPending(); });
        return e;
    }

    private void loadSettings() {
        settings = FocusRestoreSettings.fromPreferences(preferences);
        pendingManual = settings.limitWidth;
        pendingWidthDp = settings.widthDp;
        pendingDelayMs = settings.marqueeDelayMs;
        pendingCompatRetry = settings.compatRetry;
        pendingMarqueeBounce = settings.marqueeBounce;
        pendingIslandCompat = settings.islandCompat;
        pendingDisableIslandProperty = settings.disableIslandProperty;
        pendingDisableIslandFeatureCache = settings.disableIslandFeatureCache;
        pendingHideIconsMode = settings.hideIconsMode;
        pendingFocusVerticalOffsetDp = settings.focusVerticalOffsetDp;
        pendingFontSizeSp = settings.focusFontSizeSp;
        pendingTextColorLight = settings.focusTextColorLight;
        pendingTextColorDark = settings.focusTextColorDark;
        pendingFontFamily = settings.focusFontFamily;
        pendingFontWeight = settings.focusFontWeight;
        pendingFontItalic = settings.focusFontItalic;
        pendingShowAppIcon = settings.showAppIcon;
        pendingAppIconSizeDp = settings.appIconSizeDp;
        pendingFocusMarginStartDp = settings.focusMarginStartDp;
        pendingFocusMarginEndDp = settings.focusMarginEndDp;
        pendingClickOpenMode = settings.clickOpenMode;
        pendingClickDoubleTap = settings.clickDoubleTap;
        pendingFreeformFallbackDirect = settings.freeformFallbackDirect;
        pendingFreeformSizeMode = settings.freeformSizeMode;
        pendingHideOnLockscreen = settings.hideOnLockscreen;
        pendingHideWhenAppOpen = settings.hideWhenAppOpen;
        pendingClockDividerSymbol = settings.clockDividerSymbol;
        pendingClockDividerMarginStartDp = settings.clockDividerMarginStartDp;
        pendingClockDividerMarginEndDp = settings.clockDividerMarginEndDp;
        pendingClockDividerColorLight = settings.clockDividerColorLight;
        pendingClockDividerColorDark = settings.clockDividerColorDark;
        pendingClockDividerVerticalOffsetDp = settings.clockDividerVerticalOffsetDp;
        pendingFocusFadeEnabled = settings.focusFadeEnabled;
        pendingFocusFadeDurationMs = settings.focusFadeDurationMs;
        pendingIconTextGapDp = settings.iconTextGapDp;
        pendingGeneralSeparator = settings.islandGeneralSeparator;
        pendingSideSeparator = settings.islandSideSeparator;
        pendingForcePackages = new HashSet<>(settings.islandForcePackages);
    }

    private void saveSettings() {
        settings = FocusRestoreSettings.withValues(
                FocusRestoreSettings.HOOK_MODE_OS4,
                pendingManual, pendingWidthDp, pendingDelayMs,
                pendingCompatRetry, pendingMarqueeBounce, pendingIslandCompat,
                pendingDisableIslandProperty, pendingDisableIslandFeatureCache,
                pendingHideIconsMode, pendingFocusVerticalOffsetDp,
                pendingFontSizeSp, pendingTextColorLight, pendingTextColorDark,
                pendingFontFamily, pendingFontWeight, pendingFontItalic,
                pendingShowAppIcon, pendingAppIconSizeDp,
                pendingFocusMarginStartDp, pendingFocusMarginEndDp, pendingIconTextGapDp,
                pendingClickOpenMode, pendingClickDoubleTap,
                pendingFreeformFallbackDirect, pendingFreeformSizeMode,
                pendingHideOnLockscreen, pendingHideWhenAppOpen, pendingClockDividerSymbol,
                pendingClockDividerMarginStartDp, pendingClockDividerMarginEndDp,
                pendingClockDividerColorLight, pendingClockDividerColorDark,
                pendingClockDividerVerticalOffsetDp, pendingFocusFadeEnabled,
                pendingFocusFadeDurationMs,
                pendingGeneralSeparator, pendingSideSeparator, pendingForcePackages,
                // The legacy View UI does not edit the icon/text vertical offsets;
                // carry the stored values over so switching screens never resets them.
                settings.focusIconVerticalOffsetDp, settings.focusTextVerticalOffsetDp);
        boolean credentialSaved = settings.save(preferences);
        boolean hookSaved = settings.save(FocusRestoreSettings.hookPreferences(this));
        android.util.Log.i(TAG, "settings saved credential=" + credentialSaved
                + " deviceProtected=" + hookSaved + " " + settings.describe());
    }

    /**
     * Every control change lands here. The write is coalesced and silent: rapid
     * slider drags collapse into a single write a moment after the user stops.
     */
    private void markPending() {
        unsavedChanges = true;
        saveHandler.removeCallbacks(saveTask);
        saveHandler.postDelayed(saveTask, AUTO_SAVE_DELAY_MS);
    }

    /** Writes pending changes right away (debounce flush / leaving the screen). */
    private void persistNow() {
        saveHandler.removeCallbacks(saveTask);
        if (!unsavedChanges) return;
        unsavedChanges = false;
        saveSettings();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ---------- SystemUI restart (needs root) ----------

    private void showRestartSystemUiDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(createRestartDialogView(dialog));
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.86f),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private View createRestartDialogView(final Dialog dialog) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(roundedBg(colorCard, 16));
        root.setPadding(dp(20), dp(20), dp(20), dp(8));

        TextView title = text("重启系统界面", 18, colorTextPrimary);
        title.setTypeface(title.getTypeface(), 1);
        root.addView(title, matchWrap(dp(10)));

        TextView message = text("重启系统界面（SystemUI）后，刚调整的设置会立刻生效，"
                + "不需要重启手机。\n\n重启期间状态栏和通知栏会短暂消失后自动恢复，"
                + "属正常现象。\n\n此操作需要 root 权限：确认后本应用会通过 su 结束"
                + "系统界面进程，若尚未授权，请在系统弹出的授权窗口中允许。",
                13, colorTextSecondary);
        message.setLineSpacing(dp(2), 1f);
        root.addView(message, matchWrap(dp(6)));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        Button cancel = new Button(this);
        cancel.setText("取消");
        cancel.setAllCaps(false);
        cancel.setTextColor(colorTextSecondary);
        cancel.setBackgroundColor(Color.TRANSPARENT);
        cancel.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(cancel, new LinearLayout.LayoutParams(-2, dp(44)));
        Button confirm = new Button(this);
        confirm.setText("重启");
        confirm.setAllCaps(false);
        confirm.setTextColor(colorPrimary);
        confirm.setTypeface(confirm.getTypeface(), 1);
        confirm.setBackground(roundedBg(colorPrimaryLight, 12));
        confirm.setPadding(dp(20), 0, dp(20), 0);
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            restartSystemUi();
        });
        buttons.addView(confirm, new LinearLayout.LayoutParams(-2, dp(44)));
        root.addView(buttons, matchWrap(0));
        return root;
    }

    private void restartSystemUi() {
        toast("正在请求 root 权限，请在系统授权窗口中点击「允许」…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String outcome = performSystemUiRestart();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showMessageDialog("重启系统界面", outcome);
                    }
                });
            }
        }, "systemui-restart").start();
    }

    /** Simple card dialog used for the restart outcome (e.g. missing root). */
    private void showMessageDialog(String titleText, String messageText) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(roundedBg(colorCard, 16));
        root.setPadding(dp(20), dp(20), dp(20), dp(8));

        TextView heading = text(titleText, 18, colorTextPrimary);
        heading.setTypeface(heading.getTypeface(), 1);
        root.addView(heading, matchWrap(dp(10)));

        TextView body = text(messageText, 13, colorTextSecondary);
        body.setLineSpacing(dp(2), 1f);
        root.addView(body, matchWrap(dp(6)));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        Button confirm = new Button(this);
        confirm.setText("知道了");
        confirm.setAllCaps(false);
        confirm.setTextColor(colorPrimary);
        confirm.setTypeface(confirm.getTypeface(), 1);
        confirm.setBackground(roundedBg(colorPrimaryLight, 12));
        confirm.setPadding(dp(20), 0, dp(20), 0);
        confirm.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(confirm, new LinearLayout.LayoutParams(-2, dp(44)));
        root.addView(buttons, matchWrap(0));

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.86f),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    /** Runs on a worker thread; every failure mode returns a user facing message. */
    private String performSystemUiRestart() {
        ShellResult identity = runSuCommand("id", ROOT_PROBE_TIMEOUT_MS);
        if (identity.launchFailed) {
            return "未检测到 su 命令，设备可能没有 root 权限，请手动重启系统界面或手机。";
        }
        if (identity.exitCode != 0 || !identity.output.contains("uid=0")) {
            return "请为该应用授予 root 权限，然后再试一次。";
        }
        for (String command : RESTART_SYSTEMUI_COMMANDS) {
            ShellResult result = runSuCommand(command, ROOT_COMMAND_TIMEOUT_MS);
            if (!result.launchFailed && result.exitCode == 0) {
                android.util.Log.i(TAG, "SystemUI restart requested via: " + command);
                return "已发送重启指令，系统界面正在重启。";
            }
        }
        return "重启失败：已获得 root 权限，但仍无法结束系统界面进程，请手动重启。";
    }

    private static ShellResult runSuCommand(String command, long timeoutMs) {
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();
        } catch (Throwable error) {
            if (process != null) process.destroy();
            return new ShellResult(true, -1, "");
        }
        try {
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroy();
                return new ShellResult(false, -1, "");
            }
            return new ShellResult(false, process.exitValue(), readProcessOutput(process));
        } catch (Throwable error) {
            process.destroy();
            return new ShellResult(false, -1, "");
        }
    }

    private static String readProcessOutput(Process process) {
        StringBuilder builder = new StringBuilder();
        InputStream stream = process.getInputStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) builder.append('\n');
                builder.append(line);
            }
        } catch (Throwable ignored) {
            // Output is only used for the uid=0 probe; a read failure means "no root".
        }
        return builder.toString();
    }

    private static final class ShellResult {
        final boolean launchFailed;
        final int exitCode;
        final String output;

        ShellResult(boolean launchFailed, int exitCode, String output) {
            this.launchFailed = launchFailed;
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    private Drawable roundedBg(int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp((int) radiusDp));
        return drawable;
    }

    /**
     * Adds the status bar inset to the view's own padding so its background
     * (the card colour) is painted all the way up under the status bar.
     */
    private void applyTopInsets(View view) {
        final int left = view.getPaddingLeft();
        final int top = view.getPaddingTop();
        final int right = view.getPaddingRight();
        final int bottom = view.getPaddingBottom();
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(left, top + statusBarInset(insets), right, bottom);
            return insets;
        });
        view.requestApplyInsets();
    }

    /** Same idea for the fixed bottom bar and the navigation / gesture inset. */
    private void applyBottomInsets(View view) {
        final int left = view.getPaddingLeft();
        final int top = view.getPaddingTop();
        final int right = view.getPaddingRight();
        final int bottom = view.getPaddingBottom();
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(left, top, right, bottom + navigationBarInset(insets));
            return insets;
        });
        view.requestApplyInsets();
    }

    @SuppressWarnings("deprecation")
    private static int statusBarInset(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= 30) {
            return insets.getInsets(WindowInsets.Type.statusBars()).top;
        }
        return insets.getSystemWindowInsetTop();
    }

    @SuppressWarnings("deprecation")
    private static int navigationBarInset(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= 30) {
            return insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
        }
        int bottom = insets.getSystemWindowInsetBottom();
        if (Build.VERSION.SDK_INT >= 29) {
            bottom = Math.max(bottom, insets.getSystemGestureInsets().bottom);
        }
        return bottom;
    }

    /**
     * Miuix switch: a 44x26dp pill with a flat white 22dp thumb inset by 2dp,
     * primary when on and the "secondary" token when off.
     */
    private void styleSwitch(Switch s) {
        android.graphics.drawable.GradientDrawable on =
                new android.graphics.drawable.GradientDrawable();
        on.setColor(colorPrimary);
        on.setCornerRadius(dp(13));
        on.setSize(dp(44), dp(26));
        android.graphics.drawable.GradientDrawable off =
                new android.graphics.drawable.GradientDrawable();
        off.setColor(colorTrack);
        off.setCornerRadius(dp(13));
        off.setSize(dp(44), dp(26));
        android.graphics.drawable.StateListDrawable track =
                new android.graphics.drawable.StateListDrawable();
        track.addState(new int[]{android.R.attr.state_checked}, on);
        track.addState(new int[]{}, off);
        android.graphics.drawable.GradientDrawable dot =
                new android.graphics.drawable.GradientDrawable();
        dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        dot.setColor(Color.WHITE);
        dot.setSize(dp(22), dp(22));
        s.setThumbDrawable(new android.graphics.drawable.InsetDrawable(
                dot, dp(2), dp(2), dp(2), dp(2)));
        s.setTrackDrawable(track);
        s.setShowText(false);
        s.setSwitchMinWidth(dp(44));
        s.setSwitchPadding(0);
        s.setPadding(0, 0, 0, 0);
        if (Build.VERSION.SDK_INT >= 21) {
            s.setThumbTintList(null);
            s.setTrackTintList(null);
        }
    }

    private void styleSeekBar(SeekBar s) {
        if (Build.VERSION.SDK_INT >= 21) {
            s.setProgressTintList(ColorStateList.valueOf(colorPrimary));
            s.setThumbTintList(ColorStateList.valueOf(colorPrimary));
            s.setProgressBackgroundTintList(ColorStateList.valueOf(colorSliderTrack));
        }
    }

    private void openExternalLink(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            toast("设备没有可用的浏览器，无法打开链接。");
        }
    }

    /**
     * Edge to edge: transparent system bars so the screen background and the top
     * / bottom card surfaces run right up to the screen edges, with the icon
     * colour picked to stay readable on top of them.
     */
    @SuppressWarnings("deprecation")
    private void configureSystemBars(Window w) {
        // Creating the decor view up front keeps every insets / appearance call
        // below safe on MIUI, where the Window level helpers assume the decor
        // view already exists.
        View decor = w.getDecorView();
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                w.setDecorFitsSystemWindows(false);
            } else {
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            }
            // setStatusBarColor is a no op on API 35+ where edge to edge is enforced.
            if (Build.VERSION.SDK_INT < 35) {
                w.setStatusBarColor(Color.TRANSPARENT);
                w.setNavigationBarColor(Color.TRANSPARENT);
            }
            if (Build.VERSION.SDK_INT >= 29) {
                w.setStatusBarContrastEnforced(false);
                w.setNavigationBarContrastEnforced(false);
            }
            applySystemBarIconAppearance(decor);
        } catch (Throwable t) {
            // Never let a ROM quirk in the system bar setup take the activity
            // down - edge to edge is cosmetic, the settings screen is not.
            android.util.Log.w(TAG, "system bar configuration failed, using defaults", t);
        }
        decor.requestApplyInsets();
    }

    /** Picks light or dark status / navigation bar icons for the active theme. */
    @SuppressWarnings("deprecation")
    private void applySystemBarIconAppearance(View decor) {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller == null) return;
            int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            controller.setSystemBarsAppearance(darkTheme ? 0 : mask, mask);
        } else {
            int flags = decor.getSystemUiVisibility();
            if (!darkTheme) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                flags &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }
            decor.setSystemUiVisibility(flags);
        }
    }

    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private LinearLayout.LayoutParams matchWrap(int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = margin;
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
