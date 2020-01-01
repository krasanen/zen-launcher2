package fr.neamar.kiss.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;



import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.pojo.ShortcutsPojo;
import fr.neamar.kiss.shortcut.SaveAllOreoShortcutsAsync;
import fr.neamar.kiss.shortcut.SaveSingleOreoShortcutAsync;

import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED;

public class ShortcutUtil {

    private static final String TAG = ShortcutUtil.class.getSimpleName();

    /**
     * @return shortcut id generated from shortcut name
     */
    public static String generateShortcutId(String shortcutName){
        return ShortcutsPojo.SCHEME + shortcutName.toLowerCase(Locale.ROOT);
    }

    /**
     * @return true if shortcuts are enabled in settings and android version is higher or equals android 8
     */
    public static boolean areShortcutsEnabled(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                prefs.getBoolean("enable-shortcuts", true);

    }

    /**
     * Save all oreo shortcuts to DB
     */
    public static void addAllShortcuts(Context context){
        new SaveAllOreoShortcutsAsync(context).execute();
    }

    /**
     * Save single shortcut to DB via pin request
     */
    @TargetApi(Build.VERSION_CODES.O)
    public static void addShortcut(Context context, Intent intent){
        new SaveSingleOreoShortcutAsync(context, intent).execute();
    }

    /**
     * Remove all shortcuts saved in the database
     */
    public static void removeAllShortcuts(Context context){
        DBHelper.removeAllShortcuts(context);
    }

    /**
     * @return all shortcuts from all applications available on the device
     */
    @TargetApi(Build.VERSION_CODES.O)
    public static List<ShortcutInfo> getAllShortcuts(Context context) {
        return getShortcut(context, null);
    }

    /**
     * @return all shortcuts for given package name
     */
    @TargetApi(Build.VERSION_CODES.O)
    public static List<ShortcutInfo> getShortcut(Context context, String packageName) {
        List<ShortcutInfo> shortcutInfoList = new ArrayList<>();

        UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

        LauncherApps.ShortcutQuery shortcutQuery = new LauncherApps.ShortcutQuery();
        shortcutQuery.setQueryFlags(FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED);

        if(!TextUtils.isEmpty(packageName)){
            shortcutQuery.setPackage(packageName);
        }

        for (android.os.UserHandle profile : manager.getUserProfiles()) {
            shortcutInfoList.addAll(launcherApps.getShortcuts(shortcutQuery, profile));
        }

        return shortcutInfoList;
    }
    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
    /**
     * Create ShortcutPojo from ShortcutInfo
     */
    @TargetApi(Build.VERSION_CODES.O)
    public static ShortcutsPojo createShortcutPojo(Context context, ShortcutInfo shortcutInfo, boolean includePackageName){

        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        // id isn't used after being saved in the DB.
        String id = ShortcutsPojo.SCHEME + ShortcutsPojo.OREO_PREFIX + shortcutInfo.getId();

        final Drawable iconDrawable = launcherApps.getShortcutIconDrawable(shortcutInfo, 0);
        ShortcutsPojo pojo = new ShortcutsPojo(id, shortcutInfo.getPackage(), shortcutInfo.getId(),
                drawableToBitmap(iconDrawable));

        String appName = getAppNameFromPackageName(context, shortcutInfo.getPackage());

        if (shortcutInfo.getShortLabel() != null) {
            if(includePackageName && !TextUtils.isEmpty(appName)){
                pojo.setName(appName + ": " + shortcutInfo.getShortLabel().toString());
            } else {
                pojo.setName(shortcutInfo.getShortLabel().toString());
            }
        } else if (shortcutInfo.getLongLabel() != null) {
            if(includePackageName && !TextUtils.isEmpty(appName)){
                pojo.setName(appName + ": " + shortcutInfo.getLongLabel().toString());
            } else {
                pojo.setName(shortcutInfo.getLongLabel().toString());
            }
        } else {
            Log.d(TAG, "Invalid shortcut " + pojo.id + ", ignoring");
            return null;
        }

        return pojo;
    }

    /**
     *
     * @return App name from package name
     */
    public static String getAppNameFromPackageName(Context context, String Packagename) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo info = packageManager.getApplicationInfo(Packagename, PackageManager.GET_META_DATA);
            String appName = (String) packageManager.getApplicationLabel(info);
            return appName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }



}