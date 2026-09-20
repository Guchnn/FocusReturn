/*
 * HyperOS3FocusRestore - Copyright (C) ImKani.
 * Modifications Copyright (C) 2026 Guchnn <https://github.com/Guchnn>.
 * This copy is a derivative work maintained by Guchnn; see MODIFICATIONS.md.
 * Licensed under the GNU General Public License v3.0 only (GPL-3.0-only).
 *
 * New file 2026-09-19: opens the source app of a focus notification either in a
 * MIUI / HyperOS small window (freeform) or directly, when its notification is
 * tapped.
 * Modified 2026-09-19 (1.3.0): the small window now has two size modes instead
 * of one hardcoded aspect. "System default" hands the launch to the platform
 * without any explicit bounds, so the ROM computes its own default freeform
 * bounds for the current orientation. "Measured" reproduces the Xiaomi default
 * recorded through ADB on a 1200x2670 display, where the freeform task was
 * 1200x1800 px - i.e. the window spans the screen's short side and 1.5x the
 * short side along the long axis. See MODIFICATIONS.md.
 * Modified 2026-09-19 (1.4.0): the measured mode now follows the user's own
 * measurement of a normal default small window (847x1350 px on a 1200x2670
 * screen) and became the default; the plain platform default measured only
 * ~412x732 px, far too small. See MODIFICATIONS.md.
 *
 * This file is part of a derivative work distributed under GPL-3.0-only.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package com.guchnn.focusreturn;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.lang.reflect.Method;

/** Launches an app's main activity when a focus notification is tapped. */
final class FocusFreeformLauncher {
    private static final String TAG = "FocusReturn";
    /** android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM */
    private static final int WINDOWING_MODE_FREEFORM = 5;
    /**
     * Reference default small window measured on a Xiaomi 15 (1200x2670 px):
     * a normal MIUI small window is 847x1350 px, i.e. 847/1200 of the screen's
     * short side and 1350/2670 of its long side. Those fractions are applied to
     * whatever device we run on, with the axes swapped in landscape.
     *
     * <p>For comparison, NOT sending any bounds makes the platform compute its
     * own default, which measured only ~412x732 <em>px</em> on the same device
     * (the AOSP dp constants used without density scaling) - far too small,
     * which is why the measured fractions are the shipped default.</p>
     */
    /**
     * Aspect ratio (long side / short side) of the ROM's own small window.
     *
     * <p>Measured on a Xiaomi 15 (1200x2670, 520dpi) by capturing the same app
     * through the system's own path (sidebar / global small-window gesture) and
     * through this module, then diffing <code>dumpsys window</code>:
     *
     * <pre>
     *   system: Requested w=1200 h=1920   (app config sw369dp w369dp h591dp nrml)
     *   module: Requested w=847  h=1350   (app config sw261dp w261dp h415dp smll)
     * </pre>
     *
     * <p>The task's logical size is the full short side by 1.6x that; MIUI then
     * draws the whole task at <code>MiuiMultiWindowUtils.getOriFreeformScale()</code>
     * (= 0.7), which is why the window <em>looks</em> like ~840x1344 on screen.
     * Earlier builds requested that visible size directly, so MIUI scaled it down
     * a second time (window looked ~590x945) and the app laid out inside a
     * genuinely tiny viewport ({@code SCREENLAYOUT_SIZE_SMALL}) - text overflowed
     * and the layout did not match the ROM's own small window.
     */
    private static final float MEASURED_ASPECT = 1.6f;

    /**
     * MIUI's own display scale for freeform tasks
     * ({@code MiuiMultiWindowUtils.getOriFreeformScale()}, measured constant 0.7
     * on screenType 1). The visible window is the task rectangle scaled by this
     * factor and anchored at the task's <em>top-left</em> corner, so the position
     * that actually shows on screen is the task's left/top while the visible
     * extent is only {@code DISPLAY_SCALE} of the task size. Used below to centre
     * the part the user can actually see - never applied to the size we request.
     */
    private static final float DISPLAY_SCALE = 0.7f;
    private static Method setLaunchWindowingMode;
    private static boolean reflectionResolved;

    static final int MODE_FREEFORM = 1;
    static final int MODE_DIRECT = 2;

    private FocusFreeformLauncher() {
    }

