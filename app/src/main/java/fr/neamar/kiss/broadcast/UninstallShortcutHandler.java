package fi.zmengames.zlauncher.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import fi.zmengames.zlauncher.DataHandler;
import fi.zmengames.zlauncher.KissApplication;
import fi.zmengames.zlauncher.dataprovider.ShortcutsProvider;
import fi.zmengames.zlauncher.pojo.ShortcutsPojo;

public class UninstallShortcutHandler extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent data) {

        DataHandler dh = KissApplication.getApplication(context).getDataHandler();
        ShortcutsProvider sp = dh.getShortcutsProvider();

        if (sp == null)
            return;

        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Log.d("onReceive", "Uninstall shortcut " + name);

        ShortcutsPojo pojo = (ShortcutsPojo) sp.findByName(name);
        if (pojo == null) {
            Log.d("onReceive", "Shortcut " + name + " not found");
            return;
        }

        dh.removeShortcut(pojo);

    }

}
