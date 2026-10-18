package org.lineageos.kyoto.refreshrate;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Settings > Display > Notification light. Pixel-style page: main switch, pattern, brightness
 * with a live preview, Do Not Disturb option and a per-app list.
 */
public class NotificationLightActivity extends Activity {

    private static final long PREVIEW_MS = 3000;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mEndPreview = () -> NotificationLed.off(this);

    private Switch mMainSwitch;
    private Switch mDndSwitch;
    private SeekBar mBrightness;
    private View mOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_light);
        findViewById(R.id.back).setOnClickListener(v -> finish());

        mOptions = findViewById(R.id.options);
        mMainSwitch = findViewById(R.id.main_switch);
        findViewById(R.id.main_switch_row).setOnClickListener(v -> {
            boolean enabled = !NotificationLed.isEnabled(this);
            NotificationLed.prefs(this).edit()
                    .putBoolean(NotificationLed.KEY_ENABLED, enabled).apply();
            updateViews();
        });

        findViewById(R.id.row_pulse).setOnClickListener(v -> setPattern(NotificationLed.PATTERN_PULSE));
        findViewById(R.id.row_blink).setOnClickListener(v -> setPattern(NotificationLed.PATTERN_BLINK));

        mBrightness = findViewById(R.id.brightness);
        mBrightness.setMin(1);
        mBrightness.setMax(NotificationLed.MAX_BRIGHTNESS);
        mBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                if (!fromUser) return;
                NotificationLed.prefs(NotificationLightActivity.this).edit()
                        .putInt(NotificationLed.KEY_BRIGHTNESS, value).apply();
                preview();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });

        findViewById(R.id.preview).setOnClickListener(v -> preview());

        mDndSwitch = findViewById(R.id.dnd_switch);
        findViewById(R.id.dnd_row).setOnClickListener(v -> {
            NotificationLed.prefs(this).edit().putBoolean(NotificationLed.KEY_RESPECT_DND,
                    !NotificationLed.respectsDnd(this)).apply();
            updateViews();
        });

        findViewById(R.id.access_row).setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                        .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                                listenerComponent().flattenToString())));

        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationLed.grantListenerAccess(this);
        updateViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mEndPreview);
        NotificationLed.off(this);
    }

    private ComponentName listenerComponent() {
        return new ComponentName(this, LedNotificationListener.class);
    }

    private void setPattern(int pattern) {
        NotificationLed.prefs(this).edit().putInt(NotificationLed.KEY_PATTERN, pattern).apply();
        updateViews();
        preview();
    }

    private void preview() {
        mHandler.removeCallbacks(mEndPreview);
        NotificationLed.off(this);
        NotificationLed.show(this, NotificationLed.getPattern(this), NotificationLed.getBrightness(this));
        mHandler.postDelayed(mEndPreview, PREVIEW_MS);
    }

    private void updateViews() {
        boolean enabled = NotificationLed.isEnabled(this);
        mMainSwitch.setChecked(enabled);
        mOptions.setAlpha(enabled ? 1f : 0.38f);
        setEnabledRecursive(mOptions, enabled);

        int pattern = NotificationLed.getPattern(this);
        ((RadioButton) findViewById(R.id.radio_pulse)).setChecked(pattern == NotificationLed.PATTERN_PULSE);
        ((RadioButton) findViewById(R.id.radio_blink)).setChecked(pattern == NotificationLed.PATTERN_BLINK);
        mBrightness.setProgress(NotificationLed.getBrightness(this));
        mDndSwitch.setChecked(NotificationLed.respectsDnd(this));

        boolean granted = getSystemService(NotificationManager.class)
                .isNotificationListenerAccessGranted(listenerComponent());
        findViewById(R.id.access_row).setVisibility(granted ? View.GONE : View.VISIBLE);
    }

    private static void setEnabledRecursive(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setEnabledRecursive(group.getChildAt(i), enabled);
            }
        }
    }

    /** Launchable apps, loaded off the main thread, one switch row each. */
    private void loadApps() {
        PackageManager pm = getPackageManager();
        new Thread(() -> {
            Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            Map<String, ResolveInfo> byPackage = new LinkedHashMap<>();
            for (ResolveInfo info : pm.queryIntentActivities(launcher, 0)) {
                byPackage.putIfAbsent(info.activityInfo.packageName, info);
            }
            byPackage.remove(getPackageName());
            List<Object[]> apps = new ArrayList<>();
            for (ResolveInfo info : byPackage.values()) {
                apps.add(new Object[] {
                    info.activityInfo.packageName,
                    info.loadLabel(pm).toString(),
                    info.loadIcon(pm),
                });
            }
            Collator collator = Collator.getInstance();
            apps.sort((a, b) -> collator.compare((String) a[1], (String) b[1]));
            mHandler.post(() -> showApps(apps));
        }).start();
    }

    private void showApps(List<Object[]> apps) {
        LinearLayout list = findViewById(R.id.app_list);
        LayoutInflater inflater = getLayoutInflater();
        Set<String> disabled = NotificationLed.getDisabledApps(this);
        for (Object[] app : apps) {
            String pkg = (String) app[0];
            View row = inflater.inflate(R.layout.item_app_switch, list, false);
            ((ImageView) row.findViewById(R.id.icon)).setImageDrawable((Drawable) app[2]);
            ((TextView) row.findViewById(R.id.label)).setText((String) app[1]);
            Switch toggle = row.findViewById(R.id.toggle);
            toggle.setChecked(!disabled.contains(pkg));
            row.setOnClickListener(v -> {
                toggle.setChecked(!toggle.isChecked());
                NotificationLed.setAppEnabled(this, pkg, toggle.isChecked());
            });
            list.addView(row);
        }
        findViewById(R.id.apps_loading).setVisibility(View.GONE);
        setEnabledRecursive(mOptions, NotificationLed.isEnabled(this));
    }
}
