package fr.neamar.kiss.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.result.AppResult;
import fr.neamar.kiss.result.Result;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService {
    private static final String TAG = NotificationListener.class.getSimpleName();
    public static final String NOTIFICATION_PREFERENCES_NAME = "notifications";

    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getBaseContext().getSharedPreferences(NOTIFICATION_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        if (BuildConfig.DEBUG)  Log.i(TAG, "Notification listener connected");

        // Build a map of notifications currently displayed,
        // ordered per package
        StatusBarNotification[] sbns = getActiveNotifications();
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, Set<String>> notificationsByPackage = new HashMap<>();
        for (StatusBarNotification sbn : sbns) {
            if(isNotificationTrivial(sbn.getNotification())) {
                continue;
            }
            String title = sbn.getNotification().extras.getString("android.title");
            String packageName = sbn.getPackageName();
            if (!notificationsByPackage.containsKey(packageName+":"+title)) {
                notificationsByPackage.put(packageName+title, new HashSet<String>());
            }

            if (notificationsByPackage.containsKey(packageName+title)) {
                editor.putStringSet(packageName+title, notificationsByPackage.get(packageName+title));
            } else {
                editor.remove(packageName+title);
            }
            notificationsByPackage.get(packageName+title).add(Integer.toString(sbn.getId()));
        }

        editor.apply();
    }

    @Override
    public void onListenerDisconnected() {
        if (BuildConfig.DEBUG)  Log.i(TAG, "Notification listener disconnected");

        // Clean up everything we have in memory to ensure we don't keep displaying trailing dots.
        // We don't use .clear() to ensure listeners are properly called.
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> packages = prefs.getAll().keySet();

        for (String packageName : packages) {
            editor.remove(packageName);
        }
        editor.apply();

        super.onListenerDisconnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if(isNotificationTrivial(sbn.getNotification())) {
            return;
        }

        Pojo pojo  = KissApplication.getApplication(getApplicationContext()).getDataHandler().getAppProvider().findByPackageName(sbn.getPackageName());
        if (pojo!=null){
            pojo.setHasNotification(true);
            pojo.setNotificationPackage(sbn.getPackageName());
        }
        String title = sbn.getNotification().extras.getString("android.title");
        String category = sbn.getNotification().category;
        Set<String> currentNotifications = getCurrentNotificationsForPackage(sbn.getPackageName()+title);

        currentNotifications.add(Integer.toString(sbn.getId()));
        prefs.edit().putStringSet(sbn.getPackageName()+title, currentNotifications).apply();


        if (category!=null && (category.equals(Notification.CATEGORY_MESSAGE) || category.equals(Notification.CATEGORY_CALL))) {
            Pojo contact  = KissApplication.getApplication(getApplicationContext()).getDataHandler().getContactsProvider().findByName(title);
            if (contact!=null){
                prefs.edit().putStringSet(sbn.getPackageName()+title, currentNotifications).apply();
                contact.setHasNotification(true);
                contact.setNotificationPackage(sbn.getPackageName());
            }
        }
        if (BuildConfig.DEBUG) Log.v(TAG, "Added notification for " + sbn.getPackageName() + " title:" +title + " category:"+category);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if(isNotificationTrivial(sbn.getNotification())) {
            return;
        }
        Pojo pojo  = KissApplication.getApplication(getApplicationContext()).getDataHandler().getAppProvider().findByPackageName(sbn.getPackageName());
        if (pojo!=null){
            pojo.setHasNotification(false);
        }
        String title = sbn.getNotification().extras.getString("android.title");
        String category = sbn.getNotification().category;
        Set<String> currentNotifications = getCurrentNotificationsForPackage(sbn.getPackageName()+title);

        currentNotifications.remove(Integer.toString(sbn.getId()));

        SharedPreferences.Editor editor = prefs.edit();
        if (currentNotifications.isEmpty()) {
            // Clean up!
            editor.remove(sbn.getPackageName()+title);
        } else {
            editor.putStringSet(sbn.getPackageName()+title, currentNotifications);
        }
        editor.apply();

        if (category!=null && (category.equals(Notification.CATEGORY_MESSAGE) || category.equals(Notification.CATEGORY_CALL))) {
            Pojo contact  = KissApplication.getApplication(getApplicationContext()).getDataHandler().getContactsProvider().findByName(title);
            if (contact!=null){
                contact.setHasNotification(false);
            }
        }
        if (BuildConfig.DEBUG) Log.v(TAG, "Removed notification for " + sbn.getPackageName() + ": " + currentNotifications.toString());
    }

    public Set<String> getCurrentNotificationsForPackage(String packageName) {
        Set<String> currentNotifications = prefs.getStringSet(packageName, null);
        if (currentNotifications == null) {
            return new HashSet<>();
        } else {
            // The set returned by getStringSet() should NOT be modified
            // see https://developer.android.com/reference/android/content/SharedPreferences.html#getStringSet(java.lang.String,%2520java.util.Set%3Cjava.lang.String%3E)
            return new HashSet<>(currentNotifications);
        }
    }

    // Low priority notifications should not be displayed
    public boolean isNotificationTrivial(Notification notification) {
        return notification.priority <= Notification.PRIORITY_MIN;
    }
}