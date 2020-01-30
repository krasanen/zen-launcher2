package fr.neamar.kiss.forwarder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;

import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.dataprovider.ContactsProvider;

import static android.app.Activity.RESULT_OK;
import static fr.neamar.kiss.MainActivity.MY_PERMISSIONS_CAMERA;


public class Permission extends Forwarder {
    private static final String TAG = Permission.class.getSimpleName();
    private static final int PERMISSION_READ_CONTACTS = 0;
    private static final int PERMISSION_CALL_PHONE = 1;

    // Static weak reference to the main activity, this is sadly required
    // to ensure classes requesting permission can access activity.requestPermission()
    private static WeakReference<MainActivity> currentMainActivity = new WeakReference<MainActivity>(null);

    /**
     * Sometimes, we need to wait for the user to give us permission before we can start an intent.
     * Store the intent here for later use.
     * Ideally, we'd want to use MainActivity to store this, but MainActivity has stateNotNeeded=true
     * which means it's always rebuild from scratch, we can't store any state in it.
     * This means that when we use pendingIntent, it's highly likely taht by the time we end up using it,
     * currentMainActivity will have changed
     */
    private static Intent pendingIntent = null;

    /**
     * Try to start the dialer with specified intent, if we have permission already
     * Otherwise, ask for permission and store the intent for future use;
     *
     * @return true if we do have permission already, false if we're asking for permission now and will handle dispatching the intent in the Forwarder
     */
    public static boolean ensureCallPhonePermission(Intent pendingIntent) {
        MainActivity mainActivity = Permission.currentMainActivity.get();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mainActivity != null && mainActivity.checkSelfPermission(android.Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            mainActivity.requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE},
                    Permission.PERMISSION_CALL_PHONE);
            Permission.pendingIntent = pendingIntent;

            return false;
        }

        return true;
    }

    public static boolean checkContactPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    public static void askContactPermission() {
        // If we don't have permission to list contacts, ask for it.
        MainActivity mainActivity = Permission.currentMainActivity.get();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mainActivity != null) {
            mainActivity.requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS},
                    Permission.PERMISSION_READ_CONTACTS);
        }
    }

    Permission(MainActivity mainActivity) {
        super(mainActivity);
        // Store the latest reference to a MainActivity
        currentMainActivity = new WeakReference<>(mainActivity);
    }

    void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length == 0) {
            return;
        }
        if (BuildConfig.DEBUG) Log.d(TAG,"permissions:"+permissions);
        if (requestCode == PERMISSION_READ_CONTACTS && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Great! Reload the contact provider. We're done :)
            ContactsProvider contactsProvider = KissApplication.getApplication(mainActivity).getDataHandler().getContactsProvider();
            if (contactsProvider != null) {
                contactsProvider.reload();
            }
        } else if (requestCode == PERMISSION_CALL_PHONE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Great! Start the intent we stored for later use.
                KissApplication kissApplication = KissApplication.getApplication(mainActivity);
                mainActivity.startActivity(pendingIntent);
                pendingIntent = null;

                // Record launch to clear search results
                mainActivity.launchOccurred();
            } else {
                Toast.makeText(mainActivity, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == MainActivity.MY_PERMISSIONS_REQUEST_READ_STORAGE) {
            //premission to read storage
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mainActivity.signIn(mainActivity.action);

            } else {

                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(mainActivity, "We Need permission Storage", Toast.LENGTH_SHORT).show();
            }
            return;
        }else if (requestCode == MainActivity.MY_PERMISSIONS_RECORD_AUDIO) {
            //premission to read storage
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //mainActivity.checkAudio();
                // permission was granted, yay! Do the
                // contacts-related task you need to do.

            } else {

                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(mainActivity, "We Need permission to record", Toast.LENGTH_SHORT).show();
            }
            return;
        } else if (requestCode == MY_PERMISSIONS_CAMERA)
            if (BuildConfig.DEBUG) Log.i(TAG, "REQUEST_MY_PERMISSIONS_CAMERA");
            if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (BuildConfig.DEBUG) Log.i(TAG, "OK");
                mainActivity.startBarCodeScan();
            } else {
                if (BuildConfig.DEBUG) Log.i(TAG, "FALSE");
            }

    }
}
