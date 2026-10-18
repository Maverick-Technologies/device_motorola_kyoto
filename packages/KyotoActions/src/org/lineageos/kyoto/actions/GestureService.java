package org.lineageos.kyoto.actions;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

/**
 * Moto gestures for kyoto on Android 15, driven by the stock vendor's Motorola sensors:
 *   ChopChop (com.motorola.sensor.chopchop)  -> toggle flashlight
 *   Motoglance (com.motorola.sensor.glance)  -> wake the screen briefly (peek)
 *   dt-gesture (com.motorola.sensor.double_tap) -> wake the screen (double tap to wake)
 * Twist for camera is handled by the framework (config_cameraLaunchGestureSensorStringType).
 * Each one can be switched off on the Moto Actions page (see Gestures).
 */
public class GestureService extends Service {

    private static final String TAG = "KyotoActions";
    private static final String CHOP = "com.motorola.sensor.chopchop";
    private static final String GLANCE = "com.motorola.sensor.glance";
    private static final String DOUBLE_TAP = "com.motorola.sensor.double_tap";
    private static final String STOWED = "com.motorola.sensor.stowed";
    private static final long PEEK_MS = 5000;
    private static final long DEBOUNCE_MS = 1000;

    private SensorManager mSensors;
    private CameraManager mCameras;
    private PowerManager mPower;
    private Vibrator mVibrator;
    private String mTorchId;
    private boolean mTorchOn;
    private boolean mStowed;
    private long mLastChop;

    private final CameraManager.TorchCallback mTorchCallback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            if (cameraId.equals(mTorchId)) mTorchOn = enabled;
        }
    };

    private final SensorEventListener mListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent e) {
            String type = e.sensor.getStringType();
            if (CHOP.equals(type)) {
                onChop();
            } else if (GLANCE.equals(type)) {
                onPeek("glance");
            } else if (DOUBLE_TAP.equals(type)) {
                if (Gestures.isDoubleTapEnabled(GestureService.this)) wake("double tap", 0);
            } else if (STOWED.equals(type)) {
                mStowed = e.values.length > 0 && e.values[0] > 0.5f;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mSensors = getSystemService(SensorManager.class);
        mCameras = getSystemService(CameraManager.class);
        mPower = getSystemService(PowerManager.class);
        mVibrator = getSystemService(Vibrator.class);
        mTorchId = findTorch();
        mCameras.registerTorchCallback(mTorchCallback, null);
        for (Sensor s : mSensors.getSensorList(Sensor.TYPE_ALL)) {
            String type = s.getStringType();
            if (CHOP.equals(type) || GLANCE.equals(type) || DOUBLE_TAP.equals(type) || STOWED.equals(type)) {
                boolean ok = mSensors.registerListener(mListener, s, SensorManager.SENSOR_DELAY_NORMAL);
                Log.i(TAG, "listening " + s.getName() + " (" + type + ") ok=" + ok);
            }
        }
        Log.i(TAG, "started, torch camera=" + mTorchId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mSensors.unregisterListener(mListener);
        mCameras.unregisterTorchCallback(mTorchCallback);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void onChop() {
        long now = SystemClock.elapsedRealtime();
        if (!Gestures.isChopEnabled(this)) return;
        if (now - mLastChop < DEBOUNCE_MS || mTorchId == null) return;
        mLastChop = now;
        try {
            mCameras.setTorchMode(mTorchId, !mTorchOn);
            mVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK));
            Log.i(TAG, "chop chop -> torch " + (!mTorchOn ? "on" : "off"));
        } catch (CameraAccessException | IllegalArgumentException e) {
            Log.w(TAG, "torch toggle failed", e);
        }
    }

    private void onPeek(String why) {
        if (!Gestures.isPeekEnabled(this) || mPower.isInteractive() || mStowed) return;
        wake(why, PEEK_MS);
    }

    /** Turns the screen on; timeoutMs > 0 releases after that, letting it time out normally. */
    @SuppressWarnings("deprecation")
    private void wake(String why, long timeoutMs) {
        if (mPower.isInteractive()) return;
        PowerManager.WakeLock wl = mPower.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG + ":" + why);
        wl.acquire(timeoutMs > 0 ? timeoutMs : 500);
        Log.i(TAG, why + " -> wake screen");
    }

    private String findTorch() {
        try {
            for (String id : mCameras.getCameraIdList()) {
                CameraCharacteristics c = mCameras.getCameraCharacteristics(id);
                Boolean flash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                if (Boolean.TRUE.equals(flash) && facing != null
                        && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return id;
                }
            }
        } catch (CameraAccessException e) {
            Log.w(TAG, "no camera access", e);
        }
        return null;
    }
}
