package fr.neamar.kiss;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import fi.zmengames.zen.EmailProvider;
import fi.zmengames.zen.ZEvent;
import fi.zmengames.zen.ZenProvider;
import fr.neamar.kiss.dataprovider.AppProvider;
import fr.neamar.kiss.dataprovider.ContactsProvider;
import fr.neamar.kiss.dataprovider.IProvider;
import fr.neamar.kiss.dataprovider.Provider;
import fr.neamar.kiss.dataprovider.SearchProvider;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;
import fr.neamar.kiss.dataprovider.simpleprovider.CalculatorProvider;
import fr.neamar.kiss.dataprovider.simpleprovider.PhoneProvider;
import fr.neamar.kiss.dataprovider.simpleprovider.TagsProvider;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.db.ValuedHistoryRecord;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.ShortcutUtil;
import fr.neamar.kiss.utils.UserHandle;

public class DataHandler
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = DataHandler.class.getSimpleName();

    /**
     * Package the providers reside in
     */
    final static private String PROVIDER_PREFIX = IProvider.class.getPackage().getName() + ".";
    /**
     * List all known providers
     */
    final static private List<String> PROVIDER_NAMES = Arrays.asList(
            "app", "contacts", "settings", "shortcuts"
    );
    private TagsHandler tagsHandler;
    private BadgeHandler badgeHandler;
    final private Context context;
    private String currentQuery;
    private final Map<String, ProviderEntry> providers = new HashMap<>();
    public boolean allProvidersHaveLoaded = false;
    private final long start;


    public BadgeHandler getBadgeHandler() {
        if (badgeHandler == null) {
            badgeHandler = new BadgeHandler(context);
        }
        return badgeHandler;
    }

    /**
     * Initialize all providers
     */
    public DataHandler(Context context) {
        start = System.currentTimeMillis();

        // Make sure we are in the context of the main activity
        // (otherwise we might receive an exception about broadcast listeners not being able
        //  to bind to services)
        this.context = context.getApplicationContext();


        // Monitor changes for service preferences (to automatically start and stop services)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);

        // Connect to initial providers
        // Those are the complex providers, that are defined as Android services
        // to survive even if the app's UI is killed
        // (this way, we don't need to reload the app list everytime for instance)
        for (String providerName : PROVIDER_NAMES) {
            if (prefs.getBoolean("enable-" + providerName, true)) {
                this.connectToProvider(providerName);
            }
        }

        // Some basic providers are defined directly,
        // as we don't need the overhead of a service for them
        // Those providers don't expose a service connection,
        // and you can't bind / unbind to them dynamically.
        ProviderEntry calculatorEntry = new ProviderEntry();
        calculatorEntry.provider = new CalculatorProvider();
        this.providers.put("calculator", calculatorEntry);
        ProviderEntry phoneEntry = new ProviderEntry();
        phoneEntry.provider = new PhoneProvider(context);
        this.providers.put("phone", phoneEntry);
        ProviderEntry searchEntry = new ProviderEntry();
        searchEntry.provider = new SearchProvider(context);
        this.providers.put("search", searchEntry);
        ProviderEntry zenEntry = new ProviderEntry();
        zenEntry.provider = new ZenProvider(context);
        this.providers.put("zen", zenEntry);
        ProviderEntry tagsEntry = new ProviderEntry();
        tagsEntry.provider = new TagsProvider();
        this.providers.put("tags", tagsEntry);
        ProviderEntry emailEntry = new ProviderEntry();
        emailEntry.provider = new EmailProvider(context);
        this.providers.put("email", emailEntry);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.startsWith("enable-")) {
            String providerName = key.substring(7);
            if (PROVIDER_NAMES.contains(providerName)) {
                if (sharedPreferences.getBoolean(key, true)) {
                    this.connectToProvider(providerName);
                } else {
                    this.disconnectFromProvider(providerName);
                }
            }
        }
    }

    /**
     * Generate an intent that can be used to start or stop the given provider
     *
     * @param name The name of the provider
     * @return Android intent for this provider
     */
    private Intent providerName2Intent(String name) {
        // Build expected fully-qualified provider class name
        StringBuilder className = new StringBuilder(50);
        className.append(PROVIDER_PREFIX);
        className.append(Character.toUpperCase(name.charAt(0)));
        className.append(name.substring(1).toLowerCase(Locale.ROOT));
        className.append("Provider");

        // Try to create reflection class instance for class name
        try {
            return new Intent(this.context, Class.forName(className.toString()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Require the data handler to be connected to the data provider with the given name
     *
     * @param name Data provider name (i.e.: `ContactsProvider` → `"contacts"`)
     */
    private void connectToProvider(final String name) {
        // Do not continue if this provider has already been connected to
        if (this.providers.containsKey(name)) {
            return;
        }

        if (BuildConfig.DEBUG) Log.v(TAG, "Connecting to " + name);


        // Find provider class for the given service name
        Intent intent = this.providerName2Intent(name);
        if (intent == null) {
            return;
        }

        // Send "start service" command first so that the service can run independently
        // of the activity

        KissApplication.startLaucherService(intent, this.context);

        final ProviderEntry entry = new ProviderEntry();

        // Connect and bind to provider service
        this.context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                if (BuildConfig.DEBUG) Log.d(TAG,"onServiceConnected: "+service.getClass());
                if (service instanceof Provider.LocalBinder) {
                    // We've bound to LocalService, cast the IBinder and get LocalService instance
                    Provider.LocalBinder binder = (Provider.LocalBinder) service;
                    IProvider provider = binder.getService();

                    // Update provider info so that it contains something useful
                    entry.provider = provider;
                    entry.connection = this;

                    if (provider.isLoaded()) {
                        handleProviderLoaded();
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
            }
        }, Context.BIND_AUTO_CREATE);

        // Add empty provider object to list of providers
        this.providers.put(name, entry);
    }

    /**
     * Terminate any connection between the data handler and the data provider with the given name
     *
     * @param name Data provider name (i.e.: `AppProvider` → `"app"`)
     */
    private void disconnectFromProvider(String name) {
        // Skip already disconnected services
        ProviderEntry entry = this.providers.get(name);
        if (entry == null) {
            return;
        }

        // Disconnect from provider service
        this.context.unbindService(entry.connection);

        // Stop provider service
        this.context.stopService(new Intent(this.context, entry.provider.getClass()));

        // Remove provider from list
        this.providers.remove(name);
    }

    /**
     * Called when some event occurred that makes us believe that all data providers
     * might be ready now
     */
    public void handleProviderLoaded() {
        if (this.allProvidersHaveLoaded) {
            return;
        }

        // Make sure that all providers are fully connected
        for (ProviderEntry entry : this.providers.values()) {
            if (entry.provider == null || !entry.provider.isLoaded()) {
                return;
            }
        }

        long time = System.currentTimeMillis() - start;
        if (BuildConfig.DEBUG) Log.v(TAG, "Time to load all providers: " + time + "ms");

        this.allProvidersHaveLoaded = true;

        // Broadcast the fact that the new providers list is ready

        EventBus.getDefault().postSticky(new ZEvent(ZEvent.State.FULL_LOAD_OVER));
   }




    /**
     * Get records for this query.
     *
     * @param query    query to run
     * @param searcher the searcher currently running
     */
    public void requestResults(String query, Searcher searcher) {
        currentQuery = query;
        for (ProviderEntry entry : this.providers.values()) {
            if (searcher.isCancelled())
                break;
            if (entry.provider == null)
                continue;
            // Retrieve results for query:
            entry.provider.requestResults(query, searcher);
        }
    }

    /**
     * Get records for this query.
     *
     * @param searcher the searcher currently running
     */
    public void requestAllRecords(Searcher searcher) {
        for (ProviderEntry entry : this.providers.values()) {
            if (searcher.isCancelled())
                break;
            if (entry.provider == null)
                continue;

            List<? extends Pojo> pojos = entry.provider.getPojos();
            if (pojos != null)
                searcher.addResult(pojos.toArray(new Pojo[0]));
        }
    }

    /**
     * Return previously selected items.<br />
     * May return null if no items were ever selected (app first use)<br />
     * May return an empty set if the providers are not done building records,
     * in this case it is probably a good idea to call this function 500ms after
     *
     * @param context        android context
     * @param itemCount      max number of items to retrieve, total number may be less (search or calls are not returned for instance)
     * @param historyMode    Recency vs Frecency vs Frequency vs Adaptive vs Alphabetically
     * @param itemsToExcludeById Items to exclude from history by their id
     * @return pojos in recent history
     */
    public ArrayList<Pojo> getHistory(Context context, int itemCount, String historyMode, Set<String> itemsToExcludeById) {
        // Pre-allocate array slots that are likely to be used based on the current maximum item
        // count
        ArrayList<Pojo> history = new ArrayList<>(Math.min(itemCount, 256));

        // Max sure that we get enough items, regardless of how many may be excluded
        int extendedItemCount = itemCount + itemsToExcludeById.size();

        // Read history
        List<ValuedHistoryRecord> ids = DBHelper.getHistory(context, extendedItemCount, historyMode);

        // Find associated items
        for (int i = 0; i < ids.size(); i++) {
            // Ask all providers if they know this id
            Pojo pojo = getPojo(ids.get(i).record);

            if (pojo == null) {
                continue;
            }

            if(itemsToExcludeById.contains(pojo.id)) {
                continue;
            }

            history.add(pojo);

            // Break if maximum number of items have been retrieved
            if (history.size() >= itemCount) {
                break;
            }
        }

        return history;
    }

    public int getHistoryLength() {
        return DBHelper.getHistoryLength(this.context);
    }

    /**
     * Query database for item and return its name
     *
     * @param id      globally unique ID, usually starts with provider scheme, e.g. "app://" or "contact://"
     * @return name of item (i.e. app name)
     */
    public String getItemName(String id) {
        // Ask all providers if they know this id
        Pojo pojo = getPojo(id);

        return (pojo != null) ? pojo.getName() : "???";
    }

    public boolean addShortcut(ShortcutRecord record) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Adding shortcut for " + record.packageName);
        if (DBHelper.insertShortcut(this.context, record)){
            if (this.getShortcutsProvider()!=null) {
                this.getShortcutsProvider().reload();
            }
            return true;
        } else {
            return false;
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    public void addShortcut(String packageName) {

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        List<ShortcutInfo> shortcuts;
        try {
            shortcuts = ShortcutUtil.getShortcut(context, packageName);
        } catch (SecurityException | IllegalStateException e) {
            e.printStackTrace();
            return;
        }

        for (ShortcutInfo shortcutInfo : shortcuts) {
            // Create Pojo
            ShortcutRecord record = ShortcutUtil.createShortcutRecord(context, shortcutInfo, true);
            if (record == null) {
                continue;
            }
            // Add shortcut to the DataHandler
            addShortcut(record);
        }

        if (!shortcuts.isEmpty() && this.getShortcutsProvider() != null) {
            this.getShortcutsProvider().reload();
        }
    }

    public void clearHistory() {
        DBHelper.clearHistory(this.context);
    }

    public void removeShortcut(ShortcutPojo shortcut) {
        // Also remove shortcut from favorites
        removeFromFavorites(shortcut.id);
        DBHelper.removeShortcut(this.context, shortcut);

        if (this.getShortcutsProvider() != null) {
            this.getShortcutsProvider().reload();
        }
    }

    public void removeShortcuts(String packageName) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        // Remove all shortcuts from favorites for given package name
        List<ShortcutRecord> shortcutsList = DBHelper.getShortcuts(context, packageName);
        for (ShortcutRecord shortcut : shortcutsList) {
            String id = ShortcutUtil.generateShortcutId(shortcut.name);
            removeFromFavorites(id);
        }

        DBHelper.removeShortcuts(this.context, packageName);

        if (this.getShortcutsProvider() != null) {
            this.getShortcutsProvider().reload();
        }
    }

    @NonNull
    public Set<String> getExcludedFromHistory() {
        Set<String> excluded = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("excluded-apps-from-history", null);
        if (excluded == null) {
            excluded = new HashSet<>();
            excluded.add(context.getPackageName());
        }
        return excluded;
    }

    @NonNull
    public Set<String> getExcluded() {
        Set<String> excluded = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("excluded-apps", null);
        if (excluded == null) {
            excluded = new HashSet<>();
            excluded.add(context.getPackageName());
        }
        return excluded;
    }

    /**
     * Get ids that should be excluded from apps
     *
     * @return set of favorite ids
     */
    @NonNull
    public Set<String> getExcludedFavorites() {
        Set<String> excludedFavorites = new HashSet<>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("exclude-favorites-apps", false)) {
            String favApps = prefs.getString("favorite-apps-list", "");
            excludedFavorites.addAll(Arrays.asList(favApps.split(";")));
        }
        return excludedFavorites;
    }

    public void addToExcludedFromHistory(AppPojo app) {
        // The set needs to be cloned and then edited,
        // modifying in place is not supported by putStringSet()
        Set<String> excluded = new HashSet<>(getExcludedFromHistory());
        excluded.add(app.id);

        if (ShortcutUtil.areShortcutsEnabled(context)) {
            // Add all shortcuts for given package name to being excluded from history
            List<ShortcutRecord> shortcutsList = DBHelper.getShortcuts(context, app.packageName);
            for (ShortcutRecord shortcut : shortcutsList) {
                String id = ShortcutUtil.generateShortcutId(shortcut.name);
                excluded.add(id);
            }
            // Refresh shortcuts
            if (!shortcutsList.isEmpty() && this.getShortcutsProvider() != null) {
                this.getShortcutsProvider().reload();
            }
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps-from-history", excluded).apply();
        app.setExcludedFromHistory(true);
    }

    public void removeFromExcludedFromHistory(AppPojo app) {
        // The set needs to be cloned and then edited,
        // modifying in place is not supported by putStringSet()
        Set<String> excluded = new HashSet<>(getExcludedFromHistory());
        excluded.remove(app.id);

        if (ShortcutUtil.areShortcutsEnabled(context)) {
            // Add all shortcuts for given package name to being included in history
            List<ShortcutRecord> shortcutsList = DBHelper.getShortcuts(context, app.packageName);
            for (ShortcutRecord shortcut : shortcutsList) {
                String id = ShortcutUtil.generateShortcutId(shortcut.name);
                excluded.remove(id);
            }
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps-from-history", excluded).apply();
        app.setExcludedFromHistory(false);
    }

    public void addToExcluded(AppPojo app) {
        // The set needs to be cloned and then edited,
        // modifying in place is not supported by putStringSet()
        Set<String> excluded = new HashSet<>(getExcluded());
        excluded.add(app.getComponentName());
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps", excluded).apply();
        app.setExcluded(true);

        // Ensure it's removed from favorites too
        DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
        dataHandler.removeFromFavorites(app.id);

        //Exclude shortcuts for this app
        removeShortcuts(app.packageName);
    }

    public void removeFromExcluded(AppPojo app) {
        // The set needs to be cloned and then edited,
        // modifying in place is not supported by putStringSet()
        Set<String> excluded = new HashSet<>(getExcluded());
        excluded.remove(app.getComponentName());
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps", excluded).apply();
        app.setExcluded(false);

        //Add shortcuts for this app
        addShortcut(app.packageName);
    }

    public void removeFromExcluded(String packageName) {
        Set<String> excluded = getExcluded();
        Set<String> newExcluded = new HashSet<>();
        for (String excludedItem : excluded) {
            if (!excludedItem.contains(packageName + "/")) {
                newExcluded.add(excludedItem);
            }
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps", newExcluded).apply();
    }

    public void removeFromExcluded(UserHandle user) {
        // This is only intended for apps from foreign-profiles
        if (user.isCurrentUser()) {
            return;
        }

        Set<String> excluded = getExcluded();
        Set<String> newExcluded = new HashSet<>();
        for (String excludedItem : excluded) {
            if (!user.hasStringUserSuffix(excludedItem, '#')) {
                newExcluded.add(excludedItem);
            }
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("excluded-apps", newExcluded).apply();
    }

    /**
     * Return all applications (including excluded)
     *
     * @return pojos for all applications
     */
    @Nullable
    public List<Pojo> getApplications() {
        AppProvider appProvider = getAppProvider();
        return appProvider != null ? appProvider.getAllApps() : null;
    }

    @Nullable
    public List<Pojo> getApplicationsCached() {
        AppProvider appProvider = getAppProvider();
        return appProvider != null ? appProvider.getAllAppsCached() : null;
    }


    @Nullable
    public List<Pojo> getAppsWithNotif() {
        AppProvider appProvider = getAppProvider();
        return appProvider != null ? appProvider.getAppsWithNotif() : null;
    }

    @Nullable
    public List<Pojo> getContactssWithNotif() {
        ContactsProvider contactsProvider = getContactsProvider();
        return contactsProvider != null ? contactsProvider.getContactsWithNotif() : null;
    }

    @Nullable
    public ContactsProvider getContactsProvider() {
        ProviderEntry entry = this.providers.get("contacts");
        return (entry != null) ? ((ContactsProvider) entry.provider) : null;
    }

    @Nullable
    public ShortcutsProvider getShortcutsProvider() {
        ProviderEntry entry = this.providers.get("shortcuts");
        return (entry != null) ? ((ShortcutsProvider) entry.provider) : null;
    }

    @Nullable
    public AppProvider getAppProvider() {
        ProviderEntry entry = this.providers.get("app");
        return (entry != null) ? ((AppProvider) entry.provider) : null;
    }

    @Nullable
    public SearchProvider getSearchProvider() {
        ProviderEntry entry = this.providers.get("search");
        return (entry != null) ? ((SearchProvider) entry.provider) : null;
    }

    /**
     * Return most used items.<br />
     * May return null if no items were ever selected (app first use)
     *
     * @return favorites' pojo
     */
    public ArrayList<Pojo> getFavorites() {

        String favApps = PreferenceManager.getDefaultSharedPreferences(this.context).
                getString("favorite-apps-list", "");
        List<String> favAppsList = Arrays.asList(favApps.split(";"));
        ArrayList<Pojo> favorites = new ArrayList<>(favAppsList.size());
        // Find associated items
        for (int i = 0; i < favAppsList.size(); i++) {
            Pojo pojo = getPojo(favAppsList.get(i));
            if (pojo != null) {
                favorites.add(pojo);
            }
        }

        return favorites;
    }

    /**
     * This method is used to set the specific position of an app in the fav array.
     *
     * @param context  The mainActivity context
     * @param id       the app you want to set the position of
     * @param position the new position of the fav
     */
    public void setFavoritePosition(MainActivity context, String id, int position) {
        if (BuildConfig.DEBUG) Log.d(TAG,"setFavoritePosition:" + id + ":"+position);
        List<Pojo> currentFavorites = getFavorites();
        List<String> favAppsList = new ArrayList<>();

        for (Pojo pojo : currentFavorites) {
            favAppsList.add(pojo.getFavoriteId());
        }

        int currentPos = favAppsList.indexOf(id);
        if (currentPos == -1) {
            Log.e(TAG, "Couldn't find id in favAppsList");
            return;
        }
        // Clamp the position so we don't just extend past the end of the array.
        position = Math.min(position, favAppsList.size() - 1);

        favAppsList.remove(currentPos);
        favAppsList.add(position, id);

        String newFavList = TextUtils.join(";", favAppsList);

        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("favorite-apps-list", newFavList + ";").apply();

        context.onFavoriteChange();
    }

    /**
     * Helper function to get the position of a favorite. Used mainly by the drag and drop system to know where to place the dropped app.
     *
     * @param context mainActivity context
     * @param id      the app you want to get the position of.
     * @return favorite position
     */
    public int getFavoritePosition(String id) {
        String favApps = PreferenceManager.getDefaultSharedPreferences(this.context).
                getString("favorite-apps-list", "");
        assert favApps != null;
        List<String> favAppsList = new ArrayList<>(Arrays.asList(favApps.split(";")));

        return favAppsList.indexOf(id);
    }

    public void addToFavorites(String id) {

        String favApps = PreferenceManager.getDefaultSharedPreferences(context).
                getString("favorite-apps-list", "");

        // Check if we are already a fav icon
        assert favApps != null;
        if (favApps.contains(id + ";")) {
            //shouldn't happen
            return;
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("favorite-apps-list", favApps + id + ";").apply();

        boolean excludedApps = PreferenceManager.getDefaultSharedPreferences(context).
                getBoolean("exclude-favorites-apps", false);
        if (excludedApps) {
            getAppProvider().reload();
        }
    }

    public void removeFromFavorites(String id) {
        String favApps = PreferenceManager.getDefaultSharedPreferences(context).
                getString("favorite-apps-list", "");

        // Check if we are not already a fav icon
        assert favApps != null;
        if (!favApps.contains(id + ";")) {
            //shouldn't happen
            return;
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("favorite-apps-list", favApps.replace(id + ";", "")).apply();

        boolean excludedApps = PreferenceManager.getDefaultSharedPreferences(context).
                getBoolean("exclude-favorites-apps", false);
        if (excludedApps) {
            getAppProvider().reload();
        }
    }

    @SuppressWarnings("StringSplitter")
    public void removeFromFavorites(UserHandle user) {
        // This is only intended for apps from foreign-profiles
        if (user.isCurrentUser()) {
            return;
        }

        String[] favAppList = PreferenceManager.getDefaultSharedPreferences(this.context)
                .getString("favorite-apps-list", "").split(";");

        StringBuilder favApps = new StringBuilder();
        for (String favAppID : favAppList) {
            if (!favAppID.startsWith("app://") || !user.hasStringUserSuffix(favAppID, '/')) {
                favApps.append(favAppID);
                favApps.append(";");
            }
        }

        PreferenceManager.getDefaultSharedPreferences(this.context).edit()
                .putString("favorite-apps-list", favApps.toString()).apply();

        boolean excludedApps = PreferenceManager.getDefaultSharedPreferences(context).
                getBoolean("exclude-favorites-apps", false);
        if (excludedApps) {
            getAppProvider().reload();
        }
    }

    /**
     * Insert specified ID (probably a pojo.id) into history
     *
     * @param id pojo.id of item to record
     */
    public void addToHistory(String id) {
        if (id.isEmpty()) {
            return;
        }

        boolean frozen = PreferenceManager.getDefaultSharedPreferences(context).
                getBoolean("freeze-history", false);

        Set<String> excludedFromHistory = getExcludedFromHistory();

        if (!frozen && !excludedFromHistory.contains(id)) {
            DBHelper.insertHistory(this.context, currentQuery, id);
        }
    }

    private Pojo getPojo(String id) {
        // Ask all providers if they know this id
        for (ProviderEntry entry : this.providers.values()) {
            if (entry.provider != null && entry.provider.mayFindById(id)) {
                return entry.provider.findById(id);
            }
        }

        return null;
    }

    public TagsHandler getTagsHandler() {
        if (tagsHandler == null) {
            tagsHandler = new TagsHandler(context);
        }
        return tagsHandler;
    }

    public void resetTagsHandler() {
        tagsHandler = new TagsHandler(this.context);
    }

    public void renameApp(String componentName, String newName) {
        DBHelper.addCustomAppName(context, componentName, newName);
    }

    public void removeRenameApp(String componentName, String defaultName) {
        DBHelper.removeCustomAppName(context, componentName);
    }

    public long setCustomAppIcon(String componentName) {
        return DBHelper.addCustomAppIcon(context, componentName);
    }

    public long removeCustomAppIcon(String componentName) {
        return DBHelper.removeCustomAppIcon(context, componentName);
    }

    static final class ProviderEntry {
        public IProvider provider = null;
        ServiceConnection connection = null;
    }
}
