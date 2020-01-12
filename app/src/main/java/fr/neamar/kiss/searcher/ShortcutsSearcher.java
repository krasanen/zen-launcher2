package fr.neamar.kiss.searcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoComparator;

/**
 * Returns the list of all applications on the system
 */
public class ShortcutsSearcher extends Searcher {
    private final boolean web;

    public ShortcutsSearcher(MainActivity activity, boolean web) {
        super(activity, "<shortcuts>");
        this.web = web;
    }

    @Override
    PriorityQueue<Pojo> getPojoProcessor(Context context) {
        // Sort from A to Z, so reverse (last item needs to be A, listview starts at the bottom)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Apply app sorting preference
        if (prefs.getString("sort-apps", "alphabetical").equals("alphabetical")) {
            return new PriorityQueue<>(DEFAULT_MAX_RESULTS, new PojoComparator());
        } else {
            return new PriorityQueue<>(DEFAULT_MAX_RESULTS, Collections.reverseOrder(new PojoComparator()));
        }
    }

    @Override
    protected int getMaxResultCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return null;
        List<Pojo> shortcutPojos = null;
        ShortcutsProvider shortcutsProvider = KissApplication.getApplication(activity).getDataHandler().getShortcutsProvider();
        if (!web){
            if (shortcutsProvider!=null) shortcutPojos = shortcutsProvider.getPojos();
        } else {
            if (shortcutsProvider!=null) shortcutPojos = shortcutsProvider.getWebPojos();
        }

        if (shortcutPojos != null)
            this.addResult(shortcutPojos.toArray(new Pojo[0]));
        return null;
    }

    @Override
    protected void onPostExecute(Void param) {
        super.onPostExecute(param);
        // Build sections for fast scrolling
        activityWeakReference.get().adapter.buildSections(false);
    }
}
