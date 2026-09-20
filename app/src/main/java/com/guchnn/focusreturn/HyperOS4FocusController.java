/*
 * HyperOS3FocusRestore - Copyright (C) ImKani.
 * Modifications Copyright (C) 2026 Guchnn <https://github.com/Guchnn>.
 * This copy is a derivative work maintained by Guchnn; see MODIFICATIONS.md.
 * Licensed under the GNU General Public License v3.0 only (GPL-3.0-only).
 *
 * Modified 2026-09-18: applies the custom focus text style to the fallback
 * text view and to the whole focus content view tree.
 * Modified 2026-09-19: selects the light/dark scene colour from the status bar
 * tint (real background) rather than the system night mode, and publishes the
 * tint so the scene stays live when the status bar recolours.
 * See MODIFICATIONS.md.
 *
 * This file is part of a derivative work distributed under GPL-3.0-only.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package com.guchnn.focusreturn;

import android.animation.ValueAnimator;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** HyperOS 4 notification state and status-bar rendering bridge. */
final class HyperOS4FocusController {
    interface ItemFactory {
        DisplayItem create(Object notificationEntry);
        HookSettings settings();
    }

    interface Logger {
        void log(String message);
        void error(String stage, Throwable throwable);
    }

    static final class DisplayItem {
        final String key;
        final String packageName;
        final String text;
        final String source;
        final RemoteViews remoteViews;
        final RemoteViews remoteViewsNight;
        final PendingIntent contentIntent;
        final int priority;
        long updateSequence;

        DisplayItem(String key, String packageName, String text, String source,
                    RemoteViews remoteViews, RemoteViews remoteViewsNight,
                    PendingIntent contentIntent, int priority) {
            this.key = key;
            this.packageName = packageName;
            this.text = text;
            this.source = source;
            this.remoteViews = remoteViews;
            this.remoteViewsNight = remoteViewsNight;
            this.contentIntent = contentIntent;
            this.priority = priority;
        }

        boolean hasContent() {
            return remoteViews != null || remoteViewsNight != null || !TextUtils.isEmpty(text);
        }
    }

    private final ClassLoader classLoader;
    private final Context context;
    private final ItemFactory itemFactory;
    private final Logger logger;
    private final Map<String, DisplayItem> items = new LinkedHashMap<>();
    private final Set<Object> registeredPipelines = Collections.newSetFromMap(
            new WeakHashMap<Object, Boolean>());
    private long updateSequence;
    private ViewGroup statusBarRoot;
    private ViewGroup primarySlot;
    private FocusHostView focusHost;
    private TextView statusBarClock;
    private View notificationIcons;
    private int notificationIconsOriginalVisibility;
    private boolean notificationIconsHidden;
    /** Icon children hidden because they belong to the focus notification app. */
    private final java.util.LinkedHashMap<View, Integer> hiddenFocusAppIcons =
            new java.util.LinkedHashMap<>();
    /** Package whose icons are currently hidden, null when the whole container is. */
    private String hiddenIconsPackage;
    /** Container classes whose layout pass we already hooked (once per class). */
    private final Set<Class<?>> iconLayoutHookInstalled = Collections.newSetFromMap(
            new WeakHashMap<Class<?>, Boolean>());
    private Object darkDispatcher;
    private Object darkReceiver;
    private Class<?> darkDispatcherClass;
    private int currentTint = Color.WHITE;
    /** Poll interval while watching whether the focus app is in the foreground. */
    private static final long APP_OPEN_POLL_MS = 400L;
    private final android.os.Handler appOpenHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable appOpenPoll;
    /** Foreground state the last render acted on, so the poll only reacts to flips. */
    private boolean lastAppOpenState;

    /** How long a show/hide takes, cached so the hide path can read it too. */
    private boolean fadeEnabled = FocusRestoreSettings.DEFAULT_FOCUS_FADE_ENABLED;
    private long fadeDurationMs = FocusRestoreSettings.DEFAULT_FOCUS_FADE_DURATION_MS;
    /**
     * Bumped on every render. A fade-out captures it and bails out when the
     * value moved on, so a notification arriving mid-fade can never be hidden
     * by the animation that was already running.
     */
    private int renderGeneration;

    HyperOS4FocusController(ClassLoader classLoader, Context context,
                            ItemFactory itemFactory, Logger logger) {
        this.classLoader = classLoader;
        this.context = context;
        this.itemFactory = itemFactory;
        this.logger = logger;
        registerLockscreenReceiver();
    }

