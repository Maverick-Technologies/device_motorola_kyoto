package org.lineageos.kyoto.refreshrate;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/**
 * Supplies the live summaries for the entries this app injects into Settings > Display:
 * Refresh rate ("Adaptive", "60 Hz", "90 Hz", getSummary) and Notification light ("On"/"Off",
 * getLedSummary). Settings calls content://<authority>/<method> via
 * com.android.settings.summary_uri.
 */
public class RefreshRateSummaryProvider extends ContentProvider {

    private static final String METHOD_GET_SUMMARY = "getSummary";
    private static final String METHOD_GET_LED_SUMMARY = "getLedSummary";
    private static final String KEY_SUMMARY = "com.android.settings.summary";

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        int summary;
        if (METHOD_GET_SUMMARY.equals(method)) {
            summary = RefreshRateMode.titleRes(RefreshRateMode.get(getContext()));
        } else if (METHOD_GET_LED_SUMMARY.equals(method)) {
            summary = NotificationLed.isEnabled(getContext())
                    ? R.string.led_summary_on : R.string.led_summary_off;
        } else {
            return null;
        }
        Bundle result = new Bundle();
        result.putString(KEY_SUMMARY, getContext().getString(summary));
        return result;
    }

    @Override public boolean onCreate() { return true; }
    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
