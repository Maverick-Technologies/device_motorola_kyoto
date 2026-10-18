package org.lineageos.kyoto.refreshrate;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * The single LED above the display on kyoto (MT6360 PMU RGB LED channel 1, exposed by the
 * kernel as /sys/class/leds/charging). Stock MediaTek lights HAL drives red/green/blue nodes
 * that do not exist on this phone, so Android's notification light never reaches it.
 *
 * Both patterns run on the MT6360 breath engine, so the hardware animates while the CPU sleeps.
 * Its timing nodes are recreated root-owned whenever the trigger changes, so the app does not
 * touch sysfs itself: it publishes the wanted state ("off", "pulse N", "blink N") and a
 * privileged side applies it (source build: init.kyoto.rc on sys.kyoto.led; GSI test module:
 * a root helper watching files/led_state).
 */
public final class NotificationLed {

    private static final String TAG = "NotificationLed";
    private static final String STATE_FILE = "led_state";
    private static final String STATE_PROP = "sys.kyoto.led";
    // Pulse peaks lower than the chosen brightness so it stays soft.
    private static final float PULSE_PEAK = 0.7f;

    public static final int PATTERN_PULSE = 0;
    public static final int PATTERN_BLINK = 1;

    public static final int MAX_BRIGHTNESS = 12;

    static final String PREFS = "notification_light";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_PATTERN = "pattern";
    static final String KEY_BRIGHTNESS = "brightness";
    static final String KEY_RESPECT_DND = "respect_dnd";
    static final String KEY_DISABLED_APPS = "disabled_apps";

    private static String sLastState;

    private NotificationLed() {}

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, true);
    }

    public static int getPattern(Context context) {
        return prefs(context).getInt(KEY_PATTERN, PATTERN_PULSE);
    }

    public static int getBrightness(Context context) {
        return prefs(context).getInt(KEY_BRIGHTNESS, 6);
    }

    public static boolean respectsDnd(Context context) {
        return prefs(context).getBoolean(KEY_RESPECT_DND, true);
    }

    public static Set<String> getDisabledApps(Context context) {
        return new HashSet<>(prefs(context).getStringSet(KEY_DISABLED_APPS, new HashSet<>()));
    }

    public static void setAppEnabled(Context context, String pkg, boolean enabled) {
        Set<String> disabled = getDisabledApps(context);
        if (enabled) disabled.remove(pkg); else disabled.add(pkg);
        prefs(context).edit().putStringSet(KEY_DISABLED_APPS, disabled).apply();
    }

    /**
     * Gives LedNotificationListener notification access. Works for the platform-signed build
     * (MANAGE_NOTIFICATION_LISTENERS); on the GSI test module the boot script grants it instead.
     */
    public static void grantListenerAccess(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        ComponentName listener = new ComponentName(context, LedNotificationListener.class);
        if (nm.isNotificationListenerAccessGranted(listener)) return;
        try {
            nm.getClass().getMethod("setNotificationListenerAccessGranted",
                    ComponentName.class, boolean.class).invoke(nm, listener, true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.i(TAG, "cannot grant listener access: " + e);
        }
    }

    /** Turns the LED on with the given pattern and brightness (1..12). */
    public static synchronized void show(Context context, int pattern, int brightness) {
        int level = Math.max(1, Math.min(MAX_BRIGHTNESS, brightness));
        if (pattern == PATTERN_BLINK) {
            publish(context, "blink " + level);
        } else {
            publish(context, "pulse " + Math.max(1, Math.round(level * PULSE_PEAK)));
        }
    }

    public static synchronized void off(Context context) {
        publish(context, "off");
    }

    private static void publish(Context context, String state) {
        if (state.equals(sLastState)) return;
        sLastState = state;
        try {
            Class<?> props = Class.forName("android.os.SystemProperties");
            Method set = props.getMethod("set", String.class, String.class);
            set.invoke(null, STATE_PROP, state.replace(' ', '_'));
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Not allowed outside the source build; the state file covers the GSI.
        }
        File file = new File(context.getFilesDir(), STATE_FILE);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(state.getBytes(StandardCharsets.US_ASCII));
        } catch (IOException e) {
            Log.w(TAG, "write " + file + " failed: " + e.getMessage());
        }
    }
}
