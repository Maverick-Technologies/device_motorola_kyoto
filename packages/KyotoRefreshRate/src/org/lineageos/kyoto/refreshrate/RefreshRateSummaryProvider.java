package org.lineageos.kyoto.refreshrate;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/**
 * Supplies the live summary ("Adaptive", "60 Hz", "90 Hz") for the Refresh rate entry in
 * Settings > Display. Settings calls content://<authority>/getSummary via
 * com.android.settings.summary_uri.
 */
public class RefreshRateSummaryProvider extends ContentProvider {

    private static final String METHOD_GET_SUMMARY = "getSummary";
    private static final String KEY_SUMMARY = "com.android.settings.summary";

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (!METHOD_GET_SUMMARY.equals(method)) return null;
        Bundle result = new Bundle();
        result.putString(KEY_SUMMARY,
                getContext().getString(RefreshRateMode.titleRes(RefreshRateMode.get(getContext()))));
        return result;
    }

    @Override public boolean onCreate() { return true; }
    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
