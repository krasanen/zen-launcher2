package fr.neamar.kiss.preference;

import android.content.Context;
import android.os.PowerManager;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.util.Log;

import fr.neamar.kiss.BuildConfig;

public class ProximitySwitch extends SwitchPreference {
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;

    public ProximitySwitch(Context context) {
        this(context, null);
    }

    public ProximitySwitch(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchPreferenceStyle);
        mPowerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
    }

    public ProximitySwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onClick() {
        if (!isChecked()) {
            turnOffScreen();
        } else {
            turnOnScreen();
        }
        super.onClick();
    }

    public void turnOnScreen(){
        // turn on screen
        if (BuildConfig.DEBUG) Log.v("ProximitySwitch", "ON!");
        if (mWakeLock!=null) {
            mWakeLock.release();
        }
    }


    public void turnOffScreen(){
        // turn off screen
        if (BuildConfig.DEBUG) Log.v("ProximitySwitch", "OFF!");
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, ProximitySwitch.class.getName());
        mWakeLock.acquire();
    }
}
