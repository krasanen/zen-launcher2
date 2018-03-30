package fi.zmengames.zlauncher.forwarder;

import android.content.SharedPreferences;

import fi.zmengames.zlauncher.MainActivity;

abstract class Forwarder {
    final MainActivity mainActivity;
    final SharedPreferences prefs;

    Forwarder(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.prefs = mainActivity.prefs;
    }
}