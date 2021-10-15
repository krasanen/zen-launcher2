package fi.zmengames.zen;

import static fi.zmengames.zen.ZEvent.State.DISABLE_PROXIMITY;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.SwitchPreference;

public class ProximityLockSwitch extends SwitchPreference {
    public ProximityLockSwitch(Context context) {
        super(context);
    }
    public ProximityLockSwitch(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchPreferenceStyle);

    }
    public ProximityLockSwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onClick() {
        if (isChecked()) {
            disableProximityLock();
        } else {
            enableProximityLock();
        }
        super.onClick
                ();
    }

    private void disableProximityLock() {
        if (BuildConfig.DEBUG) Log.v("ProximityLockSwitch", "disableProximityLock");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        Intent proximity = new Intent(getContext(), LauncherService.class);
        proximity.setAction(DISABLE_PROXIMITY.toString());
        prefs.edit().putBoolean("proximity-switch-lock", false).commit();
        KissApplication.startLaucherService(proximity, getContext());
    }

    private void enableProximityLock() {
        if (BuildConfig.DEBUG) Log.v("ProximityLockSwitch", "enableProximityLock");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean("device-admin-switch", true).commit();
    }
}
