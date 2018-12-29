package fr.neamar.kiss.loader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        Class<Settings> clazz = Settings.class;
        Field[] arr = clazz.getFields(); // Get all public fields of your class
        for (Field f : arr) {
            if (f.getName().contains("ACTION") && f.getType().equals(String.class)) { // check if field is a String
                String s = null; // get value of each field
                try {
                    s = (String)f.get(null);
                    if (s.startsWith("android.settings.")) {
                        Intent testIntent = new Intent(s);
                        List<ResolveInfo> infos =  pm.queryIntentActivities(testIntent, 0);
                        if (infos.size() > 0) {
                            settings.add(createPojo(s.substring(17),
                                    s, getActivityIcon(context.get(), infos.get(0).activityInfo.packageName, infos.get(0).activityInfo.name)));

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


    private SettingsPojo createPojo(String name, String settingName, Drawable resId) {
        SettingsPojo pojo = new SettingsPojo();
        pojo.id = pojoScheme + settingName.toLowerCase(Locale.ENGLISH);
        pojo.setName(name, true);
        pojo.settingName = settingName;
        pojo.icon = resId;

        return pojo;
    }
}
