package fi.zmengames.zlauncher;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.*;
import android.util.Log;

import java.util.ArrayList;

import fr.neamar.kiss.pojo.ContactsPojo;

public class ContactsProjection {
    //todo: this is signal mimetype, get whatsapp mimetype also and add to contact menu a call item!
    public static final String SIGNAL_CONTACT_MIMETYPE = "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.contact";
    public static final String WHATSAPP_CONTACT_MIMETYPE ="vnd.android.cursor.item/vnd.com.whatsapp.profile";
    public static final String SIGNAL_CALL_MIMETYPE = "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.call";
    public static final String WHATSAPP_CALL_MIMETYPE = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call";
    public static final String FACEBOOK_CALL_MIMETYPE = "vnd.android.cursor.item/com.facebook.messenger.audiocall";
    public static final String FECEBOOK_CONTACT_MIMETYPE = "vnd.android.cursor.item/com.facebook.messenger.chat";

    String  sort = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

    private static final String TAG = ContactsProjection.class.getSimpleName();
    public void getContacts(Context context, ArrayList<ContactsPojo> contacts) {
        // Get a cursor over every contact.

        String[] projection = new String[] {ContactsContract.Data._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Data.DATA1};
        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                projection,
                ContactsContract.Data.MIMETYPE + " = ?",
                new String[] {SIGNAL_CONTACT_MIMETYPE},
                sort);
        while (cursor.moveToNext()) {
            int id=cursor.getInt(cursor.getColumnIndex(ContactsContract.Data._ID));

            String name=cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            Log.d(TAG,"id:"+id+"+ name:"+name +  " phoneNumber:"+phoneNumber);

        }

        /*
        Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);
        while (phones.moveToNext()) {
            String name=phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            Log.d(TAG,"name2:"+name +  " phoneNumber2:"+phoneNumber);
            int type = phones.getInt(phones.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
            Log.d(TAG,"mimetype:"+phones.getString(phones.getColumnIndex(ContactsContract.Data.MIMETYPE)));

            Log.d(TAG,"data1:"+phones.getString(phones.getColumnIndex(ContactsContract.Data.DATA1)));
            Log.d(TAG,"data2:"+phones.getString(phones.getColumnIndex(ContactsContract.Data.DATA2)));
            Log.d(TAG,"data3:"+phones.getString(phones.getColumnIndex(ContactsContract.Data.DATA3)));



            switch (type) {
                case CommonDataKinds.Phone.TYPE_HOME:
                    // do something with the Home number here...
                    break;
                case CommonDataKinds.Phone.TYPE_MOBILE:
                    // do something with the Mobile number here...
                    break;
                case CommonDataKinds.Phone.TYPE_WORK:
                    // do something with the Work number here...
                    break;
                case CommonDataKinds.Phone.TYPE_OTHER:

                    break;
            }
        }
        phones.close();
        */
    }
    }



