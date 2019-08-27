package fi.zmengames.zen;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.SwitchPreference;

import static fi.zmengames.zen.LauncherService.DISABLE_PROXIMITY;
import static fr.neamar.kiss.MainActivity.REQUEST_DEVICE_ADMIN_PROXIMITY_LOCK;

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
        if (!prefs.getBoolean("double-click-locks-screen", false)) {
            KissApplication.getApplication(getContext()).getMainActivity().disableDeviceAdmin();
        }
        Intent proximity = new Intent(getContext(), LauncherService.class);
        proximity.setAction(DISABLE_PROXIMITY);
        prefs.edit().putBoolean("proximity-switch-lock", false).commit();
        KissApplication.startLaucherService(proximity, getContext());
    }

    private void enableProximityLock() {
        if (BuildConfig.DEBUG) Log.v("ProximityLockSwitch", "enableProximityLock");
        ComponentName compName = new ComponentName(getContext(), ZenAdmin.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Permission is needed to be able to lock device directly from Zen Launcher");
        KissApplication.getApplication(getContext()).getMainActivity().startActivityForResult(intent, REQUEST_DEVICE_ADMIN_PROXIMITY_LOCK);
    }
}
