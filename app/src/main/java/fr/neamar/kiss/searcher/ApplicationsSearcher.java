package fi.zmengames.zlauncher.searcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import fi.zmengames.zlauncher.KissApplication;
import fi.zmengames.zlauncher.MainActivity;
import fi.zmengames.zlauncher.pojo.Pojo;
import fi.zmengames.zlauncher.pojo.PojoComparator;

/**
 * Returns the list of all applications on the system
 */
public class ApplicationsSearcher extends Searcher {
    public ApplicationsSearcher(MainActivity activity) {
        super(activity, "<application>");
    }

    @Override
    PriorityQueue<Pojo> getPojoProcessor(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Apply app sorting preference
        if (prefs.getString("sort-apps", "alphabetical").equals("invertedAlphabetical")) {
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

        List<Pojo> pojos = KissApplication.getApplication(activity).getDataHandler().getApplications();
        this.addResult(pojos.toArray(new Pojo[0]));
        return null;
    }
}
