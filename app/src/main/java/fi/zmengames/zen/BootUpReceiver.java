package fi.zmengames.zen;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;

public class BootUpReceiver extends BroadcastReceiver {
    private static final String TAG = BootUpReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        ZEvent event = new ZEvent(ZEvent.State.RELOAD_APPS);
        EventBus.getDefault().postSticky(event);
    }
}