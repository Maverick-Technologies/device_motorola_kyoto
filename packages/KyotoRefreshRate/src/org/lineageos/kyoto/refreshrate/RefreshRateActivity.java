package org.lineageos.kyoto.refreshrate;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

/**
 * Settings > Display > Refresh rate. Full-page chooser laid out like Pixel's radio-button
 * settings pages (e.g. Screen resolution): large title, description, three options, footer.
 */
public class RefreshRateActivity extends Activity {

    private final int[] mRowIds = {R.id.row_adaptive, R.id.row_60, R.id.row_90};
    private final int[] mRadioIds = {R.id.radio_adaptive, R.id.radio_60, R.id.radio_90};
    private final int[] mModes = {
        RefreshRateMode.ADAPTIVE, RefreshRateMode.FIXED_60, RefreshRateMode.FIXED_90,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refresh_rate);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        for (int i = 0; i < mRowIds.length; i++) {
            final int mode = mModes[i];
            findViewById(mRowIds[i]).setOnClickListener(v -> {
                RefreshRateMode.set(this, mode);
                updateChecked();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateChecked();
    }

    private void updateChecked() {
        int current = RefreshRateMode.get(this);
        for (int i = 0; i < mRadioIds.length; i++) {
            RadioButton radio = findViewById(mRadioIds[i]);
            radio.setChecked(mModes[i] == current);
            View row = findViewById(mRowIds[i]);
            row.setSelected(mModes[i] == current);
        }
    }
}
