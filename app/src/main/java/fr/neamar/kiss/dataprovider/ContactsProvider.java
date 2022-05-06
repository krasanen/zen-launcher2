package fr.neamar.kiss.dataprovider;

import android.database.ContentObserver;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.forwarder.Permission;
import fr.neamar.kiss.loader.LoadContactsPojos;
import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.pojo.PhoneAddPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.FuzzyScore;

import static fr.neamar.kiss.dataprovider.simpleprovider.PhoneProvider.PHONE_SCHEME;

public class ContactsProvider extends Provider<ContactsPojo> {
    private static final String TAG = ContactsProvider.class.getSimpleName();
    ArrayList<Pojo> records = new ArrayList<>();
    private final ContentObserver cObserver = new ContentObserver(null) {

        @Override
        public void onChange(boolean selfChange) {
            //reload contacts
            reload();
        }
    };

    @Override
    public void reload() {
        super.reload();
        this.initialize(new LoadContactsPojos(this));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // register content observer if we have permission
        if(Permission.checkContactPermission(this)) {
            getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, false, cObserver);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //deregister content observer
        getContentResolver().unregisterContentObserver(cObserver);
    }


    public ArrayList<Pojo> getAllContacts() {
        if (BuildConfig.DEBUG) Log.d(TAG,"getAllContacts");
        records.clear();
        PhoneAddPojo pojoPhone = new PhoneAddPojo(PHONE_SCHEME + "", "");
        pojoPhone.relevance = 20;
        pojoPhone.normalizedName = StringNormalizer.normalizeWithResult("", false);

        records.add(pojoPhone);
        for (ContactsPojo pojo : pojos) {
            pojo.relevance = 0;
            records.add(pojo);
        }
        return records;
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        if (BuildConfig.DEBUG) Log.d(TAG,"requestResults");

        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo;
        boolean match;

        for (ContactsPojo pojo : pojos) {
            matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
            match = matchInfo.match;
            pojo.relevance = matchInfo.score;

            if (pojo.normalizedNickname != null) {
                matchInfo = fuzzyScore.match(pojo.normalizedNickname.codePoints);
                if (matchInfo.match && (!match || matchInfo.score > pojo.relevance)) {
                    match = true;
                    pojo.relevance = matchInfo.score;
                }
            }

            if (!match && queryNormalized.length() > 2) {
                // search for the phone number
                matchInfo = fuzzyScore.match(pojo.normalizedPhone.codePoints);
                match = matchInfo.match;
                pojo.relevance = matchInfo.score;
            }

            if (!match && pojo.normalizedCompany!=null) {
                // search for the company
                matchInfo = fuzzyScore.match(pojo.normalizedCompany.codePoints);
                match = matchInfo.match;
                pojo.relevance = matchInfo.score;
            }

            if (!match && pojo.normalizedTitle!=null) {
                // search for the title
                matchInfo = fuzzyScore.match(pojo.normalizedTitle.codePoints);
                match = matchInfo.match;
                pojo.relevance = matchInfo.score;
            }

            if (!match && pojo.normalizedEmail !=null) {
                // search for the email
                matchInfo = fuzzyScore.match(pojo.normalizedEmail.codePoints);
                match = matchInfo.match;
                pojo.relevance = matchInfo.score;
            }

            if (match) {
                pojo.relevance += Math.min(15, pojo.timesContacted);
                if(pojo.starred) {
                    pojo.relevance += 15;
                }

                if (!searcher.addResult(pojo))
                    return;
            }
        }
    }

    /**
     * Find a ContactsPojo from a phoneNumber
     * If many contacts match, the one most often contacted will be returned
     *
     * @param phoneNumber phone number to find (will be normalized)
     * @return a contactpojo, or null.
     */
    public ContactsPojo findByPhone(String phoneNumber) {
        StringNormalizer.Result simplifiedPhoneNumber = PhoneNormalizer.simplifyPhoneNumber(phoneNumber);

        for (ContactsPojo pojo : pojos) {
            if (pojo.normalizedPhone.equals(simplifiedPhoneNumber)) {
                return pojo;
            }
        }

        return null;
    }

    public ContactsPojo findByName(String name) {

        for (ContactsPojo pojo : pojos) {
            if (pojo.getName().equals(name)) {
                return pojo;
            }
        }

        return null;
    }
    public List<Pojo> getContactsWithNotif() {
        records.clear();
        for (ContactsPojo pojo : pojos) {
            pojo.relevance = 0;
            if (pojo.getNotificationCount()>0) {
                records.add(pojo);
            }
        }
        return records;
    }
    public ContactsPojo findById(int id) {

        for (ContactsPojo pojo : pojos) {
            if (pojo.contactId == id) {
                return pojo;
            }
        }

        return null;
    }
}
