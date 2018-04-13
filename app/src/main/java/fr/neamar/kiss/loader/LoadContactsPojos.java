package fr.neamar.kiss.loader;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import fi.zmengames.zlauncher.ContactsProjection;
import fr.neamar.kiss.forwarder.Permission;
import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.pojo.ContactsPojo;

import static fi.zmengames.zlauncher.ContactsProjection.SIGNAL_CALL_MIMETYPE;
import static fi.zmengames.zlauncher.ContactsProjection.SIGNAL_CONTACT_MIMETYPE;
import static fi.zmengames.zlauncher.ContactsProjection.WHATSAPP_CALL_MIMETYPE;
import static fi.zmengames.zlauncher.ContactsProjection.WHATSAPP_CONTACT_MIMETYPE;

public class LoadContactsPojos extends LoadPojos<ContactsPojo> {
    private static final String TAG = LoadPojos.class.getSimpleName();

    public LoadContactsPojos(Context context) {
        super(context, "contact://");
    }

    @Override
    protected ArrayList<ContactsPojo> doInBackground(Void... params) {
        long start = System.nanoTime();

        ArrayList<ContactsPojo> contacts = new ArrayList<>();
        Context c = context.get();
        if (c == null) {
            return contacts;
        }

        // Skip if we don't have permission to list contacts yet:(
        if (!Permission.checkContactPermission(c)) {
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

        // Prevent duplicates by keeping in memory encountered phones.
        // The string key is "phone" + "|" + "name" (so if two contacts
        // with distinct name share same number, they both get displayed)
        Map<String, ArrayList<ContactsPojo>> mapContacts = new HashMap<>();

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
                    ContactsPojo contact = new ContactsPojo();
                    contact.contactId = cur.getInt(contactId);
                    contact.lookupKey = cur.getString(lookupIndex);
                    contact.timesContacted = cur.getInt(timesContactedIndex);
                    contact.setName(cur.getString(displayNameIndex));

                    contact.phone = cur.getString(numberIndex);
                    if (contact.phone == null) {
                        contact.phone = "";
                    }
                    contact.phoneSimplified = PhoneNormalizer.simplifyPhoneNumber(contact.phone);
                    contact.starred = cur.getInt(starredIndex) != 0;
                    contact.primary = cur.getInt(isPrimaryIndex) != 0;
                    String photoId = cur.getString(photoIdIndex);
                    if (photoId != null) {
                        contact.icon = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI,
                                Long.parseLong(photoId));
                    }

                    contact.id = pojoScheme + contact.lookupKey + contact.phone;

                    if (contact.getName() != null) {
                        //TBog: contact should have the normalized name already
                        //contact.setName( contact.getName(), true );

                        if (mapContacts.containsKey(contact.lookupKey))
                            mapContacts.get(contact.lookupKey).add(contact);
                        else {
                            ArrayList<ContactsPojo> phones = new ArrayList<>();
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
                ContactsContract.Data.MIMETYPE + "= ? ",
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

        // SIGNAL
        String[] projection = new String[]{
                ContactsContract.Data._ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.Data.MIMETYPE};
        Cursor cursor = context.get().getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                projection,
                ContactsContract.Data.MIMETYPE
                        + " = ? or " + ContactsContract.Data.MIMETYPE
                        + " = ? or " + ContactsContract.Data.MIMETYPE
                        + " = ? or " + ContactsContract.Data.MIMETYPE
                        + " = ?",
                new String[]{SIGNAL_CALL_MIMETYPE,
                        WHATSAPP_CALL_MIMETYPE,
                        WHATSAPP_CONTACT_MIMETYPE,
                        SIGNAL_CONTACT_MIMETYPE},
                null);

        if (cursor != null) {
            Log.d(TAG, "SIGNAL search:");
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int mimeTypeKeyIndex = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE);
            int contactIdIndex = cursor.getColumnIndex(ContactsContract.Data._ID);
            while (cursor.moveToNext()) {

                String mimeType = cursor.getString(mimeTypeKeyIndex);
                String number = cursor.getString(numberIndex);
                int contactId = cursor.getInt(contactIdIndex);


                for (List<ContactsPojo> phones : mapContacts.values()) {
                    // Find primary phone and add this one.

                    for (ContactsPojo contact : phones) {

                        if (mimeType.equals(SIGNAL_CALL_MIMETYPE)) {

                            if (contact.phone.equals(number)) {
                                Log.d(TAG, "SIGNAL! " + number);
                                contact.signalNumber = contactId;
                                if (!contact.primary) {
                                    contact.primary = true;
                                }
                            }
                        } else if (mimeType.equals(WHATSAPP_CALL_MIMETYPE)) {
                            String numberSplit[] = number.split("@");
                            if (contact.phone.equals("+" + numberSplit[0])) {
                                Log.d(TAG, "WhatsApp! " + number);
                                contact.whatsAppNumber = contactId;
                                if (!contact.primary) {
                                    contact.primary = true;
                                }
                            }
                        } else if (mimeType.equals(WHATSAPP_CONTACT_MIMETYPE)) {

                            String numberSplit2[] = number.split("@");
                            if (contact.phone.equals("+" + numberSplit2[0])) {
                                Log.d(TAG, "WhatsApp messaging! " + number);
                                contact.whatsAppMessaging = contactId;

                            }
                        } else if (mimeType.equals(SIGNAL_CONTACT_MIMETYPE)) {

                            if (contact.phone.equals(number)) {
                                Log.d(TAG, "SIGNAL messaging! " + number);
                                contact.signalMessaging = contactId;

                            }
                        }
                    }
                }
            }
        }
        cursor.close();


        for (List<ContactsPojo> phones : mapContacts.values()) {

            // Find primary phone and add this one.
            Boolean hasPrimary = false;
            for (ContactsPojo contact : phones) {
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
                    if (!added.contains(contact.phoneSimplified)) {
                        added.add(contact.phoneSimplified);
                        contacts.add(contact);
                    }
                }
            }
        }

        long end = System.nanoTime();
        Log.i("time", Long.toString((end - start) / 1000000) + " milliseconds to list contacts");
        return contacts;
    }
}
