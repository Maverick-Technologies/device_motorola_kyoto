package org.lineageos.kyoto.motosettings;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;

import java.util.List;

/**
 * Motorola settings store (content://com.motorola.android.providers.settings/{global,secure,
 * system}), speaking the protocol of com.motorola.android.provider.MotorolaSettings: call()
 * with GET_/PUT_/RESET_ + table, the setting name as arg and the value in "value", plus
 * query/insert fallbacks. Generation tracking is not offered, so clients do not cache.
 */
public class MotoSettingsProvider extends ContentProvider {

    private static final String AUTHORITY = "com.motorola.android.providers.settings";
    private static final String[] TABLES = {"global", "secure", "system"};
    private static final String KEY_VALUE = "value";
    private static final String COL_NAME = "name";
    private static final String COL_VALUE = "value";

    private Db mDb;

    private static final class Db extends SQLiteOpenHelper {
        Db(android.content.Context context) {
            super(context, "motorola_settings.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            for (String table : TABLES) {
                db.execSQL("CREATE TABLE " + table + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COL_NAME + " TEXT UNIQUE ON CONFLICT REPLACE, " + COL_VALUE + " TEXT)");
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
    }

    @Override
    public boolean onCreate() {
        mDb = new Db(getContext());
        return true;
    }

    private static String table(String name) {
        for (String table : TABLES) {
            if (table.equals(name)) return table;
        }
        return null;
    }

    private static String tableOf(Uri uri) {
        List<String> segments = uri.getPathSegments();
        return segments.isEmpty() ? null : table(segments.get(0));
    }

    private String get(String table, String name) {
        try (Cursor c = mDb.getReadableDatabase().query(table, new String[] {COL_VALUE},
                COL_NAME + "=?", new String[] {name}, null, null, null)) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    private void put(String table, String name, String value) {
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_VALUE, value);
        mDb.getWritableDatabase().insert(table, null, values);
        getContext().getContentResolver().notifyChange(
                Uri.parse("content://" + AUTHORITY + "/" + table + "/" + name), null);
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (method == null || arg == null) return null;
        int split = method.indexOf('_');
        if (split < 0) return null;
        String op = method.substring(0, split);
        String table = table(method.substring(split + 1));
        if (table == null) return null;
        switch (op) {
            case "GET": {
                Bundle result = new Bundle();
                result.putString(KEY_VALUE, get(table, arg));
                return result;
            }
            case "PUT":
                put(table, arg, extras != null ? extras.getString(KEY_VALUE) : null);
                return new Bundle();
            case "RESET":
                return new Bundle();
            default:
                return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        String table = tableOf(uri);
        if (table == null) return null;
        List<String> segments = uri.getPathSegments();
        if (segments.size() > 1) {
            MatrixCursor cursor = new MatrixCursor(new String[] {COL_NAME, COL_VALUE});
            String name = segments.get(1);
            String value = get(table, name);
            if (value != null) cursor.addRow(new Object[] {name, value});
            return cursor;
        }
        return mDb.getReadableDatabase().query(table, projection, selection, selectionArgs,
                null, null, sortOrder);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String table = tableOf(uri);
        if (table == null || values == null || values.getAsString(COL_NAME) == null) return null;
        String name = values.getAsString(COL_NAME);
        put(table, name, values.getAsString(COL_VALUE));
        return Uri.withAppendedPath(uri, name);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return insert(uri, values) != null ? 1 : 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String table = tableOf(uri);
        if (table == null) return 0;
        return mDb.getWritableDatabase().delete(table, selection, selectionArgs);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }
}
