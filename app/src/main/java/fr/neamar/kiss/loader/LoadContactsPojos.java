package fr.neamar.kiss.loader;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.dataprovider.ContactsProvider;
import fr.neamar.kiss.forwarder.Permission;
import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.pojo.ContactsPojo;

import static fi.zmengames.zlauncher.ContactsProjection.FACEBOOK_CALL_MIMETYPE;
import static fi.zmengames.zlauncher.ContactsProjection.FECEBOOK_CONTACT_MIMETYPE;
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
                    ContactsPojo contact = new ContactsPojo();
                    contact.contactId = cur.getInt(contactId);
                    contact.lookupKey = cur.getString(lookupIndex);
                    contact.timesContacted = cur.getInt(timesContactedIndex);
                    contact.setName(cur.getString(displayNameIndex));

                    contact.phone = cur.getString(numberIndex);
                    if (contact.phone == null) {
                        contact.phone = "";
                    }
                    contact.normalizedPhone = PhoneNormalizer.simplifyPhoneNumber(contact.phone);
                    contact.starred = cur.getInt(starredIndex) != 0;
                    contact.primary = cur.getInt(isPrimaryIndex) != 0;
                    String photoId = cur.getString(photoIdIndex);
                    if (photoId != null) {
                        contact.icon = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI,
                                Long.parseLong(photoId));
                        Log.d(TAG,"icon1:"+ contact.icon);
                    }

                    contact.id = pojoScheme + contact.lookupKey + contact.phone;

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

        // SOME stuff
        String[] projection = new String[]{
                ContactsContract.Data._ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.Data.LOOKUP_KEY,

                ContactsContract.Data.MIMETYPE};
        Cursor cursor = context.get().getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                projection,
                ContactsContract.Data.MIMETYPE
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
                        FECEBOOK_CONTACT_MIMETYPE},
                null);

        if (cursor != null) {
            Log.d(TAG, "Some search:");
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int lookupKeyIndex = cursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
            int mimeTypeKeyIndex = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE);
            int contactIdIndex = cursor.getColumnIndex(ContactsContract.Data._ID);
             /*Cursor contactCursor = context.get().getContentResolver().query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    new String[]{ContactsContract.RawContacts._ID,
                            ContactsContract.RawContacts.CONTACT_ID},
                    ContactsContract.RawContacts.ACCOUNT_TYPE + "= ?",
                    new String[]{"com.facebook"},
                    null);


            //ArrayList for Store Whatsapp Contact
            ArrayList<String> myWhatsappContacts = new ArrayList<>();

            if (contactCursor != null) {
                if (contactCursor.getCount() > 0) {
                    if (contactCursor.moveToFirst()) {
                        do {
                            //whatsappContactId for get Number,Name,Id ect... from  ContactsContract.CommonDataKinds.Phone
                            String whatsappContactId = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));

                            if (whatsappContactId != null) {
                                //Get Data from ContactsContract.CommonDataKinds.Phone of Specific CONTACT_ID
                                Cursor whatsAppContactCursor = context.get().getContentResolver().query(
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                        new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                                ContactsContract.CommonDataKinds.Phone.DATA1,
                                                ContactsContract.CommonDataKinds.Phone.DATA2,
                                                ContactsContract.CommonDataKinds.Phone.DATA3,
                                                ContactsContract.CommonDataKinds.Phone.DATA15},
                                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                        new String[]{whatsappContactId}, null);

                                if (whatsAppContactCursor != null) {
                                    whatsAppContactCursor.moveToFirst();
                                    String id = whatsAppContactCursor.getString(whatsAppContactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                                    String name = whatsAppContactCursor.getString(whatsAppContactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                                    String number = whatsAppContactCursor.getString(whatsAppContactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                    String data1 = whatsAppContactCursor.getString(whatsAppContactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA1));
                                    String data2 = whatsAppContactCursor.getString(whatsAppContactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA2));
                                    String data3 = whatsAppContactCursor.getString(whatsAppContactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA3));
                                    String data4 = whatsAppContactCursor.getString(whatsAppContactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA15));
                                    whatsAppContactCursor.close();

                                    //Add Number to ArrayList
                                    myWhatsappContacts.add(number);

                                    Log.d(TAG, " FaceBook contact id  :  " + id);
                                    Log.d(TAG, " FaceBook contact name :  " + name);
                                    Log.d(TAG, " FaceBook contact number :  " + number);
                                    Log.d(TAG, " FaceBook contact data1 : " + data1);
                                    Log.d(TAG, " FaceBook contact data2 : " + data2);
                                    Log.d(TAG, " FaceBook contact data3 : " + data3);
                                    Log.d(TAG, " FaceBook contact data4 : " + data4);
                                }
                            }
                        } while (contactCursor.moveToNext());

                    }
                }
            }

            Log.d(TAG, " WhatsApp contact size :  " + myWhatsappContacts.size());*/

            while (cursor.moveToNext()) {
                String lookupKey = cursor.getString(lookupKeyIndex);
                String mimeType = cursor.getString(mimeTypeKeyIndex);
                String number = cursor.getString(numberIndex);
                String name = cursor.getString(nameIndex);
                int contactId = cursor.getInt(contactIdIndex);
                if (mapContacts.containsKey(lookupKey)) {
                    if (mimeType.equals(SIGNAL_CALL_MIMETYPE)) {
                        if (lookupKey != null && mapContacts.containsKey(lookupKey)) {
                            for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                                Log.d(TAG, "SIGNAL! " + number);
                                contact.SignalCalling = contactId;
                                if (!contact.primary) {
                                    contact.primary = true;
                                }
                            }
                        }

                    }

                    if (mimeType.equals(WHATSAPP_CALL_MIMETYPE)) {
                        if (lookupKey != null && mapContacts.containsKey(lookupKey)) {
                            for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                                Log.d(TAG, "WhatsApp! " + number);
                                contact.whatsAppCalling = contactId;
                                if (!contact.primary) {
                                    contact.primary = true;
                                }



                            }

                        }
                    }

                    if (mimeType.equals(WHATSAPP_CONTACT_MIMETYPE)) {
                        if (lookupKey != null && mapContacts.containsKey(lookupKey)) {
                            for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                                Log.d(TAG, "WhatsApp messaging! " + number);
                                contact.whatsAppMessaging = contactId;
                            }
                        }
                    }
                    if (mimeType.equals(SIGNAL_CONTACT_MIMETYPE)) {
                        if (lookupKey != null && mapContacts.containsKey(lookupKey)) {
                            for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                                Log.d(TAG, "SIGNAL messaging! " + number);
                                contact.signalMessaging = contactId;
                            }
                        }

                    }
                    if (mimeType.equals(FACEBOOK_CALL_MIMETYPE)) {
                        if (lookupKey != null && mapContacts.containsKey(lookupKey)) {
                            for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                                Log.d(TAG, "FACEBOOK call! " + contactId);
                                contact.faceCalling = contactId;

                            }

                        }
                    }

                    if (mimeType.equals(FECEBOOK_CONTACT_MIMETYPE)) {
                        if (lookupKey != null && mapContacts.containsKey(lookupKey)) {
                            for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                                Log.d(TAG, "FACEBOOK messaging! " + contactId);
                                contact.faceMessaging = contactId;
                            }
                        }


                    }
                }
            }
        }
        cursor.close();


        for (
                Set<ContactsPojo> phones : mapContacts.values())

        {

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
                    if (!added.contains(contact.normalizedPhone.toString())) {
                        added.add(contact.normalizedPhone.toString());
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