    /**
     * Opens {@code packageName}. When {@code mode} is {@link #MODE_FREEFORM} the
     * app launches in a small window; if that is unavailable and {@code
     * fallbackToDirect} is true (or for {@link #MODE_DIRECT}) it opens normally.
     * {@code sizeMode} picks how the small window is sized: the platform default
     * or the measured Xiaomi default for the current orientation. Every failure
     * is swallowed after logging so a tap never crashes SystemUI.
     */
    static void launch(Context context, String packageName, int mode, boolean fallbackToDirect,
                       int sizeMode) {
        if (context == null || packageName == null || packageName.length() == 0) return;
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(packageName);
        if (intent == null) {
            Log.i(TAG, "launch: no launch intent for " + packageName);
            return;
        }
        // NEW_TASK only, no MULTIPLE_TASK: like MIUI's own small-window entry we
        // want the app's existing task to move into freeform (a normal resize the
        // app can handle) instead of spawning a duplicate task that ends up in a
        // mis-scaled compatibility mode.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (mode == MODE_FREEFORM) {
            Bundle options = freeformOptions(context, sizeMode);
            if (options != null) {
                try {
                    context.startActivity(intent, options);
                    Log.i(TAG, "launch: started " + packageName + " in small window sizeMode="
                            + FocusRestoreSettings.describeFreeformSize(sizeMode));
                    return;
                } catch (Throwable throwable) {
                    Log.e(TAG, "launch: freeform failed for " + packageName, throwable);
                    if (!fallbackToDirect) return;
                }
            } else if (!fallbackToDirect) {
                Log.i(TAG, "launch: freeform unavailable for " + packageName + ", no fallback");
                return;
            }
        }
        launchDirect(context, intent, packageName);
    }

    private static void launchDirect(Context context, Intent intent, String packageName) {
        try {
            context.startActivity(intent);
            Log.i(TAG, "launch: started " + packageName + " normally");
        } catch (Throwable throwable) {
            Log.e(TAG, "launch: direct start failed for " + packageName, throwable);
        }
    }

    /**
     * Builds an ActivityOptions bundle requesting a freeform launch, or
     * {@code null} when the hidden API cannot be reached (caller falls back).
     * For {@link FocusRestoreSettings#FREEFORM_SIZE_SYSTEM} no bounds are sent,
     * which is what makes the platform apply its own default freeform size for
     * the current orientation.
     */
    private static Bundle freeformOptions(Context context, int sizeMode) {
        if (Build.VERSION.SDK_INT < 26) return null;
        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            Method method = resolveSetter();
            if (method == null) return null;
            method.invoke(options, WINDOWING_MODE_FREEFORM);
            if (sizeMode == FocusRestoreSettings.FREEFORM_SIZE_MEASURED) {
                Rect bounds = measuredBounds(context);
                if (bounds != null) {
                    options.setLaunchBounds(bounds);
                    Log.i(TAG, "launch: measured freeform bounds=" + bounds.toShortString());
                } else {
                    Log.i(TAG, "launch: measured bounds unavailable, using system default");
                }
            }
            return options.toBundle();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Reproduces the ROM's own freeform task size: the full short side on one
     * axis and {@link #MEASURED_ASPECT} times the short side on the other,
     * centred on the current display. Do <em>not</em> pre-compensate for MIUI's
     * own 0.7 display scale - the platform applies that itself.
     */
    private static Rect measuredBounds(Context context) {
        try {
            Point size = realDisplaySize(context);
            if (size.x <= 0 || size.y <= 0) return null;
            int shortSide = Math.min(size.x, size.y);
            int longSpan = Math.round(shortSide * MEASURED_ASPECT);
            int width;
            int height;
            if (size.y >= size.x) {
                // Portrait: full width, 1.6x that tall (1200x1920 on a Xiaomi 15).
                width = shortSide;
                height = longSpan;
            } else {
                // Landscape: the long side goes across, so the window stays wide.
                width = longSpan;
                height = shortSide;
            }
            width = Math.max(1, Math.min(width, size.x));
            height = Math.max(1, Math.min(height, size.y));
            // The visible window is the task rect scaled by DISPLAY_SCALE from its
            // top-left, so centre that visible part horizontally. Without this the
            // task's left edge became the visible left edge and the window sat
            // flush against the left of the screen.
            // Vertically the plain task centring is kept: measured against the
            // ROM's own placement it lands at 375px vs the ROM's 406px on a
            // Xiaomi 15, i.e. already in the right place.
            int visibleWidth = Math.round(width * DISPLAY_SCALE);
            int left = Math.max(0, (size.x - visibleWidth) / 2);
            int top = Math.max(0, (size.y - height) / 2);
            return new Rect(left, top, left + width, top + height);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static Point realDisplaySize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return null;
        Display display = wm.getDefaultDisplay();
        if (display == null) return null;
        // getRealSize() still reports the physical size of the display in the
        // current rotation, which is exactly the coordinate space freeform
        // launch bounds are expressed in.
        Point size = new Point();
        display.getRealSize(size);
        return size;
    }

    private static synchronized Method resolveSetter() {
        if (reflectionResolved) return setLaunchWindowingMode;
        reflectionResolved = true;
        try {
            setLaunchWindowingMode = ActivityOptions.class.getMethod(
                    "setLaunchWindowingMode", int.class);
        } catch (Throwable ignored) {
            setLaunchWindowingMode = null;
        }
        return setLaunchWindowingMode;
    }
}
