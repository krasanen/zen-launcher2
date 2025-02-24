package fr.neamar.kiss;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import fi.zmengames.zen.ZEvent;

public class SamsungBadgeObserver extends ContentObserver {
    private static final String TAG = SamsungBadgeObserver.class.getSimpleName();
    private final Context context;

    public SamsungBadgeObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
    }

    @Override
    public void onChange(boolean selfChange, Uri pUri) {
        super.onChange(selfChange);
        if (BuildConfig.DEBUG) Log.i(TAG, "onChange: selfChange: "+selfChange + " Uri:"+pUri);
        // query badge status on content provider
        loadBadges(context);
    }

    public static boolean providerExists(Context context) {
        Uri uri = Uri.parse("content://com.sec.badge/apps");
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
        }catch (SecurityException e){
            if (BuildConfig.DEBUG) Log.i(TAG,"e:"+e);
        }
        boolean exists = cursor != null;

        if(cursor != null)
            cursor.close();
        if (BuildConfig.DEBUG) Log.i(TAG, "providerExists: "+exists);
        return exists;
    }
    private final static Executor mExecutor = Executors.newSingleThreadExecutor();
    /** Queries current badge status on ContentResolver for all packages on it
     * Updates the badges count on BadgeHandler
     */
    public static void loadBadges(Context context) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (BuildConfig.DEBUG) Log.i(TAG, "loadBadges");

                Uri uri = Uri.parse("content://com.sec.badge/apps");
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                // Return if cursor is null. Means provider does not exists
                if (cursor == null) {
                    return;
                }

                try {
                    if (!cursor.moveToFirst()) {
                        // No results. Nothing to query
                        return;
                    }
                    BadgeHandler badgeHandler = KissApplication.getApplication(context).getDataHandler().getBadgeHandler();

                    cursor.moveToPosition(-1);
                    while (cursor.moveToNext()) {
                        String packageName = cursor.getString(1);

                        // java.lang.SecurityException: Permission Denial: writing com.sec.android.provider.badge.BadgeProvider uri content://com.sec.badge/apps from pid=28449, uid=10602 requires com.sec.android.provider.badge.permission.WRITE, or grantUriPermission()
                        //resetBadgeCount(context,packageName);

                        int badgeCount = cursor.getInt(3);


                        if (BadgeHandler.getBadgeCount(packageName) != badgeCount) {
                            if (BuildConfig.DEBUG) {
                                Log.i(TAG, "loadBadges, setBadgeCount, packageName:" + packageName + " Badges:" + badgeCount);
                            }
                            badgeHandler.setBadgeCount(packageName, badgeCount);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        });
    }
    private static void resetBadgeCount(Context context, String packageName){
        ContentValues cv = new ContentValues();
        //context.grantUriPermission("com.sec.android.provider.badge.BadgeProvider", Uri.parse("content://com.sec.badge/apps"), Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cv.put("badgecount", 0);
        context.getContentResolver().update(Uri.parse("content://com.sec.badge/apps"), cv, "package=?", new String[] {packageName});

    }
}