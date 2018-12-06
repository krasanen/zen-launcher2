/* Gracenote Android Music SDK Sample Application
 *
 * Copyright (C) 2010 Gracenote, Inc. All Rights Reserved.
 */
package fi.zmengames.zlauncher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.gracenote.gnsdk.GnDescriptor;
import com.gracenote.gnsdk.GnError;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLicenseInputMode;
import com.gracenote.gnsdk.GnList;
import com.gracenote.gnsdk.GnLocale;
import com.gracenote.gnsdk.GnLocaleGroup;
import com.gracenote.gnsdk.GnLookupData;
import com.gracenote.gnsdk.GnLookupLocalStreamIngest;
import com.gracenote.gnsdk.GnLookupLocalStreamIngestStatus;
import com.gracenote.gnsdk.GnManager;
import com.gracenote.gnsdk.GnMic;
import com.gracenote.gnsdk.GnMusicIdStream;
import com.gracenote.gnsdk.GnMusicIdStreamIdentifyingStatus;
import com.gracenote.gnsdk.GnMusicIdStreamPreset;
import com.gracenote.gnsdk.GnMusicIdStreamProcessingStatus;
import com.gracenote.gnsdk.GnRegion;
import com.gracenote.gnsdk.GnResponseAlbums;
import com.gracenote.gnsdk.GnStatus;
import com.gracenote.gnsdk.GnStorageSqlite;
import com.gracenote.gnsdk.GnUser;
import com.gracenote.gnsdk.GnUserStore;
import com.gracenote.gnsdk.IGnAudioSource;
import com.gracenote.gnsdk.IGnCancellable;
import com.gracenote.gnsdk.IGnLookupLocalStreamIngestEvents;
import com.gracenote.gnsdk.IGnMusicIdStreamEvents;
import com.gracenote.gnsdk.IGnSystemEvents;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;


import static fi.zmengames.zlauncher.GracenoteMusicIDWidget.appString;

/**
 * This class is used to render history detail into the screen.
 */
public final class HistoryDetails extends Activity {

    public static final String 				gnsdkClientId 			= "485123343";
    public static final String 				gnsdkClientTag 			= "D5310AEAD2EBCEED3387025F2E996131";
    public static final String 				gnsdkLicenseFilename 	= "license.txt";	// app expects this file as an "asset"
    private static final String    		gnsdkLogFilename 		= "sample.log";
    private static final String 		appString				= "GFM Sample";

    CheckBox deletecheckBox;
    DatabaseAdapter db = null;
    List<String> deleteId = new ArrayList<String>();
    public static final int DELETE_OPTION_ID = 0;
    public static final int DELETE_OPTION_ALL = 1;
    TextView trackText, artistText, albumText, dateText;
    ImageView coverArtImage;
    HistoryListAdapter adapter;
    ListView historylist;
    int i = 0;
    Cursor cursor;
    private IGnAudioSource gnMicrophone;
    private GnManager gnManager;
    private GnUser gnUser;
    private Activity activity;
    private Context context;

    private static final String TAG = HistoryDetails.class.getSimpleName();
    /**
     * This class is used as a database Adapter to render a history.
     */
    private class HistoryListAdapter extends CursorAdapter {
        private final LayoutInflater mInflater;

        public HistoryListAdapter(Context context, Cursor cursor) {
            super(context, cursor);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return super.getView(position, convertView, parent);
        }

