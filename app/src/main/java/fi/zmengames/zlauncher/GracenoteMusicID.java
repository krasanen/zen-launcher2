/* Gracenote Android Music SDK Sample Application
 *
 * Copyright (C) 2010 Gracenote, Inc. All Rights Reserved.
 */

package fi.zmengames.zlauncher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.gracenote.gnsdk.*;

import fr.neamar.kiss.R;


/**
 * <p>
 * This reference application introduces the basic coding techniques for
 * accessing Gracenote's file and stream recognition technology, and metadata and 
 * content (such as Cover Art).
 * </p>
 * <p>
 * Recognize all audio in MediaStore by pressing "LibraryID" button, utilizing file
 * recognition technology.
 * Recognize audio from the device's microphone by pressing the "IDNow" button,
 * utilizing stream recognition technology.
 * </p>
 * <p>
 * Once a file or a stream has been recognized its metadata and Cover Art is displayed 
 * in the main results pane. Note to save memory only the first 10 results display Cover
 * Art. Additional detail can be viewed by pressing on a result.
 * </p>
 */
public class GracenoteMusicID extends Activity {

	// set these values before running the sample
	static final String 				gnsdkClientId 			= "485123343";
	static final String 				gnsdkClientTag 			= "D5310AEAD2EBCEED3387025F2E996131";
	static final String 				gnsdkLicenseFilename 	= "license.txt";	// app expects this file as an "asset"
	private static final String    		gnsdkLogFilename 		= "sample.log";
	private static final String 		appString				= "GFM Sample";
	
	private Activity					activity;
	private Context 					context;
	
	// ui objects
	private TextView 					statusText;
	private Button						buttonSettings;
	private SettingsMenu				settingsMenu;
	private Button 						buttonIDNow;
	private Button 						buttonTextSearch;
	private Button						buttonHistory; 
	private Button						buttonLibraryID;
	private Button						buttonCancel;
	private Button						buttonVisShowHide;
	private LinearLayout				linearLayoutHomeContainer;
	private LinearLayout				linearLayoutVisContainer;
	private AudioVisualizeDisplay		audioVisualizeDisplay;
	private boolean						visShowing;

	protected ViewGroup 				metadataListing;
	private final int 					metadataMaxNumImages 	= 10;
	private ArrayList<mOnClickListener>	metadataRow_OnClickListeners;

	// Gracenote objects
	private GnManager 					gnManager;
	private GnUser             			gnUser;
	private GnMusicIdStream 			gnMusicIdStream;
	private IGnAudioSource				gnMicrophone;
	private GnLog						gnLog;
	private List<GnMusicId>				idObjects				= new ArrayList<GnMusicId>();
	private List<GnMusicIdFile>			fileIdObjects			= new ArrayList<GnMusicIdFile>();
	private List<GnMusicIdStream>		streamIdObjects			= new ArrayList<GnMusicIdStream>();
	
	// store some tracking info about the most recent MusicID-Stream lookup
	protected volatile boolean 			lastLookup_local		 = false;	// indicates whether the match came from local storage
	protected volatile long				lastLookup_matchTime 	 = 0;  		// total lookup time for query
	protected volatile long				lastLookup_startTime;  				// start time of query
	private volatile boolean			audioProcessingStarted   = false;
	private volatile boolean			analyzingCollection 	 = false;
	private volatile boolean			analyzeCancelled 	 	 = false;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		createUI();

		activity = this;
		context  = this.getApplicationContext();
				
		// check the client id and tag have been set
		if ( (gnsdkClientId == null) || (gnsdkClientTag == null) ){
			showError( "Please set Client ID and Client Tag" );
			return;
		}
		
		// get the gnsdk license from the application assets
		String gnsdkLicense = null;
		if ( (gnsdkLicenseFilename == null) || (gnsdkLicenseFilename.length() == 0) ){
			showError( "License filename not set" );
		} else {
			gnsdkLicense = getAssetAsString( gnsdkLicenseFilename );
			if ( gnsdkLicense == null ){
				showError( "License file not found: " + gnsdkLicenseFilename );
				return;
			}
		}
		
		try {
			
			// GnManager must be created first, it initializes GNSDK
			gnManager = new GnManager( context, gnsdkLicense, GnLicenseInputMode.kLicenseInputModeString );
			
			// provide handler to receive system events, such as locale update needed
			gnManager.systemEventHandler( new SystemEvents() );

			// get a user, if no user stored persistently a new user is registered and stored
			// Note: Android persistent storage used, so no GNSDK storage provider needed to store a user
			gnUser = new GnUser( new GnUserStore(context), gnsdkClientId, gnsdkClientTag, appString );

			// enable storage provider allowing GNSDK to use its persistent stores
			GnStorageSqlite.enable();
			
			// enable local MusicID-Stream recognition (GNSDK storage provider must be enabled as pre-requisite)
			GnLookupLocalStream.enable();
			
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
			Thread ingestThread = new Thread( new LocalBundleIngestRunnable(context) );
			ingestThread.start();									
			
			// Set up for continuous listening from the microphone
			// - create microphone, this can live for lifetime of app
			// - create GnMusicIdStream instance, this can live for lifetime of app
			// - configure
			// Starting and stopping continuous listening should be started and stopped
			// based on Activity life-cycle, see onPause and onResume for details
			// To show audio visualization we wrap GnMic in a visualization adapter
			gnMicrophone = new AudioVisualizeAdapter( new GnMic() );
			gnMusicIdStream = new GnMusicIdStream( gnUser, GnMusicIdStreamPreset.kPresetMicrophone, new MusicIDStreamEvents() );
			gnMusicIdStream.options().lookupData(GnLookupData.kLookupDataContent, true);
			gnMusicIdStream.options().lookupData(GnLookupData.kLookupDataSonicData, true);
			gnMusicIdStream.options().resultSingle( true );
			
			// Retain GnMusicIdStream object so we can cancel an active identification if requested
			streamIdObjects.add( gnMusicIdStream );

		} catch ( GnException e ) {

			Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
			showError(e.errorAPI() + ": " + e.errorDescription() );
			return;
			
		} catch ( Exception e ) {
			if(e.getMessage() != null){
				Log.e(appString, e.getMessage() );
				showError( e.getMessage() );
			}
			else{
				e.printStackTrace();
				setUIState(UIState.DISABLED);
			}
			return;

		}

