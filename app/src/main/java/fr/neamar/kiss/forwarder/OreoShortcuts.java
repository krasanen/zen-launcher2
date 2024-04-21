package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.content.pm.LauncherApps;
import android.util.Log;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.preference.DefaultLauncherPreference;
import fr.neamar.kiss.utils.ShortcutUtil;

import static fi.zmengames.zen.ZEvent.State.ACTION_SET_DEFAULT_LAUNCHER;

public class OreoShortcuts extends Forwarder {
    private static final String TAG = OreoShortcuts.class.getSimpleName();
    OreoShortcuts(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        // Shortcuts in Android O
        ShortcutRecord record = new ShortcutRecord();
        record.name = mainActivity.getString(R.string.default_launcher_title);
        record.packageName = "zen";
        record.intentUri = ACTION_SET_DEFAULT_LAUNCHER.toString();
        DataHandler dataHandler = KissApplication.getApplication(mainActivity).getDataHandler();
        dataHandler.addShortcut(record);
        if (ShortcutUtil.areShortcutsEnabled(mainActivity)) {

            // On first run save all shortcuts
            if (prefs.getBoolean("first-run-shortcuts", true)) {

                DefaultLauncherPreference.AsyncTaskCompleteListener callback = (result, activity) -> {
                    // Handle the result here
                    if (result) {
                        ShortcutUtil.addAllShortcuts(mainActivity);
                        // Set flag to false
                        prefs.edit().putBoolean("first-run-shortcuts", false).apply();
                    } else {
                        if (BuildConfig.DEBUG) Log.d(TAG,"not a default launcher");
                    }
                };

                DefaultLauncherPreference.ResolveDefaultLauncherTask myAsyncTask = new DefaultLauncherPreference.ResolveDefaultLauncherTask(callback,mainActivity);
                myAsyncTask.execute();

            }

            Intent intent = mainActivity.getIntent();
            if (intent != null) {
                final String action = intent.getAction();
                if (LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT.equals(action)) {
                    // Save single shortcut via a pin request
                    ShortcutUtil.addShortcut(mainActivity, intent);
                }
            }
        }

    }

}
