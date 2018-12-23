package fr.neamar.kiss.result;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import fi.zmengames.zlauncher.IconHelper;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.forwarder.Permission;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.searcher.QueryInterface;
import fr.neamar.kiss.ui.ImprovedQuickContactBadge;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.FuzzyScore;

public class ContactsResult extends Result {
    private static final String TAG = ContactsResult.class.getSimpleName();
    private final ContactsPojo contactPojo;
    private final QueryInterface queryInterface;
    private Drawable icon = null;

    ContactsResult(QueryInterface queryInterface, ContactsPojo contactPojo) {
        super(contactPojo);
        this.contactPojo = contactPojo;
        this.queryInterface = queryInterface;
    }

    @Override
    public View display(Context context, int position, final View convertView, FuzzyScore fuzzyScore) {
        View view = convertView;
        if (convertView == null)
            view = inflateFromId(context, R.layout.item_contact);

        // Contact name
        TextView contactName = view.findViewById(R.id.item_contact_name);
        displayHighlighted(contactPojo.normalizedName, contactPojo.getName(), fuzzyScore, contactName, context);

        // Contact phone
        TextView contactPhone = view.findViewById(R.id.item_contact_phone);
        displayHighlighted(contactPojo.normalizedPhone, contactPojo.phone, fuzzyScore, contactPhone, context);

        // Contact nickname
        TextView contactNickname = view.findViewById(R.id.item_contact_nickname);
        if (contactPojo.getNickname().isEmpty()) {
            contactNickname.setVisibility(View.GONE);
        } else {
            displayHighlighted(contactPojo.normalizedNickname, contactPojo.getNickname(), fuzzyScore, contactNickname, context);
        }

        // Contact photo
        ImprovedQuickContactBadge contactIcon = view
                .findViewById(R.id.item_contact_icon);

        if (contactIcon.getTag() instanceof ContactsPojo && contactPojo.equals(contactIcon.getTag())) {
            icon = contactIcon.getDrawable();
        }
        this.setAsyncDrawable(contactIcon);

        contactIcon.assignContactUri(Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                String.valueOf(contactPojo.lookupKey)));
        contactIcon.setExtraOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                recordLaunch(v.getContext());
                queryInterface.launchOccurred();
            }
        });

        int primaryColor = UIColors.getPrimaryColor(context);


        // SOME call
        final ImageButton someCallButton = view.findViewById(R.id.item_contact_action_some_call);
        ViewGroup.LayoutParams viewParams = someCallButton.getLayoutParams();
        int width = viewParams.width;
        int height = viewParams.height;
        if (contactPojo.whatsAppCalling != 0) {
            IconHelper iconHelper = new IconHelper(context.getPackageManager(), context);
            someCallButton.setImageBitmap(iconHelper.getCroppedBitmap("com.whatsapp", width, height, "Call", Color.WHITE));

           // someCallButton.setImageResource(R.drawable.call_whatsapp);
        } else if (contactPojo.SignalCalling != 0) {
            IconHelper iconHelper = new IconHelper(context.getPackageManager(), context);
            someCallButton.setImageBitmap(iconHelper.getCroppedBitmap("org.thoughtcrime.securesms", width, height, "Call", Color.WHITE));
        } else if (contactPojo.faceCalling != 0) {
            IconHelper iconHelper = new IconHelper(context.getPackageManager(), context);
            someCallButton.setImageBitmap(iconHelper.getCroppedBitmap("com.facebook.orca", width, height, "Call", Color.BLUE));
        }


        someCallButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contactPojo.whatsAppCalling != 0) {
                    Log.d(TAG, "whatsAppCalling:" + contactPojo.whatsAppCalling);

                    openGenericSomeApp(contactPojo.whatsAppCalling, v.getContext());
                } else if (contactPojo.SignalCalling != 0) {
                    Log.d(TAG, "SignalCalling:" + contactPojo.SignalCalling);

                    openGenericSomeApp(contactPojo.SignalCalling, v.getContext());
                } else if (contactPojo.faceCalling != 0) {

                    Log.d(TAG, "faceCalling:" + contactPojo.faceCalling);

                    openFacebook(contactPojo.faceCalling, v.getContext(), true);
                }
            }
        });
        // IM Phone action
        if (contactPojo.whatsAppCalling != 0 ||
                contactPojo.SignalCalling != 0 ||
                contactPojo.faceCalling != 0) {
            // IM Phone action
            someCallButton.setVisibility(View.VISIBLE);
        } else {
            someCallButton.setVisibility(View.GONE);
        }

        // SOME message
        ImageButton someMessageButton = view.findViewById(R.id.item_contact_action_some_message);
        someMessageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contactPojo.whatsAppMessaging != 0) {
                    Log.d(TAG, "whatsAppMessaging:" + contactPojo.whatsAppMessaging);
                    openGenericSomeApp(contactPojo.whatsAppMessaging, v.getContext());
                } else if (contactPojo.signalMessaging != 0) {
                    Log.d(TAG, "signalMessaging:" + contactPojo.signalMessaging);
                    openGenericSomeApp(contactPojo.signalMessaging, v.getContext());
                } else if (contactPojo.faceMessaging != 0) {
                    Log.d(TAG, "faceMessaging:" + contactPojo.faceMessaging);
                    openFacebook(contactPojo.faceMessaging, v.getContext(), false);
                }

            }
        });

        // SOME message button
        ViewGroup.LayoutParams viewParamsMsg = someMessageButton.getLayoutParams();
        int widthMsg = viewParamsMsg.width;
        int heightMsg = viewParamsMsg.height;
        if (contactPojo.whatsAppMessaging != 0) {
            IconHelper iconHelper = new IconHelper(context.getPackageManager(), context);
            someMessageButton.setImageBitmap(iconHelper.getCroppedBitmap("com.whatsapp", widthMsg, heightMsg, "Msg", Color.WHITE));
            // someCallButton.setImageResource(R.drawable.call_whatsapp);
        } else if (contactPojo.signalMessaging != 0) {
            IconHelper iconHelper = new IconHelper(context.getPackageManager(), context);
            someMessageButton.setImageBitmap(iconHelper.getCroppedBitmap("org.thoughtcrime.securesms", widthMsg, heightMsg, "Msg", Color.WHITE));
        } else if (contactPojo.faceMessaging != 0) {
            IconHelper iconHelper = new IconHelper(context.getPackageManager(), context);
            someMessageButton.setImageBitmap(iconHelper.getCroppedBitmap("com.facebook.orca", widthMsg, heightMsg, "Msg", Color.BLUE));
        }


        if (contactPojo.whatsAppMessaging != 0 ||
                contactPojo.signalMessaging != 0 ||
                contactPojo.faceMessaging != 0) {
            // IM Phone action
            someMessageButton.setVisibility(View.VISIBLE);
        } else {
            someMessageButton.setVisibility(View.GONE);
        }
        someMessageButton.setColorFilter(null);
        // Phone action
        ImageButton phoneButton = view.findViewById(R.id.item_contact_action_phone);
        phoneButton.setColorFilter(primaryColor);
        // Message action
        ImageButton messageButton = view.findViewById(R.id.item_contact_action_message);
        messageButton.setColorFilter(primaryColor);

        PackageManager pm = context.getPackageManager();

        if (pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            phoneButton.setVisibility(View.VISIBLE);
            messageButton.setVisibility(View.VISIBLE);
            phoneButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchCall(v.getContext());
                }
            });

            messageButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    launchMessaging(v.getContext());
                }
            });

            if (contactPojo.homeNumber)
                messageButton.setVisibility(View.INVISIBLE);
            else
                messageButton.setVisibility(View.VISIBLE);

        } else {
            phoneButton.setVisibility(View.INVISIBLE);
            messageButton.setVisibility(View.INVISIBLE);
        }

        return view;
    }

    @Override
    protected ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView) {
        adapter.add(new ListPopup.Item(context, R.string.menu_remove));
        adapter.add(new ListPopup.Item(context, R.string.menu_contact_copy_phone));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_add));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_remove));

        return inflatePopupMenu(adapter, context);
    }

    @Override
    protected Boolean popupMenuClickHandler(Context context, RecordAdapter parent, int stringId) {
        switch (stringId) {
            case R.string.menu_contact_copy_phone:
                copyPhone(context, contactPojo);
                return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId);
    }

    @SuppressWarnings("deprecation")
    private void copyPhone(Context context, ContactsPojo contactPojo) {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        assert clipboard != null;
        android.content.ClipData clip = android.content.ClipData.newPlainText(
                "Phone number for " + contactPojo.getName(),
                contactPojo.phone);
        clipboard.setPrimaryClip(clip);
    }

    @Override
    boolean isDrawableCached() {
        return icon != null;
    }

    @Override
    void setDrawableCache(Drawable drawable) {
        icon = drawable;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Drawable getDrawable(Context context) {
        synchronized (this) {
            Log.d(TAG,"getDrawable:"+contactPojo.icon);
            if (isDrawableCached())
                return icon;
            if (contactPojo.icon != null) {
                Log.d(TAG,"getDrawable:"+contactPojo.icon);
                InputStream inputStream = null;
                try {
                    inputStream = context.getContentResolver()
                            .openInputStream(contactPojo.icon);
                    return icon = Drawable.createFromStream(inputStream, null);
                } catch (FileNotFoundException ignored) {
                    Log.d(TAG,"getDrawable FileNotFoundException"+contactPojo.icon);
                    //inputStream = getPhotoInputStream(contactPojo.icon, context);
                    //return icon = Drawable.createFromStream(inputStream, null);

                   // loadDisplayPhoto(getWhatsappPhotoUri(context), context);
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }

            // Default icon
            return icon = context.getResources()
                    .getDrawable(R.drawable.ic_contact);
        }
    }

    public Uri getWhatsappPhotoUri(int whatsappContactId, Context context) {
        ContentResolver cr = context.getContentResolver();

        Cursor contactCursor = cr.query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[] { ContactsContract.RawContacts._ID, ContactsContract.RawContacts.CONTACT_ID } ,
                ContactsContract.RawContacts.ACCOUNT_TYPE + "= ?",
                new String[] { "com.whatsapp" }, null);
        if (contactCursor  != null && contactCursor .moveToFirst()) {
            Uri photoId = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, whatsappContactId);
            Uri photoUri = Uri.withAppendedPath(photoId, ContactsContract.RawContacts.DisplayPhoto.CONTENT_DIRECTORY);
            return photoUri;
        }
        return null;

    }



    @Override
    public void doLaunch(Context context, View v) {
        Intent viewContact = new Intent(Intent.ACTION_VIEW);

        viewContact.setData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                String.valueOf(contactPojo.lookupKey)));
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewContact.setSourceBounds(v.getClipBounds());
        }

        viewContact.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        viewContact.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(viewContact);
    }

    private void launchMessaging(final Context context) {
        String url = "sms:" + Uri.encode(contactPojo.phone);
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                recordLaunch(context);
                queryInterface.launchOccurred();
            }
        }, KissApplication.TOUCH_DELAY);

    }

    @SuppressLint("MissingPermission")
    private void launchCall(final Context context) {
        // Create the intent to start a phone call
        String url = "tel:" + Uri.encode(contactPojo.phone);
        Intent phoneIntent = new Intent(Intent.ACTION_CALL, Uri.parse(url));
        phoneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Make sure we have permission to call someone as this is considered a dangerous permission
       if(Permission.ensureCallPhonePermission(phoneIntent)) {
           // Pre-android 23, or we already have permission
           context.startActivity(phoneIntent);

           // Register launch in the future
           // (animation delay)
           Handler handler = new Handler();
           handler.postDelayed(new Runnable() {
               @Override
               public void run() {
                   recordLaunch(context);
                   queryInterface.launchOccurred();
               }
           }, KissApplication.TOUCH_DELAY);
       }
    }


    private void launchWhatsAppCall(final Context context) {
        // Create the intent to start a phone call
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        int contactId = contactPojo.contactId;
        Log.d(TAG, "launchWhatsAppCall, contactId:" + contactId);
        // the _ids you save goes here at the end of /data/12562
        intent.setDataAndType(Uri.parse("content://com.android.contacts/data/" + contactId),
                "vnd.android.cursor.item/vnd.com.whatsapp.voip.call");
        // intent.setPackage("com.whatsapp");

        intent.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.accountsync.ProfileActivity"));

        if (Permission.ensureCallPhonePermission(intent)) {
            // Pre-android 23, or we already have permission
            context.startActivity(intent);

            // Register launch in the future
            // (animation delay)
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recordLaunch(context);
                    queryInterface.launchOccurred();
                }
            }, KissApplication.TOUCH_DELAY);
        }

    }

    private void launchSignalCall(final Context context) {
        // Create the intent to start a phone call
        String SIGNAL_CONTACT_MIMETYPE = "vnd.androidcursor.item/vnd.org.thoughtcrime.securesms.call";
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        int contactId = contactPojo.contactId;
        Log.d(TAG, "launchSignalCall, contactId:" + contactId);
        // the _ids you save goes here at the end of /data/12562
        intent.setDataAndType(Uri.parse("content://com.android.contacts/data/" + contactId),
                SIGNAL_CONTACT_MIMETYPE);

        intent.setComponent(new ComponentName("org.thoughtcrime.securesms", "org.thoughtcrime.securesms.webrtc.VoiceCallShare"));


        if (Permission.ensureCallPhonePermission(intent)) {
            // Pre-android 23, or we already have permission
            context.startActivity(intent);

            // Register launch in the future
            // (animation delay)
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recordLaunch(context);
                    queryInterface.launchOccurred();
                }
            }, KissApplication.TOUCH_DELAY);
        }


        /*
                Log.d(TAG,"openGenericSomeApp:"+SignalCalling);
        Cursor c = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                new String[] { ContactsContract.Contacts.Data._ID }, ContactsContract.Data.DATA1 + "=?",
                new String[] { SignalCalling }, null);
        c.moveToFirst();
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.contacts/data/" + c.getString(0)));
        Log.d(TAG,"c.getString(0):"+c.getString(0)+" i:"+i.getAction());
        i.setDataAndType(Uri.parse("content://com.android.contacts/data/" + c.getString(0)),
                SIGNAL_CALL_MIMETYPE);
        context.startActivity(i);
        c.close();
         */

    }

    private void openGenericSomeApp(int signalNumber, Context context) {
        Log.d(TAG, "openGenericSomeApp:" + signalNumber);
     /*   Cursor c = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                new String[] { ContactsContract.Contacts.Data._ID }, ContactsContract.Data._ID + "=?",
                new String[] { Integer.toString(SignalCalling) }, null);
        c.moveToFirst();
*/
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.contacts/data/" + signalNumber));

        context.startActivity(i);
        //   c.close();
    }

    private void openFacebook(int whatsAppnumber, Context context, boolean call) {
        Log.d(TAG, "openFacebook:" + whatsAppnumber);
     /*   Cursor c = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                new String[] { ContactsContract.Contacts.Data._ID }, ContactsContract.Data._ID + "=?",
                new String[] { Integer.toString(whatsAppnumber) }, null);
        c.moveToFirst();
        //c.moveToNext(); //gets the call intent */
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.contacts/data/" + whatsAppnumber));
        if (!call) {
            i.setDataAndType(Uri.parse("content://com.android.contacts/data/" + whatsAppnumber),
                    "vnd.android.cursor.item/com.facebook.messenger.chat");
        } else {
            i.setDataAndType(Uri.parse("content://com.android.contacts/data/" + whatsAppnumber),
                    "vnd.android.cursor.item/com.facebook.messenger.audiocall");
        }


        i.setComponent(new ComponentName("com.facebook.orca", "com.facebook.messenger.intents.IntentHandlerActivity"));
        ;
        context.startActivity(i);
        //  c.close();
    }

}
