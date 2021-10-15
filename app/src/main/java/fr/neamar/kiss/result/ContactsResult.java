package fr.neamar.kiss.result;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import fi.zmengames.zen.IconHelper;
import fi.zmengames.zen.Utility;
import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.forwarder.Permission;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.searcher.ContactSearcher;
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


    private void addCallItemsToMenu(PopupMenu popupExcludeMenu, View view) {

        if (pojo.getHasNotification()){
            popupExcludeMenu.getMenu().add(OPEN_NOTIFICATION,Menu.NONE, Menu.NONE,R.string.open_notification_package);
        }
        popupExcludeMenu.getMenu().add(CELL_CALL,Menu.NONE, Menu.NONE,R.string.ui_item_contact_hint_call);
        if (contactPojo.whatsAppCalling != 0) {
            popupExcludeMenu.getMenu().add(WHATSAPP_CALL,Menu.NONE, Menu.NONE,R.string.ui_item_contact_hint_call_whatsapp);
        }
        if (contactPojo.signalCalling != 0) {
            popupExcludeMenu.getMenu().add(SIGNAL_CALL,Menu.NONE, Menu.NONE,R.string.ui_item_contact_hint_call_signal);
        }
        if (contactPojo.facebookCalling != 0) {
            popupExcludeMenu.getMenu().add(FACEBOOK_CALL,Menu.NONE, Menu.NONE,R.string.ui_item_contact_hint_call_facebook);
        }

        popupExcludeMenu.getMenu().add(COPY_NUMBER_CALL,Menu.NONE, Menu.NONE,R.string.menu_contact_copy_phone);


    }
    private final static int OPEN_NOTIFICATION = 0;
    private final static int CELL_CALL = 1;
    private final static int WHATSAPP_CALL = 2;
    private final static int SIGNAL_CALL = 3;
    private final static int FACEBOOK_CALL = 4;
    private final static int COPY_NUMBER_CALL = 5;
    private final static int CELL_MSG = 6;
    private final static int WHATSAPP_MSG = 7;
    private final static int SIGNAL_MSG = 8;
    private final static int FACEBOOK_MSG = 9;

    private void buildCallPopupMenu(final View view) {
        Context context = view.getContext();


        PopupMenu popupExcludeMenu = new PopupMenu(context, view);

        addCallItemsToMenu(popupExcludeMenu, view);
        //Adding menu items

        setMenuItemsClickable(popupExcludeMenu, view);

        Utility.showPopup(popupExcludeMenu, KissApplication.getApplication(context).getMainActivity());


    }

    private void setMenuItemsClickable(PopupMenu popupExcludeMenu, View view) {
        //registering popup with OnMenuItemClickListener
        popupExcludeMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                Context context = view.getContext();
                switch (item.getGroupId()) {
                    case OPEN_NOTIFICATION:
                        openNotification(context);
                        break;
                    case CELL_CALL:
                        launchCall(context);
                        break;
                    case WHATSAPP_CALL:
                        launchWhatsAppCall(context);
                        break;
                    case SIGNAL_CALL:
                        launchSignalCall(context);
                        break;
                    case FACEBOOK_CALL:
                        openFacebook(context, true);
                        break;
                    case COPY_NUMBER_CALL:
                        copyPhone(context, contactPojo);
                        break;
                    case CELL_MSG:
                        launchMessaging(context);
                        break;
                    case WHATSAPP_MSG:
                        openGenericSomeApp(contactPojo.whatsAppMessaging, context);
                        break;
                    case SIGNAL_MSG:
                        openGenericSomeApp(contactPojo.signalMessaging, context);
                        break;
                    case FACEBOOK_MSG:
                        openFacebook(context, false);
                        break;
                }
                return true;
            }
        });
    }

    private void openNotification(Context context) {

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(pojo.getNotificationPackage());
        context.startActivity( launchIntent );
    }

    private void addMsgItemsToMenu(PopupMenu popupExcludeMenu, View view ){
        popupExcludeMenu.getMenu().add(CELL_MSG,Menu.NONE, Menu.NONE,R.string.ui_item_contact_hint_sms);
        if (contactPojo.whatsAppMessaging != 0) {
            popupExcludeMenu.getMenu().add(WHATSAPP_MSG,Menu.NONE, Menu.NONE,R.string.ui_item_contact_hint_message_whatsapp);
        }
        if (contactPojo.signalMessaging != 0) {
            popupExcludeMenu.getMenu().add(SIGNAL_MSG,Menu.NONE, Menu.NONE,R.string.ui_item_contact_hint_message_signal);
        }
        if (contactPojo.facebookMessaging != 0) {
            popupExcludeMenu.getMenu().add(FACEBOOK_MSG,Menu.NONE, Menu.NONE,R.string.ui_item_contact_hint_message_facebook);
        }

    }

    private void buildContactClickPopupMenu(final View view) {
        Context context = view.getContext();
        PopupMenu popupExcludeMenu = new PopupMenu(context, view);
        //Adding menu items
        addCallItemsToMenu(popupExcludeMenu, view);
        addMsgItemsToMenu(popupExcludeMenu, view);
        setMenuItemsClickable(popupExcludeMenu, view);
        popupExcludeMenu.show();

    }

    private void buildMsgPopupMenu(final View view) {
        Context context = view.getContext();
        PopupMenu popupExcludeMenu = new PopupMenu(context, view);
        //Adding menu items
        addMsgItemsToMenu(popupExcludeMenu, view);
        setMenuItemsClickable(popupExcludeMenu, view);

        popupExcludeMenu.show();

    }

    @NonNull
    @Override
    public View display(Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {

        if (view == null)
            view = inflateFromId(context, R.layout.item_contact, parent);

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

        String line = null;

        // Company / Title
        if (!contactPojo.getCompany().isEmpty() || !contactPojo.getTitle().isEmpty() ) {
            line = contactPojo.getCompany();
            if (line.isEmpty()){
                line = contactPojo.getTitle();
            } else {
                if (!contactPojo.getTitle().isEmpty()) {
                    line += " / " + contactPojo.getTitle();
                }
            }
        } else { // Email
            if (contactPojo.normalizedEmail!= null && !contactPojo.normalizedEmail.toString().isEmpty()) {
                line =  contactPojo.normalizedEmail.toString();
            }
        }

        // Contact title
        TextView title = view.findViewById(R.id.item_contact_title);
        if (line==null) {
            title.setVisibility(View.GONE);
        } else {
            title.setVisibility(View.VISIBLE);
            displayHighlighted(StringNormalizer.normalizeWithResult(line,false), line, fuzzyScore, title, context);
        }

        // Contact photo
        ImprovedQuickContactBadge contactIcon = view
                .findViewById(R.id.item_contact_icon);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean("icons-hide", false)) {
            if (contactIcon.getTag() instanceof ContactsPojo && contactPojo.equals(contactIcon.getTag())) {
                icon = contactIcon.getDrawable();
            }
            this.setAsyncDrawable(contactIcon);
        } else {
            contactIcon.setImageDrawable(null);
        }

        ImageView notificationView = view.findViewById(R.id.item_notification_dot);
        notificationView.setVisibility(pojo.getHasNotification() ? View.VISIBLE : View.GONE);
        notificationView.setTag(pojo.getName());

        int primaryColor = UIColors.getPrimaryColor(context);
        notificationView.setColorFilter(primaryColor);



        contactIcon.assignContactUri(Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                String.valueOf(contactPojo.lookupKey)));
        contactIcon.setExtraOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                recordLaunch(v.getContext(), queryInterface);
            }
        });

        //  SOME call
        if (prefs.getBoolean("someCallButton", false)) {
            addSomeCallButton(view);
        }

        // SOME message button
        if (prefs.getBoolean("someMsgButton", false)) {
            addSomeMsgButton(view);
        }

        // Phone action
        ImageButton phoneButton = view.findViewById(R.id.item_contact_action_phone);
        phoneButton.setColorFilter(primaryColor);


        PackageManager pm = context.getPackageManager();

        if (pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            phoneButton.setVisibility(View.VISIBLE);

            phoneButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchCall(v.getContext());
                    recordLaunch(context, queryInterface);
                }
            });
            phoneButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (BuildConfig.DEBUG) Log.i(TAG,"onLongClick");
                    buildCallPopupMenu(view);

                    return true;
                }
            });



        } else {
            phoneButton.setVisibility(View.INVISIBLE);

        }

        return view;
    }

    private void addSomeMsgButton(View view) {
        // SOME message
        Context context=view.getContext();
        ImageButton someMessageButton = view.findViewById(R.id.item_contact_action_some_message);
        someMessageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contactPojo.whatsAppMessaging != 0) {
                    if (BuildConfig.DEBUG)
                        Log.i(TAG, "whatsAppMessaging:" + contactPojo.whatsAppMessaging);
                    openGenericSomeApp(contactPojo.whatsAppMessaging, v.getContext());
                } else if (contactPojo.signalMessaging != 0) {
                    if (BuildConfig.DEBUG)
                        Log.i(TAG, "signalMessaging:" + contactPojo.signalMessaging);
                    openGenericSomeApp(contactPojo.signalMessaging, v.getContext());
                } else if (contactPojo.facebookMessaging != 0) {
                    if (BuildConfig.DEBUG)
                        Log.i(TAG, "facebookMessaging:" + contactPojo.facebookMessaging);
                    openFacebook(v.getContext(), false);
                }

            }
        });

        ViewGroup.LayoutParams viewParamsMsg = someMessageButton.getLayoutParams();
        int widthMsg = viewParamsMsg.width;
        int heightMsg = viewParamsMsg.height;
        if (contactPojo.whatsAppMessaging != 0) {
            if (ContactSearcher.whatsAppIcon == null) {
                IconHelper iconHelper = new IconHelper(context.getPackageManager(), context);
                someMessageButton.setImageBitmap(iconHelper.getCroppedBitmap("com.whatsapp", widthMsg, heightMsg, null, Color.WHITE));
            } else {
                someMessageButton.setImageBitmap(ContactSearcher.whatsAppIcon);
            }

        } else if (contactPojo.signalMessaging != 0) {
            if (ContactSearcher.signalIcon == null) {
                IconHelper iconHelper = new IconHelper(context.getPackageManager(), context);
                someMessageButton.setImageBitmap(iconHelper.getCroppedBitmap("org.thoughtcrime.securesms", widthMsg, heightMsg, null, Color.WHITE));
            } else {
                someMessageButton.setImageBitmap(ContactSearcher.signalIcon);
            }
        } else if (contactPojo.facebookMessaging != 0) {
            if (ContactSearcher.facebookIcon == null) {
                IconHelper iconHelper = new IconHelper(context.getPackageManager(), context);
                someMessageButton.setImageBitmap(iconHelper.getCroppedBitmap("com.facebook.orca", widthMsg, heightMsg, null, Color.BLUE));
            } else {
                someMessageButton.setImageBitmap(ContactSearcher.facebookIcon);
            }
        }


        if (contactPojo.whatsAppMessaging != 0 ||
                contactPojo.signalMessaging != 0 ||
                contactPojo.facebookMessaging != 0) {
            // IM Phone action
            someMessageButton.setVisibility(View.VISIBLE);
        } else {
            someMessageButton.setVisibility(View.GONE);
        }
        someMessageButton.setColorFilter(null);
    } // SOME message button

    private void addSomeCallButton(View view) {
        Context context = view.getContext();
        final ImageButton someCallButton = view.findViewById(R.id.item_contact_action_some_call);
        ViewGroup.LayoutParams viewParams = someCallButton.getLayoutParams();
        int width = viewParams.width;
        int height = viewParams.height;
        if (contactPojo.whatsAppCalling != 0) {
            IconHelper iconHelper = new IconHelper(context.getPackageManager(), context);
            someCallButton.setImageBitmap(iconHelper.getCroppedBitmap("com.whatsapp", width, height, "Call", Color.WHITE));

            // someCallButton.setImageResource(R.drawable.call_whatsapp);
        } else if (contactPojo.signalCalling != 0) {
            IconHelper iconHelper = new IconHelper(context.getPackageManager(), context);
            someCallButton.setImageBitmap(iconHelper.getCroppedBitmap("org.thoughtcrime.securesms", width, height, "Call", Color.WHITE));
        } else if (contactPojo.facebookCalling != 0) {
            IconHelper iconHelper = new IconHelper(context.getPackageManager(), context);
            someCallButton.setImageBitmap(iconHelper.getCroppedBitmap("com.facebook.orca", width, height, "Call", Color.BLUE));
        }
        someCallButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contactPojo.whatsAppCalling != 0) {
                    if (BuildConfig.DEBUG)
                        Log.i(TAG, "whatsAppCalling:" + contactPojo.whatsAppCalling);

                    openGenericSomeApp(contactPojo.whatsAppCalling, v.getContext());
                } else if (contactPojo.signalCalling != 0) {
                    if (BuildConfig.DEBUG)
                        Log.i(TAG, "signalCalling:" + contactPojo.signalCalling);

                    openGenericSomeApp(contactPojo.signalCalling, v.getContext());
                } else if (contactPojo.facebookCalling != 0) {

                    if (BuildConfig.DEBUG)
                        Log.i(TAG, "facebookCalling:" + contactPojo.facebookCalling);

                    openFacebook(v.getContext(), true);
                }
            }
        });

        // IM Phone action
        if (contactPojo.whatsAppCalling != 0 ||
                contactPojo.signalCalling != 0 ||
                contactPojo.facebookCalling != 0) {
            // IM Phone action
            someCallButton.setVisibility(View.VISIBLE);
        } else {
            someCallButton.setVisibility(View.GONE);
        }
    } // SOME call


    @Override
    protected ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView) {
        adapter.add(new ListPopup.Item(context, R.string.ui_item_contact_hint_call));
        adapter.add(new ListPopup.Item(context, R.string.ui_item_contact_hint_sms));
        if (contactPojo.whatsAppCalling != 0) {
            adapter.add(new ListPopup.Item(context, R.string.ui_item_contact_hint_call_whatsapp));
            adapter.add(new ListPopup.Item(context, R.string.ui_item_contact_hint_message_whatsapp));
        }
        if (contactPojo.signalCalling != 0) {
            adapter.add(new ListPopup.Item(context, R.string.ui_item_contact_hint_call_signal));
            adapter.add(new ListPopup.Item(context, R.string.ui_item_contact_hint_message_signal));
        }
        if (contactPojo.facebookCalling != 0) {
            adapter.add(new ListPopup.Item(context, R.string.ui_item_contact_hint_call_facebook));
            adapter.add(new ListPopup.Item(context, R.string.ui_item_contact_hint_message_facebook));
        }
        if (contactPojo.emailLookupKey != 0) {
            adapter.add(new ListPopup.Item(context, R.string.ui_item_contact_email));
        }
        adapter.add(new ListPopup.Item(context, R.string.menu_remove));
        adapter.add(new ListPopup.Item(context, R.string.menu_contact_copy_phone));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_add));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_remove));
        return inflatePopupMenu(adapter, context);
    }

    @Override
    protected boolean popupMenuClickHandler(Context context, RecordAdapter parent, int stringId, View parentView) {
        switch (stringId) {
            case R.string.menu_contact_copy_phone:
                copyPhone(context, contactPojo);
                return true;
            case R.string.ui_item_contact_hint_call_whatsapp:
                launchWhatsAppCall(context);
                return true;
            case R.string.ui_item_contact_hint_call_signal:
                launchSignalCall(context);
                return true;
            case R.string.ui_item_contact_hint_call_facebook:
                openFacebook(context, true);
                return true;
            case R.string.ui_item_contact_hint_message_whatsapp:
                openGenericSomeApp(contactPojo.whatsAppMessaging, context);
                return true;
            case R.string.ui_item_contact_hint_message_signal:
                openGenericSomeApp(contactPojo.signalMessaging, context);
                return true;
            case R.string.ui_item_contact_hint_message_facebook:
                openFacebook(context, false);
                return true;
            case R.string.ui_item_contact_hint_call:
                launchCall(context);
                return true;
            case R.string.ui_item_contact_hint_sms:
                launchMessaging(context);
                return true;
            case R.string.ui_item_contact_email:
                openEmail(context);
                return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId, parentView);
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
            if (BuildConfig.DEBUG) Log.i(TAG, "getDrawable:" + contactPojo.icon);
            if (isDrawableCached())
                return icon;
            if (contactPojo.icon != null) {
                if (BuildConfig.DEBUG) Log.i(TAG, "getDrawable:" + contactPojo.icon);
                InputStream inputStream = null;
                try {
                    inputStream = context.getContentResolver()
                            .openInputStream(contactPojo.icon);
                    return icon = Drawable.createFromStream(inputStream, null);
                } catch (FileNotFoundException ignored) {
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
                new String[]{ContactsContract.RawContacts._ID, ContactsContract.RawContacts.CONTACT_ID},
                ContactsContract.RawContacts.ACCOUNT_TYPE + "= ?",
                new String[]{"com.whatsapp"}, null);
        if (contactCursor != null && contactCursor.moveToFirst()) {
            Uri photoId = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, whatsappContactId);
            Uri photoUri = Uri.withAppendedPath(photoId, ContactsContract.RawContacts.DisplayPhoto.CONTENT_DIRECTORY);
            return photoUri;
        }
        return null;

    }


    @Override
    public void doLaunch(Context context, View v) {
        buildContactClickPopupMenu(v);

        /*Intent viewContact = new Intent(Intent.ACTION_VIEW);

        viewContact.setData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                String.valueOf(contactPojo.lookupKey)));
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewContact.setSourceBounds(v.getClipBounds());
        }

        viewContact.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        viewContact.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(viewContact);
        */
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
                recordLaunch(context, queryInterface);
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
        if (Permission.ensureCallPhonePermission(phoneIntent)) {
            // Pre-android 23, or we already have permission
            context.startActivity(phoneIntent);

            // Register launch in the future
            // (animation delay)
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recordLaunch(context, queryInterface);
                }
            }, KissApplication.TOUCH_DELAY);
        }
    }


    private void launchWhatsAppCall(final Context context) {
        // Create the intent to start a phone call


        // Pre-android 23, or we already have permission
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        int contactId = contactPojo.whatsAppCalling;
        if (BuildConfig.DEBUG) Log.i(TAG, "launchWhatsAppCall, contactId:" + contactId);
        // the _ids you save goes here at the end of /data/12562
        intent.setDataAndType(Uri.parse("content://com.android.contacts/data/" + contactId),
                "vnd.android.cursor.item/vnd.com.whatsapp.voip.call");
        // intent.setPackage("com.whatsapp");

        // TODO: get this intent from contacts database instead of hardcoding?
        intent.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.accountsync.CallContactLandingActivity"));

        // whatsapp calling needs call permission
        if (Permission.ensureCallPhonePermission(intent)) {
            try {
                context.startActivity(intent);
            } catch (Exception e){
                Log.i(TAG,"exception in launchWhatsAppCall, try 1: " +e);
                try {
                    intent.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.accountsync.ProfileActivity"));
                    context.startActivity(intent);
                } catch (Exception e2){
                    Log.i(TAG,"exception in launchWhatsAppCall, try 2: " +e2);
                }
            }

            // Register launch in the future
            // (animation delay)
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recordLaunch(context, queryInterface);
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
        int contactId = contactPojo.signalCalling;
        if (BuildConfig.DEBUG) Log.i(TAG, "launchSignalCall, contactId:" + contactId);
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
                    recordLaunch(context, queryInterface);
                }
            }, KissApplication.TOUCH_DELAY);
        }


        /*
                if(BuildConfig.DEBUG) Log.i(TAG,"openGenericSomeApp:"+signalCalling);
        Cursor c = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                new String[] { ContactsContract.Contacts.Data._ID }, ContactsContract.Data.DATA1 + "=?",
                new String[] { signalCalling }, null);
        c.moveToFirst();
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.contacts/data/" + c.getString(0)));
        if(BuildConfig.DEBUG) Log.i(TAG,"c.getString(0):"+c.getString(0)+" i:"+i.getAction());
        i.setDataAndType(Uri.parse("content://com.android.contacts/data/" + c.getString(0)),
                SIGNAL_CALL_MIMETYPE);
        context.startActivity(i);
        c.close();
         */

    }

    private void openGenericSomeApp(int contactId, Context context) {
        if (BuildConfig.DEBUG) Log.i(TAG, "openGenericSomeApp:" + contactId);
     /*   Cursor c = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                new String[] { ContactsContract.Contacts.Data._ID }, ContactsContract.Data._ID + "=?",
                new String[] { Integer.toString(signalCalling) }, null);
        c.moveToFirst();
*/
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.contacts/data/" + contactId));

        context.startActivity(i);
        //   c.close();
    }

    private void openFacebook(Context context, boolean call) {
        if (BuildConfig.DEBUG) Log.i(TAG, "openFacebook:" + contactPojo.facebookCalling);
     /*   Cursor c = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                new String[] { ContactsContract.Contacts.Data._ID }, ContactsContract.Data._ID + "=?",
                new String[] { Integer.toString(whatsAppnumber) }, null);
        c.moveToFirst();
        //c.moveToNext(); //gets the call intent */
        Intent i;
        if (!call) {
            i = new Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.contacts/data/" + contactPojo.facebookMessaging));
            i.setDataAndType(Uri.parse("content://com.android.contacts/data/" + contactPojo.facebookMessaging),
                    "vnd.android.cursor.item/com.facebook.messenger.chat");
        } else {
            i = new Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.contacts/data/" + contactPojo.facebookCalling));
            i.setDataAndType(Uri.parse("content://com.android.contacts/data/" + contactPojo.facebookCalling),
                    "vnd.android.cursor.item/com.facebook.messenger.audiocall");
        }

        i.setComponent(new ComponentName("com.facebook.orca", "com.facebook.messenger.intents.IntentHandlerActivity"));

        context.startActivity(i);
        //  c.close();
    }

    private void openEmail(Context context) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("content://com.android.contacts/data/" + contactPojo.emailLookupKey));
        intent.setDataAndType(Uri.parse("content://com.android.contacts/data/" + contactPojo.emailLookupKey),
                ContactsContract.CommonDataKinds.Email.CONTENT_TYPE);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{String.valueOf(contactPojo.normalizedEmail)});
        context.startActivity(Intent.createChooser(intent, "Send Email"));
    }
}
