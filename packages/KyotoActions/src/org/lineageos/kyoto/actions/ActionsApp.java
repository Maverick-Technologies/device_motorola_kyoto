package org.lineageos.kyoto.actions;

import android.app.Application;
import android.content.Intent;

/** Persistent system app: the process lives for the whole session and hosts the service. */
public class ActionsApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, GestureService.class));
    }
}
