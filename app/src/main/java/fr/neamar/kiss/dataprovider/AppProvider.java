package fr.neamar.kiss.dataprovider;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Process;
import android.os.UserManager;
import android.preference.PreferenceManager;

import java.util.ArrayList;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

import fi.zmengames.zen.AlarmUtils;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.broadcast.PackageAddedRemovedHandler;
import fr.neamar.kiss.cache.MemoryCacheHelper;
import fr.neamar.kiss.loader.LoadAppPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.Base64Serialize;
import fr.neamar.kiss.utils.FuzzyScore;
import fr.neamar.kiss.utils.UserHandle;

import static fr.neamar.kiss.notification.NotificationListener.NOTIFICATION_PREFERENCES_NAME;


public class AppProvider extends Provider<AppPojo> {
    private SharedPreferences prefs;
    private static final String TAG = AppProvider.class.getSimpleName();

    @Override
    @SuppressLint("NewApi")
    public void onCreate() {

        prefs = getBaseContext().getSharedPreferences(NOTIFICATION_PREFERENCES_NAME, Context.MODE_PRIVATE);
        loadCache();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            // Package installation/uninstallation events for the main
            // profile are still handled using PackageAddedRemovedHandler itself
            final UserManager manager = (UserManager) this.getSystemService(Context.USER_SERVICE);
            assert manager != null;

            final LauncherApps launcher = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;

            launcher.registerCallback(new LauncherApps.Callback() {
                @Override
                public void onPackageAdded(String packageName, android.os.UserHandle user) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                "android.intent.action.PACKAGE_ADDED",
                                packageName, new UserHandle(manager.getSerialNumberForUser(user), user), false
                        );
                    }
                }

                @Override
                public void onPackageChanged(String packageName, android.os.UserHandle user) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                "android.intent.action.PACKAGE_ADDED",
                                packageName, new UserHandle(manager.getSerialNumberForUser(user), user), true
                        );
                    }
                }

                @Override
                public void onPackageRemoved(String packageName, android.os.UserHandle user) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                "android.intent.action.PACKAGE_REMOVED",
                                packageName, new UserHandle(manager.getSerialNumberForUser(user), user), false
                        );
                    }
                }

                @Override
                public void onPackagesAvailable(String[] packageNames, android.os.UserHandle user, boolean replacing) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                "android.intent.action.MEDIA_MOUNTED",
                                null, new UserHandle(manager.getSerialNumberForUser(user), user), false
                        );
                    }
                }

                @Override
                public void onPackagesUnavailable(String[] packageNames, android.os.UserHandle user, boolean replacing) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                "android.intent.action.MEDIA_UNMOUNTED",
                                null, new UserHandle(manager.getSerialNumberForUser(user), user), false
                        );
                    }
                }
            });
        }

        // Get notified when app changes on standard user profile
        IntentFilter appChangedFilter = new IntentFilter();
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        appChangedFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        appChangedFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        appChangedFilter.addDataScheme("package");
        appChangedFilter.addDataScheme("file");
        this.registerReceiver(new PackageAddedRemovedHandler(), appChangedFilter);

        super.onCreate();
    }

    LoadAppPojos loadAppPojos;
    @Override
    public void reload() {
        super.reload();
        this.initialize(loadAppPojos = new LoadAppPojos(this));
    }

    /**
     * @param query    The string to search for
     * @param searcher The receiver of results
     */

    @Override
    public void requestResults(String query, Searcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo;
        boolean match;

        for (AppPojo pojo : pojos) {
            if(pojo.isExcluded()) {
                continue;
            }

            matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
            match = matchInfo.match;
            pojo.relevance = matchInfo.score;

            // check relevance for tags
            if (pojo.getNormalizedTags() != null) {
                matchInfo = fuzzyScore.match(pojo.getNormalizedTags().codePoints);
                if (matchInfo.match && (!match || matchInfo.score > pojo.relevance)) {
                    match = true;
                    pojo.relevance = matchInfo.score;
                }
            }

            if (match && !searcher.addResult(pojo)) {
                return;
            }
        }
    }

    /**
     * Return a Pojo
     *
     * @param id we're looking for
     * @return an AppPojo, or null
     */
    @Override
    public Pojo findById(String id) {
        for (Pojo pojo : pojos) {
            if (pojo.id.equals(id)) {
                return pojo;
            }
        }

        return null;
    }

    public Pojo findByPackageName(String packageName){
        for (Pojo pojo : pojos) {
            if (pojo.id.contains(packageName)) {
                return pojo;
            }
        }
        return null;
    }

    public ArrayList<Pojo> getAllApps() {
        ArrayList<Pojo> records = new ArrayList<>(pojos.size());

        for (AppPojo pojo : pojos) {
            pojo.relevance = 0;
            if(pojo.isExcluded()) continue;
            records.add(pojo);
        }
        return records;
    }
    public void removeApp(AppPojo appPojo) {
        pojos.remove(appPojo);
    }


    public List<Pojo> getAppsWithNotif() {
        ArrayList<Pojo> records = new ArrayList<>(pojos.size());
        for (AppPojo pojo : pojos) {
            if (pojo!=null) {
                pojo.relevance = 0;
                if (pojo.getBadgeCount() > 0 || pojo.getNotificationCount()>0) {
                    if(pojo.isExcluded()) continue;
                    records.add(pojo);
                }
            }
        }
        for (long idAlarm : AlarmUtils.getAlarmIds(getApplicationContext())) {
            Calendar calAlarm = Calendar.getInstance();
            calAlarm.setTimeZone(TimeZone.getTimeZone("GMT"));
            calAlarm.setTimeInMillis(idAlarm);
            SearchPojo pojo = new SearchPojo(calAlarm.getTime().toString(), String.valueOf(idAlarm),SearchPojo.ZEN_ALARM);
            pojo.relevance = 0;
            pojo.setName(calAlarm.getTime().toString());
            records.add(pojo);
        }

        return records;
    }

    @Override
    public void loadOver(ArrayList<AppPojo> results) {
        super.loadOver(results);
        saveCache();
    }

    private void saveCache() {
        HashSet<String> appSet = new HashSet<>();
        Set<String> allKeys = new HashSet<>(prefs.getAll().keySet());
        for (AppPojo appPojo : pojos) {
            String serializedPojo = Base64Serialize.encode(appPojo.id, appPojo.packageName, appPojo.activityName, appPojo.getName(), appPojo.isExcluded(), appPojo.isExcludedFromHistory());
            appSet.add(serializedPojo);
            appPojo.setHasNotification(allKeys.contains(appPojo.packageName));
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putStringSet("AppProviderCache", appSet).apply();
    }

    private void loadCache() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> appSet = prefs.getStringSet("AppProviderCache", null);
        if (appSet == null)
            return;
        pojos.clear();
        for (String serializedPojo : appSet) {
            Object[] pojoData = Base64Serialize.decode(serializedPojo);
            if (pojoData == null || pojoData.length != 6)
                continue;
            AppPojo app = new AppPojo((String) pojoData[0], (String) pojoData[1], (String) pojoData[2], new UserHandle(),(Boolean) pojoData[4],(Boolean) pojoData[5]);
            app.setName((String) pojoData[3]);
            pojos.add(app);
        }
    }

    public void addApp(String packageName, String className, UserHandle user, Context context) {
        pojos.add(loadAppPojos.loadApp(packageName, user, className, context));
        MemoryCacheHelper.cacheAppIconDrawable(context, new ComponentName(packageName, className), user);
    }
}
