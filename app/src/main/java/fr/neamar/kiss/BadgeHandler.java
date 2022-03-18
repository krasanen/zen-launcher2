package fr.neamar.kiss;

import android.content.Context;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

import fi.zmengames.zen.ZEvent;
import fr.neamar.kiss.db.DBHelper;


public class BadgeHandler {
    private static final String TAG = BadgeHandler.class.getSimpleName();
    Context context;
    //cached badges
    public static Map<String, Integer> badgeCache = new HashMap<>();

    BadgeHandler(Context context) {
        this.context = context;
        badgeCache = DBHelper.loadBadges(this.context);
    }

    public static int getBadgeCount(String packageName) {
        Integer badgeCount = badgeCache.get(packageName);
        if (badgeCount == null) {
            return 0;
        }
        if (BuildConfig.DEBUG && badgeCount > 0) {
            Log.i(TAG, "getBadgeCount, packageName:" + packageName + " badges:" + badgeCount);
        }
        return badgeCount;
    }


    public void setBadgeCount(String packageName, int badge_count) {
        Integer badgeCount = badgeCache.get(packageName);
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "setBadgeCount, packageName:" + packageName + " badge_count:" + badge_count + " badgeCount" + badgeCount);
        }
        badgeCache.put(packageName, badge_count);
        DBHelper.setBadgeCount(this.context, packageName, badge_count);
        ZEvent event = new ZEvent(ZEvent.State.BADGE_COUNT,packageName);
        EventBus.getDefault().post(event);
    }


}