        @Override
        public void bindView(View view, Context context, final Cursor cursor) {
            coverArtImage = (ImageView) view.findViewById(R.id.CoverArtImage);
            coverArtImage.setImageResource(R.drawable.no_cover_art);
            final String artist=cursor.getString(3);
            final String track=cursor.getString(2);

            byte[] theByteArray = cursor.getBlob(4);
            if (theByteArray != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(theByteArray, 0,
                        theByteArray.length);
                coverArtImage.setImageBitmap(bitmap);
            }
            coverArtImage.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    String uri = "https://www.google.com/#q="+URLEncoder.encode(artist +" "+ track+ " lyrics");
                    Log.d(TAG,"uri"+uri);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(browserIntent);
                }
            });
            trackText = (TextView) view.findViewById(R.id.TrackTextView);
            trackText.setText(track);

            artistText = (TextView) view.findViewById(R.id.ArtistTextView);
            artistText.setText(artist);

            albumText = (TextView) view.findViewById(R.id.AlbumTextView);
            albumText.setText(cursor.getString(1));

            dateText = (TextView) view.findViewById(R.id.DateTimeTextView);
            dateText.setText(currentTimeZonedate(cursor.getString(5)));

            deletecheckBox.setTag(cursor.getString(0));
            deletecheckBox.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    CheckBox chk = (CheckBox) v;
                    if (chk.isChecked()) {
                        deleteId.add(chk.getTag().toString());
                        //System.out.println("Chaked......" + chk.getTag());
                    } else {
                        deleteId.remove(chk.getTag().toString());
                        //System.out.println("UnChaked......" + chk.getTag());
                    }
                }
            });
        }

        @Override
        public View newView(Context context, final Cursor cursor,
                            ViewGroup parent) {
            final View view = mInflater.inflate(R.layout.history_row, parent,
                    false);
            view.setTag(cursor.getString(0));
            deletecheckBox = (CheckBox) view.findViewById(R.id.DeleteCheckbox);
            return view;
        }
    }











    /**
     * Helpers to read license file from assets as string
     */
    private String getAssetAsString(String assetName) {

        String assetString = null;
        InputStream assetStream;

        try {

            assetStream = this.getApplicationContext().getAssets().open(assetName);
            if (assetStream != null) {

                java.util.Scanner s = new java.util.Scanner(assetStream).useDelimiter("\\A");

                assetString = s.hasNext() ? s.next() : "";
                assetStream.close();

            } else {
                Log.e(appString, "Asset not found:" + assetName);
            }

        } catch (IOException e) {

            Log.e(appString, "Error getting asset as string: " + e.getMessage());

        }

        return assetString;
    }

    /**
     * GNSDK bundle ingest status event delegate
     */
    private class BundleIngestEvents implements IGnLookupLocalStreamIngestEvents {

        @Override
        public void statusEvent(GnLookupLocalStreamIngestStatus status, String bundleId, IGnCancellable canceller) {
            setStatus("Bundle ingest progress: " + status.toString(), true);
        }
    }

    /**
     * Loads a locale
     */
    class LocaleLoadRunnable implements Runnable {
        GnLocaleGroup group;
        GnLanguage language;
        GnRegion region;
        GnDescriptor descriptor;
        GnUser user;


        LocaleLoadRunnable(
                GnLocaleGroup group,
                GnLanguage language,
                GnRegion region,
                GnDescriptor descriptor,
                GnUser user) {
            this.group = group;
            this.language = language;
            this.region = region;
            this.descriptor = descriptor;
            this.user = user;
        }

        @Override
        public void run() {
            try {

                GnLocale locale = new GnLocale(group, language, region, descriptor, gnUser);
                locale.setGroupDefault();

            } catch (GnException e) {
                Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            }
        }
    }

    /**
     * Loads a local bundle for MusicID-Stream lookups
     */
    class LocalBundleIngestRunnable implements Runnable {
        Context context;

        LocalBundleIngestRunnable(Context context) {
            this.context = context;
        }

        public void run() {
            try {

                // our bundle is delivered as a package asset
                // to ingest the bundle access it as a stream and write the bytes to
                // the bundle ingester
                // bundles should not be delivered with the package as this, rather they
                // should be downloaded from your own online service

                InputStream bundleInputStream = null;
                int ingestBufferSize = 1024;
                byte[] ingestBuffer = new byte[ingestBufferSize];
                int bytesRead = 0;

                GnLookupLocalStreamIngest ingester = new GnLookupLocalStreamIngest(new BundleIngestEvents());

                try {

                    bundleInputStream = context.getAssets().open("1557.b");

                    do {

                        bytesRead = bundleInputStream.read(ingestBuffer, 0, ingestBufferSize);
                        if (bytesRead == -1)
                            bytesRead = 0;

                        ingester.write(ingestBuffer, bytesRead);

                    } while (bytesRead != 0);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                ingester.flush();

            } catch (GnException e) {
                Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            }

        }
    }

    /**
     * Updates a locale
     */
    class LocaleUpdateRunnable implements Runnable {
        GnLocale locale;
        GnUser user;


        LocaleUpdateRunnable(
                GnLocale locale,
                GnUser user) {
            this.locale = locale;
            this.user = user;
        }

        @Override
        public void run() {
            try {
                locale.update(user);
            } catch (GnException e) {
                Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            }
        }
    }

    /**
     * Updates a list
     */
    class ListUpdateRunnable implements Runnable {
        GnList list;
        GnUser user;


        ListUpdateRunnable(
                GnList list,
                GnUser user) {
            this.list = list;
            this.user = user;
        }

        @Override
        public void run() {
            try {
                list.update(user);
            } catch (GnException e) {
                Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            }
        }
    }

    /**
     * Receives system events from GNSDK
     */
    class SystemEvents implements IGnSystemEvents {
        @Override
        public void localeUpdateNeeded(GnLocale locale) {
            // Locale update is detected
            Thread localeUpdateThread = new Thread(new LocaleUpdateRunnable(locale, gnUser));
            localeUpdateThread.start();
        }

        @Override
        public void listUpdateNeeded(GnList list) {
            // List update is detected
            Thread listUpdateThread = new Thread(new ListUpdateRunnable(list, gnUser));
            listUpdateThread.start();
        }

        @Override
        public void systemMemoryWarning(long currentMemorySize, long warningMemorySize) {
            // only invoked if a memory warning limit is configured
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.history_layout);
                MainActivity.checkPermissionRecordAudio(this);
        historylist = (ListView) findViewById(R.id.HistoryList);
        historylist.setVisibility(View.VISIBLE);
        View launcherButton = findViewById(android.R.id.home);
        launcherButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        activity = this;
        context = this.getApplicationContext();

        // check the client id and tag have been set
        if ((gnsdkClientId == null) || (gnsdkClientTag == null)) {
            showError("Please set Client ID and Client Tag");
            return;
        }

        // get the gnsdk license from the application assets
        String gnsdkLicense = null;
        if ((gnsdkLicenseFilename == null) || (gnsdkLicenseFilename.length() == 0)) {
            showError("License filename not set");
        } else {
            gnsdkLicense = getAssetAsString(gnsdkLicenseFilename);
            if (gnsdkLicense == null) {
                showError("License file not found: " + gnsdkLicenseFilename);
                return;
            }
        }

        try {

            // GnManager must be created first, it initializes GNSDK
            gnManager = new GnManager(context, gnsdkLicense, GnLicenseInputMode.kLicenseInputModeString);

            // provide handler to receive system events, such as locale update needed
            gnManager.systemEventHandler(new SystemEvents());

            // get a user, if no user stored persistently a new user is registered and stored
            // Note: Android persistent storage used, so no GNSDK storage provider needed to store a user
            gnUser = new GnUser(new GnUserStore(context), gnsdkClientId, gnsdkClientTag, appString);
            db = new DatabaseAdapter(HistoryDetails.this, gnUser);
            getHistory();
            // enable storage provider allowing GNSDK to use its persistent stores
            GnStorageSqlite.enable();

            // Loads data to support the requested locale, data is downloaded from Gracenote Service if not
            // found in persistent storage. Once downloaded it is stored in persistent storage (if storage
            // provider is enabled). Download and write to persistent storage can be lengthy so perform in 
            // another thread
            Thread localeThread = new Thread(
                    new LocaleLoadRunnable(GnLocaleGroup.kLocaleGroupMusic,
                            GnLanguage.kLanguageEnglish,
                            GnRegion.kRegionGlobal,
                            GnDescriptor.kDescriptorDefault,
                            gnUser)
            );
            localeThread.start();

            // Ingest MusicID-Stream local bundle, perform in another thread as it can be lengthy
            Thread ingestThread = new Thread(new LocalBundleIngestRunnable(context));
            ingestThread.start();

            // Set up for continuous listening from the microphone
            // - create microphone, this can live for lifetime of app
            // - create GnMusicIdStream instance, this can live for lifetime of app
            // - configure
            // Starting and stopping continuous listening should be started and stopped
            // based on Activity life-cycle, see onPause and onResume for details
            // To show audio visualization we wrap GnMic in a visualization adapter
            gnMicrophone = new AudioVisualizeAdapter(new GnMic());
            gnMusicIdStream = new GnMusicIdStream(gnUser, GnMusicIdStreamPreset.kPresetMicrophone, new MusicIDStreamEvents());
            gnMusicIdStream.options().lookupData(GnLookupData.kLookupDataContent, true);
            gnMusicIdStream.options().lookupData(GnLookupData.kLookupDataSonicData, true);
            gnMusicIdStream.options().resultSingle(true);

            // Retain GnMusicIdStream object so we can cancel an active identification if requested


        } catch (GnException e) {

            Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            showError(e.errorAPI() + ": " + e.errorDescription());
            return;

        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(appString, e.getMessage());
                showError(e.getMessage());
            } else {
                e.printStackTrace();
                //  setUIState(GracenoteMusicID.UIState.DISABLED);
            }
            return;

        }

    }



    public void displayLoader(Boolean display) {
        View launcherButton = findViewById(android.R.id.home);
        final View loaderSpinner = findViewById(R.id.loaderBar2);
        int animationDuration = getResources().getInteger(
                android.R.integer.config_longAnimTime);
        launcherButton.setVisibility(View.INVISIBLE);
        // Do not display animation if launcher button is already visible
        if (!display && launcherButton.getVisibility() == View.INVISIBLE) {
            launcherButton.setVisibility(View.VISIBLE);

            // Animate transition from loader to launch button
            launcherButton.setAlpha(0);
            launcherButton.animate()
                    .alpha(1f)
                    .setDuration(animationDuration)
                    .setListener(null);
            loaderSpinner.animate()
                    .alpha(0f)
                    .setDuration(animationDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            loaderSpinner.setVisibility(View.GONE);
                            loaderSpinner.setAlpha(1);
                        }
                    });
        } else if (display) {
            launcherButton.setVisibility(View.INVISIBLE);
            loaderSpinner.setVisibility(View.VISIBLE);
        }
    }

    private void animate(){
        View launcherButton = findViewById(android.R.id.home);
        int animationDuration = 2000;
        // get the center for the clipping circle
        int cx = (launcherButton.getLeft() + launcherButton.getRight()) / 2;
        int cy = (launcherButton.getTop() + launcherButton.getBottom()) / 2;

        // get the final radius for the clipping circle
        int finalRadius = getActionBar().getHeight();
        final Animator anim = ViewAnimationUtils.createCircularReveal(launcherButton, cx, cy, 0, finalRadius);
        anim.setDuration(animationDuration);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });
        anim.start();
    }

    private static boolean idRunning = false;
    void idNow() {
        if (gnMusicIdStream == null || gnMicrophone == null) {
            return;
        }
        animate();
        audioProcessThread = new Thread(new AudioProcessRunnable());
        audioProcessThread.start();

        // calling gnMusicIdStream.identifyAlbumAsync() in musicIdStreamProcessingStatusEvent() only after audio-processing-started callback is received

    }

    /**
     * Audio visualization adapter.
     * Sits between GnMic and GnMusicIdStream to receive audio data as it
     * is pulled from the microphone allowing an audio visualization to be
     * implemented.
     */
    class AudioVisualizeAdapter implements IGnAudioSource {

        private IGnAudioSource audioSource;
        private int numBitsPerSample;
        private int numChannels;
        private static final int SCALE_FACTOR = 15;
        private static final int PADDING = 20;
        private static final int MIN_FLIP_INTERVAL_MS = 100;

        public AudioVisualizeAdapter(IGnAudioSource audioSource) {
            this.audioSource = audioSource;
        }

        @Override
        public long sourceInit() {
            if (audioSource == null) {
                return 1;
            }
            long retVal = audioSource.sourceInit();

            // get format information for use later
            if (retVal == 0) {
                numBitsPerSample = (int) audioSource.sampleSizeInBits();
                numChannels = (int) audioSource.numberOfChannels();
            }

            return retVal;
        }

        @Override
        public long numberOfChannels() {
            return numChannels;
        }

        @Override
        public long sampleSizeInBits() {
            return numBitsPerSample;
        }

        @Override
        public long samplesPerSecond() {
            if (audioSource == null) {
                return 0;
            }
            return audioSource.samplesPerSecond();
        }

        @Override
        public long getData(ByteBuffer buffer, long bufferSize) {
            if (audioSource == null) {
                return 0;
            }


            long numBytes = audioSource.getData(buffer, bufferSize);

            if (numBytes != 0) {
                // perform visualization effect here
                // Note: Since API level 9 Android provides
                // android.media.audiofx.Visualizer which can be used to obtain
                // the
                // raw waveform or FFT, and perform measurements such as peak
                // RMS. You may wish to consider Visualizer class
                // instead of manually extracting the audio as shown here.
                // This sample does not use Visualizer so it can demonstrate how
                // you can access the raw audio for purposes
                // not limited to visualization.


            }

            return numBytes;
        }

        @Override
        public void sourceClose() {
            if (audioSource != null) {
                audioSource.sourceClose();
            }
        }


    }


    private Object lock = new Object();

    class VisDisplayThread extends Thread {

        public void run() {

            while (true) {

                try {
                    synchronized (lock) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    break;
                }


            }

        }


    }

    /**
     * Helper to set the application status message
     */
    protected volatile long lastLookup_matchTime = 0;

    /**
     * GNSDK MusicID-Stream event delegate
     */
    public class MusicIDStreamEvents implements IGnMusicIdStreamEvents {

        HashMap<String, String> gnStatus_to_displayStatus;

        public MusicIDStreamEvents() {
            gnStatus_to_displayStatus = new HashMap<String, String>();
            gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingStarted.toString(), "Identification started");
            gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingFpGenerated.toString(), "Fingerprinting complete");
            gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingLocalQueryStarted.toString(), "Lookup started");
            gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingOnlineQueryStarted.toString(), "Lookup started");
