package fi.zmengames.zen;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.SwitchPreference;

public class VibrateOnAlarmSwitch extends SwitchPreference {
    public VibrateOnAlarmSwitch(Context context) {
        super(context);
    }
    public VibrateOnAlarmSwitch(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchPreferenceStyle);

    }
    public VibrateOnAlarmSwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    protected void onBindView(View view) {
        super.onBindView(view);
    }

    @Override
    protected void onClick() {
        if (BuildConfig.DEBUG) Log.v("VibrateOnAlarmSwitch", "onClick");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (isChecked()) {
            if (prefs.getBoolean("vibrate-on-alarm", false)) {
                enableVibrate(true);
            }
        } else {
            enableVibrate(false);
        }
        super.onClick
                ();
    }

    private void enableVibrate(boolean b) {
    }


}