    /**
     * Re-evaluates the focus display when the keyguard shows or goes away so
     * "hide on lock screen" reacts without waiting for the next notification
     * update. Entirely best effort: any failure only means the setting is
     * applied on the next regular render.
     */
    private void registerLockscreenReceiver() {
        try {
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override public void onReceive(Context ctx, Intent intent) {
                    renderBest();
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, 0x2 /* RECEIVER_NOT_EXPORTED */);
            } else {
                context.registerReceiver(receiver, filter);
            }
        } catch (Throwable throwable) {
            logger.error("OS4 registerLockscreenReceiver", throwable);
        }
    }

    private boolean isKeyguardLocked() {
        try {
            KeyguardManager km = (KeyguardManager)
                    context.getSystemService(Context.KEYGUARD_SERVICE);
            return km != null && km.isKeyguardLocked();
        } catch (Throwable ignored) {
            return false;
        }
    }

    void install() {
        hookNotifPipeline();
        hookStatusBarView();
        hookClockTint();
    }

    private void hookNotifPipeline() {
        try {
            Class<?> pipelineClass = FocusReflection.findClass(classLoader,
                    "com.android.systemui.statusbar.notification.collection.NotifPipeline");
            XposedBridge.hookAllConstructors(pipelineClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    registerPipeline(param.thisObject);
                }
            });
            logger.log("OS4 notifPipelineListener=hooked");
        } catch (Throwable throwable) {
            logger.error("OS4 hookNotifPipeline", throwable);
        }
    }

    private void registerPipeline(Object pipeline) {
        if (pipeline == null) return;
        synchronized (registeredPipelines) {
            if (!registeredPipelines.add(pipeline)) return;
        }
        try {
            Class<?> listenerClass = FocusReflection.findClass(classLoader,
                    "com.android.systemui.statusbar.notification.collection.notifcollection.NotifCollectionListener");
            Object listener = Proxy.newProxyInstance(classLoader, new Class<?>[]{listenerClass},
                    new NotificationListener());
            Method addListener = pipeline.getClass().getMethod("addCollectionListener", listenerClass);
            addListener.invoke(pipeline, listener);
            logger.log("OS4 notifPipelineListener=registered");
            Object existing = XposedHelpers.callMethod(pipeline, "getAllNotifs");
            if (existing instanceof Collection) {
                for (Object entry : new ArrayList<>((Collection<?>) existing)) {
                    updateEntry(entry, "initial");
                }
            }
        } catch (Throwable throwable) {
            logger.error("OS4 registerNotifPipeline", throwable);
        }
    }

    private final class NotificationListener implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("toString".equals(name)) return "HyperOS4FocusRestoreNotifListener";
            if ("hashCode".equals(name)) return System.identityHashCode(proxy);
            if ("equals".equals(name)) return args != null && args.length == 1 && proxy == args[0];
            try {
                if (("onEntryAdded".equals(name) || "onEntryUpdated".equals(name)
                        || "onEntryBind".equals(name)) && args != null && args.length > 0) {
                    updateEntry(args[0], name);
                } else if (("onEntryRemoved".equals(name) || "onEntryCleanUp".equals(name))
                        && args != null && args.length > 0) {
                    removeEntry(args[0], name);
                }
            } catch (Throwable throwable) {
                logger.error("OS4 listener " + name, throwable);
            }
            return null;
        }
    }

    private void updateEntry(Object entry, String stage) {
        if (entry == null) return;
        DisplayItem item = itemFactory.create(entry);
        String key = item == null ? entryKey(entry) : item.key;
        synchronized (items) {
            if (item == null || TextUtils.isEmpty(key) || !item.hasContent()) {
                if (!TextUtils.isEmpty(key)) items.remove(key);
            } else {
                item.updateSequence = ++updateSequence;
                items.put(key, item);
                logger.log("OS4 candidate " + stage + " key=" + key
                        + " priority=" + item.priority + " source=" + item.source
                        + " remoteViews=" + (item.remoteViews != null)
                        + " remoteViewsNight=" + (item.remoteViewsNight != null)
                        + " text=" + item.text);
            }
        }
        renderBest();
    }

    private void removeEntry(Object entry, String stage) {
        String key = entryKey(entry);
        if (TextUtils.isEmpty(key)) return;
        synchronized (items) {
            items.remove(key);
        }
        logger.log("OS4 candidate " + stage + " key=" + key);
        renderBest();
    }

    private String entryKey(Object entry) {
        Object value = field(entry, "key");
        if (value == null) value = field(entry, "mKey");
        return value == null ? null : String.valueOf(value);
    }

    private void hookStatusBarView() {
        try {
            Class<?> statusBarView = FocusReflection.findClass(classLoader,
                    "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView");
            XposedBridge.hookAllMethods(statusBarView, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject instanceof ViewGroup) {
                        attachStatusBar((ViewGroup) param.thisObject);
                    }
                }
            });
            XposedBridge.hookAllMethods(statusBarView, "onConfigurationChanged", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    renderBest();
                }
            });
            logger.log("OS4 statusBarPrimarySlot=hooked");
        } catch (Throwable throwable) {
            logger.error("OS4 hookStatusBarView", throwable);
        }
    }

    private void hookClockTint() {
        try {
            Class<?> clockClass = FocusReflection.findClass(classLoader,
                    "com.android.systemui.statusbar.views.MiuiClock");
            Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(
                    clockClass, "onDarkChanged", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.thisObject == statusBarClock) {
                                updateTint(((TextView) param.thisObject).getCurrentTextColor(),
                                        "MiuiClock.onDarkChanged");
                            }
                        }
                    });
            logger.log("OS4 clockTintHook=" + (hooks.isEmpty() ? "missing" : "hooked")
                    + " count=" + hooks.size());
        } catch (Throwable throwable) {
            logger.error("OS4 hookClockTint", throwable);
        }
    }

    private void attachStatusBar(ViewGroup statusBarView) {
        try {
            int primaryId = context.getResources().getIdentifier(
                    "ongoing_activity_chip_primary", "id", "com.android.systemui");
            View view = primaryId == 0 ? null : statusBarView.findViewById(primaryId);
            if (!(view instanceof ViewGroup)) {
                logger.log("OS4 statusBarPrimarySlot=missing id=" + primaryId);
                return;
            }
            ViewGroup slot = (ViewGroup) view;
            restoreAllNotificationIconHiding();
            FocusHostView oldHost = focusHost;
            ViewGroup oldSlot = primarySlot;
            if (oldHost != null && oldHost.getParent() instanceof ViewGroup) {
                oldHost.clearContent();
                ((ViewGroup) oldHost.getParent()).removeView(oldHost);
            }
            if (oldSlot != null && oldSlot != slot) {
                oldSlot.setVisibility(View.GONE);
            }
            FocusHostView host = new FocusHostView(slot.getContext());
            host.setId(View.generateViewId());
            host.setVisibility(View.GONE);
            slot.addView(host, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            statusBarRoot = statusBarView;
            primarySlot = slot;
            focusHost = host;
            resolveStatusBarClock();
            registerDarkReceiver();
            // HyperOS 4 hides this legacy XML slot before installing its Compose
            // chip. Preserve that inactive state so the XML defaults (phone icon,
            // 00:00:00 chronometer and background) never leak between candidates.
            slot.setVisibility(View.GONE);
            logger.log("OS4 statusBarPrimarySlot=attached id=" + primaryId
                    + " legacyInactiveVisibility=GONE children=" + slot.getChildCount());
            renderBest();
        } catch (Throwable throwable) {
            logger.error("OS4 attachStatusBar", throwable);
        }
    }

    /** Applies the "nothing to show" state to the host and the legacy slot. */
    private void applyHiddenState(FocusHostView host, String reason) {
        host.animate().cancel();
        host.clearContent();
        host.setTranslationY(0f);
        host.setAlpha(1f);
        host.setVisibility(View.GONE);
        ViewGroup slot = primarySlot;
        if (slot != null) slot.setVisibility(View.GONE);
        restoreAllNotificationIconHiding();
        logger.log("OS4 focus hidden legacySlot=GONE notificationIconsRestored=true"
                + " reason=" + reason);
    }

    private void renderBest() {
        final DisplayItem best;
        synchronized (items) {
            DisplayItem selected = null;
            for (DisplayItem candidate : items.values()) {
                if (selected == null || OS4FocusPriorityPolicy.compare(
                        candidate.priority, candidate.updateSequence,
                        selected.priority, selected.updateSequence) > 0) {
                    selected = candidate;
                }
            }
            best = selected;
        }
        final FocusHostView host = focusHost;
        if (host == null) return;
        host.post(() -> render(host, best));
    }

    private void render(FocusHostView host, DisplayItem item) {
        if (host != focusHost) return;
        HookSettings settings = item == null ? null : itemFactory.settings();

        if (settings != null) {
            fadeEnabled = settings.focusFadeEnabled;
            fadeDurationMs = settings.focusFadeDurationMs;
        }
        renderGeneration++;
        boolean lockscreenSuppressed = item != null && settings.hideOnLockscreen
                && isKeyguardLocked();
        // Hide while the focus app itself is in the foreground (full screen or
        // small window). In this case the notification icon is deliberately NOT
        // hidden, so the user still sees that something is running.
        boolean appOpenSuppressed = item != null && settings.hideWhenAppOpen
                && isFocusAppForeground(item.packageName);
        lastAppOpenState = appOpenSuppressed;
        cancelAppOpenPoll();
        // Arm the watcher even while suppressed. Arming it only on the
        // "shown" path meant nothing could ever notice the app going back to
        // the background, so the focus notification waited for the next
        // unrelated notification event (measured as a 3-6s lag after closing
        // the small window).
        if (item != null) scheduleAppOpenPoll(settings, item.packageName);
        if (item == null || lockscreenSuppressed || appOpenSuppressed) {
            String reason = lockscreenSuppressed ? "lockscreen"
                    : (appOpenSuppressed ? "appOpen" : "noItem");
            if (host.getVisibility() == View.VISIBLE && fadeEnabled && fadeDurationMs > 0) {
                final int generation = renderGeneration;
                host.animate().cancel();
                host.animate().alpha(0f).setDuration(fadeDurationMs).withEndAction(() -> {
                    if (host != focusHost || generation != renderGeneration) return;
                    if (host.getAlpha() > 0.01f) return;
                    applyHiddenState(host, reason);
                }).start();
                logger.log("OS4 focus fading out durationMs=" + fadeDurationMs
                        + " reason=" + reason);
                return;
            }
            applyHiddenState(host, reason);
            return;
        }
        hideOriginalChildren();
        applyNotificationIconHiding(settings, item.packageName);
        // Fade in only on a real transition, otherwise every notification
        // update would re-trigger the animation and flicker.
        boolean wasVisible = host.getVisibility() == View.VISIBLE;
        host.animate().cancel();
        host.setAlpha(1f);
        host.setVisibility(View.VISIBLE);
        if (!wasVisible && fadeEnabled && fadeDurationMs > 0) {
            host.setAlpha(0f);
            host.animate().alpha(1f).setDuration(fadeDurationMs).start();
        }
        // Scene of the real background behind the status bar, derived from the
        // status bar tint -- not the system dark/light theme.
        boolean darkScene = FocusTextStyle.isDarkSceneFromTint(currentTint);
        boolean night = (host.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        RemoteViews selected = night
                ? (item.remoteViewsNight != null ? item.remoteViewsNight : item.remoteViews)
                : (item.remoteViews != null ? item.remoteViews : item.remoteViewsNight);
        View content = null;
        if (selected != null) {
            try {
                content = selected.apply(host.getContext(), host);
            } catch (Throwable throwable) {
                logger.error("OS4 applyRemoteViews key=" + item.key, throwable);
            }
        }
        if (content == null && !TextUtils.isEmpty(item.text)) {
            TextView textView = new TextView(host.getContext());
            textView.setText(item.text);
            textView.setTextColor(currentTint);
            textView.setTextSize(14f);
            FocusTextStyle.apply(textView, settings, darkScene);
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            textView.setSingleLine(true);
            textView.setEllipsize(null);
            textView.setIncludeFontPadding(false);
            content = textView;
        }
        if (content == null) {
            synchronized (items) {
                items.remove(item.key);
            }
            renderBest();
            return;
        }
        host.showContent(content, item, settings);
        logger.log("OS4 focus shown key=" + item.key + " package=" + item.packageName
                + " source=" + item.source + " priority=" + item.priority
                + " widthDp=" + settings.widthDp + " limit=" + settings.limitWidth
                + " maxWidthPx=" + host.maxWidthPx
                + " hideIcons=" + FocusRestoreSettings.describeHideIcons(settings.hideIconsMode)
                + " iconVerticalDp=" + settings.focusIconVerticalOffsetDp
                + " textVerticalDp=" + settings.focusTextVerticalOffsetDp
                + " fontSizeSp=" + (FocusTextStyle.isCustom(settings)
                        ? String.valueOf(settings.focusFontSizeSp) : "system")
                + " tint=0x" + Integer.toHexString(currentTint)
                + " scene=" + FocusTextStyle.sceneLabel(darkScene)
                + " colorLight=" + FocusTextStyle.colorLabel(settings.focusTextColorLight)
                + " colorDark=" + FocusTextStyle.colorLabel(settings.focusTextColorDark)
                + " click=" + FocusRestoreSettings.describeClick(settings.clickOpenMode));
    }

    private void resolveStatusBarClock() {
        ViewGroup root = statusBarRoot;
        if (root == null) return;
        int id = root.getResources().getIdentifier("clock", "id", context.getPackageName());
        View view = id == 0 ? null : root.findViewById(id);
        statusBarClock = view instanceof TextView ? (TextView) view : null;
        if (statusBarClock != null) {
            currentTint = statusBarClock.getCurrentTextColor();
            FocusTextStyle.publishStatusBarTint(currentTint);
            logger.log("OS4 statusBarClock=resolved tint=0x"
                    + Integer.toHexString(currentTint)
                    + " scene=" + FocusTextStyle.sceneLabel(
                            FocusTextStyle.isDarkSceneFromTint(currentTint)));
        } else {
            logger.log("OS4 statusBarClock=missing id=" + id);
        }
    }

    private void registerDarkReceiver() {
        unregisterDarkReceiver();
        try {
            darkDispatcherClass = FocusReflection.findClass(classLoader,
                    "com.android.systemui.plugins.DarkIconDispatcher");
            Class<?> receiverClass = FocusReflection.findClass(classLoader,
                    "com.android.systemui.plugins.DarkIconDispatcher$DarkReceiver");
            Class<?> dependencyClass = FocusReflection.findClass(classLoader,
                    "com.android.systemui.Dependency");
            darkDispatcher = XposedHelpers.callStaticMethod(
                    dependencyClass, "get", darkDispatcherClass);
            darkReceiver = Proxy.newProxyInstance(classLoader,
                    new Class<?>[]{receiverClass}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            String name = method.getName();
                            if (("onDarkChanged".equals(name)
                                    || "onDarkChangedWithContrast".equals(name))
                                    && args != null && args.length >= 3
                                    && args[2] instanceof Integer) {
                                int tint = resolveTint(args[0], (Integer) args[2]);
                                updateTint(tint, "DarkIconDispatcher." + name);
                            } else if ("hashCode".equals(name)) {
                                return System.identityHashCode(proxy);
                            } else if ("equals".equals(name)) {
                                return args != null && args.length == 1 && proxy == args[0];
                            } else if ("toString".equals(name)) {
                                return "FocusRestoreDarkReceiver";
                            }
                            return null;
                        }
                    });
            XposedHelpers.callMethod(darkDispatcher, "addDarkReceiver", darkReceiver);
            try {
                XposedHelpers.callMethod(darkDispatcher, "applyDark", darkReceiver);
            } catch (Throwable throwable) {
                logger.error("OS4 applyInitialDark", throwable);
            }
            logger.log("OS4 darkReceiver=registered dispatcher="
                    + darkDispatcher.getClass().getName());
        } catch (Throwable throwable) {
            darkDispatcher = null;
            darkReceiver = null;
            darkDispatcherClass = null;
            logger.error("OS4 registerDarkReceiver", throwable);
        }
    }

    private int resolveTint(Object areas, int fallbackTint) {
        View tintReference = statusBarClock != null ? statusBarClock : focusHost;
        if (tintReference == null || darkDispatcherClass == null) return fallbackTint;
        try {
            Object value = XposedHelpers.callStaticMethod(
                    darkDispatcherClass, "getTint", areas, tintReference, fallbackTint);
            return value instanceof Integer ? (Integer) value : fallbackTint;
        } catch (Throwable throwable) {
            logger.error("OS4 resolveDarkTint", throwable);
            return fallbackTint;
        }
    }

    private void unregisterDarkReceiver() {
        if (darkDispatcher == null || darkReceiver == null) return;
        try {
            XposedHelpers.callMethod(darkDispatcher, "removeDarkReceiver", darkReceiver);
        } catch (Throwable throwable) {
            logger.error("OS4 unregisterDarkReceiver", throwable);
        } finally {
            darkDispatcher = null;
            darkReceiver = null;
            darkDispatcherClass = null;
        }
    }

    private void updateTint(int tint, String source) {
        if (currentTint == tint) return;
        currentTint = tint;
        FocusTextStyle.publishStatusBarTint(tint);
        FocusHostView host = focusHost;
        if (host != null) {
            applyTint(host);
            host.updateIconTint();
        }
        logger.log("OS4 tint updated source=" + source + " tint=0x"
                + Integer.toHexString(tint)
                + " scene=" + FocusTextStyle.sceneLabel(
                        FocusTextStyle.isDarkSceneFromTint(tint)));
    }

    /**
     * Keeps a user selected focus text color when the status bar tint changes:
     * OS4 recolors every focus text view on each tint update, which would
     * otherwise silently drop the custom color.
     */
    private int resolveTextColor(View view) {
        HookSettings current = null;
        try {
            if (itemFactory != null) current = itemFactory.settings();
        } catch (Throwable ignored) {
            current = null;
        }
        // Pick the light/dark scene colour from the status bar tint so the focus
        // text tracks the real background instead of the system night mode.
        int custom = FocusTextStyle.colorFor(current,
                FocusTextStyle.isDarkSceneFromTint(currentTint));
        return custom == FocusRestoreSettings.NO_FOCUS_TEXT_COLOR ? currentTint : custom;
    }

    private void applyTint(View view) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(resolveTextColor(view));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                applyTint(group.getChildAt(index));
            }
        }
    }

    /**
     * Applies the configured notification-icon hiding while a focus item shows:
     * nothing, the whole icon container (the 1.2.0 behaviour), or only the icons
     * posted by the app that owns the focus notification.
     */
    private void applyNotificationIconHiding(HookSettings settings, String focusPackage) {
        if (settings.hidesFocusAppIconOnly()) {
            hideFocusAppNotificationIcons(focusPackage);
            return;
        }
        restoreHiddenFocusAppIcons();
        if (settings.hidesAllNotificationIcons()) {
            hideNotificationIconsContainer();
        } else {
            restoreNotificationIcons();
        }
    }

    private void hideNotificationIconsContainer() {
        View icons = resolveNotificationIcons();
        if (icons == null) {
            logger.log("OS4 notificationIcons=missing requestedHidden=container");
            return;
        }
        if (!notificationIconsHidden || notificationIcons != icons) {
            restoreNotificationIcons();
            notificationIcons = icons;
            notificationIconsOriginalVisibility = icons.getVisibility();
            notificationIconsHidden = true;
        }
        icons.setVisibility(View.GONE);
        logger.log("OS4 notificationIcons=GONE id=" + icons.getId()
                + " originalVisibility=" + notificationIconsOriginalVisibility);
    }

    private View resolveNotificationIcons() {
        ViewGroup root = statusBarRoot;
        if (root == null) return null;
        int id = root.getResources().getIdentifier(
                "notificationIcons", "id", context.getPackageName());
        return id == 0 ? null : root.findViewById(id);
    }

    private void restoreNotificationIcons() {
        if (!notificationIconsHidden) return;
        View icons = notificationIcons;
        if (icons != null) icons.setVisibility(notificationIconsOriginalVisibility);
        logger.log("OS4 notificationIcons=restored id="
                + (icons == null ? 0 : icons.getId())
                + " visibility=" + notificationIconsOriginalVisibility);
        notificationIcons = null;
        notificationIconsHidden = false;
    }

    /** Restores every icon this controller may have hidden. */
    private void restoreAllNotificationIconHiding() {
        restoreNotificationIcons();
        restoreHiddenFocusAppIcons();
    }

    /**
     * Hides only the icon views posted by {@code focusPackage}, keeping every
     * other status bar notification icon on screen.
     */
    private void hideFocusAppNotificationIcons(String focusPackage) {
        View container = notificationIcons != null ? notificationIcons : resolveNotificationIcons();
        // In this mode the container itself always stays visible.
        restoreNotificationIcons();
        if (TextUtils.isEmpty(focusPackage)) {
            restoreHiddenFocusAppIcons();
            return;
        }
        if (!(container instanceof ViewGroup)) {
            logger.log("OS4 hideFocusAppIcons=missing package=" + focusPackage);
            return;
        }
        ViewGroup group = (ViewGroup) container;
        if (!focusPackage.equals(hiddenIconsPackage)) {
            restoreHiddenFocusAppIcons();
            hiddenIconsPackage = focusPackage;
        }
        hideMatchingFocusAppIcons(group, focusPackage);
    }

    private void hideMatchingFocusAppIcons(ViewGroup group, String focusPackage) {
        int added = 0;
        for (int index = 0; index < group.getChildCount(); index++) {
            View child = group.getChildAt(index);
            if (child == null || hiddenFocusAppIcons.containsKey(child)) continue;
            if (!focusPackage.equals(iconPackageOf(child))) continue;
            hiddenFocusAppIcons.put(child, child.getVisibility());
            collapseContainerIcon(group, child, true);
            child.setVisibility(View.GONE);
            added++;
        }
        if (added > 0) {
            ensureIconLayoutHook(group);
            group.requestLayout();
            logger.log("OS4 hideFocusAppIcons app=" + focusPackage
                    + " added=" + added + " total=" + hiddenFocusAppIcons.size());
        }
    }

    /**
     * Collapses or restores one child inside a {@code NotificationIconContainer}.
     *
     * <p>Writing {@code IconState.hidden} alone is NOT enough on Android 15: the
     * container's own {@code resetViewStates()} assigns {@code hidden = false} to
     * every child on each layout pass, and {@code calculateIconXTranslations()}
     * reserves horizontal space from {@code iconAppearAmount * view.getWidth()}
     * without ever looking at {@code hidden}. So the icon stayed invisible while
     * its slot kept being reserved - exactly the "hidden but still占位" symptom.
     *
     * <p>{@code iconAppearAmount} is the only input that both hides the glyph and
     * releases the space, so drive that (plus {@code hidden} as a belt) here and
     * again from {@link #ensureIconLayoutHook} right before every layout pass.
     */
    private void collapseContainerIcon(ViewGroup container, View icon, boolean collapse) {
        Object state = iconStateOf(container, icon);
        if (state == null) return;
        setBooleanFieldQuiet(state, "hidden", collapse);
        setFloatFieldQuiet(state, "iconAppearAmount", collapse ? 0f : 1f);
        setFloatFieldQuiet(state, "clampedAppearAmount", collapse ? 0f : 1f);
    }

    private Object iconStateOf(ViewGroup container, View icon) {
        if (container == null || icon == null) return null;
        try {
            Object states = XposedHelpers.getObjectField(container, "mIconStates");
            if (states instanceof Map) return ((Map<?, ?>) states).get(icon);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void setFloatFieldQuiet(Object target, String name, float value) {
        try {
            XposedHelpers.setFloatField(target, name, value);
        } catch (Throwable ignored) {
        }
    }

    private static void setBooleanFieldQuiet(Object target, String name, boolean value) {
        try {
            XposedHelpers.setBooleanField(target, name, value);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Re-applies the collapsed state at the start of every layout pass, because
     * the container rebuilds its view states on each pass. Hooked once per
     * container class; both the AOSP 15 name and the older one are attempted so
     * a renamed method degrades to "no extra reclaim" instead of crashing.
     */
    private void ensureIconLayoutHook(final ViewGroup container) {
        final Class<?> type = container.getClass();
        if (!iconLayoutHookInstalled.add(type)) return;
        StringBuilder hooked = new StringBuilder();
        for (final String name : new String[]{
                "resetViewStates", "calculateIconXTranslations", "calculateIconTranslations",
                "applyIconStates"}) {
            try {
                XposedBridge.hookAllMethods(type, name, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if ("applyIconStates".equals(name)) return;
                        try {
                            reapplyCollapsedIcons(param.thisObject);
                        } catch (Throwable ignored) {
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if ("applyIconStates".equals(name)) {
                                // The icon-state pass can flip an overflowing icon
                                // back to STATE_ICON; visibility is not part of the
                                // width budget, so simply re-hide it at the end.
                                forceHiddenIconsGone(param.thisObject);
                            } else if ("resetViewStates".equals(name)) {
                                // resetViewStates() clears every hidden flag, so it
                                // has to be followed by our state as well.
                                reapplyCollapsedIcons(param.thisObject);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                });
                hooked.append(hooked.length() == 0 ? "" : ",").append(name);
            } catch (Throwable ignored) {
                // Method not present in this ROM - try the next name.
            }
        }
        logger.log("OS4 iconLayoutHook class=" + type.getName()
                + " hooked=" + (hooked.length() == 0 ? "none" : hooked)
                + " states=" + (iconStateOf(container, firstHiddenIcon()) != null ? "ok" : "missing"));
    }

    /** Re-collapses every icon we are currently hiding inside {@code container}. */
    private void reapplyCollapsedIcons(Object container) {
        if (hiddenFocusAppIcons.isEmpty() || !(container instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) container;
        for (View icon : hiddenFocusAppIcons.keySet()) {
            if (icon == null || icon.getParent() != group) continue;
            collapseContainerIcon(group, icon, true);
        }
    }

    /** Forces our hidden icons back to {@code GONE} after the icon-state pass. */
    private void forceHiddenIconsGone(Object container) {
        if (hiddenFocusAppIcons.isEmpty() || !(container instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) container;
        for (View icon : hiddenFocusAppIcons.keySet()) {
            if (icon == null || icon.getParent() != group) continue;
            if (icon.getVisibility() != View.GONE) icon.setVisibility(View.GONE);
        }
    }

    private View firstHiddenIcon() {
        for (View icon : hiddenFocusAppIcons.keySet()) {
            if (icon != null) return icon;
        }
        return null;
    }

    /** Best effort read of the posting package behind a status bar icon view. */
    private String iconPackageOf(View iconView) {
        try {
            Object notification = field(iconView, "mNotification");
            if (notification != null) {
                Object packageName = XposedHelpers.callMethod(notification, "getPackageName");
                if (packageName instanceof String) return (String) packageName;
            }
            Object statusBarIcon = field(iconView, "mIcon");
            if (statusBarIcon != null) {
                Object pkg = field(statusBarIcon, "pkg");
                if (pkg instanceof String) return (String) pkg;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void restoreHiddenFocusAppIcons() {
        if (!hiddenFocusAppIcons.isEmpty()) {
            ViewGroup container = null;
            for (java.util.Map.Entry<View, Integer> entry : hiddenFocusAppIcons.entrySet()) {
                View icon = entry.getKey();
                if (container == null && icon.getParent() instanceof ViewGroup) {
                    container = (ViewGroup) icon.getParent();
                }
                collapseContainerIcon(
                        icon.getParent() instanceof ViewGroup ? (ViewGroup) icon.getParent() : null,
                        icon, false);
                icon.setVisibility(entry.getValue());
            }
            if (container != null) container.requestLayout();
            logger.log("OS4 hideFocusAppIcons=restored count=" + hiddenFocusAppIcons.size()
                    + " app=" + hiddenIconsPackage);
        }
        hiddenFocusAppIcons.clear();
        hiddenIconsPackage = null;
    }

    private void hideOriginalChildren() {
        ViewGroup slot = primarySlot;
        FocusHostView host = focusHost;
        if (slot == null || host == null) return;
        for (int index = 0; index < slot.getChildCount(); index++) {
            View child = slot.getChildAt(index);
            if (child != host) child.setVisibility(View.GONE);
        }
        slot.setVisibility(View.VISIBLE);
    }

    private static Object field(Object target, String name) {
        if (target == null) return null;
        try {
            return XposedHelpers.getObjectField(target, name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private final class FocusHostView extends FrameLayout {
        private ValueAnimator animator;
        private Runnable pendingAnimation;
        private View content;
        private ImageView appIconView;
        private String appIconPackage;
        private TextView dividerView;
        private int dividerColorLight = FocusRestoreSettings.NO_FOCUS_TEXT_COLOR;
        private int dividerColorDark = FocusRestoreSettings.NO_FOCUS_TEXT_COLOR;
        private String lastLayoutLog;
        private int contentInsetPx;
        private int maxWidthPx = Integer.MAX_VALUE;
        private boolean blockClicks = true;
        private long lastClickTime;

        FocusHostView(Context context) {
            super(context);
            setClipChildren(true);
            setClipToPadding(true);
        }

        void showContent(View nextContent, DisplayItem item, HookSettings settings) {
            clearContent();
            blockClicks = settings.clickOpenMode == FocusRestoreSettings.CLICK_OPEN_NONE;
            float density = getResources().getDisplayMetrics().density;
            maxWidthPx = settings.limitWidth
                    ? Math.max(1, Math.round(settings.widthDp * density)) : Integer.MAX_VALUE;

            int leftOffsetPx = 0;
            int iconTextGapPx = Math.round(settings.iconTextGapDp * density);
            // The divider's clock-side gap rides on the HOST margin, not on a
            // child margin: the host margin is the mechanism already proven to
            // move this content relative to the clock ("distance from the left
            // clock" uses exactly it), so the divider gap cannot end up being
            // swallowed by the WRAP_CONTENT FrameLayout.
            int dividerStartPx = TextUtils.isEmpty(settings.clockDividerSymbol)
                    ? 0 : Math.round(settings.clockDividerMarginStartDp * density);

            // Divider between the status bar clock and the focus notification.
            // It is the first child with no margin of its own; its clock-side gap
            // is already folded into the host margin above. Everything after it
            // shifts by its measured width plus the end gap.
            dividerView = null;
            if (!TextUtils.isEmpty(settings.clockDividerSymbol)) {
                TextView divider = new TextView(getContext());
                divider.setText(settings.clockDividerSymbol);
                divider.setTextSize(14f);
                divider.setSingleLine(true);
                divider.setIncludeFontPadding(false);
                divider.setGravity(Gravity.CENTER_VERTICAL);
                dividerColorLight = settings.clockDividerColorLight;
                dividerColorDark = settings.clockDividerColorDark;
                divider.setTextColor(resolveSceneColor(dividerColorLight, dividerColorDark));
                divider.setTranslationY(
                        Math.round(settings.clockDividerVerticalOffsetDp * density));
                int dividerEndPx = Math.round(settings.clockDividerMarginEndDp * density);
                LayoutParams dividerParams = new LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER_VERTICAL | Gravity.START);
                dividerParams.setMarginStart(0);
                addView(divider, dividerParams);
                divider.measure(
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                dividerView = divider;
                leftOffsetPx = divider.getMeasuredWidth() + dividerEndPx;
            }

            // Outer spacing from the clock (left) and the right edge of the slot.
            LayoutParams hostParams = new LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER_VERTICAL | Gravity.START);
            // Total shift of the notice away from the clock. The non-negative
            // part rides on the layout margin (the channel already proven to
            // work), the negative part on translationX: some ROM containers
            // silently drop a negative margin, while a translation always
            // applies. margin + translation == hostStartPx, so values never
            // get applied twice.
            int hostStartPx = Math.round(settings.focusMarginStartDp * density)
                    + dividerStartPx;
            hostParams.setMarginStart(Math.max(0, hostStartPx));
            hostParams.setMarginEnd(Math.round(settings.focusMarginEndDp * density));
            setLayoutParams(hostParams);
            setTranslationX(Math.min(0, hostStartPx));

            // Vertical placement is tuned per element now (icon / text each
            // have their own offset below); the host keeps the bar centre.
            setTranslationY(0);
            // App icon at the far left of the focus notification. It keeps
            // the launcher's own colours: the status bar scene must NOT tint
            // it. The icon-to-text distance is a plain gap, never a symbol.
            if (settings.showAppIcon && !TextUtils.isEmpty(item.packageName)) {
                int iconPx = Math.max(1, Math.round(settings.appIconSizeDp * density));
                ImageView icon = new ImageView(getContext());
                FocusAppIcon.apply(icon, getContext(), item.packageName,
                        settings.appIconSizeDp);
                if (icon.getVisibility() == View.VISIBLE) {
                    appIconView = icon;
                    appIconPackage = item.packageName;
                    LayoutParams iconParams = new LayoutParams(
                            iconPx, ViewGroup.LayoutParams.MATCH_PARENT,
                            Gravity.CENTER_VERTICAL | Gravity.START);
                    iconParams.setMarginStart(leftOffsetPx);
                    addView(icon, iconParams);
                    icon.setTranslationY(
                            Math.round(settings.focusIconVerticalOffsetDp * density));
                    leftOffsetPx += iconPx + iconTextGapPx;
                }
            }

            content = nextContent;
            contentInsetPx = leftOffsetPx;
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER_VERTICAL | Gravity.START);
            params.setMarginStart(leftOffsetPx);
            addView(nextContent, params);
            nextContent.setTranslationY(
                    Math.round(settings.focusTextVerticalOffsetDp * density));
            applyTint(nextContent);
            int styledTexts = FocusTextStyle.applyTree(nextContent, settings,
                    FocusTextStyle.isDarkSceneFromTint(currentTint));
            if (styledTexts > 0) {
                logger.log("OS4 focus font size=" + settings.focusFontSizeSp
                        + "sp views=" + styledTexts);
            }

            if (settings.clickOpenMode != FocusRestoreSettings.CLICK_OPEN_NONE
                    && !TextUtils.isEmpty(item.packageName)) {
                setOnClickListener(view -> {
                    // Optional double-tap trigger: the first tap only arms the
                    // gesture, a second tap inside the window fires the action,
                    // so stray single taps never launch anything.
                    if (settings.clickDoubleTap) {
                        long now = SystemClock.uptimeMillis();
                        if (now - lastClickTime > 400L) {
                            lastClickTime = now;
                            logger.log("OS4 doubleTap armed key=" + item.key);
                            return;
                        }
                        lastClickTime = 0L;
                    }
                    if (settings.clickOpenMode == FocusRestoreSettings.CLICK_OPEN_FREEFORM) {
                        FocusFreeformLauncher.launch(getContext(), item.packageName,
                                FocusFreeformLauncher.MODE_FREEFORM, settings.freeformFallbackDirect,
                                settings.freeformSizeMode);
                    } else {
                        FocusFreeformLauncher.launch(getContext(), item.packageName,
                                FocusFreeformLauncher.MODE_DIRECT, false,
                                FocusRestoreSettings.FREEFORM_SIZE_SYSTEM);
                    }
                });
            }

            requestLayout();
            long delay = Math.max(0L, Math.min(5000L, settings.marqueeDelayMs));
            pendingAnimation = () -> startScroll(settings.marqueeBounce);
            postDelayed(pendingAnimation, delay);
            logger.log("OS4 marquee scheduled key=" + item.key + " delayMs=" + delay
                    + " bounce=" + settings.marqueeBounce
                    + " icon=" + settings.showAppIcon
                    + " iconTextGapDp=" + settings.iconTextGapDp
                    + " iconVerticalDp=" + settings.focusIconVerticalOffsetDp
                    + " textVerticalDp=" + settings.focusTextVerticalOffsetDp
                + " divider=" + (TextUtils.isEmpty(settings.clockDividerSymbol)
                        ? "<off>" : settings.clockDividerSymbol)
                + " dividerStartDp=" + settings.clockDividerMarginStartDp
                + " dividerEndDp=" + settings.clockDividerMarginEndDp
                + " dividerVerticalDp=" + settings.clockDividerVerticalOffsetDp
                + " hostMarginStartDp=" + settings.focusMarginStartDp
                + " dividerStartPx=" + dividerStartPx
                + " hostStartPx=" + hostStartPx
                + " hostMarginPx=" + Math.max(0, hostStartPx)
                + " hostTransXPx=" + Math.min(0, hostStartPx));
        }

        void clearContent() {
            if (pendingAnimation != null) {
                removeCallbacks(pendingAnimation);
                pendingAnimation = null;
            }
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
            if (content != null) content.setTranslationX(0f);
            setOnClickListener(null);
            removeAllViews();
            content = null;
            appIconView = null;
            appIconPackage = null;
            dividerView = null;
            contentInsetPx = 0;
        }

        private void startScroll(boolean bounce) {
            pendingAnimation = null;
            View child = content;
            if (child == null || getVisibility() != View.VISIBLE) return;
            int availableWidth = Math.max(1, getWidth() - contentInsetPx);
            int distance = child.getMeasuredWidth() - availableWidth;
            if (distance <= 0 && child instanceof TextView) {
                TextView text = (TextView) child;
                distance = Math.round(text.getPaint().measureText(String.valueOf(text.getText())))
                        - availableWidth + text.getPaddingLeft() + text.getPaddingRight();
            }
            if (distance <= 0) {
                logger.log("OS4 marquee not needed contentWidth=" + child.getMeasuredWidth()
                        + " hostWidth=" + getWidth() + " maxWidthPx=" + maxWidthPx
                        + " contentInsetPx=" + contentInsetPx);
                return;
            }
            float direction = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? 1f : -1f;
            animator = bounce
                    ? ValueAnimator.ofFloat(0f, direction * distance, 0f)
                    : ValueAnimator.ofFloat(0f, direction * distance);
            animator.setDuration(Math.max(2500L, distance * 35L));
            animator.setInterpolator(new LinearInterpolator());
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.addUpdateListener(value -> {
                if (content == child && getVisibility() == View.VISIBLE) {
                    child.setTranslationX((Float) value.getAnimatedValue());
                }
            });
            animator.start();
            logger.log("OS4 marquee started distance=" + distance + " bounce=" + bounce
                    + " contentWidth=" + child.getMeasuredWidth()
                    + " hostWidth=" + getWidth() + " maxWidthPx=" + maxWidthPx);
        }

        /**
         * Logs the real laid-out positions once they change. If the divider gap
         * is ever reported as having no effect, these numbers say whether the
         * value reached the layout at all and which view failed to move.
         */
        private void logLayoutPositions() {
            try {
                ViewGroup parent = getParent() instanceof ViewGroup
                        ? (ViewGroup) getParent() : null;
                StringBuilder line = new StringBuilder("OS4 layout");
                if (parent != null) {
                    line.append(" slot=").append(parent.getLeft())
                            .append("+").append(parent.getWidth());
                }
                line.append(" host=").append(getLeft())
                        .append("+").append(getWidth());
                if (dividerView != null) {
                    line.append(" divider=").append(dividerView.getLeft())
                            .append("+").append(dividerView.getWidth());
                }
                if (content != null) {
                    line.append(" content=").append(content.getLeft())
                            .append("+").append(content.getWidth());
                }
                line.append(" inset=").append(contentInsetPx);
                String value = line.toString();
                if (value.equals(lastLayoutLog)) return;
                lastLayoutLog = value;
                logger.log(value);
            } catch (Throwable ignored) {
            }
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            if (changed) logLayoutPositions();
        }

        void updateIconTint() {
            // The app icon is intentionally left alone (no scene tint).
            if (dividerView != null) {
                dividerView.setTextColor(resolveSceneColor(dividerColorLight, dividerColorDark));
            }
        }

        @Override
        protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
            if (child != content || contentInsetPx <= 0) {
                return super.drawChild(canvas, child, drawingTime);
            }
            int saveCount = canvas.save();
            if (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                canvas.clipRect(0, 0, getWidth() - contentInsetPx, getHeight());
            } else {
                canvas.clipRect(contentInsetPx, 0, getWidth(), getHeight());
            }
            boolean drawn = super.drawChild(canvas, child, drawingTime);
            canvas.restoreToCount(saveCount);
            return drawn;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            return blockClicks || super.onInterceptTouchEvent(event);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return blockClicks || super.onTouchEvent(event);
        }

        @Override
        protected void onDetachedFromWindow() {
            clearContent();
            super.onDetachedFromWindow();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int unlimitedWidth = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            super.onMeasure(unlimitedWidth, heightMeasureSpec);
            int width = getMeasuredWidth();
            int mode = MeasureSpec.getMode(widthMeasureSpec);
            if (mode != MeasureSpec.UNSPECIFIED) {
                width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
            }
            width = Math.min(width, maxWidthPx);
            setMeasuredDimension(Math.max(1, width), getMeasuredHeight());
        }
    }

    /**
     * Resolves a light/dark pair against the real status bar scene, falling
     * back to the ROM tint when a scene is left unset.
     */
    private int resolveSceneColor(int lightColor, int darkColor) {
        int custom = FocusTextStyle.isDarkSceneFromTint(currentTint) ? darkColor : lightColor;
        return custom == FocusRestoreSettings.NO_FOCUS_TEXT_COLOR ? currentTint : custom;
    }

    /**
     * True when {@code packageName} owns the currently focused task, whether
     * that task is full screen or a freeform small window.
     */
    private boolean isFocusAppForeground(String packageName) {
        if (TextUtils.isEmpty(packageName)) return false;
        try {
            Object service = Class.forName("android.app.ActivityTaskManager")
                    .getMethod("getService").invoke(null);
            if (service == null) return false;
            try {
                Object info = XposedHelpers.callMethod(service, "getFocusedRootTaskInfo");
                String top = componentPackage(XposedHelpers.getObjectField(info, "topActivity"));
                if (top != null) return packageName.equals(top);
            } catch (Throwable ignored) {
            }
            java.util.List<?> tasks = (java.util.List<?>)
                    XposedHelpers.callMethod(service, "getTasks", 1);
            if (tasks != null && !tasks.isEmpty()) {
                Object task = tasks.get(0);
                String top = componentPackage(XposedHelpers.getObjectField(task, "topActivity"));
                if (top == null) {
                    top = componentPackage(XposedHelpers.getObjectField(task, "baseActivity"));
                }
                if (top != null) return packageName.equals(top);
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static String componentPackage(Object componentName) {
        if (componentName == null) return null;
        try {
            Object value = XposedHelpers.callMethod(componentName, "getPackageName");
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Re-checks the foreground state while a focus item is showing. */
    private void scheduleAppOpenPoll(final HookSettings settings, final String packageName) {
        cancelAppOpenPoll();
        if (settings == null || !settings.hideWhenAppOpen
                || TextUtils.isEmpty(packageName)) {
            return;
        }
        appOpenPoll = new Runnable() {
            @Override
            public void run() {
                appOpenPoll = null;
                boolean open = isFocusAppForeground(packageName);
                if (open != lastAppOpenState) {
                    logger.log("OS4 appOpen changed open=" + open + " package=" + packageName);
                    renderBest();
                    return;
                }
                scheduleAppOpenPoll(settings, packageName);
            }
        };
        appOpenHandler.postDelayed(appOpenPoll, APP_OPEN_POLL_MS);
    }

    private void cancelAppOpenPoll() {
        if (appOpenPoll != null) {
            appOpenHandler.removeCallbacks(appOpenPoll);
            appOpenPoll = null;
        }
    }

    private void send(PendingIntent intent, String key) {
        try {
            intent.send();
            logger.log("OS4 focus click sent key=" + key);
        } catch (PendingIntent.CanceledException exception) {
            logger.error("OS4 focus click canceled key=" + key, exception);
        }
    }
}
