package fr.neamar.kiss;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.zmengames.zen.LauncherService;
import fi.zmengames.zen.ZEvent;
import fr.neamar.kiss.broadcast.BadgeCountHandler;
import fr.neamar.kiss.db.DBHelper;

import static fi.zmengames.zen.LauncherService.zEventArrayList;

public class BadgeHandler {
    private static final String TAG = BadgeHandler.class.getSimpleName();
    Context context;
    //cached badges
    private Map<String, Integer> badgeCache;

    BadgeHandler(Context context) {
        this.context = context;
        badgeCache = DBHelper.loadBadges(this.context);
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


    public void setBadgeCount(String packageName, Integer badge_count, boolean sendIntent) {
        //upsert badge count on the db0
        Log.d(TAG,"setBadgeCount, packageName:"+packageName+" badges:"+badge_count);
        DBHelper.setBadgeCount(this.context, packageName, badge_count);
        //add to cache
        badgeCache.put(packageName, badge_count);
        ZEvent event = new ZEvent(ZEvent.State.BADGE_COUNT, packageName, badge_count);
        if (!EventBus.getDefault().isRegistered(MainActivity.class)) {
               zEventArrayList.put(event, 1);
        }
        if (sendIntent) {
            EventBus.getDefault().post(event);
        }
    }
}