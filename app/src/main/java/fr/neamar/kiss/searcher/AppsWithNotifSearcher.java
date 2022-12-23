package fr.neamar.kiss.searcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoComparator;
import fr.neamar.kiss.pojo.SearchPojo;

/**
 * Returns the list of all applications on the system
 */
public class AppsWithNotifSearcher extends Searcher {
    public AppsWithNotifSearcher(MainActivity activity) {
        super(activity, "<application>");
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

        SearchPojo pojoAddAlarm = new SearchPojo("zen://", "Alarm", "Alarm",SearchPojo.ZEN_ALARM);
        pojoAddAlarm.relevance = 0;
        pojoAddAlarm.normalizedName = StringNormalizer.normalizeWithResult("ZAlarm", false);
        this.addResult(pojoAddAlarm);

        List<Pojo> appPojos = KissApplication.getApplication(activity).getDataHandler().getAppsWithNotif();
        if (appPojos != null)
            this.addResult(appPojos.toArray(new Pojo[0]));

        List<Pojo> contactPojos = KissApplication.getApplication(activity).getDataHandler().getContactssWithNotif();
        if (contactPojos != null)
            this.addResult(contactPojos.toArray(new Pojo[0]));
        return null;

    }

    @Override
    protected void onPostExecute(Void param) {
        super.onPostExecute(param);
        // Build sections for fast scrolling
        activityWeakReference.get().adapter.buildSections();
    }
}
