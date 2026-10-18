package org.lineageos.kyoto.actions;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Switch;

/** Settings > System > Gestures > Moto Actions: one switch per gesture. */
public class MotoActionsActivity extends Activity {

    private interface Getter { boolean get(); }
    private interface Setter { void set(boolean enabled); }

    private Switch mChop, mTwist, mPeek, mDoubleTap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moto_actions);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        mChop = bind(R.id.row_chop, R.id.switch_chop,
                () -> Gestures.isChopEnabled(this), on -> Gestures.setChopEnabled(this, on));
        mTwist = bind(R.id.row_twist, R.id.switch_twist,
                () -> Gestures.isTwistEnabled(this), on -> Gestures.setTwistEnabled(this, on));
        mPeek = bind(R.id.row_peek, R.id.switch_peek,
                () -> Gestures.isPeekEnabled(this), on -> Gestures.setPeekEnabled(this, on));
        mDoubleTap = bind(R.id.row_double_tap, R.id.switch_double_tap,
                () -> Gestures.isDoubleTapEnabled(this), on -> Gestures.setDoubleTapEnabled(this, on));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Twist and double tap are system settings that can also change elsewhere.
        mChop.setChecked(Gestures.isChopEnabled(this));
        mTwist.setChecked(Gestures.isTwistEnabled(this));
        mPeek.setChecked(Gestures.isPeekEnabled(this));
        mDoubleTap.setChecked(Gestures.isDoubleTapEnabled(this));
    }

    private Switch bind(int rowId, int switchId, Getter getter, Setter setter) {
        Switch toggle = findViewById(switchId);
        findViewById(rowId).setOnClickListener(v -> {
            setter.set(!getter.get());
            toggle.setChecked(getter.get());
        });
        return toggle;
    }
}
