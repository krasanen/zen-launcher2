package fr.neamar.kiss;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import fi.zmengames.zen.ZEvent;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;

public class BadgeHandler {
    private static final String TAG = BadgeHandler.class.getSimpleName();
    Context context;
    //cached badges
    public static Map<String, Integer> badgeCache = new HashMap<>();

    BadgeHandler(Context context) {
        this.context = context;
        //badgeCache = DBHelper.loadBadges(this.context);
    }

    public Integer getBadgeCount(String packageName){
        Integer badgeCount = badgeCache.get(packageName);
        if (badgeCount == null) {
            return 0;
        }
        if (badgeCount>0){
            Log.d(TAG,"getBadgeCount, packageName:"+packageName+" badges:"+badgeCount);
        }
        return badgeCount;
    }


    public void setBadgeCount(String packageName, Integer badge_count) {
        Integer badgeCount = badgeCache.get(packageName);
        if (BuildConfig.DEBUG) Log.d(TAG,"setBadgeCount, packageName:"+packageName+" badge_count:"+badge_count + " badgeCount"+badgeCount);
        if (badgeCount!=badge_count) {
            badgeCache.put(packageName, badge_count);
            List<Pojo> apps = KissApplication.getApplication(context).getDataHandler().getApplications();
            boolean found = false;
            if (apps != null) {
                for (Pojo result : apps) {
                    AppPojo pojo = (AppPojo) result;
                    if (pojo.packageName.equals(packageName)) {
                        pojo.setBadgeCount(badge_count);
                        if (BuildConfig.DEBUG) Log.d(TAG, "setBadgeCount: count: " + pojo.getBadgeCount());
                        ZEvent event = new ZEvent(ZEvent.State.BADGE_COUNT, packageName, badge_count);
                        EventBus.getDefault().post(event);
                        break;
                    }

                }
            }
        }
    }

    public void reloadBadge(String packageName) {


    }
}