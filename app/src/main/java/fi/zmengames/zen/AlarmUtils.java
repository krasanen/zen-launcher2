package fi.zmengames.zen;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import fr.neamar.kiss.BuildConfig;

public class AlarmUtils {

    private static final String sTagAlarms = ":zenalarms";
    private static final String TAG = AlarmUtils.class.getSimpleName();
    public static void addAlarm(Context context, Intent intent, long notificationId, Calendar calendar) {
        if (BuildConfig.DEBUG) Log.d(TAG,"addAlarm: "+notificationId);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) notificationId & 0xfffffff, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
        saveAlarmId(context, notificationId, intent);

        /*
        try {
            executeBadge(context, context.getPackageName(), "AlarmUtils", getAlarmIds(context).size());
        } catch (ShortcutBadgeException e) {
            e.printStackTrace();
        }*/

    }

    public static void cancelAlarm(Context context, long notificationId) {
        if (BuildConfig.DEBUG) Log.d(TAG,"cancelAlarm: "+notificationId);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(context.getApplicationContext(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) notificationId & 0xfffffff, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        removeAlarmId(context, notificationId);

        /*
        try {
            executeBadge(context, context.getPackageName(), "AlarmUtils", getAlarmIds(context).size());
        } catch (ShortcutBadgeException e) {
            e.printStackTrace();
        }*/
    }

    public static void cancelAllAlarms(Context context) {
        for (long idAlarm : getAlarmIds(context)) {
            cancelAlarm(context, idAlarm);
        }
        EventBus.getDefault().post(new ZEvent(ZEvent.State.SHOW_TOAST, "All Zen Alarms cancelled!"));
    }

    public static boolean hasAlarm(Context context, Intent intent, int notificationId) {
        return PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_NO_CREATE) != null;
    }

    private static void saveAlarmId(Context context, long id, Intent intent) {
        List<Long> idsAlarms = getAlarmIds(context);

        if (idsAlarms.contains(id)) {
            return;
        }

        idsAlarms.add(id);

        saveIdsInPreferences(context, idsAlarms);
    }

    private static void removeAlarmId(Context context, long id) {
        List<Long> idsAlarms = getAlarmIds(context);

        for (int i = 0; i < idsAlarms.size(); i++) {
            if (idsAlarms.get(i) == id)
                idsAlarms.remove(i);
        }

        saveIdsInPreferences(context, idsAlarms);
    }

    public static List<Long> getAlarmIds(Context context) {
        List<Long> ids = new ArrayList<>();
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            JSONArray jsonArray2 = new JSONArray(prefs.getString(context.getPackageName() + sTagAlarms, "[]"));

            for (int i = 0; i < jsonArray2.length(); i++) {
                ids.add(jsonArray2.getLong(i));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ids;
    }

    private static void saveIdsInPreferences(Context context, List<Long> lstIds) {
        JSONArray jsonArray = new JSONArray();
        for (Long idAlarm : lstIds) {
            jsonArray.put(idAlarm);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getPackageName() + sTagAlarms, jsonArray.toString());

        editor.apply();
    }
}