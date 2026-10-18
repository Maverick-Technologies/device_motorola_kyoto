package org.lineageos.kyoto.refreshrate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Makes sure the notification light listener has access after boot. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            NotificationLed.grantListenerAccess(context);
        }
    }
}
