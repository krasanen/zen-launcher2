package fr.neamar.kiss.loader;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.forwarder.Permission;
import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.ContactsPojo;

import static fi.zmengames.zen.ContactsProjection.FACEBOOK_CALL_MIMETYPE;
import static fi.zmengames.zen.ContactsProjection.FECEBOOK_CONTACT_MIMETYPE;
import static fi.zmengames.zen.ContactsProjection.SIGNAL_CALL_MIMETYPE;
import static fi.zmengames.zen.ContactsProjection.SIGNAL_CONTACT_MIMETYPE;
import static fi.zmengames.zen.ContactsProjection.WHATSAPP_CALL_MIMETYPE;
import static fi.zmengames.zen.ContactsProjection.WHATSAPP_CONTACT_MIMETYPE;
import static fr.neamar.kiss.notification.NotificationListener.NOTIFICATION_PREFERENCES_NAME;

public class LoadContactsPojos extends LoadPojos<ContactsPojo> {
    private static final String TAG = LoadPojos.class.getSimpleName();
    private final SharedPreferences prefs;
    public LoadContactsPojos(Context context) {
        super(context, "contact://");
        prefs = context.getSharedPreferences(NOTIFICATION_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
    // Get specific details from organization

    @Override
    protected ArrayList<ContactsPojo> doInBackground(Void... params) {
        long start = System.nanoTime();
        ArrayList<ContactsPojo> contacts = new ArrayList<>();
        Context c = context.get();
        if(c == null) {
            return contacts;
        }

        // Skip if we don't have permission to list contacts yet:(
        if(!Permission.checkContactPermission(c)) {
            Permission.askContactPermission();
            return contacts;
        }

        // Run query
        Cursor cur = context.get().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.Contacts.LOOKUP_KEY,
                        ContactsContract.Contacts._ID,
                        ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.STARRED,
                        ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                        ContactsContract.Contacts.PHOTO_ID,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID}, null, null, ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED + " DESC");

        // Prevent duplicates by keeping in memory encountered contacts.
        Map<String, Set<ContactsPojo>> mapContacts = new HashMap<>();

        if (cur != null) {
            if (cur.getCount() > 0) {
                int lookupIndex = cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                int contactId = cur.getColumnIndex(ContactsContract.Contacts._ID);
                int timesContactedIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED);
                int displayNameIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int starredIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED);
                int isPrimaryIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY);
                int photoIdIndex = cur.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);

                while (cur.moveToNext()) {
                    String lookupKey = cur.getString(lookupIndex);
                    Integer timesContacted = cur.getInt(timesContactedIndex);
                    String name = cur.getString(displayNameIndex);
                    //getContactCompanyJob(cur.getString(contactId));
                    String phone = cur.getString(numberIndex);
                    if (phone == null) {
                        phone = "";
                    }

                    StringNormalizer.Result normalizedPhone = PhoneNormalizer.simplifyPhoneNumber(phone);
                    boolean starred = cur.getInt(starredIndex) != 0;
                    boolean primary = cur.getInt(isPrimaryIndex) != 0;
                    String photoId = cur.getString(photoIdIndex);
                    Uri icon = null;
                    if (photoId != null) {
                        icon = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI,
                                Long.parseLong(photoId));
                    }

                    ContactsPojo contact = new ContactsPojo(pojoScheme + lookupKey + phone,
                            lookupKey, phone, normalizedPhone, icon, primary, timesContacted,
                            starred, false);

                    contact.setName(name);

                    if (contact.getName() != null) {
                        //TBog: contact should have the normalized name already
                        //contact.setName( contact.getName(), true );

                        if (mapContacts.containsKey(contact.lookupKey))
                            mapContacts.get(contact.lookupKey).add(contact);
                        else {
                            Set<ContactsPojo> phones = new HashSet<>();
                            phones.add(contact);
                            mapContacts.put(contact.lookupKey, phones);
                        }
                    }
                }
            }
            cur.close();
        }

        // Retrieve contacts' nicknames
        Cursor nickCursor = context.get().getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Nickname.NAME,
                        ContactsContract.Data.LOOKUP_KEY},
                ContactsContract.Data.MIMETYPE + "= ?",
                new String[]{ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE},
                null);

        if (nickCursor != null) {
            if (nickCursor.getCount() > 0) {
                int lookupKeyIndex = nickCursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                int nickNameIndex = nickCursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME);
                while (nickCursor.moveToNext()) {
                    String lookupKey = nickCursor.getString(lookupKeyIndex);
                    String nick = nickCursor.getString(nickNameIndex);

                    if (nick != null && lookupKey != null && mapContacts.containsKey(lookupKey)) {
                        for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                            contact.setNickname(nick);
                        }
                    }
                }
            }
            nickCursor.close();
        }

        // Retrieve contacts' Organization
        Cursor workCursor = context.get().getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Organization.TITLE,
                        ContactsContract.CommonDataKinds.Organization.COMPANY,
                        ContactsContract.Data.LOOKUP_KEY},
                ContactsContract.Data.MIMETYPE + "= ?",
                new String[]{ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE},
                null);

        if (workCursor != null) {
            if (workCursor.getCount() > 0) {
                int lookupKeyIndex = workCursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                int companyIndex = workCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY);
                int titleIndex = workCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE);
                while (workCursor.moveToNext()) {
                    String lookupKey = workCursor.getString(lookupKeyIndex);
                    String title = workCursor.getString(titleIndex);
                    String company = workCursor.getString(companyIndex);
                    if ( (company !=null||title != null) && lookupKey != null && mapContacts.containsKey(lookupKey)) {
                        for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                            //contact.setNickname(nick);
                            if (company!=null) {
                                contact.setCompany(company);
                                if (BuildConfig.DEBUG) Log.i(TAG,"company:"+company);
                            }
                            if (title!=null) {
                                contact.setTitle(title);
                                if (BuildConfig.DEBUG) Log.i(TAG, "title:" + title);
                            }
                        }
                    }
                }
            }
            workCursor.close();
        }

        // other messaging and call
        String[] projection = new String[]{
                ContactsContract.Data._ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.Data.LOOKUP_KEY,

                ContactsContract.Data.MIMETYPE};
        Cursor msgCursor = context.get().getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                projection,
                ContactsContract.Data.MIMETYPE
                        + " = ? or " + ContactsContract.Data.MIMETYPE
                        + " = ? or " + ContactsContract.Data.MIMETYPE
                        + " = ? or " + ContactsContract.Data.MIMETYPE
                        + " = ? or " + ContactsContract.Data.MIMETYPE
                        + " = ? or " + ContactsContract.Data.MIMETYPE
                        + " = ? or " + ContactsContract.Data.MIMETYPE
                        + " = ?",
                new String[]{SIGNAL_CALL_MIMETYPE,
                        WHATSAPP_CALL_MIMETYPE,
                        WHATSAPP_CONTACT_MIMETYPE,
                        SIGNAL_CONTACT_MIMETYPE,
                        FACEBOOK_CALL_MIMETYPE,
                        FECEBOOK_CONTACT_MIMETYPE,
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        },
                null);

        if (msgCursor != null) {
            if (msgCursor.getCount() > 0) {
                if (BuildConfig.DEBUG) Log.i(TAG, "other messaging and call search:");
                int emailIndex = msgCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
                int lookupKeyIndex = msgCursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                int mimeTypeKeyIndex = msgCursor.getColumnIndex(ContactsContract.Data.MIMETYPE);
                int contactIdIndex = msgCursor.getColumnIndex(ContactsContract.Data._ID);

                while (msgCursor.moveToNext()) {
                    String lookupKey = msgCursor.getString(lookupKeyIndex);
                    String mimeType = msgCursor.getString(mimeTypeKeyIndex);
                    String email = msgCursor.getString(emailIndex);
                    int contactId = msgCursor.getInt(contactIdIndex);
                    if (mapContacts.containsKey(lookupKey)) {
                        if (mimeType.equals(SIGNAL_CALL_MIMETYPE)) {
                            if (lookupKey != null && mapContacts.containsKey(lookupKey)) {
                                for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                                    contact.signalCalling = contactId;
                                    if (!contact.primary) {
                                        contact.primary = true;
                                    }
                                }
                            }

                        }

                        if (mimeType.equals(WHATSAPP_CALL_MIMETYPE)) {
                            if (lookupKey != null && mapContacts.containsKey(lookupKey)) {
                                for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                                    contact.whatsAppCalling = contactId;
                                    if (BuildConfig.DEBUG)
                                        Log.i(TAG, "whatsAppCalling:" + contact.getName());
                                    if (!contact.primary) {
                                        contact.primary = true;
                                    }
                                }

                            }
                        }

                        if (mimeType.equals(WHATSAPP_CONTACT_MIMETYPE)) {
                            if (lookupKey != null && mapContacts.containsKey(lookupKey)) {
                                for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                                    contact.whatsAppMessaging = contactId;
                                }
                            }
                        }
                        if (mimeType.equals(SIGNAL_CONTACT_MIMETYPE)) {
                            if (lookupKey != null && mapContacts.containsKey(lookupKey)) {
                                for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                                    contact.signalMessaging = contactId;
                                }
                            }

                        }
                        if (mimeType.equals(FACEBOOK_CALL_MIMETYPE)) {
                            if (lookupKey != null && mapContacts.containsKey(lookupKey)) {
                                for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                                    contact.facebookCalling = contactId;
                                    if (BuildConfig.DEBUG)
                                        Log.i(TAG, "facebookCalling:" + contact.getName());
                                }

                            }
                        }

                        if (mimeType.equals(FECEBOOK_CONTACT_MIMETYPE)) {
                            if (lookupKey != null && mapContacts.containsKey(lookupKey)) {
                                for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                                    contact.facebookMessaging = contactId;
                                }
                            }
                        }
                        if (mimeType.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
                            if (lookupKey != null && mapContacts.containsKey(lookupKey)) {
                                for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                                    contact.setEmailLookupKey(contactId);
                                    contact.setNormalizedEmail(email);
                                }
                            }
                        }
                    }
                }
            }
            msgCursor.close();
        }
        Set<String> notificationKeys = new HashSet<>(prefs.getAll().keySet());
        for (
                Set<ContactsPojo> phones : mapContacts.values())

        {

            // Find primary phone and add this one.
            Boolean hasPrimary = false;
            for (ContactsPojo contact : phones) {
                contact.setHasNotification(notificationKeys.contains(contact.getName()));
                if (contact.primary) {
                    contacts.add(contact);
                    hasPrimary = true;
                    break;
                }
            }

            // If no primary available, add all (excluding duplicates).
            if (!hasPrimary) {
                HashSet<String> added = new HashSet<>(phones.size());
                for (ContactsPojo contact : phones) {
                    if (!added.contains(contact.normalizedPhone.toString())) {
                        added.add(contact.normalizedPhone.toString());
                        contacts.add(contact);
                    }
                }
            }
        }

        long end = System.nanoTime();
        if (BuildConfig.DEBUG) Log.i("time", (end - start) / 1000000 + " milliseconds to list contacts");
        return contacts;
    }

}