//			gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingEnded.toString(), "Identification complete");
        }

        @Override
        public void statusEvent(GnStatus status, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable cancellable) {

        }

        @Override
        public void musicIdStreamProcessingStatusEvent(GnMusicIdStreamProcessingStatus status, IGnCancellable canceller) {

            if (GnMusicIdStreamProcessingStatus.kStatusProcessingAudioStarted.compareTo(status) == 0) {
                try {

                    Log.i(appString, "calling idnow");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            animate();
                        }
                    });
                    gnMusicIdStream.identifyAlbumAsync();


                } catch (GnException e) {
                    e.printStackTrace();
                    try {
                        gnMusicIdStream.audioProcessStop();
                    } catch (GnException ex) {
                        // ignore
                    }
                }
            }

        }

        @Override
        public void musicIdStreamIdentifyingStatusEvent(GnMusicIdStreamIdentifyingStatus status, IGnCancellable canceller) {
            if (gnStatus_to_displayStatus.containsKey(status.toString())) {
                setStatus(String.format("%s", gnStatus_to_displayStatus.get(status.toString())), true);
            }

            if (status.compareTo(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingLocalQueryStarted) == 0) {

            } else if (status.compareTo(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingOnlineQueryStarted) == 0) {

            }

            if (status == GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingEnded) {

            }
        }

        @Override
        public void musicIdStreamAlbumResult(GnResponseAlbums result, IGnCancellable canceller) {
            lastLookup_matchTime = SystemClock.elapsedRealtime() - lastLookup_startTime;
            activity.runOnUiThread(new UpdateResultsRunnable(result));
            new Thread(new AudioProcessStopRunnable()).start();
        }

        @Override
        public void musicIdStreamIdentifyCompletedWithError(GnError error) {

            if (error.errorCode() != 0) {
                Log.i(appString, "error upon complete: " + error.errorDescription());

                //GnMusicIdStream.audioProcessStop() waits for this result callback to finish,
                //so call audioProcessStop() in another thread and don't block here
                new Thread(new AudioProcessStopRunnable()).start();
            }
        }
    }

    class AudioProcessStopRunnable implements Runnable {

        @Override
        public void run() {

            if (gnMusicIdStream != null) {

                try {
                    gnMusicIdStream.audioProcessStop();
                } catch (GnException e) {

                    Log.e(appString, e.errorCode() + ", "
                            + e.errorDescription() + ", "
                            + e.errorModule());

                }

            }

        }


    }

    /**
     * Adds album results to UI via Runnable interface
     */
    public class UpdateResultsRunnable implements Runnable {

        GnResponseAlbums albumsResult;

        UpdateResultsRunnable(GnResponseAlbums albumsResult) {
            this.albumsResult = albumsResult;
        }

        @Override
        public void run() {
            idRunning = false;
            if (albumsResult.resultCount() == 0) {

                setStatus("No match", true);

            } else {

                setStatus("Match found", true);

                trackChanges(albumsResult);

            }

        }
    }


    private synchronized void trackChanges(GnResponseAlbums albums) {
        Thread thread = new Thread(new InsertChangesRunnable(albums));
        thread.start();
    }

    class InsertChangesRunnable implements Runnable {
        GnResponseAlbums row;

        InsertChangesRunnable(GnResponseAlbums row) {
            this.row = row;
        }

        @Override
        public synchronized void run() {
            try {
                db.open();
                db.insertChanges(row);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getHistory();
                    }
                });

            } catch (GnException e) {
                // ignore
            }
        }
    }

    private void setStatus(String statusMessage, boolean clearStatus) {
        Log.d("setStatus", "statusMessage" + statusMessage);
    }

    private void showError(String errorMessage) {
        setStatus(errorMessage, true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.grace_history_menu, menu);
        return true;
    }

    protected volatile long lastLookup_startTime;
    private GnMusicIdStream gnMusicIdStream;
    Thread audioProcessThread;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.listen:
                if (!idRunning) {
                    lastLookup_startTime = SystemClock.elapsedRealtime();
                    idNow();
                }
                break;
            case R.id.grace_history_delete:
                //System.out.println("delete click");
                int count = deleteId.size();
                db.open();
                for (int i = 0; i < count; i++) {
                    db.deleterow(deleteId.get(i));
                }
                db.close();
                getHistory();
                break;
            case R.id.grace_clearall:
                db.open();
                db.deleteAll();
                db.close();
                getHistory();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }



    public void getHistory() {
        //System.out.println("count i :"+i++);
        //System.out.println("object created");
        db.open();
        cursor = db.getcursor();
        //System.out.println("cursor count :" + cursor.getCount());
        historylist.setItemsCanFocus(false);
        historylist.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        adapter = new HistoryListAdapter(HistoryDetails.this, cursor);
        historylist.setAdapter(adapter);
    }

    public String currentTimeZonedate(String date) {
        String currentDate = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy - HH:mm:ss");
        SimpleDateFormat actFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        SimpleDateFormat dateFormatTimeZome = new SimpleDateFormat("Z");
        dateFormat.setTimeZone(TimeZone.getDefault());
        TimeZone _timeZone = TimeZone.getDefault();
        long gmtRawOffset = _timeZone.getRawOffset();

        try {
            Date dateObj = actFormat.parse(date);
            currentDate = dateFormat.format(dateObj);
            String timezone = dateFormatTimeZome.format(dateObj);
            Date convertedDate = new Date(dateObj.getTime() + gmtRawOffset);
            currentDate = actFormat.format(convertedDate);
        } catch (Exception e) {
            Log.e("GN", "Exception :" + e.getMessage());
        }
        return currentDate;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("TAG", "on pause");
        idRunning = false;
        if (cursor != null)
            cursor.close();
        if (db != null)
            db.close();
        if ( gnMusicIdStream != null ) {
            Log.d(TAG,"stop gnMusicIdStream");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        // to ensure no pending identifications deliver results while your app is
                        // paused it is good practice to call cancel
                        // it is safe to call identifyCancel if no identify is pending
                        gnMusicIdStream.identifyCancel();

                        // stopping audio processing stops the audio processing thread started
                        // in onResume
                        gnMusicIdStream.audioProcessStop();
                    } catch (GnException e) {

                        Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
                        showError( e.errorAPI() + ": " +  e.errorDescription() );

                    }
                }
            }).start();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        getHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (gnMusicIdStream != null) {
            // Create a thread to process the data pulled from GnMic
            // Internally pulling data is a blocking call, repeatedly called until
            // audio processing is stopped. This cannot be called on the main thread.
            Thread audioProcessThread = new Thread(new AudioProcessRunnable());
            audioProcessThread.start();

        }
    }

    /**
     * GnMusicIdStream object processes audio read directly from GnMic object
     */
    class AudioProcessRunnable implements Runnable {

        @Override
        public void run() {
            try {
                idRunning = true;
                // start audio processing with GnMic, GnMusicIdStream pulls data from GnMic internally

                gnMusicIdStream.audioProcessStart(gnMicrophone);


            } catch (GnException e) {
                idRunning = false;
                Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
                showError(e.errorAPI() + ": " + e.errorDescription());

            }
        }
    }


}