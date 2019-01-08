package fr.neamar.kiss;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import fi.zmengames.zlauncher.LauncherService;

public class KissApplication extends Application {
    /**
     * Number of ms to wait, after a click occurred, to record a launch
     * Setting this value to 0 removes all animations
     */
    public static final int TOUCH_DELAY = 120;
    private DataHandler dataHandler;
    private RootHandler rootHandler;
    private IconsHandler iconsPackHandler;
    private MainActivity mainActivity;

    public static KissApplication getApplication(Context context) {
        return (KissApplication) context.getApplicationContext();
    }

    public MainActivity getMainActivity() {
        return mainActivity;
    }

    public DataHandler getDataHandler() {
        if (dataHandler == null) {
            dataHandler = new DataHandler(this);
        }
        return dataHandler;
    }

    public void setDataHandler(DataHandler newDataHandler) {
        dataHandler = newDataHandler;
    }
    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public RootHandler getRootHandler() {
        if (rootHandler == null) {
            rootHandler = new RootHandler(this);
        }
        return rootHandler;
    }

    public void resetRootHandler(Context ctx) {
        rootHandler.resetRootHandler(ctx);
    }

    public IconsHandler getIconsHandler() {
        if (iconsPackHandler == null) {
            iconsPackHandler = new IconsHandler(this);
        }

        return iconsPackHandler;
    }

    public void resetIconsHandler() {
        iconsPackHandler = new IconsHandler(this);
    }

    public static void startLaucherService(Intent intent, Context context){
        try {
            context.startService(intent);
        }catch (IllegalStateException e){
            if(BuildConfig.DEBUG) Log.d("KissApplication", "app in background?");
        }
    }
}
