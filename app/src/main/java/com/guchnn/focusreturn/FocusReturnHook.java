/*
 * HyperOS3FocusRestore - Copyright (C) ImKani.
 * Modifications Copyright (C) 2026 Guchnn <https://github.com/Guchnn>.
 * This copy is a derivative work maintained by Guchnn; see MODIFICATIONS.md.
 * Licensed under the GNU General Public License v3.0 only (GPL-3.0-only).
 *
 * Modified 2026-09-18: applies the custom focus text style to the focus
 * content view after setData and before the marquee starts.
 * Modified 2026-09-19: tracks the status bar background scene through a
 * DarkIconDispatcher receiver and re-applies the scene colour when it flips.
 * See MODIFICATIONS.md.
 *
 * This file is part of a derivative work distributed under GPL-3.0-only.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package com.guchnn.focusreturn;

import android.animation.ValueAnimator;
import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.database.Cursor;
import android.graphics.Rect;
import org.json.JSONObject;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class FocusReturnHook implements IXposedHookLoadPackage {
    private static final String TAG = "FocusReturn";
    private static final String SYSTEM_UI = "com.android.systemui";

    // OS3 rejects the legacy miui.focus.rv when used as contentRemoteViews.
    private static final boolean FALLBACK_MAIN_RV_FOR_STATUS_BAR = false;
    // Let HyperOS own the prompt lifecycle; forcing true leaves stale icons after clicks.
    private static final boolean FORCE_SHOULD_SHOW = false;

    private static final long SETTINGS_REFRESH_INTERVAL_MS = 1000L;
    private static final long CONVERTED_KEY_TTL_MS = 10L * 60L * 1000L;
    private static final int MAX_CONVERTED_KEYS = 128;

    private static final Set<ClassLoader> INSTALLED_CLASS_LOADERS =
            Collections.newSetFromMap(new WeakHashMap<ClassLoader, Boolean>());
    private static final Object INSTALL_LOCK = new Object();
    private static final Object SETTINGS_READ_LOCK = new Object();
    private static final ExecutorService SETTINGS_EXECUTOR =
            Executors.newSingleThreadExecutor(command -> {
                Thread thread = new Thread(command, TAG + "-settings");
                thread.setDaemon(true);
                return thread;
            });

    private ClassLoader classLoader;
    private volatile Context systemUiContext;
    // FocusedTextView.startMarqueeLocal() copies this value into TextView.
    // -1 keeps long lyrics moving instead of stopping after one pass.
    private static final int MARQUEE_REPEAT_LIMIT = -1;
    private static final int STYLE_REAPPLY_DELAY_MS = 150;
    private volatile HookSettings currentSettings = HookSettings.defaults();
    private long lastProviderReadAttemptMs = Long.MIN_VALUE;
    private long settingsReadGeneration;
    private boolean settingsReadQueued;
    private boolean hasSuccessfulProviderSettings;
    private int providerSettingsState = Integer.MIN_VALUE;
    private boolean modeHooksInstalled;
    private int installedHookMode;
    private HyperOS4FocusController os4Controller;
    private Object sceneDarkReceiver;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        // The android (system_server) scope hosts the freeform resize-compat
        // fix; everything else is SystemUI only.
        if ("android".equals(lpparam.packageName)) {
            installSystemServerHooks(lpparam);
            return;
        }
        if (!SYSTEM_UI.equals(lpparam.packageName)
                || !SYSTEM_UI.equals(lpparam.processName)) {
            return;
        }

        synchronized (INSTALL_LOCK) {
            if (!INSTALLED_CLASS_LOADERS.add(lpparam.classLoader)) {
                return;
            }
        }
        classLoader = lpparam.classLoader;
        hookApplicationAttach();
        hookDynamicIslandSystemProperty();
        disableDynamicIslandFeatureCache();
        log("loading in " + lpparam.packageName + "/" + lpparam.processName
                + " awaiting persisted hook mode; default=OS"
                + FocusRestoreSettings.DEFAULT_HOOK_MODE);
    }

    /**
     * system_server only: apps that do not declare resizeableActivity are put
     * into size-compat mode inside a freeform task, which renders them at their
     * full-screen size scaled down - text overlaps and the layout is wrong.
     * MIUI's own small windows relayout the app natively, so this hook reports
     * size-change support for freeform tasks to match that behaviour.
     * Requires the LSPosed scope to include 系统框架 (android).
     */
    private void installSystemServerHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        synchronized (INSTALL_LOCK) {
            if (!INSTALLED_CLASS_LOADERS.add(lpparam.classLoader)) {
                return;
            }
        }
        try {
            Class<?> recordClass = XposedHelpers.findClass(
                    "android.app.ActivityRecord", lpparam.classLoader);
            XposedBridge.hookAllMethods(recordClass, "supportsSizeChanges", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        if (Boolean.TRUE.equals(param.getResult())) return;
                        Object task = XposedHelpers.getObjectField(param.thisObject, "task");
                        if (task == null) return;
                        Object mode = XposedHelpers.callMethod(task, "getWindowingMode");
                        // WindowConfiguration.WINDOWING_MODE_FREEFORM == 5
                        if (mode instanceof Integer && (Integer) mode == 5) {
                            param.setResult(Boolean.TRUE);
                        }
                    } catch (Throwable ignored) {
                        // Never let anything propagate into system_server.
                    }
                }
            });
            Log.i(TAG, "systemServer supportsSizeChanges hook installed (freeform only)");
        } catch (Throwable throwable) {
            Log.e(TAG, "systemServer hook failed", throwable);
        }
    }

    private void logCapabilities() {
        Class<?> focusUtils = FocusReflection.findClass(classLoader,
                "com.android.systemui.statusbar.notification.utils.FocusUtils");
        Class<?> pipeline = FocusReflection.findClass(classLoader,
                "com.android.systemui.statusbar.notification.collection.NotifPipeline");
        Class<?> statusBar = FocusReflection.findClass(classLoader,
                "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView");
        log("capabilities OS4 "
                + FocusReflection.capability(focusUtils, "showOnStatusBar")
                + " " + FocusReflection.capability(pipeline, "addCollectionListener")
                + " " + FocusReflection.capability(statusBar, "onFinishInflate"));
    }

    private synchronized void installConfiguredModeHooks() {
        if (modeHooksInstalled) {
            log("mode hooks already installed installedMode=OS" + installedHookMode);
            return;
        }
        installedHookMode = FocusRestoreSettings.HOOK_MODE_OS4;
        modeHooksInstalled = true;
        log("installing OS4 settings=" + currentSettings.describe());
        installOS4Hooks();
    }


    /**
     * Tracks the real background behind the status bar so focus text can switch
     * between the light-scene and dark-scene colour. The signal is the status
     * bar tint published by {@code DarkIconDispatcher}, not the system night
     * mode: a focus notification can sit on a light background while the system
     * is in dark mode, and vice versa.
     */
    private void hookSceneTint() {
        try {
            Class<?> dispatcherClass = FocusReflection.findClass(classLoader,
                    "com.android.systemui.plugins.DarkIconDispatcher");
            Class<?> receiverClass = FocusReflection.findClass(classLoader,
                    "com.android.systemui.plugins.DarkIconDispatcher$DarkReceiver");
            Class<?> dependencyClass = FocusReflection.findClass(classLoader,
                    "com.android.systemui.Dependency");
            if (dispatcherClass == null || receiverClass == null || dependencyClass == null) {
                log("sceneTint=unsupported (dispatcher classes missing)");
                return;
            }
            Object dispatcher = XposedHelpers.callStaticMethod(
                    dependencyClass, "get", dispatcherClass);
            if (dispatcher == null) {
                log("sceneTint=unsupported (dispatcher unavailable)");
                return;
            }
            InvocationHandler handler = (proxy, method, args) -> {
                String name = method.getName();
                if (("onDarkChanged".equals(name)
                        || "onDarkChangedWithContrast".equals(name))
                        && args != null && args.length >= 3
                        && args[2] instanceof Integer) {
                    FocusTextStyle.publishStatusBarTint((Integer) args[2]);
                } else if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                } else if ("equals".equals(name)) {
                    return args != null && args.length == 1 && proxy == args[0];
                } else if ("toString".equals(name)) {
                    return "FocusRestoreSceneReceiver";
                }
                return null;
            };
            Object receiver = Proxy.newProxyInstance(classLoader,
                    new Class<?>[]{receiverClass}, handler);
            XposedHelpers.callMethod(dispatcher, "addDarkReceiver", receiver);
            sceneDarkReceiver = receiver;
            try {
                XposedHelpers.callMethod(dispatcher, "applyDark", receiver);
            } catch (Throwable throwable) {
                error("sceneTint.applyDark", throwable);
            }
            log("sceneTint=registered");
        } catch (Throwable throwable) {
            // Non fatal: FocusTextStyle still falls back to the clock tint.
            error("hookSceneTint", throwable);
        }
    }


    private void installOS4Hooks() {
        Context context = systemUiContext;
        if (context == null) {
            error("installOS4Hooks", new IllegalStateException("SystemUI context unavailable"));
            return;
        }
        os4Controller = new HyperOS4FocusController(classLoader, context,
                new HyperOS4FocusController.ItemFactory() {
                    @Override
                    public HyperOS4FocusController.DisplayItem create(Object notificationEntry) {
                        return createOS4DisplayItem(notificationEntry);
                    }

                    @Override
                    public HookSettings settings() {
                        return currentSettings;
                    }
                }, new HyperOS4FocusController.Logger() {
                    @Override
                    public void log(String message) {
                        FocusReturnHook.this.log(message);
                    }

                    @Override
                    public void error(String stage, Throwable throwable) {
                        FocusReturnHook.this.error(stage, throwable);
                    }
                });
        os4Controller.install();
        log("installedMode=OS4");
    }

    private void hookApplicationAttach() {
        try {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.args[0] instanceof Context) {
                                Context attachedContext = (Context) param.args[0];
                                Context applicationContext = attachedContext.getApplicationContext();
                                systemUiContext = applicationContext != null
                                        ? applicationContext : attachedContext;
                                log("SystemUI attach context available class="
                                        + systemUiContext.getClass().getName()
                                        + " applicationContext=" + (applicationContext != null)
                                        + "; loading persisted hook mode");
                                if (reloadSettings(true)) {
                                    installConfiguredModeHooks();
                                } else {
                                    log("mode hooks not installed: persisted settings unavailable; "
                                            + "restart SystemUI or device after settings storage is available");
                                }
                            }
                        }
                    });
        } catch (Throwable t) {
            error("hookApplicationAttach", t);
        }
    }

    private void hookDynamicIslandSystemProperty() {
        try {
            XposedHelpers.findAndHookMethod(
                    "android.os.SystemProperties",
                    classLoader,
                    "getBoolean",
                    String.class,
                    boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if ((!com.guchnn.focusreturn.BuildConfig.DEBUG || currentSettings.disableIslandProperty)
                                    && "feature.island.debug".equals(param.args[0])) {
                                param.setResult(false);
                                log("Dynamic Island property override: feature.island.debug=false");
                            }
                        }
                    });
        } catch (Throwable t) {
            error("hookDynamicIslandSystemProperty", t);
        }
    }

    private void disableDynamicIslandFeatureCache() {
        if (com.guchnn.focusreturn.BuildConfig.DEBUG && !currentSettings.disableIslandFeatureCache) return;
        try {
            Class<?> config = FocusReflection.findClass(
                    "com.android.systemui.statusbar.notification.DynamicFeatureConfig",
                    classLoader);
            XposedHelpers.setStaticBooleanField(config, "FEATURE_DYNAMIC_ISLAND", false);
            log("Dynamic Island feature cache disabled: FEATURE_DYNAMIC_ISLAND=false");
        } catch (Throwable t) {
            // The property hook still covers initialization if this class is not loaded yet.
            error("set FEATURE_DYNAMIC_ISLAND", t);
        }
    }

    private synchronized boolean reloadSettings(boolean force) {
        long now = SystemClock.elapsedRealtime();
        if (!force && lastProviderReadAttemptMs != Long.MIN_VALUE
                && now - lastProviderReadAttemptMs < SETTINGS_REFRESH_INTERVAL_MS) {
            return hasSuccessfulProviderSettings;
        }
        lastProviderReadAttemptMs = now;
        if (!force && settingsReadQueued) return hasSuccessfulProviderSettings;
        final long generation = ++settingsReadGeneration;
        if (force) {
            synchronized (SETTINGS_READ_LOCK) {
                return readProviderSettings(generation);
            }
        }
        settingsReadQueued = true;
        SETTINGS_EXECUTOR.execute(() -> {
            try {
                synchronized (SETTINGS_READ_LOCK) {
                    readProviderSettings(generation);
                }
            } finally {
                synchronized (FocusReturnHook.this) {
                    settingsReadQueued = false;
                }
            }
        });
        return hasSuccessfulProviderSettings;
    }

    private boolean readProviderSettings(long generation) {
        try {
            Context context = systemUiContext;
            if (context == null) {
                Object currentApplication = XposedHelpers.callStaticMethod(
                        Class.forName("android.app.ActivityThread"), "currentApplication");
                if (currentApplication instanceof Context) {
                    Context application = (Context) currentApplication;
                    Context applicationContext = application.getApplicationContext();
                    context = applicationContext != null ? applicationContext : application;
                    systemUiContext = context;
                }
            }
            if (context == null) {
                logProviderSettingsState(false, "application context unavailable");
                return false;
            }
            HookSettings next = HookSettingsReader.read(context);
            if (next == null) {
                logProviderSettingsState(false, "provider query returned no settings");
                return false;
            }
            synchronized (this) {
                if (generation != settingsReadGeneration) return false;
                currentSettings = next;
            }
            hasSuccessfulProviderSettings = true;
            logProviderSettingsState(true, null);
            return true;
        } catch (Throwable t) {
            logProviderSettingsState(false, t.getClass().getSimpleName());
            error("readProviderSettings", t);
            return false;
        }
    }

    private void logProviderSettingsState(boolean available, String reason) {
        int state = available ? 1 : hasSuccessfulProviderSettings ? 0 : -1;
        if (providerSettingsState == state) return;
        providerSettingsState = state;
        if (available) {
            log("provider settings updated: " + currentSettings.describe());
        } else {
            log("provider settings unavailable: "
                    + (hasSuccessfulProviderSettings ? "keeping cached settings" : "using defaults")
                    + (TextUtils.isEmpty(reason) ? "" : " (" + reason + ")"));
        }
    }

    private boolean shouldConvert(FocusData data) {
        if (data == null || !data.hasIslandParam || !currentSettings.islandCompat) return false;
        return !data.isOriginalFocus
                || currentSettings.islandForcePackages.contains(data.packageName)
                || isSmsVerificationCode(data);
    }

    private boolean isSmsVerificationCode(FocusData data) {
        if (data == null || !"com.android.mms".equals(data.packageName)
                || TextUtils.isEmpty(data.islandParam)
                || data.islandParam.length() > 256 * 1024) return false;
        try {
            JSONObject root = new JSONObject(data.islandParam);
            return root.optInt("protocol", -1) == 1
                    && "verifyCode".equals(root.optString("scene"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean hasConvertibleIslandContent(FocusData data) {
        if (!currentSettings.islandCompat || data == null || !data.hasIslandParam) return false;
        IslandText text = extractIslandContent(data);
        return text != null && !TextUtils.isEmpty(text.text);
    }

    private IslandText extractIslandContent(FocusData data) {
        if (data == null || TextUtils.isEmpty(data.islandParam)) return null;
        IslandPayloadParser.ParsedText parsed = IslandPayloadParser.parse(
                data.islandParam, currentSettings.generalSeparator, currentSettings.sideSeparator);
        return parsed == null ? null : new IslandText(parsed.text, parsed.source);
    }

    @SuppressWarnings("unused")
    private IslandText extractIslandContentLegacy(FocusData data) {
        if (data == null || TextUtils.isEmpty(data.islandParam)) return null;
        // A notification that already has focus data must use that data as-is.
        if (data.hasExplicitFocusData && !data.hasIslandParam) {
            log("island conversion skipped because explicit focus data already exists");
            return null;
        }
        try {
            JSONObject root = new JSONObject(data.islandParam);
            JSONObject v2 = root.optJSONObject("param_v2");
            if (v2 == null) v2 = root;

            // Older HyperOS focus payloads (notably SMS verification) use protocol 1.
            if (root.optInt("protocol", 3) == 1 || "verifyCode".equals(root.optString("scene"))) {
                String legacy = joinTexts(root, "protocol1", "title", "desc1", "desc2");
                if (!TextUtils.isEmpty(legacy)) {
                    log("island content source=protocol1:" + root.optString("scene", "legacy")
                            + " text=" + legacy);
                    return new IslandText(legacy, "protocol1:" + root.optString("scene", "legacy"));
                }
                return null;
            }

            JSONObject base = v2.optJSONObject("baseInfo");
            JSONObject highlight = v2.optJSONObject("highlightInfo");
            JSONObject highlightV3 = v2.optJSONObject("highlightInfoV3");
            JSONObject chat = v2.optJSONObject("chatInfo");
            JSONObject hint = v2.optJSONObject("hintInfo");

            String source = null;
            String result = joinTexts(base, "title", "subTitle", "specialTitle",
                    "extraTitle", "content", "subContent");
            if (base != null && !TextUtils.isEmpty(result)) {
                // Some HyperOS 3.0.5 builds drop BaseInfo.title while retaining
                // subTitle/content. The same primary title remains in the island
                // imageTextInfo payload, so restore it before displaying the focus text.
                String islandTitle = findPrimaryIslandTitle(v2.optJSONObject("param_island"));
                if (!TextUtils.isEmpty(islandTitle) && !result.startsWith(islandTitle)) {
                    result = joinText(islandTitle, result);
                    log("restored missing baseInfo title from param_island=" + islandTitle);
                }
                String islandExtra = findIslandText(v2.optJSONObject("param_island"));
                String hintExtra = joinTexts(hint, "hintInfo", "title", "content", "subContent");
                result = appendDistinctText(result, hintExtra);
                if (!TextUtils.isEmpty(hintExtra)) {
                    log("merged hintInfo content into baseInfo");
                }
                if (!TextUtils.isEmpty(islandExtra)) {
                    String merged = appendDistinctText(result, islandExtra);
                    if (!TextUtils.equals(result, merged)) {
                        result = merged;
                        log("merged additional param_island content into baseInfo");
                    }
                }
            }
            if (!TextUtils.isEmpty(result)) source = "baseInfo";
            if (TextUtils.isEmpty(result)) {
                result = joinTexts(highlight, "title", "content", "subContent");
                if (!TextUtils.isEmpty(result)) source = "highlightInfo";
            }
            if (TextUtils.isEmpty(result)) {
                result = joinTexts(highlightV3, "primaryText", "secondaryText", "highLightText",
                        "label");
                if (!TextUtils.isEmpty(result)) source = "highlightInfoV3";
            }
            if (TextUtils.isEmpty(result)) {
                result = joinTexts(chat, "title", "content");
                if (!TextUtils.isEmpty(result)) source = "chatInfo";
            }
            if (TextUtils.isEmpty(result)) {
                JSONObject iconText = v2.optJSONObject("iconTextInfo");
                result = joinCompact(firstText(iconText, "title"), firstText(iconText, "content"));
                result = joinCompact(result, firstText(iconText, "subContent"));
                if (!TextUtils.isEmpty(result)) source = "iconTextInfo";
            }
            if (TextUtils.isEmpty(result)) {
                result = joinTexts(v2.optJSONObject("animTextInfo"), "title", "content");
                if (!TextUtils.isEmpty(result)) source = "animTextInfo";
            }
            if (TextUtils.isEmpty(result)) {
                result = joinTexts(v2.optJSONObject("coverInfo"), "title", "content", "subContent");
                if (!TextUtils.isEmpty(result)) source = "coverInfo";
            }
            if (TextUtils.isEmpty(result)) {
                result = joinTexts(hint, "title", "subTitle", "content", "subContent");
                if (!TextUtils.isEmpty(result)) {
                    String aodTitle = firstText(v2, "aodTitle");
                    if (!TextUtils.isEmpty(aodTitle)) {
                        result = joinText(aodTitle, result);
                    } else {
                        String ticker = cleanText(v2.optString("ticker", null));
                        if (!TextUtils.isEmpty(ticker)) result = ticker;
                    }
                    source = "hintInfo";
                }
            }
            if (TextUtils.isEmpty(result)) {
                JSONObject multiProgress = v2.optJSONObject("multiProgressInfo");
                result = progressText(multiProgress);
                if (!TextUtils.isEmpty(result)) source = "multiProgressInfo";
            }
            if (TextUtils.isEmpty(result)) {
                result = progressText(v2.optJSONObject("progressInfo"));
                if (!TextUtils.isEmpty(result)) source = "progressInfo";
            }
            if (TextUtils.isEmpty(result)) {
                result = joinTexts(v2.optJSONObject("stepInfo"), "title", "content", "subContent", "step");
                if (!TextUtils.isEmpty(result)) source = "stepInfo";
            }
            if (TextUtils.isEmpty(result)) {
                JSONObject island = v2.optJSONObject("param_island");
                result = findIslandText(island);
                if (!TextUtils.isEmpty(result)) source = "param_island";
            }
            if (TextUtils.isEmpty(result)) {
                result = cleanText(v2.optString("ticker", null));
                if (!TextUtils.isEmpty(result)) source = "ticker";
            }
            if (TextUtils.isEmpty(result) && v2 != root) {
                result = cleanText(root.optString("ticker", null));
                if (!TextUtils.isEmpty(result)) source = "custom.ticker";
            }
            if (TextUtils.isEmpty(result)) return null;
            log("island content source=" + source + " text=" + result);
            return new IslandText(result, source);
        } catch (Throwable t) {
            log("island param parse failed");
            return null;
        }
    }

    private String joinTexts(JSONObject object, String ignoredSource, String... keys) {
        if (object == null) return null;
        String result = null;
        for (String key : keys) result = joinText(result, firstText(object, key));
        return result;
    }

    private String progressText(JSONObject object) {
        if (object == null) return null;
        String result = joinText(firstText(object, "title", "content", "label"),
                percentageText(object));
        if (!TextUtils.isEmpty(result)) return result;
        JSONObject nested = object.optJSONObject("progressInfo");
        return nested == null ? null : joinText(firstText(nested, "title", "content", "label"),
                percentageText(nested));
    }

    private static String percentageText(JSONObject object) {
        if (object == null || !object.has("progress")) return null;
        Object value = object.opt("progress");
        if (value == null || value == JSONObject.NULL) return null;
        String text = cleanText(String.valueOf(value));
        return TextUtils.isEmpty(text) ? null : (text.endsWith("%") ? text : text + "%");
    }

    private static String sourceFor(JSONObject v2, String result) {
        if (v2.has("baseInfo")) return "baseInfo";
        if (v2.has("highlightInfo")) return "highlightInfo";
        if (v2.has("chatInfo")) return "chatInfo";
        if (v2.has("hintInfo")) return "hintInfo";
        if (v2.has("multiProgressInfo")) return "multiProgressInfo";
        if (v2.has("param_island")) return "param_island";
        return "ticker";
    }

    private String findIslandText(JSONObject island) {
        if (island == null) return null;
        String result = null;
        result = appendDistinctText(result, joinTexts(island, "param_island", "title", "content", "frontTitle"));

        JSONObject big = island.optJSONObject("bigIslandArea");
        JSONObject left = big == null ? null : big.optJSONObject("imageTextInfoLeft");
        JSONObject text = left == null ? null : firstObject(left, "textInfo", "miui.focus.paramtextInfo");
        String leftText = joinTexts(text, "imageTextInfoLeft", "frontTitle", "title", "content", "subContent");

        // BigIslandArea is explicitly a two-sided payload. The side separator is
        // reserved for the boundary between the left and right areas.
        JSONObject right = big == null ? null : big.optJSONObject("imageTextInfoRight");
        text = right == null ? null : firstObject(right, "textInfo", "miui.focus.paramtextInfo");
        String rightText = joinTexts(text, "imageTextInfoRight", "frontTitle", "title", "content", "subContent");
        String sideText = appendSideText(leftText, rightText);
        result = appendDistinctText(result, sideText);

        result = appendDistinctText(result,
                progressText(big == null ? null : firstObject(big,
                        "progressTextInfo", "fixedWidthDigitInfo", "sameWidthDigitInfo")));
        JSONObject small = island.optJSONObject("smallIslandArea");
        result = appendDistinctText(result,
                joinTexts(small, "smallIslandArea", "title", "content", "subContent"));
        return result;
    }

    private String appendSideText(String first, String second) {
        return appendDistinctText(first, second, currentSettings.sideSeparator);
    }

    private String appendDistinctText(String first, String second) {
        return appendDistinctText(first, second, currentSettings.generalSeparator);
    }

    private static String appendDistinctText(String first, String second, String separator) {
        first = cleanText(first);
        second = cleanText(second);
        if (TextUtils.isEmpty(second)) return first;
        if (TextUtils.isEmpty(first)) return second;
        if (first.equals(second) || first.contains(second)) return first;
        if (second.contains(first)) return second;
        return first + separator + second;
    }

    private static String findPrimaryIslandTitle(JSONObject island) {
        if (island == null) return null;
        JSONObject big = island.optJSONObject("bigIslandArea");
        JSONObject left = big == null ? null : big.optJSONObject("imageTextInfoLeft");
        JSONObject text = left == null ? null : firstObject(left, "textInfo", "miui.focus.paramtextInfo");
        return firstText(text, "title", "frontTitle", "content");
    }

    private static JSONObject firstObject(JSONObject object, String... keys) {
        if (object == null) return null;
        for (String key : keys) {
            JSONObject value = object.optJSONObject(key);
            if (value != null) return value;
        }
        return null;
    }

    private static String firstText(JSONObject object, String... keys) {
        if (object == null) return null;
        for (String key : keys) {
            String value = cleanText(object.optString(key, null));
            if (!TextUtils.isEmpty(value)) return value;
        }
        return null;
    }

    private String joinCompact(String first, String second) {
        first = cleanText(first);
        second = cleanText(second);
        if (TextUtils.isEmpty(first)) return second;
        if (TextUtils.isEmpty(second) || first.equals(second)) return first;
        return first + currentSettings.generalSeparator + second;
    }

    private String joinText(String first, String second) {
        first = cleanText(first);
        second = cleanText(second);
        if (TextUtils.isEmpty(first)) return second;
        if (TextUtils.isEmpty(second) || first.equals(second)) return first;
        return first + currentSettings.generalSeparator + second;
    }

    private static String cleanText(String value) {
        if (value == null) return null;
        value = value.trim();
        return value.length() == 0 ? null : value;
    }

    private HyperOS4FocusController.DisplayItem createOS4DisplayItem(Object entry) {
        reloadSettings(false);
        Object expanded = getField(entry, "mSbn");
        if (expanded == null) expanded = getField(entry, "sbn");
        if (expanded == null) return null;
        FocusData data = inspectExpanded(expanded);
        if (data == null) return null;
        Object keyValue = getField(entry, "key");
        if (keyValue == null) keyValue = getField(entry, "mKey");
        String key = stringValue(keyValue);
        if (TextUtils.isEmpty(key)) return null;

        Notification notification = null;
        try {
            Object value = XposedHelpers.callMethod(expanded, "getNotification");
            if (value instanceof Notification) notification = (Notification) value;
        } catch (Throwable t) {
            error("OS4 getNotification key=" + key, t);
        }
        PendingIntent contentIntent = notification == null ? null : notification.contentIntent;

        boolean hasNativeStatusBarContent = OS4FocusPriorityPolicy.hasNativeStatusBarContent(
                data.barRv != null || data.barNightRv != null,
                !TextUtils.isEmpty(data.ticker), data.hasIslandParam);
        log("OS4 classification key=" + key + " package=" + data.packageName
                + " originalFocusField=" + data.originalFocusField
                + " explicitFocus=" + data.explicitFocus
                + " isOriginalFocus=" + data.isOriginalFocus
                + " hasBarRemoteViews=" + (data.barRv != null || data.barNightRv != null)
                + " ticker=" + data.ticker
                + " hasIslandParam=" + data.hasIslandParam
                + " islandParam=" + data.islandParam
                + " nativeStatusBarContent=" + hasNativeStatusBarContent
                + " forcePackage=" + currentSettings.islandForcePackages.contains(data.packageName));
        if (data.isOriginalFocus && hasNativeStatusBarContent) {
            boolean showOnStatusBar = false;
            try {
                Class<?> utils = FocusReflection.findClass(classLoader,
                        "com.android.systemui.statusbar.notification.utils.FocusUtils");
                Object result = XposedHelpers.callStaticMethod(utils, "showOnStatusBar", expanded);
                showOnStatusBar = Boolean.TRUE.equals(result);
            } catch (Throwable t) {
                error("OS4 native showOnStatusBar key=" + key, t);
            }
            if (!showOnStatusBar) {
                log("OS4 native Focus rejected by showOnStatusBar key=" + key
                        + " " + data.summary());
                return null;
            }
            return new HyperOS4FocusController.DisplayItem(key, data.packageName,
                    cleanText(data.ticker), "nativeFocus", data.barRv, data.barNightRv,
                    contentIntent, OS4FocusPriorityPolicy.PRIORITY_NATIVE_FOCUS);
        }

        // OS4 may mark an island notification as Focus before it has any native
        // status-bar content. Classify without mutating mIsFocusNotification.
        if (data.hasIslandParam && !hasNativeStatusBarContent) {
            data.isOriginalFocus = false;
        }
        if (!shouldConvert(data)) return null;
        IslandText islandText = extractIslandContent(data);
        if (islandText == null || TextUtils.isEmpty(islandText.text)) return null;
        int priority;
        String source;
        if (currentSettings.islandForcePackages.contains(data.packageName)) {
            priority = OS4FocusPriorityPolicy.PRIORITY_ISLAND_WHITELIST;
            source = "islandWhitelist:" + islandText.source;
        } else if (isSmsVerificationCode(data)) {
            priority = OS4FocusPriorityPolicy.PRIORITY_SMS_VERIFICATION;
            source = "smsVerification:" + islandText.source;
        } else {
            priority = OS4FocusPriorityPolicy.PRIORITY_ISLAND;
            source = "island:" + islandText.source;
        }
        return new HyperOS4FocusController.DisplayItem(key, data.packageName,
                islandText.text, source, null, null, contentIntent, priority);
    }

    private FocusData inspectBean(Object bean) {
        if (bean == null) return null;
        try {
            FocusData data = inspectExpanded(getField(bean, "sbn"));
            if (data == null) data = new FocusData();
            data.key = stringValue(getField(bean, "notifKey"));
            if (TextUtils.isEmpty(data.packageName)) data.packageName = packageFromKey(data.key);
            data.content = stringValue(getField(bean, "content"));
            data.contentRv = asRemoteViews(getField(bean, "contentRemoteViews"));
            data.contentNightRv = asRemoteViews(getField(bean, "contentNightRemoteViews"));
            return data;
        } catch (Throwable t) {
            error("inspectBean", t);
            return null;
        }
    }

    private FocusData inspectExpanded(Object expanded) {
        if (expanded == null) return null;
        try {
            FocusData data = new FocusData();
            data.packageName = notificationPackageName(expanded);
            boolean originalFocusField = getBooleanField(expanded, "mIsFocusNotification", false);
            data.originalFocusField = originalFocusField;
            data.isFocus = originalFocusField;

            Notification notification = null;
            try {
                notification = (Notification) XposedHelpers.callMethod(expanded, "getNotification");
            } catch (Throwable ignored) {
                Object sbnNotification = invokeNoArg(expanded, "getNotification");
                if (sbnNotification instanceof Notification) notification = (Notification) sbnNotification;
            }

            if (notification == null || notification.extras == null) return data;
            Bundle extras = notification.extras;
            boolean explicitFocus = extras.getBoolean("miui.focus.isFocus", false);
            data.explicitFocus = explicitFocus;
            data.isFocus = data.isFocus || explicitFocus;
            data.islandParam = extras.getString("miui.focus.param");
            if (TextUtils.isEmpty(data.islandParam)) {
                data.islandParam = extras.getString("miui.focus.param.custom");
            }
            data.hasIslandParam = !TextUtils.isEmpty(data.islandParam);
            data.ticker = extras.getString("miui.focus.ticker");
            data.mainRv = getRemoteViews(extras, "miui.focus.rv");
            data.mainNightRv = getRemoteViews(extras, "miui.focus.rvNight");
            data.barRv = getRemoteViews(extras, "miui.focus.rvBar");
            data.barNightRv = getRemoteViews(extras, "miui.focus.rvBarNight");
            data.hasMainRv = data.mainRv != null || data.mainNightRv != null;
            data.hasBarRv = data.barRv != null || data.barNightRv != null;
            boolean hasTicker = !TextUtils.isEmpty(data.ticker);
            data.hasExplicitFocusData = FocusPriorityPolicy.hasExplicitFocusData(
                    explicitFocus, data.hasMainRv, data.hasBarRv, hasTicker, data.hasIslandParam);
            data.isOriginalFocus = FocusPriorityPolicy.isOriginalFocus(
                    originalFocusField, explicitFocus, data.hasMainRv, data.hasBarRv,
                    hasTicker, data.hasIslandParam);
            return data;
        } catch (Throwable t) {
            error("inspectExpanded", t);
            return null;
        }
    }

    private static String notificationPackageName(Object expanded) {
        try {
            Object value = invokeNoArg(expanded, "getPackageName");
            if (value != null) return String.valueOf(value);
        } catch (Throwable ignored) {
        }
        try {
            Object sbn = getField(expanded, "mSbn");
            if (sbn == null) sbn = getField(expanded, "sbn");
            if (sbn != null) {
                Object value = invokeNoArg(sbn, "getPackageName");
                if (value != null) return String.valueOf(value);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String packageFromKey(String key) {
        if (TextUtils.isEmpty(key)) return null;
        String[] parts = key.split("\\|", 5);
        return parts.length > 1 && parts[1].length() > 0 ? parts[1] : null;
    }

    private static RemoteViews getRemoteViews(Bundle extras, String key) {
        try {
            Parcelable value = extras.getParcelable(key);
            return value instanceof RemoteViews ? (RemoteViews) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static RemoteViews asRemoteViews(Object value) {
        return value instanceof RemoteViews ? (RemoteViews) value : null;
    }

    private static Object getField(Object target, String name) {
        if (target == null) return null;
        try {
            return XposedHelpers.getObjectField(target, name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean getBooleanField(Object target, String name, boolean fallback) {
        if (target == null) return fallback;
        try {
            return XposedHelpers.getBooleanField(target, name);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static Object invokeNoArg(Object target, String method) throws Exception {
        Method value = target.getClass().getMethod(method);
        value.setAccessible(true);
        return value.invoke(target);
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static void log(String value) {
        Log.i(TAG, value);
        XposedBridge.log(TAG + ": " + value);
    }

    private static void error(String stage, Throwable t) {
        Log.e(TAG, stage, t);
        XposedBridge.log(TAG + " ERROR " + stage + ": " + Log.getStackTraceString(t));
    }

    private static final class OriginalBeanState {
        final Object expanded;
        final boolean originalFocus;
        String originalContent;
        String lastConvertedContent;

        OriginalBeanState(Object expanded, boolean originalFocus, String originalContent) {
            this.expanded = expanded;
            this.originalFocus = originalFocus;
            this.originalContent = originalContent;
        }
    }

    private static final class IslandText {
        final String text;
        final String source;

        IslandText(String text, String source) {
            this.text = text;
            this.source = source;
        }
    }

    private static final class FocusData {
        boolean isFocus;
        boolean originalFocusField;
        boolean explicitFocus;
        boolean isOriginalFocus;
        boolean hasMainRv;
        boolean hasBarRv;
        boolean hasIslandParam;
        boolean hasExplicitFocusData;
        String islandParam;
        String key;
        String packageName;
        String ticker;
        String content;
        RemoteViews mainRv;
        RemoteViews mainNightRv;
        RemoteViews barRv;
        RemoteViews barNightRv;
        RemoteViews contentRv;
        RemoteViews contentNightRv;

        boolean hasDisplayContent() {
            return hasMainRv || hasBarRv || !TextUtils.isEmpty(ticker)
                    || !TextUtils.isEmpty(content) || contentRv != null;
        }

        String summary() {
            return "key=" + key
                    + " focus=" + isFocus
                    + " ticker=" + !TextUtils.isEmpty(ticker)
                    + " content=" + !TextUtils.isEmpty(content)
                    + " rv=" + (mainRv != null)
                    + " rvNight=" + (mainNightRv != null)
                    + " rvBar=" + (barRv != null)
                    + " rvBarNight=" + (barNightRv != null)
                    + " islandParam=" + hasIslandParam
                    + " contentRv=" + (contentRv != null)
                    + " contentNightRv=" + (contentNightRv != null);
        }
    }
}
