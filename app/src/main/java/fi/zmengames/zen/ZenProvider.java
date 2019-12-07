package fi.zmengames.zen;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.simpleprovider.SimpleProvider;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.searcher.Searcher;

import static fi.zmengames.zen.AlarmActivity.ALARM_TIME;


public class ZenProvider extends SimpleProvider {
    private static final String TAG = ZenProvider.class.getSimpleName();
    private final Context mContext;
    public static String mAlarm = null;
    public static String mAlarm_en = null;
    public static String mLockIn = null;
    public static String mLockIn_en = null;
    public static String mMinutes = null;
    public static String mHours = null;
    public static String mAfter = null;
    private String mAlarmTime = "";

    public ZenProvider(Context context) {
        mContext = context;
        mAlarm = mContext.getString(R.string.alarmIn).toLowerCase();
        mAlarm_en = getLocaleStringResource(new Locale("en"), R.string.alarmIn, mContext).toLowerCase();
        mMinutes = mContext.getString(R.string.minutes);
        mHours = mContext.getString(R.string.hours);
        mLockIn = mContext.getString(R.string.lockIn).toLowerCase();
        mLockIn_en = getLocaleStringResource(new Locale("en"), R.string.lockIn, mContext).toLowerCase();
        mAfter = mContext.getString(R.string.after);
    }

    public static String getLocaleStringResource(Locale requestedLocale, int resourceId, Context context) {
        String result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) { // use latest api
            Configuration config = new Configuration(context.getResources().getConfiguration());
            config.setLocale(requestedLocale);
            result = context.createConfigurationContext(config).getText(resourceId).toString();
        }
        else { // support older android versions
            Resources resources = context.getResources();
            Configuration conf = resources.getConfiguration();
            Locale savedLocale = conf.locale;
            conf.locale = requestedLocale;
            resources.updateConfiguration(conf, null);

            // retrieve resources from desired locale
            result = resources.getString(resourceId);

            // restore original locale
            conf.locale = savedLocale;
            resources.updateConfiguration(conf, null);
        }

        return result;
    }
    @Override
    public void requestResults(String query, Searcher searcher) {
        // Now create matcher object.
        if (BuildConfig.DEBUG) Log.i(TAG, "requestResults:" + query);
        String minutes = "";
        String orgQuery = query;
        mAlarmTime = "";
        Matcher matcher = Pattern.compile("\\b([0-9]|0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])\\s*([AaPp][Mm])").matcher(query);
        Matcher matcher24h = Pattern.compile("\\b([0-9]|0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])").matcher(query);
        if (matcher.find()) {
            if (BuildConfig.DEBUG) Log.i(TAG, "requestResults: match!" + query);
            try {
                mAlarmTime = matcher.group();
            } catch (IllegalStateException e){
                Log.d(TAG,""+e);
            }
            if (BuildConfig.DEBUG) Log.i(TAG, "requestResults: minutes:" + mAlarmTime);
            minutes = mAlarmTime;
        } else if (matcher24h.find()){
            if (BuildConfig.DEBUG) Log.i(TAG, "requestResults: matcher24h match!" + query);
            try {
                mAlarmTime = matcher24h.group();
            } catch (IllegalStateException e){
                Log.d(TAG,""+e);
            }
            if (BuildConfig.DEBUG) Log.i(TAG, "requestResults: matcher24h minutes:" + mAlarmTime);
            minutes = mAlarmTime;
        }

        else {
            matcher = Pattern.compile("\\d+").matcher(query);
            if (matcher.find()) {
                minutes = matcher.group();
                int i = matcher.start();
                if (i > 0) {
                    query = query.substring(0, i);
                }
            }
        }


        if (query.toLowerCase(Locale.ROOT).trim().contains(mAlarm) ||
                query.toLowerCase(Locale.ROOT).trim().contains(mAlarm_en)) {
            String url = mAlarm + minutes;
            SearchPojo pojo2 = getPojo(query, url, minutes, false);
            pojo2.relevance = 0;
            pojo2.id +=orgQuery;
            searcher.addResult(pojo2);

            // only one result required for setting time alarm
            if (!mAlarmTime.isEmpty()) return;

            if (!minutes.isEmpty()) {
                url = mAlarm + Integer.valueOf(minutes) * 60;
            }

            SearchPojo pojo = getPojo(query, url, minutes, true);
            pojo.relevance = 0;
            searcher.addResult(pojo);
            addAlarms(searcher);

        } else if (query.toLowerCase(Locale.ROOT).trim().contains(mLockIn)||
                query.toLowerCase(Locale.ROOT).trim().contains(mLockIn_en)) {
            String url = mLockIn + minutes;
            SearchPojo pojo2 = getPojo(query, url, minutes, false);
            pojo2.id +=orgQuery;
            pojo2.relevance = 0;
            searcher.addResult(pojo2);

            // only one result required for setting time lock
            if (!mAlarmTime.isEmpty()) return;

            if (!minutes.isEmpty()) {
                url = mLockIn + Integer.valueOf(minutes) * 60;
            }

            SearchPojo pojo = getPojo(query, url, minutes, true);
            pojo.relevance = 0;
            searcher.addResult(pojo);

        }

    }

    private void addAlarms(Searcher searcher) {
        List<Long> idsAlarms = AlarmUtils.getAlarmIds(mContext);
        for (long idAlarm : idsAlarms) {
            Calendar calAlarm = Calendar.getInstance();
            calAlarm.setTimeZone(TimeZone.getTimeZone("GMT"));
            calAlarm.setTimeInMillis(idAlarm);
            SearchPojo pojo = new SearchPojo(calAlarm.getTime().toString(), String.valueOf(idAlarm),SearchPojo.ZEN_ALARM);
            pojo.relevance = 0;
            searcher.addResult(pojo);
        }
    }

    private SearchPojo getPojo(String query, String url, String minutes, boolean hours) {
        return new SearchPojo("zen://", query + ((minutes.isEmpty()) ? "" : ((query.contains(mAfter)) ? "" : ((mAlarmTime.isEmpty()) ? mAfter : "")) + " ") + ((mAlarmTime.isEmpty()) ? minutes : "") + " " + ((minutes.isEmpty()) ? "" : ((hours) ? ((mAlarmTime.isEmpty()) ? mHours : "") : ((mAlarmTime.isEmpty()) ? mMinutes : ""))), url, SearchPojo.ZEN_QUERY);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public Intent getIntent(PendingIntent pendingIntent) throws IllegalStateException {
        try {
            Method getIntent = PendingIntent.class.getDeclaredMethod("getIntent");
            return (Intent) getIntent.invoke(pendingIntent);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
