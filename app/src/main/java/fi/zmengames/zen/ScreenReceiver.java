package fi.zmengames.zen;

import static fi.zmengames.zen.ZEvent.State.SCREEN_OFF;
import static fi.zmengames.zen.ZEvent.State.SCREEN_ON;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;

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
            proximity.setAction(SCREEN_OFF.toString());
            KissApplication.startLaucherService(proximity, context);
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            // and do whatever you need to do here
            if (BuildConfig.DEBUG) Log.d(TAG, "ACTION_SCREEN_ON");
            Intent proximity = new Intent(context, LauncherService.class);
            proximity.setAction(SCREEN_ON.toString());
            KissApplication.startLaucherService(proximity, context);

        }

    }

}