package fr.neamar.kiss.loader;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Settings;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.SettingsPojo;

public class LoadSettingsPojos extends LoadPojos<SettingsPojo> {

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
    @Override
    protected ArrayList<SettingsPojo> doInBackground(Void... params) {
        ArrayList<SettingsPojo> settings = new ArrayList<>();

        if(context.get() == null) {
            return settings;
        }

        PackageManager pm = context.get().getPackageManager();
        addPredefinedSettingPojos(settings,pm);
        Class<Settings> clazz = Settings.class;
        Field[] arr = clazz.getFields(); // Get all public fields of your class
        for (Field f : arr) {
            if (f.getName().contains("ACTION_") && f.getType().equals(String.class)) { // check if field is a String
                String s = null; // get value of each field
                try {
                    s = (String)f.get(null);
                    if (s.startsWith("android.settings.")) {
                        Intent testIntent = new Intent(s);
                        List<ResolveInfo> infos =  pm.queryIntentActivities(testIntent, 0);
                        if (infos.size() > 0) {
                            SettingsPojo pojo = createPojo(s.substring(17),
                                    s, getActivityIcon(context.get(), infos.get(0).activityInfo.packageName, infos.get(0).activityInfo.name));
                            if(!settings.contains(pojo)) {
                                settings.add(pojo);
                            }
                        } else {
                            //No Application can handle your intent
                        }

                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                // add s to a List
                System.out.println(s);
            }

        }
        return settings;
    }
    private void addPredefinedSettingPojos(ArrayList<SettingsPojo> settings, PackageManager pm){
        settings.add(createPojo(context.get().getString(R.string.settings_airplane),
                Settings.ACTION_AIRPLANE_MODE_SETTINGS, context.get().getResources().getDrawable(R.drawable.setting_airplane)));
        settings.add(createPojo(context.get().getString(R.string.settings_device_info),
                Settings.ACTION_DEVICE_INFO_SETTINGS, context.get().getResources().getDrawable(R.drawable.setting_info)));
        settings.add(createPojo(context.get().getString(R.string.settings_applications),
                Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, context.get().getResources().getDrawable(R.drawable.setting_apps)));
        settings.add(createPojo(context.get().getString(R.string.settings_connectivity),
                Settings.ACTION_WIRELESS_SETTINGS, context.get().getResources().getDrawable(R.drawable.setting_wifi)));
        settings.add(createPojo(context.get().getString(R.string.settings_storage),
                Settings.ACTION_INTERNAL_STORAGE_SETTINGS, context.get().getResources().getDrawable(R.drawable.setting_storage)));
        settings.add(createPojo(context.get().getString(R.string.settings_accessibility),
                Settings.ACTION_ACCESSIBILITY_SETTINGS, context.get().getResources().getDrawable(R.drawable.setting_accessibility)));
        settings.add(createPojo(context.get().getString(R.string.settings_battery),
                Intent.ACTION_POWER_USAGE_SUMMARY, context.get().getResources().getDrawable(R.drawable.setting_battery)));
        int resId = context.get().getResources().getIdentifier("com.android.settings.TetherSettings",null,null);
        if (resId!=0) {
            settings.add(createPojo(context.get().getString(R.string.settings_tethering),
                    "com.android.settings",
                    context.get().getResources().getDrawable(resId)));
        }
        settings.add(createPojo(context.get().getString(R.string.settings_sound),
                Settings.ACTION_SOUND_SETTINGS, context.get().getResources().getDrawable(R.drawable.setting_dev)));
        settings.add(createPojo(context.get().getString(R.string.settings_display),
                Settings.ACTION_DISPLAY_SETTINGS, context.get().getResources().getDrawable(R.drawable.setting_dev)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
                settings.add(createPojo(context.get().getString(R.string.settings_nfc),
                        Settings.ACTION_NFC_SETTINGS, context.get().getResources().getDrawable(R.drawable.setting_nfc)));
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.add(createPojo(context.get().getString(R.string.settings_dev),
                    Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, context.get().getResources().getDrawable(R.drawable.setting_dev)));
        }
    }


    private SettingsPojo createPojo(String name, String settingName, Drawable resId) {
        SettingsPojo pojo = new SettingsPojo();
        pojo.id = pojoScheme + settingName.toLowerCase(Locale.ENGLISH);
        pojo.setName(name, true);
        pojo.settingName = settingName;
        pojo.icon = resId;

        return pojo;
    }
}
