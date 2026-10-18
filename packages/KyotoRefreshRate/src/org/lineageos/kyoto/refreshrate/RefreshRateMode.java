package org.lineageos.kyoto.refreshrate;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

/**
 * Reads and writes the refresh rate mode through the System settings AOSP's
 * "Smooth display" switch uses:
 *   Adaptive: peak 90, min 0   (display manager drops to 60 Hz when idle)
 *   60 Hz:    peak 60, min 0
 *   90 Hz:    peak 90, min 90
 */
final class RefreshRateMode {

    static final int ADAPTIVE = 0;
    static final int FIXED_60 = 1;
    static final int FIXED_90 = 2;

    private static final String PEAK = "peak_refresh_rate";
    private static final String MIN = "min_refresh_rate";
    private static final float HIGH = 90f;

    private RefreshRateMode() {}

    static int get(Context context) {
        ContentResolver cr = context.getContentResolver();
        float peak = Settings.System.getFloat(cr, PEAK, HIGH);
        float min = Settings.System.getFloat(cr, MIN, 0f);
        if (peak <= 60f) return FIXED_60;
        if (min >= HIGH) return FIXED_90;
        return ADAPTIVE;
    }

    static void set(Context context, int mode) {
        ContentResolver cr = context.getContentResolver();
        switch (mode) {
            case FIXED_60:
                Settings.System.putFloat(cr, PEAK, 60f);
                Settings.System.putFloat(cr, MIN, 0f);
                break;
            case FIXED_90:
                Settings.System.putFloat(cr, PEAK, HIGH);
                Settings.System.putFloat(cr, MIN, HIGH);
                break;
            default:
                Settings.System.putFloat(cr, PEAK, HIGH);
                Settings.System.putFloat(cr, MIN, 0f);
                break;
        }
    }

    static int titleRes(int mode) {
        switch (mode) {
            case FIXED_60: return R.string.mode_60;
            case FIXED_90: return R.string.mode_90;
            default: return R.string.mode_adaptive;
        }
    }
}
