package fi.zmengames.zen;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.SwitchPreference;

import static android.content.Context.DEVICE_POLICY_SERVICE;
import static fr.neamar.kiss.MainActivity.REQUEST_DEVICE_ADMIN_LOCK;

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