		setStatus( "" , true);
		((TextView) findViewById(R.id.sdkVersionText)).setText("Gracenote SDK " + GnManager.productVersion());
		setUIState( UIState.READY );
	}

    @Override
	protected void onResume() {
		super.onResume();
		
		if ( gnMusicIdStream != null ) {
			
			// Create a thread to process the data pulled from GnMic
			// Internally pulling data is a blocking call, repeatedly called until
			// audio processing is stopped. This cannot be called on the main thread.
			Thread audioProcessThread = new Thread(new AudioProcessRunnable());
			audioProcessThread.start();
			
		}
    }
    

	@Override
	protected void onPause() {
		super.onPause();
		
		if ( gnMusicIdStream != null ) {
			
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
	}


	private void createUI() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main_grace);

		buttonIDNow = (Button) findViewById(R.id.buttonIDNow);
		buttonIDNow.setEnabled( false );
		buttonIDNow.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				setUIState( UIState.INPROGRESS );
				clearResults();

				try {

					gnMusicIdStream.identifyAlbumAsync();
					lastLookup_startTime = SystemClock.elapsedRealtime();

				} catch (GnException e) {

					Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
					showError( e.errorAPI() + ": " +  e.errorDescription() );

				}
			}
		});

		buttonLibraryID = (Button) findViewById(R.id.buttonLibraryID);
		buttonLibraryID.setEnabled( false );
		buttonLibraryID.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				clearResults();

				Thread runThread = new Thread( new LibraryIDRunnable() );
				runThread.start();
			}
		});

		buttonTextSearch = (Button) findViewById(R.id.buttonText);
		buttonTextSearch.setEnabled( false );
		buttonTextSearch.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				AlertDialog.Builder alert = new AlertDialog.Builder( GracenoteMusicID.this );

				alert.setTitle("Text Search");
				alert.setMessage("Enter 1 to 3 fields");

				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				final View textSearchView = inflater.inflate( R.layout.text_search, null );

				alert.setView(textSearchView);

				alert.setPositiveButton("Search",
					new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialog, int whichButton ) {

							TextView artistTextView = (TextView) textSearchView.findViewById(R.id.TextArtist);
							String artist 			= artistTextView.getText().toString();
							TextView albumTextView 	= (TextView) textSearchView.findViewById(R.id.TextAlbum);
							String album 			= albumTextView.getText().toString();
							TextView trackTextView 	= (TextView) textSearchView.findViewById(R.id.TextTrack);
							String track 			= trackTextView.getText().toString();

							setUIState( UIState.INPROGRESS );
							clearResults();

							Thread textSearchThread = new Thread( new TextSearchRunnable( artist, album, track ));
							textSearchThread.start();
						}
					});

				alert.setNegativeButton("Cancel", null);
				alert.show();
			}
		});

		buttonHistory = (Button) findViewById(R.id.buttonHistory);
		buttonHistory.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(GracenoteMusicID.this, HistoryDetails.class));
			}
		});

		buttonCancel = (Button) findViewById(R.id.buttoncancel);
		buttonCancel.setEnabled( false );
		buttonCancel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setStatus( "Cancelling...", true );
				setUIState( UIState.DISABLED );

				Iterator<GnMusicIdStream> streamIdIter = streamIdObjects.iterator();
				while( streamIdIter.hasNext() ){
					try{
						streamIdIter.next().identifyCancel();
					}catch(GnException e){
						//ignore
					}
				}

				Iterator<GnMusicIdFile> fileIdIter = fileIdObjects.iterator();
				while( fileIdIter.hasNext() ){
					fileIdIter.next().cancel();
				}

				Iterator<GnMusicId> idIter = idObjects.iterator();
				while( idIter.hasNext() ){
					idIter.next().setCancel(true);
				}

				// if analyzing collection set a flag to cancel it
				if ( analyzingCollection == true )
					analyzeCancelled = true;
			}
		});

		settingsMenu = new SettingsMenu();
		buttonSettings = (Button) findViewById(R.id.buttonSettings);
		buttonSettings.setEnabled( true );
		buttonSettings.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				settingsMenu.dialog.show();
			}
		});

		metadataListing = (ViewGroup) findViewById(R.id.metadataListing);
		metadataRow_OnClickListeners = new ArrayList<mOnClickListener>();
		statusText = (TextView) findViewById(R.id.statusText);

		buttonVisShowHide = (Button) findViewById(R.id.buttonVisShowHide);
		buttonVisShowHide.setEnabled( true );
		buttonVisShowHide.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( visShowing == false ) {
					buttonVisShowHide.setText("Close");
				} else {
					buttonVisShowHide.setText("Show Visualization");
				}
				visShowing = !visShowing;
				audioVisualizeDisplay.setDisplay(visShowing, false);
			}
		});

		linearLayoutVisContainer = (LinearLayout)findViewById(R.id.linearLayoutVisContainer);
		audioVisualizeDisplay = new AudioVisualizeDisplay(this,linearLayoutVisContainer,0);

		linearLayoutHomeContainer = (LinearLayout)findViewById(R.id.home_container);
	}
	
	/**
	 * Audio visualization adapter.
	 * Sits between GnMic and GnMusicIdStream to receive audio data as it
	 * is pulled from the microphone allowing an audio visualization to be 
	 * implemented.
	 */
	class AudioVisualizeAdapter implements IGnAudioSource {
		
		private IGnAudioSource 	audioSource;
		private int				numBitsPerSample;
		private int				numChannels;
		
		public AudioVisualizeAdapter( IGnAudioSource audioSource ){
			this.audioSource = audioSource;
		}
		
		@Override
		public long sourceInit() {
			if ( audioSource == null ){
				return 1;
			}
			long retVal = audioSource.sourceInit();
			
			// get format information for use later
			if ( retVal == 0 ) {
				numBitsPerSample = (int)audioSource.sampleSizeInBits();
				numChannels = (int)audioSource.numberOfChannels();
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
			if ( audioSource == null ){
				return 0;
			}
			return audioSource.samplesPerSecond();
		}		

		@Override
		public long getData(ByteBuffer buffer, long bufferSize) {
			if ( audioSource == null ){
				return 0;
			}
			
			long numBytes = audioSource.getData(buffer, bufferSize);
			
			if ( numBytes != 0 ) {
				// perform visualization effect here
				// Note: Since API level 9 Android provides android.media.audiofx.Visualizer which can be used to obtain the
				// raw waveform or FFT, and perform measurements such as peak RMS. You may wish to consider Visualizer class
				// instead of manually extracting the audio as shown here.
				// This sample does not use Visualizer so it can demonstrate how you can access the raw audio for purposes
				// not limited to visualization.
				audioVisualizeDisplay.setAmplitudePercent(rmsPercentOfMax(buffer,bufferSize,numBitsPerSample,numChannels), true);
			}
			
			return numBytes;
		}

		@Override
		public void sourceClose() {
			if ( audioSource != null ){
				audioSource.sourceClose();
			}
		}
		
		// calculate the rms as a percent of maximum 
		private int rmsPercentOfMax( ByteBuffer buffer, long bufferSize, int numBitsPerSample, int numChannels) {
			double rms = 0.0;
			if ( numBitsPerSample == 8 ) {
				rms = rms8( buffer, bufferSize, numChannels );
				return (int)((rms*100)/(double)((double)(Byte.MAX_VALUE/2)));
			} else {
				rms = rms16( buffer, bufferSize, numChannels );
				return (int)((rms*100)/(double)((double)(Short.MAX_VALUE/2)));
			}
		}
		
		// calculate the rms of a buffer containing 8 bit audio samples
		private double rms8 ( ByteBuffer buffer, long bufferSize, int numChannels ) {
			
		    long sum = 0;
		    long numSamplesPerChannel = bufferSize/numChannels;
		    
		    for(int i = 0; i < numSamplesPerChannel; i+=numChannels)
		    {
		    	byte sample = buffer.get();
		        sum += (sample * sample);
		    }
		 
		    return Math.sqrt( (double)(sum / numSamplesPerChannel) );
		}
		
		// calculate the rms of a buffer containing 16 bit audio samples
		private double rms16 ( ByteBuffer buffer, long bufferSize, int numChannels ) {
			
		    long sum = 0;
		    long numSamplesPerChannel = (bufferSize/2)/numChannels;	// 2 bytes per sample
		    
		    buffer.rewind();
		    for(int i = 0; i < numSamplesPerChannel; i++)
		    {
		    	short sample = Short.reverseBytes(buffer.getShort()); // reverse because raw data is little endian but Java short is big endian
		    	
		    	sum += (sample * sample);
		    	if ( numChannels == 2 ){
		    		buffer.getShort();
		    	}
		    }
		    
		    return Math.sqrt( (double)(sum / numSamplesPerChannel) );
		}
	}
			
	/**
	 * GnMusicIdStream object processes audio read directly from GnMic object
	 */
	class AudioProcessRunnable implements Runnable {

		@Override
		public void run() {
			try {
				
				// start audio processing with GnMic, GnMusicIdStream pulls data from GnMic internally
				gnMusicIdStream.audioProcessStart( gnMicrophone );
				
			} catch (GnException e) {
				
				Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
				showError( e.errorAPI() + ": " +  e.errorDescription() );
				
			}
		}
	}

	/** 
	 * Performs Library ID on user's entire music collection 
	 */
	class LibraryIDRunnable implements Runnable {

		@Override
		public void run() {

			boolean hasFiles = false;
			GnMusicIdFile gnMusicIdFile = null;
			Cursor cursor = null;
			
			try {
				
				analyzingCollection = true;
				
				setUIState( UIState.INPROGRESS );
				
				activity.runOnUiThread(new UpdateStatusRunnable("Analyzing user's collection", true));

				// Create GnMusicIdFile object and configure it
				// The events delegate is required for adding MusicID-File fingerprints generated from
				// decoded audio and receiving results, and no match and completed notifications
				gnMusicIdFile = new GnMusicIdFile(gnUser, new MusicIDFileEvents());
				gnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
				gnMusicIdFile.options().batchSize(10); // todo: comment
				
				// Retain the object so we can cancel if requested, we remove it when it's done
				fileIdObjects.add(gnMusicIdFile); 
				
				// You must tell GnMusicIdFile about all audio files in the user's collection. 
				// GnMusicIdFile will use all information available to recognize an audio file, including metadata
				// from tags, hints in the file name and path, and MusicID-File fingerprints generated from
				// the file's decoded audio. Don't worry if one or more of these aren't available, a positive
				// identification can still be made, so include all information you can.
				
				// Use MediaStore to determine all of the music files on the device.
				String[] projection = { MediaStore.Audio.Media.ALBUM,
						MediaStore.Audio.Media.ARTIST,
						MediaStore.Audio.Media.TITLE,
						MediaStore.Audio.Media.DATA, // file path
						MediaStore.Audio.Media.TRACK // track number
				};

				// Selection is everything that is music, we want to add it all!
				String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
				cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null);

				if (cursor != null) {
					if (cursor.getCount() > 0) {
	
						hasFiles = true;
	
						while (cursor.moveToNext()) {
							
							if ( analyzeCancelled ) 
								break;
	
							// MediaStore can provide valuable information about the audio files, such as
							// the tag data. Beware though, Android has a bug where MP3 tag reading does not work.
							// We recommend using a third party tag reader, otherwise rely on the file name and path
							// and MusicID-File fingerprint if audio can be decoded.
							
							String absoluteFilename = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

							// Add the audio file. Each audio file must have a unique identifier, filename works well on
							// Android but any unique identifier will work.
							GnMusicIdFileInfo fileInfo = gnMusicIdFile.fileInfos().add(absoluteFilename);
							
							// Set the filename as this can be used for hints that help recognition
							fileInfo.fileName(absoluteFilename);
	
							// Set as many metadata fields as you can
							String albumTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
							if (albumTitle != null && !albumTitle.isEmpty()) {
								fileInfo.albumTitle(albumTitle);
							}
	
							String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
							if (artist != null && !artist.isEmpty()) {
								fileInfo.albumArtist(artist);
							}
	
							String trackTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
							if (trackTitle != null && !trackTitle.isEmpty()) {
								fileInfo.trackTitle(trackTitle);
							}
	
							String trackNumber = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
							if (trackNumber != null && !trackNumber.isEmpty()) {
								try {
									fileInfo.trackNumber(Integer.parseInt(trackNumber));
								} catch (NumberFormatException e) {
								}
							}
	
						}
					}
	
					cursor.close();
				}
				
				analyzingCollection = false;
				if ( analyzeCancelled == true )
				{
					activity.runOnUiThread(new UpdateStatusRunnable("Cancelled", true));
					setUIState(UIState.READY);
				}
				else
				{
					if (hasFiles) {
						
						// With all audio files added to the GnMusicIdFile instance do the Library ID
						// The delegate is used to add fingerprints, we add these "just in time" because
						// audio decoding is slow, so we want to maximize concurrent operations, such
						// as querying and sorting results while doing decoding
						gnMusicIdFile.doLibraryId(GnMusicIdFileResponseType.kResponseAlbums);
	
					} else {
	
						activity.runOnUiThread(new UpdateStatusRunnable("No files to recognize", true));
						setUIState(UIState.READY);
	
					}
				}

			} catch (GnException e) {

				Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
				showError(e.errorAPI() + ": " + e.errorDescription());

			} finally {

				if (gnMusicIdFile != null) {
					fileIdObjects.remove(gnMusicIdFile);
				}
				
				analyzingCollection = false;
				analyzeCancelled = false;
			}
		}

	}
	
	
	/**
	 * Loads a locale
	 */
	class LocaleLoadRunnable implements Runnable {
		GnLocaleGroup	group;
		GnLanguage		language; 
		GnRegion		region;
		GnDescriptor	descriptor;
		GnUser			user;
		
		
		LocaleLoadRunnable( 
				GnLocaleGroup group, 
				GnLanguage		language, 
				GnRegion		region,
				GnDescriptor	descriptor,
				GnUser			user) {
			this.group 		= group;
			this.language 	= language;
			this.region 	= region;
			this.descriptor = descriptor;
			this.user 		= user;
		}
			
		@Override
		public void run() {
			try {
				
				GnLocale locale = new GnLocale(group,language,region,descriptor,gnUser);
				locale.setGroupDefault();
				
			} catch (GnException e) {
				Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
			}
		}
	}

    /**
     * Updates a locale
     */
    class LocaleUpdateRunnable implements Runnable {
        GnLocale		locale;
        GnUser			user;


        LocaleUpdateRunnable(
                GnLocale		locale,
                GnUser			user) {
            this.locale 	= locale;
            this.user 		= user;
        }

        @Override
        public void run() {
            try {
                locale.update(user);
            } catch (GnException e) {
                Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
            }
        }
    }

    /**
     * Updates a list
     */
    class ListUpdateRunnable implements Runnable {
        GnList			list;
        GnUser			user;


        ListUpdateRunnable(
                GnList			list,
                GnUser			user) {
            this.list 		= list;
            this.user 		= user;
        }

        @Override
        public void run() {
            try {
                list.update(user);
            } catch (GnException e) {
                Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
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
				
				InputStream 	bundleInputStream 	= null;
				int				ingestBufferSize	= 1024;
				byte[] 			ingestBuffer 		= new byte[ingestBufferSize];
				int				bytesRead			= 0;
				
				GnLookupLocalStreamIngest ingester = new GnLookupLocalStreamIngest(new BundleIngestEvents());
				
				try {
					
					bundleInputStream = context.getAssets().open("1557.b");
					
					do {
						
						bytesRead = bundleInputStream.read(ingestBuffer, 0, ingestBufferSize);
						if ( bytesRead == -1 )
							bytesRead = 0;
					
						ingester.write( ingestBuffer, bytesRead );
						
					} while( bytesRead != 0 );
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				ingester.flush();
				
			} catch (GnException e) {
				Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
			}
			
		}
	}


	/**
	 * Receives system events from GNSDK 
	 */
	class SystemEvents implements IGnSystemEvents {
		@Override
		public void localeUpdateNeeded( GnLocale locale ){
			// Locale update is detected
			Thread localeUpdateThread = new Thread(new LocaleUpdateRunnable(locale,gnUser));
			localeUpdateThread.start();
		}

		@Override
		public void listUpdateNeeded( GnList list ) {
			// List update is detected
			Thread listUpdateThread = new Thread(new ListUpdateRunnable(list,gnUser));
			listUpdateThread.start();
		}

		@Override
		public void systemMemoryWarning(long currentMemorySize, long warningMemorySize) {
			// only invoked if a memory warning limit is configured			
		}
	}
	
	
	/** 
	 * Performs Text Search 
	 */
	class TextSearchRunnable implements Runnable {

		String artist;
		String album;
		String track;
		
		TextSearchRunnable( String locArtist, String locAlbum, String locTrack ){
			artist = locArtist;
			album = locAlbum;
			track = locTrack;
		}
		
		@Override
		public void run() {
			
			GnMusicId musicId = null;
			
			try {
				
				// Create GnMusicId object and configure
				musicId = new GnMusicId( gnUser, new StatusEvents() );
				musicId.options().lookupData(GnLookupData.kLookupDataContent, true);
				
				// Retain GnMusicId object so we can cancel text search if requested
				idObjects.add( musicId ); 
				
				GnResponseAlbums result = musicId.findAlbums( album, track, artist, null, null );
				
				activity.runOnUiThread( new UpdateResultsRunnable( result ) );
				
			} catch ( GnException e ) {
				
				if ( GnError.isErrorEqual(e.errorCode(),GnError.GNSDKERR_Aborted))
					setStatus( "Cancelled", true );
				else
					showError( e.errorAPI() + ": " +  e.errorDescription() );
				Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
				
				
			} finally {

				if ( musicId != null ){
					idObjects.remove( musicId );
				}
				
			}
			
			setUIState( UIState.READY );
		}
		
	}


	/**
	 * GNSDK status event delegate 
	 */
	private class StatusEvents implements IGnStatusEvents {

		@Override
		public void statusEvent( GnStatus status, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable cancellable ) {
			setStatus( String.format("%d%%",percentComplete), true );
		}

	};
	
	/** 
	 * GNSDK MusicID-Stream event delegate 
	 */
	public class MusicIDStreamEvents implements IGnMusicIdStreamEvents {

		HashMap<String, String> gnStatus_to_displayStatus;
		
		public MusicIDStreamEvents(){
			gnStatus_to_displayStatus = new HashMap<String,String>();
			gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingStarted.toString(), "Identification started");
			gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingFpGenerated.toString(), "Fingerprinting complete");
			gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingLocalQueryStarted.toString(), "Lookup started");
			gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingOnlineQueryStarted.toString(), "Lookup started");			
