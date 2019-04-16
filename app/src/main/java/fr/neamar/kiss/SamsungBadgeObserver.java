package fr.neamar.kiss;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import fr.neamar.kiss.BadgeHandler;
import fr.neamar.kiss.KissApplication;

public class SamsungBadgeObserver extends ContentObserver {
    private static final String TAG = SamsungBadgeObserver.class.getSimpleName();
    private Context context;

    public SamsungBadgeObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
    }

    @Override
    public void onChange(boolean selfChange, Uri pUri) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onChange: Uri:"+pUri);
        // query badge status on content provider
        loadBadges(context);

    }

    public static boolean providerExists(Context context) {
        Uri uri = Uri.parse("content://com.sec.badge/apps");
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
        }catch (SecurityException e){
            Log.d(TAG,"e:"+e);
        }
        boolean exists = cursor != null;

        if(cursor != null)
            cursor.close();
        if (BuildConfig.DEBUG) Log.d(TAG, "providerExists: "+exists);
        return exists;
    }

    /** Queries current badge status on ContentResolver for all packages on it
     * Updates the badges count on BadgeHandler
     */
    public static void loadBadges(Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
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
                        int badgeCount = cursor.getInt(3);
                        if (BuildConfig.DEBUG) Log.d(TAG, "loadBadges, packageName:"+packageName+ " Badges:"+badgeCount);
                        if (badgeHandler.getBadgeCount(packageName)!=badgeCount) {
                            badgeHandler.setBadgeCount(packageName, badgeCount);
                        }
                    }
                } finally {
                    cursor.close();
                }


            }
        }).start();
    }
}