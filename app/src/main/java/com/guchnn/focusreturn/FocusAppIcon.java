/*
 * HyperOS3FocusRestore - Copyright (C) ImKani.
 * Modifications Copyright (C) 2026 Guchnn <https://github.com/Guchnn>.
 * This copy is a derivative work maintained by Guchnn; see MODIFICATIONS.md.
 * Licensed under the GNU General Public License v3.0 only (GPL-3.0-only).
 *
 * New file 2026-09-19: loads the issuing app's launcher icon for the focus
 * notification.
 * Modified 2026-09-20 (1.5.1): the icon is no longer tinted. Tinting replaced it
 * with the adaptive monochrome layer, which made every app look identical and
 * inverted it against the light/dark scene; the real launcher icon is now always
 * used as-is. See MODIFICATIONS.md.
 *
 * This file is part of a derivative work distributed under GPL-3.0-only.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package com.guchnn.focusreturn;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/** Loads the app icon shown at the left of a focus notification. */
final class FocusAppIcon {
    private FocusAppIcon() {
    }

    /**
     * Resolves the issuing app's real launcher icon.
     *
     * <p>Deliberately returns the full-colour icon: earlier builds preferred the
     * adaptive monochrome layer and tinted it with the status bar content colour,
     * which made every app look like the same recoloured glyph and inverted it
     * between light and dark scenes. The launcher icon must keep its own colours,
     * so no tint is applied here or in {@link #apply}.
     */
    static Drawable load(Context context, String packageName) {
        if (context == null || packageName == null || packageName.length() == 0) return null;
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationIcon(packageName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Applies the icon to {@code view}, sizing it to {@code sizeDp}. The view is
     * hidden when no icon can be resolved.
     */
    static void apply(ImageView view, Context context, String packageName, int sizeDp) {
        if (view == null) return;
        Drawable drawable = load(context, packageName);
        if (drawable == null) {
            view.setVisibility(android.view.View.GONE);
            view.setImageDrawable(null);
            return;
        }
        float density = view.getResources().getDisplayMetrics().density;
        int px = Math.max(1, Math.round(sizeDp * density));
        view.setLayoutParams(new android.view.ViewGroup.LayoutParams(px, px));
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setAdjustViewBounds(false);
        view.setImageDrawable(drawable);
        view.setVisibility(android.view.View.VISIBLE);
    }
}
