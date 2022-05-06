package fr.neamar.kiss.notification;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fi.zmengames.zen.ZEvent;
import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.dataprovider.AppProvider;
import fr.neamar.kiss.dataprovider.ContactsProvider;
import fr.neamar.kiss.pojo.Pojo;

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
    private static String extractStringFromExtra(Bundle extras, String key) {
        Object extra = extras.get(key);
        if (extra == null) {
            return null;
        } else if (extra instanceof String) {
            return (String) extra;
        } else if (extra instanceof SpannableString) {
            return extra.toString();
        } else {
            Log.e(TAG, "Don't know how to extract text from extra of type: " + extra.getClass().getCanonicalName());
            return null;
        }
    }
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        if (BuildConfig.DEBUG)  Log.i(TAG, "Notification listener connected");

        // Build a map of notifications currently displayed,
        // ordered per package
        try {
            StatusBarNotification[] sbns = getActiveNotifications();
            SharedPreferences.Editor editor = prefs.edit();
            Map<String, Set<String>> notificationsByPackage = new HashMap<>();
            for (StatusBarNotification sbn : sbns) {
                if(isNotificationTrivial(sbn.getNotification())) {
                    continue;
                }
                String title = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    title = extractStringFromExtra(sbn.getNotification().extras, "android.title");
                }
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
        } catch (SecurityException e){
            e.printStackTrace();
        }


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

        String title = null;
        String category = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            title = extractStringFromExtra(sbn.getNotification().extras, "android.title");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                category = sbn.getNotification().category;
            }
        }

        Set<String> currentNotifications = getCurrentNotificationsForPackage(sbn.getPackageName()+title);

        currentNotifications.add(Integer.toString(sbn.getId()));



        if (title !=null && category!=null && (category.equals(Notification.CATEGORY_MESSAGE) || category.equals(Notification.CATEGORY_CALL))) {
            ContactsProvider contactsProvider = KissApplication.getApplication(getApplicationContext()).getDataHandler().getContactsProvider();
            Pojo contact = null;
            if (contactsProvider != null) {
                contact = contactsProvider.findByName(title);
            }
            if (contact!=null){
                if (BuildConfig.DEBUG) Log.v(TAG, "1. Package:"+sbn.getPackageName()+" title:"+title);
                prefs.edit().putStringSet(sbn.getPackageName()+title, currentNotifications).apply();
                contact.incrementNotifications();
                contact.setNotificationPackage(sbn.getPackageName());
            }
        }else {
            AppProvider appProvider = KissApplication.getApplication(getApplicationContext()).getDataHandler().getAppProvider();
            Pojo appPojo = null;
            if (appProvider!=null){
                appPojo  = appProvider.findByPackageName(sbn.getPackageName());
            }
            if (appPojo!=null){
                if (BuildConfig.DEBUG) Log.v(TAG, "2. Package:"+sbn.getPackageName()+" title:"+title);
                prefs.edit().putStringSet(sbn.getPackageName(), currentNotifications).apply();
                appPojo.incrementNotifications();
            }
        }
        if (BuildConfig.DEBUG) Log.v(TAG, "Added notification for " + sbn.getPackageName() + " title:" +title + " category:"+category);
        if (KissApplication.getApplication(getApplicationContext()).getDataHandler().isFavorite(sbn.getPackageName())){
            EventBus.getDefault().post(new ZEvent(ZEvent.State.NOTIFICATION_COUNT, sbn.getPackageName()));
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if(isNotificationTrivial(sbn.getNotification())) {
            return;
        }

        String title = null;
        String category = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            title = extractStringFromExtra(sbn.getNotification().extras, "android.title");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                category = sbn.getNotification().category;
            }
        }

        Set<String> currentNotifications = getCurrentNotificationsForPackage(sbn.getPackageName()+title);

        currentNotifications.remove(Integer.toString(sbn.getId()));

        SharedPreferences.Editor editor = prefs.edit();

        if (title!= null && category!=null && (category.equals(Notification.CATEGORY_MESSAGE) || category.equals(Notification.CATEGORY_CALL))) {
            if (currentNotifications.isEmpty()) {
                // Clean up!
                editor.remove(sbn.getPackageName()+title);
            } else {
                editor.putStringSet(sbn.getPackageName()+title, currentNotifications);
            }
            ContactsProvider contactsProvider = KissApplication.getApplication(getApplicationContext()).getDataHandler().getContactsProvider();
            Pojo contact = null;
            if (contactsProvider != null) {
                contact = contactsProvider.findByName(title);
            }
            if (contact!=null){
                contact.setHasNotification(false);
            }
        } else {
            if (currentNotifications.isEmpty()) {
                // Clean up!
                editor.remove(sbn.getPackageName());
            } else {
                editor.putStringSet(sbn.getPackageName(), currentNotifications);
            }
            AppProvider appProvider = KissApplication.getApplication(getApplicationContext()).getDataHandler().getAppProvider();
            Pojo pojo = null;
            if (appProvider!=null) {
                pojo = appProvider.findByPackageName(sbn.getPackageName());
            }
            if (pojo!=null){
                pojo.setHasNotification(false);
            }
        }
        editor.apply();
        if (KissApplication.getApplication(getApplicationContext()).getDataHandler().isFavorite(sbn.getPackageName())){
            EventBus.getDefault().post(new ZEvent(ZEvent.State.NOTIFICATION_COUNT, sbn.getPackageName()));
        }
        if (BuildConfig.DEBUG) Log.v(TAG, "Removed notification for " + sbn.getPackageName() + title + ": " + currentNotifications.toString());
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