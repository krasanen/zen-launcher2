package fi.zmengames.zen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import fr.neamar.kiss.BuildConfig;

public class MediaMountListener extends BroadcastReceiver {
    private static final String TAG = MediaMountListener.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Media mounted, reload apps, action:"+intent.getAction());

        ZEvent event = new ZEvent(ZEvent.State.RELOAD_APPS);
        EventBus.getDefault().postSticky(event);

    }
}
