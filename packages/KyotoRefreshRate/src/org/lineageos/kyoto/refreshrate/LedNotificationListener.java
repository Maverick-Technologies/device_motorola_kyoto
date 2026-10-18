package org.lineageos.kyoto.refreshrate;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Set;

/**
 * Drives the notification LED. It is lit while the screen is off and at least one notification
 * qualifies: posted after the screen last turned off, clearable, importance default or higher,
 * from an app the user left enabled. Turning the screen on counts as having seen them.
 */
public class LedNotificationListener extends NotificationListenerService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private PowerManager mPowerManager;
    private boolean mConnected;
    // Notifications posted before this time have been seen (screen was on).
    private long mSeenUntil;

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mSeenUntil = System.currentTimeMillis();
            update();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mPowerManager = getSystemService(PowerManager.class);
        mSeenUntil = System.currentTimeMillis();
    }

    @Override
    public void onListenerConnected() {
        mConnected = true;
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenReceiver, filter);
        NotificationLed.prefs(this).registerOnSharedPreferenceChangeListener(this);
        update();
    }

    @Override
    public void onListenerDisconnected() {
        mConnected = false;
        unregisterReceiver(mScreenReceiver);
        NotificationLed.prefs(this).unregisterOnSharedPreferenceChangeListener(this);
        NotificationLed.off(this);
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) { update(); }
    @Override public void onNotificationRemoved(StatusBarNotification sbn) { update(); }
    @Override public void onNotificationRankingUpdate(RankingMap rankingMap) { update(); }
    @Override public void onInterruptionFilterChanged(int interruptionFilter) { update(); }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        update();
    }

    private void update() {
        if (!mConnected) return;
        if (shouldLight()) {
            NotificationLed.show(this, NotificationLed.getPattern(this),
                    NotificationLed.getBrightness(this));
        } else {
            NotificationLed.off(this);
        }
    }

    private boolean shouldLight() {
        if (!NotificationLed.isEnabled(this) || mPowerManager.isInteractive()) return false;
        if (NotificationLed.respectsDnd(this)
                && getCurrentInterruptionFilter() > NotificationManager.INTERRUPTION_FILTER_ALL) {
            return false;
        }
        StatusBarNotification[] active;
        try {
            active = getActiveNotifications();
        } catch (SecurityException e) {
            return false;
        }
        if (active == null) return false;
        Set<String> disabled = NotificationLed.getDisabledApps(this);
        RankingMap rankings = getCurrentRanking();
        Ranking ranking = new Ranking();
        for (StatusBarNotification sbn : active) {
            if (sbn.getPostTime() <= mSeenUntil) continue;
            if (sbn.isOngoing() || !sbn.isClearable()) continue;
            String pkg = sbn.getPackageName();
            if (disabled.contains(pkg) || "android".equals(pkg) || getPackageName().equals(pkg)) {
                continue;
            }
            if (rankings != null && rankings.getRanking(sbn.getKey(), ranking)) {
                if (ranking.getImportance() < NotificationManager.IMPORTANCE_DEFAULT) continue;
                if (ranking.isSuspended()) continue;
            }
            return true;
        }
        return false;
    }
}
