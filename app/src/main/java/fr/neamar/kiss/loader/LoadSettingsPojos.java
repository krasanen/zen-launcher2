package fr.neamar.kiss.loader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;

import java.util.List;
import java.util.Locale;

import androidx.annotation.DrawableRes;

import fi.zmengames.zen.LauncherService;
import fr.neamar.kiss.BuildConfig;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.SettingsPojo;

import static fr.neamar.kiss.forwarder.Favorites.DEFAULT_RESOLVER;


public class LoadSettingsPojos extends LoadPojos<SettingsPojo> {
    private static final String TAG = LoadSettingsPojos.class.getSimpleName();

    public LoadSettingsPojos(Context context) {
        super(context, "setting://");
    }

    public Drawable getActivityIcon(
            Context context,
            String packageName, String activityName) {

        PackageManager packageManager = context.getPackageManager();

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, activityName));
        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);
        Drawable drawable;
        if (resolveInfo.activityInfo.icon!=0) {
            drawable = resolveInfo.loadIcon(packageManager);
        } else {
            drawable = context.getResources().getDrawable(android.R.drawable.ic_menu_preferences);
        }
        return drawable;
    }

    private String querySettingPkgName() {
        Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
        List<ResolveInfo> resolveInfos = context.get().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfos == null || resolveInfos.size() == 0) {
            return "";
        }
        return resolveInfos.get(0).loadLabel(context.get().getPackageManager()).toString();
    }

    @Override
    protected ArrayList<SettingsPojo> doInBackground(Void... params) {
        String settingsPkgName = querySettingPkgName();
        ArrayList<SettingsPojo> settings = new ArrayList<>();

        if (context.get() == null) {
            return settings;
        }

        PackageManager pm = context.get().getPackageManager();
        addPredefinedSettingPojos(settings, pm);

        Class<Settings> clazz = Settings.class;
        Field[] arr = clazz.getFields(); // Get all public fields of your class
        for (Field f : arr) {
            outerloop:
            if (f.getName().contains("ACTION_") && f.getType().equals(String.class)) { // check if field is a String
                if (BuildConfig.DEBUG) Log.i(TAG, "f: " + f);
                String s = null; // get value of each field
                try {
                    s = (String)f.get(null);
                    if (BuildConfig.DEBUG) Log.i(TAG, "s: " + s);
                    Intent testIntent = new Intent(s);
                    ResolveInfo resolveInfo = pm.resolveActivity(testIntent, PackageManager.MATCH_SYSTEM_ONLY);
                    if (resolveInfo != null) {
                        if ((resolveInfo.activityInfo.name != null) && (!resolveInfo.activityInfo.name.equals(DEFAULT_RESOLVER))) {
                            String label = resolveInfo.loadLabel(pm).toString();
                            if (!label.isEmpty()) {
                                if (BuildConfig.DEBUG) Log.i(TAG, "loadLabel: " + resolveInfo.loadLabel(pm).toString());
                                for (SettingsPojo pojo:settings)
                                    if (pojo.getName().equals(label)||label.equals(settingsPkgName)||label.isEmpty()){
                                        break outerloop;
                                    }
                                }
                                SettingsPojo pojo = createPojo(label, resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                                if (!settings.contains(pojo)) {
                                    settings.add(pojo);
                                }
                            }
                        } else {
                            //No Application can handle your intent
                        }

                } catch (Exception e) {

                }

            }
        }
        return settings;
    }

    private void addPredefinedSettingPojos(ArrayList<SettingsPojo> settings, PackageManager pm){
        settings.add(createPojo(context.get().getString(R.string.settings_airplane),
                Settings.ACTION_AIRPLANE_MODE_SETTINGS, R.drawable.setting_airplane));
        settings.add(createPojo(context.get().getString(R.string.settings_device_info),
                Settings.ACTION_DEVICE_INFO_SETTINGS, R.drawable.setting_info));
        settings.add(createPojo(context.get().getString(R.string.settings_applications),
                Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, R.drawable.apps_grid));
        settings.add(createPojo(context.get().getString(R.string.settings_connectivity),
                Settings.ACTION_WIRELESS_SETTINGS, R.drawable.setting_wifi));
        settings.add(createPojo(context.get().getString(R.string.settings_storage),
                Settings.ACTION_INTERNAL_STORAGE_SETTINGS, R.drawable.setting_storage));
        settings.add(createPojo(context.get().getString(R.string.settings_accessibility),
                Settings.ACTION_ACCESSIBILITY_SETTINGS, R.drawable.setting_accessibility));
        settings.add(createPojo(context.get().getString(R.string.settings_battery),
                Intent.ACTION_POWER_USAGE_SUMMARY, R.drawable.setting_battery));
        settings.add(createPojo(context.get().getString(R.string.settings_tethering), "com.android.settings",
                "com.android.settings.TetherSettings", R.drawable.setting_tethering));
        settings.add(createPojo(context.get().getString(R.string.settings_sound),
                Settings.ACTION_SOUND_SETTINGS, R.drawable.settings_sound));
        settings.add(createPojo(context.get().getString(R.string.settings_display),
                Settings.ACTION_DISPLAY_SETTINGS, R.drawable.setting_dev));

        settings.add(createPojo(context.get().getString(R.string.menu_settings),
                Settings.ACTION_DISPLAY_SETTINGS, R.drawable.settings));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
                settings.add(createPojo(context.get().getString(R.string.settings_nfc),
                        Settings.ACTION_NFC_SETTINGS, R.drawable.setting_nfc));
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.add(createPojo(context.get().getString(R.string.settings_dev),
                    Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, R.drawable.setting_dev));
        }
        settings.add(createPojo(context.get().getString(R.string.wifi_on),
                MainActivity.WIFI_ON, R.drawable.setting_wifi));
        settings.add(createPojo(context.get().getString(R.string.wifi_off),
                MainActivity.WIFI_OFF, R.drawable.setting_wifi));

        settings.add(createPojo(context.get().getString(R.string.nightmodeOn),
                LauncherService.NIGHTMODE_ON, R.drawable.settings));

        settings.add(createPojo(context.get().getString(R.string.nightmodeOff),
                LauncherService.NIGHTMODE_OFF, R.drawable.settings));

        settings.add(createPojo(context.get().getString(R.string.flashlight_on),
                MainActivity.FLASHLIGHT_ON, R.drawable.lightbulp_on));

        settings.add(createPojo(context.get().getString(R.string.flashlight_off),
                MainActivity.FLASHLIGHT_OFF, R.drawable.lightbulp));

        settings.add(createPojo(context.get().getString(R.string.menu_wallpaper),
                MainActivity.UPDATE_WALLPAPER, R.drawable.settings));

        settings.add(createPojo(context.get().getString(R.string.barcode_reader),
                MainActivity.BARCODE_READER, R.drawable.barcode_scan));
    }

    private SettingsPojo createPojo(String name, String packageName, String settingName,
                                    @DrawableRes int resId) {
        SettingsPojo pojo = new SettingsPojo(getId(settingName), settingName, packageName, resId);
        assingName(pojo, name);
        return pojo;
    }

    private SettingsPojo createPojo(String name, String settingName, @DrawableRes  int resId) {
        SettingsPojo pojo = new SettingsPojo(getId(settingName), settingName, resId);
        assingName(pojo, name);
        return pojo;
    }
    private SettingsPojo createPojo(String name, String packageName, String settingName) {
        SettingsPojo pojo = new SettingsPojo(getId(settingName), settingName, packageName, R.drawable.settings);
        assingName(pojo, name);
        return pojo;
    }

    private String getId(String settingName) {
        return pojoScheme + settingName.toLowerCase(Locale.ENGLISH);
    }

    private void assingName(SettingsPojo pojo, String name) {
        pojo.setName(name, true);
    }
}
