package fi.zmengames.zen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;

import static fi.zmengames.zen.LauncherService.DISABLE_PROXIMITY;
import static fi.zmengames.zen.LauncherService.ENABLE_PROXIMITY;
import static fi.zmengames.zen.LauncherService.SCREEN_OFF;
import static fi.zmengames.zen.LauncherService.SCREEN_ON;


public class ScreenReceiver extends BroadcastReceiver {
    private static final String TAG = ScreenReceiver.class.getSimpleName();
    private final Context context;

    public ScreenReceiver(Context context) {
        this.context = context;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG) Log.d(TAG, intent.getAction());

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "ACTION_SCREEN_OFF");
            Intent proximity = new Intent(context, LauncherService.class);
            proximity.setAction(SCREEN_OFF);
            KissApplication.startLaucherService(proximity, context);
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            // and do whatever you need to do here
            if (BuildConfig.DEBUG) Log.d(TAG, "ACTION_SCREEN_ON");
            Intent proximity = new Intent(context, LauncherService.class);
            proximity.setAction(SCREEN_ON);
            KissApplication.startLaucherService(proximity, context);

        }

    }

}