package fr.neamar.kiss.cache;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import java.lang.ref.WeakReference;


import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.utils.UserHandle;

public class MemoryCacheHelper {

    // Get max available VM memory, exceeding this amount will throw an
    // OutOfMemory exception. Stored in kilobytes as LruCache takes an
    // int in its constructor.
    private final static int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    // Use 1/8th of the available memory for this memory cache.
    private final static int cacheSize = maxMemory / 8;
    private static final LruCache<AppIconHandle, Drawable> sAppIconCache = new LruCache<>(cacheSize);
    private static boolean sPrefNoCache = false;

    private static final String TAG = MemoryCacheHelper.class.getSimpleName();


    /**
     * If the app icon is not found in the cache we load it. Else return cache value. Synchronous function.
     *
     * @param context    context to use
     * @param className  app
     * @param userHandle android.os.UserHandle
     * @return app icon drawable from cache
     */
    public static Drawable getAppIconDrawable(@NonNull Context context, ComponentName className, UserHandle userHandle) {
        AppIconHandle handle = new AppIconHandle(className, userHandle);
        return getAppIconDrawable(context, handle);
    }

    /**
     * This is called from the async task. To prevent accidents, do not access sAppIconCache without a sync block
     *
     * @param context context to use
     * @param handle  wrapper for app name
     * @return app icon drawable from cache
     */
    private static Drawable getAppIconDrawable(@NonNull Context context, AppIconHandle handle) {
        Drawable drawable;
            drawable = sAppIconCache.get(handle);
            if (drawable == null) {
                if (BuildConfig.DEBUG) Log.d(TAG,"icon not in cache: "+ handle.componentName);
                drawable = KissApplication.getApplication(context).getIconsHandler()
                        .getDrawableIconForPackage(handle.componentName, handle.userHandle);
                if (!sPrefNoCache && drawable!=null) {
                        sAppIconCache.put(handle, drawable);
                }

        }
        return drawable;
    }

    public static void cacheAppIconDrawable(@NonNull Context context, ComponentName className, UserHandle userHandle) {
        if (sPrefNoCache)
            return;
        AppIconHandle handle = new AppIconHandle(className, userHandle);
        if (sAppIconCache.get(handle)==null)
            new AsyncAppIconLoad(context, handle).execute();
    }

    public static void trimMemory() {
        synchronized (sAppIconCache) {
            if (BuildConfig.DEBUG) Log.d(TAG,"trimMemory");
            sAppIconCache.evictAll();

        }
    }

    @Nullable
    public static Drawable getCachedAppIconDrawable(ComponentName className, UserHandle userHandle) {
        AppIconHandle handle = new AppIconHandle(className, userHandle);
        synchronized (sAppIconCache) {
            return sAppIconCache.get(handle);
        }
    }

    public static void updatePreferences(SharedPreferences prefs) {
        sPrefNoCache = !prefs.getBoolean("keep-icons-in-memory", false);
        if (sPrefNoCache)
            trimMemory();
    }

    private static class AsyncAppIconLoad extends AsyncTask<Void, Void, Drawable> {
        final WeakReference<Context> contextRef;
        final AppIconHandle handle;

        public AsyncAppIconLoad(@NonNull Context context, AppIconHandle handle) {
            this.contextRef = new WeakReference<>(context);
            this.handle = handle;
        }

        @Override
        protected Drawable doInBackground(Void... voids) {
            Context context = contextRef.get();
            if (isCancelled() || context == null)
                return null;
            return getAppIconDrawable(context, handle.componentName, handle.userHandle);
        }
    }

    public static class AppIconHandle implements Comparable<AppIconHandle> {
        final ComponentName componentName;
        final UserHandle userHandle;

        AppIconHandle(ComponentName componentName, UserHandle userHandle) {
            this.componentName = componentName;
            this.userHandle = userHandle;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AppIconHandle handle = (AppIconHandle) o;

            if (componentName != null ? !componentName.equals(handle.componentName) : handle.componentName != null)
                return false;
            return userHandle != null ? userHandle.equals(handle.userHandle) : handle.userHandle == null;
        }

        @Override
        public int hashCode() {
            int result = componentName != null ? componentName.hashCode() : 0;
            result = 31 * result + (userHandle != null ? userHandle.hashCode() : 0);
            return result;
        }

        @Override
        public int compareTo(@NonNull AppIconHandle o) {
            return userHandle.equals(o.userHandle) ? componentName.compareTo(o.componentName) : (userHandle.hashCode() - o.userHandle.hashCode());
        }
    }
}