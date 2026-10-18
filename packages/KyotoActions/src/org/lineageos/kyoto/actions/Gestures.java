package org.lineageos.kyoto.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

/**
 * On/off state of the Moto Actions. Chop chop and peek are handled by GestureService and kept
 * in app preferences; twist (the framework camera-launch gesture) and double tap to wake are
 * system settings, so the Settings app and GestureLauncherService see the same value.
 */
public final class Gestures {

    static final String PREFS = "moto_actions";
    static final String KEY_CHOP = "chop_chop";
    static final String KEY_PEEK = "peek";
    private static final String SECURE_CAMERA_GESTURE_DISABLED = "camera_gesture_disabled";
    private static final String SECURE_DOUBLE_TAP = "double_tap_to_wake";

    private Gestures() {}

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isChopEnabled(Context context) {
        return prefs(context).getBoolean(KEY_CHOP, true);
    }

    public static void setChopEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_CHOP, enabled).apply();
    }

    public static boolean isPeekEnabled(Context context) {
        return prefs(context).getBoolean(KEY_PEEK, true);
    }

    public static void setPeekEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_PEEK, enabled).apply();
    }

    public static boolean isTwistEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                SECURE_CAMERA_GESTURE_DISABLED, 0) == 0;
    }

    public static void setTwistEnabled(Context context, boolean enabled) {
        Settings.Secure.putInt(context.getContentResolver(),
                SECURE_CAMERA_GESTURE_DISABLED, enabled ? 0 : 1);
    }

    public static boolean isDoubleTapEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), SECURE_DOUBLE_TAP, 1) == 1;
    }

    public static void setDoubleTapEnabled(Context context, boolean enabled) {
        Settings.Secure.putInt(context.getContentResolver(), SECURE_DOUBLE_TAP, enabled ? 1 : 0);
    }
}
