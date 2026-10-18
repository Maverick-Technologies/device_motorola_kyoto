package org.lineageos.kyoto.actions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Starts the gesture service after boot and after an app update. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, GestureService.class));
    }
}
