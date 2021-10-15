package fi.zmengames.zen;


import android.content.Context;
import android.content.pm.PackageManager;

import java.util.regex.Pattern;

import fr.neamar.kiss.dataprovider.simpleprovider.SimpleProvider;
import fr.neamar.kiss.pojo.PhoneAddPojo;
import fr.neamar.kiss.pojo.PhonePojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.searcher.Searcher;

public class EmailProvider extends SimpleProvider {
    public static final String EMAIL_SCHEME = "email://";
    private final boolean deviceIsPhone;
    public static final Pattern emailPattern = Pattern.compile("/([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9_-]+)/gi");

    public EmailProvider(Context context) {
        PackageManager pm = context.getPackageManager();
        deviceIsPhone = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        // Append an item only if query looks like a email address and device has phone capabilities
        if (deviceIsPhone && emailPattern.matcher(query).find()) {
            searcher.addResult(getResult(query));
            searcher.addResult(getResultAdd(query));
        }
    }

    @Override
    public boolean mayFindById(String id) {
        return id.startsWith(EMAIL_SCHEME);
    }

    public Pojo findById(String id) {
        return getResult(id.replaceFirst(Pattern.quote(EMAIL_SCHEME), ""));
    }

    private Pojo getResult(String emailAddress) {
        PhonePojo pojo = new PhonePojo(EMAIL_SCHEME + emailAddress, emailAddress);
        pojo.relevance = 20;
        pojo.setName(emailAddress, false);
        return pojo;
    }
    private Pojo getResultAdd(String emailAddress) {
        PhoneAddPojo pojo = new PhoneAddPojo(EMAIL_SCHEME + emailAddress, emailAddress);
        pojo.relevance = 20;
        pojo.setName(emailAddress, false);
        return pojo;
    }
}