//			gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingEnded.toString(), "Identification complete");
		}
		
		@Override
		public void statusEvent( GnStatus status, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable cancellable ) {

		}
		
		@Override
		public void musicIdStreamProcessingStatusEvent( GnMusicIdStreamProcessingStatus status, IGnCancellable canceller ) {
			
			if(GnMusicIdStreamProcessingStatus.kStatusProcessingAudioStarted.compareTo(status) == 0)
			{
				audioProcessingStarted = true;				
				activity.runOnUiThread(new Runnable (){
					public void run(){
						buttonIDNow.setEnabled(true);
					}
				});
				
			}
			
		}
		
		@Override
		public void musicIdStreamIdentifyingStatusEvent( GnMusicIdStreamIdentifyingStatus status, IGnCancellable canceller ) {
			if(gnStatus_to_displayStatus.containsKey(status.toString())){
				setStatus( String.format("%s", gnStatus_to_displayStatus.get(status.toString())), true );
			}
			
			if(status.compareTo( GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingLocalQueryStarted ) == 0 ){
				lastLookup_local = true;
			}
			else if(status.compareTo( GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingOnlineQueryStarted ) == 0){
				lastLookup_local = false;
			}
			
			if ( status == GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingEnded )
			{
				setUIState( UIState.READY );
			}
		}
				
			
		@Override
		public void musicIdStreamAlbumResult( GnResponseAlbums result, IGnCancellable canceller ) {
			lastLookup_matchTime = SystemClock.elapsedRealtime() - lastLookup_startTime;
			activity.runOnUiThread(new UpdateResultsRunnable( result ));
		}

		@Override
		public void musicIdStreamIdentifyCompletedWithError(GnError error) {
			if ( error.isCancelled() )
				setStatus( "Cancelled", true );
			else
				setStatus( error.errorDescription(), true );
			setUIState( UIState.READY );
		}
	}
	
	
	/** 
	 * GNSDK MusicID-File event delegate 
	 */
	private class MusicIDFileEvents implements IGnMusicIdFileEvents {
		
		HashMap<String, String> gnStatus_to_displayStatus;	
		
		public MusicIDFileEvents(){
			gnStatus_to_displayStatus = new HashMap<String,String>();
			gnStatus_to_displayStatus.put("kMusicIdFileCallbackStatusProcessingBegin", "Begin processing file");
			gnStatus_to_displayStatus.put("kMusicIdFileCallbackStatusFileInfoQuery", "Querying file info");
			gnStatus_to_displayStatus.put("kMusicIdFileCallbackStatusProcessingComplete", "Identification complete");												
		}
		
		
		@Override
		public void gatherFingerprint(GnMusicIdFileInfo fileInfo, long currentFile, long totalFiles, IGnCancellable cancelable){

			// If the audio file can be decoded then provide a MusicID-File fingerprint
			//
			// GnAudioFile uses Gracenote's audio decoder, if your application uses a proprietary audio
			// format you can decode the audio and provide it manually using GnMusicIdFileInfo.fingerprintBegin,
			// fingerprintWrite and fingerprintEnd; or create your own audio file decoder class that implements
			// IGnAudioSource.
			try {
				
				if ( GnAudioFile.isFileFormatSupported(fileInfo.fileName())) {
					fileInfo.fingerprintFromSource( new GnAudioFile( new File(fileInfo.fileName())) );
				}
				
			} catch (GnException e) {
				if ( GnError.isErrorEqual(e.errorCode(),GnError.GNSDKERR_Aborted) == false )
				{
					Log.e(appString, "error in fingerprinting file: " + e.errorAPI() + ", " + e.errorModule() + ", " + e.errorDescription());
				}
			}
		}
		
		@Override
		public void gatherMetadata(GnMusicIdFileInfo fileInfo, long currentFile, long totalFiles, IGnCancellable cancelable) {
			// Skipping this here as metadata has been previously loaded for all files
			// You could provide metadata "just in time" instead of before invoking Track/Album/Library ID, which 
			// means you would add it in this delegate method for the file represented by fileInfo
		}
		
		
		@Override
		public void statusEvent( GnStatus status, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable cancellable ) {
			setStatus( String.format("%d%%",percentComplete), true );
		}
		
		@Override
		public void musicIdFileStatusEvent(GnMusicIdFileInfo fileinfo, GnMusicIdFileCallbackStatus midf_status, long currentFile, long totalFiles, IGnCancellable canceller){

			try {
				String status = midf_status.toString();
				if (gnStatus_to_displayStatus.containsKey(status)) {
					String filename = fileinfo.identifier();
					if (filename != null) {
						status = gnStatus_to_displayStatus.get(status) + ": " + filename;
						setStatus(status, true);
					}
		
				}

			} catch (Exception e) {
				Log.e(appString, "error in retrieving musidIdFileStatus");
			}
	
		}

		@Override
		public void musicIdFileAlbumResult( GnResponseAlbums albumResult, long currentAlbum, long totalAlbums, IGnCancellable cancellable  ) {
			// match found!
			activity.runOnUiThread( new UpdateResultsRunnable( albumResult ) );
		}

		@Override
		public void musicIdFileResultNotFound( GnMusicIdFileInfo fileInfo, long currentFile, long totalFiles, IGnCancellable cancellable ) {
			// no match found for the audio file represented by fileInfo
			try {
				Log.i(appString,"GnMusicIdFile no match found for " + fileInfo.identifier());
			} catch (GnException e) {
			}
		}

		@Override
		public void musicIdFileComplete( GnError musicidfileCompleteError ) {
			
			if ( musicidfileCompleteError.errorCode() == 0 ){
				setStatus( "Success", true );
				
			} else {
				
				if ( musicidfileCompleteError.isCancelled() )
					setStatus( "Cancelled", true );
				else
					setStatus(musicidfileCompleteError.errorDescription(), true );
				Log.e(appString, musicidfileCompleteError.errorAPI() + ": " + musicidfileCompleteError.errorDescription() );
			}
			setUIState( UIState.READY );
		}


		@Override
		public void musicIdFileMatchResult( GnResponseDataMatches matchResult, long currentFile, long totalFiles, IGnCancellable cancellable) {
			// handle match result
			// match result only received if requested match results when initiating query
		}		
	}
	
	
	/**
	 * GNSDK bundle ingest status event delegate
	 */
	private class BundleIngestEvents implements IGnLookupLocalStreamIngestEvents{
		
		@Override
		public void statusEvent(GnLookupLocalStreamIngestStatus status, String bundleId, IGnCancellable canceller) {
				setStatus("Bundle ingest progress: " + status.toString() , true);
		}
	}


	/**
	 * Helpers to read license file from assets as string
	 */
	private String getAssetAsString( String assetName ){
		
		String 		assetString = null;
		InputStream assetStream;
		
		try {
			
			assetStream = this.getApplicationContext().getAssets().open(assetName);
			if(assetStream != null){
				
				java.util.Scanner s = new java.util.Scanner(assetStream).useDelimiter("\\A");
				
				assetString = s.hasNext() ? s.next() : "";
				assetStream.close();
				
			}else{
				Log.e(appString, "Asset not found:" + assetName);
			}
			
		} catch (IOException e) {
			
			Log.e( appString, "Error getting asset as string: " + e.getMessage() );
			
		}
		
		return assetString;
	}
	

	/**
	 * Helpers to enable/disable the application widgets
	 */
	enum UIState{
		DISABLED,
		READY,
		INPROGRESS
	}
	
	private void setUIState( UIState uiState ) {
		activity.runOnUiThread(new SetUIState(uiState));		
	}
	
	class SetUIState implements Runnable {

		UIState uiState;
		SetUIState( UIState uiState ){
			this.uiState = uiState;
		}
		
		@Override
		public void run() {
			
			boolean enabled = (uiState == UIState.READY);

			buttonIDNow.setEnabled( enabled && audioProcessingStarted);
			buttonTextSearch.setEnabled( enabled );
			buttonHistory.setEnabled( enabled );			
			buttonLibraryID.setEnabled( enabled );
			buttonCancel.setEnabled( (uiState == UIState.INPROGRESS) );			 	
			buttonSettings.setEnabled(enabled);
		}
		
	}
	
	/**
	 * Helper to set the application status message
	 */
	private void setStatus( String statusMessage, boolean clearStatus ){
		activity.runOnUiThread(new UpdateStatusRunnable( statusMessage, clearStatus ));
	}
	
	class UpdateStatusRunnable implements Runnable {

		boolean clearStatus;
		String status;
		
		UpdateStatusRunnable( String status, boolean clearStatus ){
			this.status = status;
			this.clearStatus = clearStatus;
		}
		
		@Override
		public void run() {
			statusText.setVisibility(View.VISIBLE);
			if (clearStatus) {
				statusText.setText(status);
			} else {
				statusText.setText((String) statusText.getText() + "\n" + status);
			}
		}
		
	}
	
	/**
	 * Helpers to load and set cover art image in the application display 
	 */
	void loadAndDisplayCoverArt(String coverArtUrl, ImageView imageView ){
		Thread runThread = new Thread( new CoverArtLoaderRunnable( coverArtUrl, imageView ) );
		runThread.start();
	}
	
	class CoverArtLoaderRunnable implements Runnable {
		
		String 	coverArtUrl;
		ImageView 	imageView;
		
		CoverArtLoaderRunnable( String coverArtUrl, ImageView imageView){
			this.coverArtUrl = coverArtUrl;
			this.imageView = imageView;
		}

		@Override
		public void run() {

			Drawable coverArt = null;

			if (coverArtUrl != null && !coverArtUrl.isEmpty()) {
				try {
					GnAssetFetch assetData = new GnAssetFetch(gnUser,coverArtUrl);
					byte[] data = assetData.data();
					coverArt =  new BitmapDrawable(BitmapFactory.decodeByteArray(data, 0, data.length));
				} catch (GnException e) {
					e.printStackTrace();
				}

			}

			if (coverArt != null) {
				setCoverArt(coverArt, imageView);
			} else {
				setCoverArt(getResources().getDrawable(R.drawable.no_image),imageView);
			}

		}
		
	}

	private void setCoverArt( Drawable coverArt, ImageView coverArtImage ){
		activity.runOnUiThread(new SetCoverArtRunnable(coverArt, coverArtImage));
	}
	
	class SetCoverArtRunnable implements Runnable {
		
		Drawable coverArt;
		ImageView coverArtImage;
		
		SetCoverArtRunnable( Drawable locCoverArt, ImageView locCoverArtImage) {
			coverArt = locCoverArt;
			coverArtImage = locCoverArtImage;
		}
		
		@Override
		public void run() {
			coverArtImage.setImageDrawable(coverArt);
		}
	}
	
	/** 
	 * Adds album results to UI via Runnable interface 
	 */
	class UpdateResultsRunnable implements Runnable {

		GnResponseAlbums albumsResult;

		UpdateResultsRunnable(GnResponseAlbums albumsResult) {
			this.albumsResult = albumsResult;
		}

		@Override
		public void run() {
			try {
				if (albumsResult.resultCount() == 0) {
					
					setStatus("No match", true);

				} else {
					
					setStatus("Match found", true);
					GnAlbumIterator iter = albumsResult.albums().getIterator();
					while (iter.hasNext()) {
						updateMetaDataFields(iter.next(), true, false);
					
					}
					trackChanges(albumsResult);

				}
			} catch (GnException e) {
				setStatus(e.errorDescription(), true);
				return;
			}

		}
	}
	
	/**
	 * Adds the provided album as a new row on the application display
	 * @throws GnException 
	 */
	private void updateMetaDataFields(final GnAlbum album, boolean displayNoCoverArtAvailable, boolean fromTxtOrLyricSearch) throws GnException {
		
		// Load metadata layout from resource .xml
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View metadataView = inflater.inflate(R.layout.file_meta_data, null);

		metadataListing.addView(metadataView);

		final ImageView coverArtImage = (ImageView) metadataView.findViewById(R.id.coverArtImage);

		TextView albumText = (TextView) metadataView.findViewById( R.id.albumName );
		TextView trackText = (TextView) metadataView.findViewById( R.id.trackTitle );
		TextView artistText = (TextView) metadataView.findViewById( R.id.artistName );

		// enable pressing row to get track listing 
		 metadataView.setClickable(true); 
		 mOnClickListener onClickListener = new mOnClickListener(album, coverArtImage);
		 if(metadataRow_OnClickListeners.add(onClickListener)){
			 metadataView.setOnClickListener(onClickListener);	
		 }
		 
		if (album == null) {
			
			coverArtImage.setVisibility(View.GONE);
			albumText.setVisibility(View.GONE);
			trackText.setVisibility(View.GONE);
			// Use the artist text field to display the error message
			//artistText.setText("Music Not Identified");
		} else {
			
			// populate the display tow with metadata and cover art

			albumText.setText( album.title().display() );
			String artist = album.trackMatched().artist().name().display();
			
			//use album artist if track artist not available
			if(artist.isEmpty()){
				artist = album.artist().name().display();
			}
			artistText.setText( artist );
			
			if ( album.trackMatched() != null ) {
				trackText.setText( album.trackMatched().title().display() );
			} else {
				trackText.setText("");
			}

			// limit the number of images added to display so we don't run out of memory,
			// a real app would page the results
			if ( metadataListing.getChildCount() <= metadataMaxNumImages ){
				String coverArtUrl = album.coverArt().asset(GnImageSize.kImageSizeSmall).url();
				loadAndDisplayCoverArt( coverArtUrl, coverArtImage );
			} else {
				coverArtImage.setVisibility(View.GONE);
			}
			
		}
	}
	
	/**
	 * Helper to clear the results from the application display
	 */
	private void clearResults() {
		statusText.setText("");
		metadataListing.removeAllViews();
	}
	
	/**
	 * Helper to show and error
	 */
	private void showError( String errorMessage ) {
		setStatus( errorMessage, true );
		setUIState( UIState.DISABLED );
	}
	
		
	private void enableLocalSearchOnly( boolean enabled ){
	
		if(gnMusicIdStream != null){
			try {
				if(enabled) {
					gnMusicIdStream.options().lookupMode(GnLookupMode.kLookupModeLocal); //restrict lookups to local db
				} else {
					gnMusicIdStream.options().lookupMode(GnLookupMode.kLookupModeOnline);
				}
			} catch (GnException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void enableDebugLog(boolean enabled){
		
		try {
			if (enabled) {

				if ( null == gnLog ){
					String gracenoteLogFilename = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + gnsdkLogFilename;
					
					gnLog = new GnLog(gracenoteLogFilename);
					Log.i("debug", gracenoteLogFilename);
					gnLog.columns(new GnLogColumns().all());
					gnLog.filters(new GnLogFilters().all());
				}
				
				gnLog.enable(GnLogPackageType.kLogPackageAll);

			} else if ( gnLog != null ){
				
				gnLog.enable(GnLogPackageType.kLogPackageAll);	
			}
			
		} catch (GnException e) {
			
			Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
			
		}
	}
		
	class SettingsMenu {
		
		private CheckBox debugCheckBox;
		private CheckBox localSearchCheckBox;
		AlertDialog dialog; 
		
		SettingsMenu(){
			
			AlertDialog.Builder builder = new AlertDialog.Builder( GracenoteMusicID.this );
			builder.setTitle("Settings");
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View settingsView = inflater.inflate( R.layout.settings, null );
			builder.setView(settingsView);
			builder.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick( DialogInterface dialog, int whichButton ) {
						applySettingsUpdate();
					}
				});

			builder.setNegativeButton("Cancel", null);
			dialog = builder.create();
			
			debugCheckBox = (CheckBox) settingsView.findViewById(R.id.debugLogCheckBox);
			localSearchCheckBox = (CheckBox) settingsView.findViewById(R.id.localSearchCheckBox);
									
		}
		
		private void applySettingsUpdate(){
			enableDebugLog(debugCheckBox.isChecked());
			enableLocalSearchOnly(localSearchCheckBox.isChecked());
		}
	}
	
	
	class AudioVisualizeDisplay {
		
		private Activity					activity;
		private ViewGroup					displayContainer;
		private View						view;
		ImageView 							bottomDisplayImageView;
		ImageView 							topDisplayImageView;
		private int							displayIndex;
		private float						zeroScaleFactor = 0.50f;
		private float						maxScaleFactor = 1.50f;
		private int							currentPercent = 50;
		private boolean						isDisplayed = false;
		private final int					zeroDelay = 150; // in milliseconds
		
		Timer 								zeroTimer;
		
		private FrameLayout.LayoutParams	bottomDisplayLayoutParams;
		private int							bottomDisplayImageHeight;
		private int							bottomDisplayImageWidth;
		private FrameLayout.LayoutParams 	topDisplayLayoutParams;
		private int							topDisplayImageHeight;
		private int							topDisplayImageWidth;
		
		AudioVisualizeDisplay( Activity activity, ViewGroup displayContainer, int displayIndex ) {
			this.activity = activity;
			this.displayContainer = displayContainer;
			this.displayIndex = displayIndex;
			
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.visual_audio,null);
			
			// bottom layer
			bottomDisplayImageView = (ImageView)view.findViewById(R.id.imageViewAudioVisBottomLayer);
			bottomDisplayLayoutParams = (FrameLayout.LayoutParams)bottomDisplayImageView.getLayoutParams();
			BitmapDrawable bd=(BitmapDrawable) activity.getResources().getDrawable(R.drawable.colored_ring);
			bottomDisplayImageHeight=(int)((float)bd.getBitmap().getHeight() * zeroScaleFactor);
			bottomDisplayImageWidth=(int)((float)bd.getBitmap().getWidth() * zeroScaleFactor);
			
			// top layer
			topDisplayImageView = (ImageView)view.findViewById(R.id.imageViewAudioVisTopLayer);
			topDisplayLayoutParams = (FrameLayout.LayoutParams)topDisplayImageView.getLayoutParams();
			bd=(BitmapDrawable) activity.getResources().getDrawable(R.drawable.gracenote_logo);
			topDisplayImageHeight=(int)((float)bd.getBitmap().getHeight() * zeroScaleFactor);
			topDisplayImageWidth=(int)((float)bd.getBitmap().getWidth() * zeroScaleFactor);
			
			// set the size of the visualization image view container large enough to hold vis image
			TableRow tableRow = (TableRow)view.findViewById(R.id.tableRowVisImageContainer);
			tableRow.setMinimumHeight((int)(((float)bottomDisplayImageHeight * maxScaleFactor)) + 20); // room for scaling plus some padding
			tableRow.setGravity(Gravity.CENTER);  
		}
		
		// display or hide the visualization view in the display container provided during construction
		void setDisplay( boolean display, boolean doOnMainThread ){

			// why do we set amplitude percentage here?
			// when we display the visualizer image we want it scaled, but setting the layout parameters
			// in the constructor doesn't nothing, so we call it hear to prevent it showing up full size and
			// then scaling a fraction of a second later when the first amplitude percent change
			// comes in
			if ( display ){
				SetDisplayAmplitudeRunnable setDisplayAmplitudeRunnable = new SetDisplayAmplitudeRunnable(currentPercent);
				if ( doOnMainThread ){
					activity.runOnUiThread(setDisplayAmplitudeRunnable);
				}else{
					setDisplayAmplitudeRunnable.run();
				}
			}
			
			SetDisplayRunnable setDisplayRunnable = new SetDisplayRunnable(display); 
			if ( doOnMainThread ){
				activity.runOnUiThread(setDisplayRunnable);
			}else{
				setDisplayRunnable.run();
			}
			
			linearLayoutHomeContainer.postInvalidate();
		}
		
		void setAmplitudePercent( int amplitudePercent, boolean doOnMainThread ){
			if ( isDisplayed && (currentPercent != amplitudePercent)){
				SetDisplayAmplitudeRunnable setDisplayAmplitudeRunnable = new SetDisplayAmplitudeRunnable(amplitudePercent); 
				if ( doOnMainThread ){
					activity.runOnUiThread(setDisplayAmplitudeRunnable);
				}else{
					setDisplayAmplitudeRunnable.run();
				}
				currentPercent = amplitudePercent;
				
				// zeroing timer - cancel if we got a new amplitude before
				try {
					if ( zeroTimer != null ) {
						zeroTimer.cancel();
						zeroTimer = null;
					}
					zeroTimer = new Timer();
					zeroTimer.schedule( new ZeroTimerTask(),zeroDelay);
				} catch (IllegalStateException e){ 
				}
			}
		}
		
		int displayHeight(){
			return bottomDisplayImageHeight;
		}
		
		int displayWidth(){
			return bottomDisplayImageWidth;
		}
		
		class SetDisplayRunnable implements Runnable {
			boolean display;
			
			SetDisplayRunnable(boolean display){
				this.display = display;
			}
			
			@Override
			public void run() {
				if ( isDisplayed && (display == false) ) {
					displayContainer.removeViewAt( displayIndex );
					isDisplayed = false;
				} else if ( isDisplayed == false ) {
					displayContainer.addView(view, displayIndex);
					isDisplayed = true;
				}
				
			}
		}
		
		class SetDisplayAmplitudeRunnable implements Runnable {
			int percent;
			
			SetDisplayAmplitudeRunnable(int percent){
				this.percent = percent;
			}
			
			@Override
			public void run() {
				float scaleFactor = zeroScaleFactor + ((float)percent/100); // zero position plus audio wave amplitude percent
				if ( scaleFactor > maxScaleFactor )
					scaleFactor = maxScaleFactor;
				bottomDisplayLayoutParams.height = (int)((float)bottomDisplayImageHeight * scaleFactor); 
				bottomDisplayLayoutParams.width = (int)((float)bottomDisplayImageWidth * scaleFactor);
				bottomDisplayImageView.setLayoutParams( bottomDisplayLayoutParams );
				
				topDisplayLayoutParams.height = (int)((float)topDisplayImageHeight * zeroScaleFactor); 
				topDisplayLayoutParams.width = (int)((float)topDisplayImageWidth * zeroScaleFactor);
				topDisplayImageView.setLayoutParams( topDisplayLayoutParams );
			}
		}	
		
		class ZeroTimerTask extends TimerTask {

			@Override
			public void run() {
				zeroTimer = null;
				setAmplitudePercent(0,true);
			}
			
		}
		
	}
		
		
	/**
	 * Helper - implements OnClickListener for a metadata row,
	 * launches detail view
	 *
	 */
	class mOnClickListener implements OnClickListener{
		
		DetailView detailView;
		
		mOnClickListener(GnAlbum album, ImageView imageView){
			detailView = new DetailView(album, GracenoteMusicID.this);	
			
		}
		
		@Override
		public void onClick (View v){
			detailView.show(v);
		}
	}
	
		
	/**
	 * History Tracking:
	 * initiate the process to insert values into database.
	 * 
	 * @param row
	 *            - contains all the information to be inserted into DB,
	 *            except location.
	 */
	private synchronized void trackChanges(GnResponseAlbums albums) {		
		Thread thread = new Thread (new InsertChangesRunnable(albums));
		thread.start();
				
	}
	
	class InsertChangesRunnable implements Runnable {
		GnResponseAlbums row;

		InsertChangesRunnable(GnResponseAlbums row) {
			this.row = row;
		}

		@Override
		public void run() {
			try {
				DatabaseAdapter db = new DatabaseAdapter(GracenoteMusicID.this,gnUser);
				db.open();
				db.insertChanges(row);
				db.close();
			} catch (GnException e) {
				// ignore
			}
		}
	}
	
	
}
