package fi.zmengames.zlauncher;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.gracenote.gnsdk.GnDescriptor;
import com.gracenote.gnsdk.GnError;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnImageSize;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLicenseInputMode;
import com.gracenote.gnsdk.GnList;
import com.gracenote.gnsdk.GnLocale;
import com.gracenote.gnsdk.GnLocaleGroup;
import com.gracenote.gnsdk.GnLookupData;
import com.gracenote.gnsdk.GnLookupLocalStream;
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import fr.neamar.kiss.R;

public class 
GracenoteMusicIDWidget {

	static final String 				appString				= "GFM IdNow Widget";	
	private Context 					context;
	
	// Gracenote objects
	private GnManager 					gnManager;
	private GnUser 						gnUser;
	private GnMusicIdStream	 			gnMusicIdStream;
	private IGnAudioSource				gnMicrophone;
	private List<GnMusicIdStream>		streamIdObjects			= new ArrayList<GnMusicIdStream>();
	
	private AppWidgetManager 			appWidgetManager 		= null;
	private RemoteViews					remoteViews				= null; 
	private ComponentName				componentName			= null;
	
	private static volatile Integer 	flipInterval;
	private Object 						lock 					= new Object();
	private volatile boolean 			visDisplay_currently_visible 		= false;
	private volatile boolean			visDisplay_hide						= false; 
	private VisDisplayThread 			visThread;
	
	Thread audioProcessThread;
	
		
	
	GracenoteMusicIDWidget(Context in_context) {
		try{
		
		context = in_context;
		appWidgetManager = AppWidgetManager.getInstance(context);
		componentName = new ComponentName(context, IdNowWidgetProvider.class);
		remoteViews = new RemoteViews(context.getPackageName(), R.layout.widgetlayout);
		GnInit();	
		} catch (Exception e){
			e.printStackTrace();
		}

		
	}
	
	
	public void GnInit(){
		
		Log.i(appString, "GnInit()");
		
		// check the client id and tag have been set
		if ( (GracenoteMusicID.gnsdkClientId == null) || (GracenoteMusicID.gnsdkClientTag == null) ){
			setStatus("Please set Client ID and Client Tag" );
			Log.e(appString, "client id/tag is null");
			return;
		}
		
		// get the gnsdk license from the application assets
		String gnsdkLicense = null;
		if ( (GracenoteMusicID.gnsdkLicenseFilename == null) || (GracenoteMusicID.gnsdkLicenseFilename.length() == 0) ){
			 setStatus("License filename not set" );
		} else {
			gnsdkLicense = getAssetAsString( GracenoteMusicID.gnsdkLicenseFilename );
			if ( gnsdkLicense == null ){
				setStatus( "License file not found: " + GracenoteMusicID.gnsdkLicenseFilename );
				return;
			}
		}
		
		try {
			
			// GnManager must be created first, it initializes GNSDK
			gnManager = new GnManager( context, gnsdkLicense, GnLicenseInputMode.kLicenseInputModeString );
			
			// provide handler to receive system events, such as locale update needed
			gnManager.systemEventHandler( new SystemEvents() );
			
			// get a user, if no user stored persistently a new user is registered and stored 
			// Note: Android persistent storage used, so no GNSDK storage provider needed to store user
			gnUser = new GnUser( new GnUserStore(context), GracenoteMusicID.gnsdkClientId, GracenoteMusicID.gnsdkClientTag, appString );

			// enable storage provider allowing GNSDK to use its persistent stores
			GnStorageSqlite.enable();
			
			// enable local MusicID-Stream recognition (GNSDK storage provider must be enabled as pre-requisite)
			GnLookupLocalStream.enable();
			
			// Loads data to support the requested locale, data is downloaded from Gracenote Service if not
			// found in persistent storage. Once downloaded it is stored in persistent storage.
			// Download and write to persistent storage can be lengthy so perform in another thread
			Thread localeThread = new Thread( 
									new LocaleLoadRunnable(GnLocaleGroup.kLocaleGroupMusic,
										GnLanguage.kLanguageEnglish, 
										GnRegion.kRegionGlobal,
										GnDescriptor.kDescriptorDefault,
										gnUser) 
									);
			localeThread.start();	
			
			// Ingest MusicID-Stream local bundle, perform in another thread as it can be a lengthy process
			Thread ingestThread = new Thread( new LocalBundleIngestRunnable(context) );
			ingestThread.start();		
			
			
			// Set up for continuous listening from the microphone
			// - create microphone, this can live for lifetime of app
			// - create GnMusicIdStream instance, this can live for lifetime of
			// app
			// - configure
			// starting and stopping continuous listening should be started and
			// stopped
			// based on Activity life-cycle, see onPause and onResume for
			// details
			// To show audio visualization we wrap GnMic in a visualization
			// adapter
			gnMicrophone = new AudioVisualizeAdapter( new GnMic() );
			gnMusicIdStream = new GnMusicIdStream(gnUser, GnMusicIdStreamPreset.kPresetMicrophone, new MusicIDStreamEvents());
			gnMusicIdStream.options().lookupData(GnLookupData.kLookupDataContent, true);
			gnMusicIdStream.options().resultSingle(true);
			streamIdObjects.add(gnMusicIdStream); // retain event object so we can cancel if requested	
						
		} catch ( GnException e ) {
			
			Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
			setStatus(e.errorAPI() + ": " + e.errorDescription() );
			return;
			
		} catch ( Exception e ) {
			if(e.getMessage() != null){
				Log.e(appString, e.getMessage() );
				setStatus( e.getMessage() );
			}
			else{
				e.printStackTrace();
			}
			return;
			
		}
		
		setStatus("Gracenote SDK " + GnManager.productVersion());
		
	}
	
	
	void cancel(){
		
		Iterator<GnMusicIdStream> streamIdIter = streamIdObjects.iterator();
		while( streamIdIter.hasNext() ){
			try{
				streamIdIter.next().identifyCancel();
			}catch(GnException e){
				// ignore
			}
		}
		setStatus("Cancelled");
		enableCancel(false);
		
		try {
			if(gnMusicIdStream != null)
				gnMusicIdStream.audioProcessStop();
		} catch (GnException e) {
			e.printStackTrace();
		}
		
	}
	
	void idNow() {

		if (gnMusicIdStream == null || gnMicrophone == null) {
			return;
		}

		audioProcessThread = new Thread(new AudioProcessRunnable());
		audioProcessThread.start();
		
		// calling gnMusicIdStream.identifyAlbumAsync() in musicIdStreamProcessingStatusEvent() only after audio-processing-started callback is received
		
		visDisplay_hide = false;
		try {

			if (visThread == null) {
				visThread = new VisDisplayThread();				
				visThread.makeDisplayVisible(true);
				visThread.start();
			}

		} catch (Exception e) {
			try {
				gnMusicIdStream.audioProcessStop();
			} catch (GnException ex) {
				// ignore
			}
		}
	}
	
	
	
	/** 
	 * GNSDK MusicID-Stream event delegate 
	 */
	private class MusicIDStreamEvents implements IGnMusicIdStreamEvents {

		HashMap<String, String> gnStatus_to_displayStatus;
		
		public MusicIDStreamEvents(){
			gnStatus_to_displayStatus = new HashMap<String,String>();
			gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingStarted.toString(), "Identification started");
			gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingFpGenerated.toString(), "Fingerprinting complete");
			gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingLocalQueryStarted.toString(), "Lookup started");
			gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingOnlineQueryStarted.toString(), "Lookup started");			
			gnStatus_to_displayStatus.put(GnMusicIdStreamIdentifyingStatus.kStatusIdentifyingEnded.toString(), "Identification complete");
		}
		
		@Override
		public void statusEvent( GnStatus status, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable cancellable ) {
			//setStatus( String.format("%d%%",percentComplete), true );
		}
		
		@Override
		public void musicIdStreamProcessingStatusEvent( GnMusicIdStreamProcessingStatus status, IGnCancellable canceller ) {
			
			if (GnMusicIdStreamProcessingStatus.kStatusProcessingAudioStarted.compareTo(status) == 0) {
				try {

					Log.i(appString, "calling idnow");
					gnMusicIdStream.identifyAlbumAsync();
					enableCancel(true);
					clearDisplay();

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
		public void musicIdStreamIdentifyingStatusEvent( GnMusicIdStreamIdentifyingStatus status, IGnCancellable canceller ) {
			if(gnStatus_to_displayStatus.containsKey(status.toString())){
				setStatus( String.format("%s", gnStatus_to_displayStatus.get(status.toString())) );
			}
			
		}
				
			
		@Override
		public void musicIdStreamAlbumResult( GnResponseAlbums result, IGnCancellable canceller ) {
						
			try {
				
				if(result.resultCount() > 0 && result.albums().count() > 0){
				
					// display the first match result	
									
					String artist = result.albums().at(0).next().trackMatched().artist().name().display(); //todo: add scrollpane to UI to accomodate longer strings
					
					artist = artist + result.albums().at(0).next().artist().name().display();
					
					String trackTitle = result.albums().at(0).next().trackMatched().title().display();
					String albumTitle = result.albums().at(0).next().title().display();		
		           
		            remoteViews.setTextViewText(R.id.albumTitleWidgetText, albumTitle);
		            remoteViews.setTextViewText(R.id.trackTitleWidgetText, trackTitle);
		            remoteViews.setTextViewText(R.id.artistText, artist);
		            appWidgetManager.updateAppWidget(componentName, remoteViews);
		        	            
		            String coverArtUrl = result.albums().at(0).next().coverArt().asset(GnImageSize.kImageSizeSmall).url();
		            loadAndDisplayCoverArt(coverArtUrl);            
				}
				else{
					setStatus("No Match");
				}
				
				
			} catch (GnException e) {
				e.printStackTrace();
			}
			
			
			//GnMusicIdStream.audioProcessStop() waits for this result callback to finish,
			//so call audioProcessStop() in another thread and don't block here
			new Thread(new AudioProcessStopRunnable()).start();
			
			
		}

		@Override
		public void musicIdStreamIdentifyCompletedWithError(GnError error) {

			if (error.errorCode() != 0) {
				Log.i(appString, "error upon complete: " + error.errorDescription());
				setStatus("Error: " + error.errorDescription());
				
				//GnMusicIdStream.audioProcessStop() waits for this result callback to finish,
				//so call audioProcessStop() in another thread and don't block here
				new Thread(new AudioProcessStopRunnable()).start();
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
				
				GnLocale locale = new GnLocale(
						GnLocaleGroup.kLocaleGroupMusic,
						GnLanguage.kLanguageEnglish, 
						GnRegion.kRegionGlobal,
						GnDescriptor.kDescriptorDefault,
						gnUser);
				locale.setGroupDefault();
				
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
			try {
				locale.update( gnUser );
			} catch (GnException e) {
				Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
			}
		}

		@Override
		public void listUpdateNeeded( GnList list ) {
			// Locale update is detected
			try {
				list.update( gnUser );
			} catch (GnException e) {
				Log.e( appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule() );
			}
			
		}

		@Override
		public void systemMemoryWarning(long currentMemorySize, long warningMemorySize) {
			// only invoked if a memory warning limit is configured			
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
				setStatus( e.errorAPI() + ": " +  e.errorDescription() );
				
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
	
	
	class AudioProcessStopRunnable implements Runnable {
		
		@Override
		public void run() {

			if (gnMusicIdStream != null) {

				try {
					// stopping audio processing stops the audio processing thread
					gnMusicIdStream.audioProcessStop();
					visDisplay_hide = true; // IGnAudioSource's getData() will be invoked for one final time after audioProcessStop(), so setting this flag here to ignore visualization update in getData()
					visThread.makeDisplayVisible(false);
					enableCancel(false);

				} catch (GnException e) {

					Log.e(appString,  e.errorCode() + ", "
									+ e.errorDescription() + ", "
									+ e.errorModule());
					setStatus(e.errorAPI() + ": " + e.errorDescription());

				}

			}

		}
				
		
		
	}
	
	
	
	
		
	void loadAndDisplayCoverArt(String coverArtUrl){
		Thread runThread = new Thread( new CoverArtLoaderRunnable( coverArtUrl) );
		runThread.start();
	}
	
	class CoverArtLoaderRunnable implements Runnable {
		
		String 	coverArtUrl;
	
		
		CoverArtLoaderRunnable( String coverArtUrl){
			this.coverArtUrl = coverArtUrl;
		
		}

		@Override
		public void run() {

			Bitmap coverArt = null;
			if (coverArtUrl != null /*&& !coverArtUrl.isEmpty()*/) {
				URL url;
				try {
					url = new URL("http://" + coverArtUrl);
					InputStream input = new BufferedInputStream(url.openStream());
					coverArt = BitmapFactory.decodeStream(input);

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
           
			if (coverArt != null) {									
	            remoteViews.setImageViewBitmap(R.id.coverartWidget, coverArt);	
			} else {
				remoteViews.setImageViewResource(R.id.coverartWidget, R.drawable.no_image);
			}
			appWidgetManager.updateAppWidget(componentName, remoteViews); 		
		}
		
	}
	
	private void clearDisplay(){
		
		remoteViews.setImageViewResource(R.id.coverartWidget, R.drawable.no_image);
		remoteViews.setTextViewText(R.id.albumTitleWidgetText, "");
		remoteViews.setTextViewText(R.id.artistText, "\tListening ... ");
		remoteViews.setTextViewText(R.id.trackTitleWidgetText, "");
		
		appWidgetManager.updateAppWidget(componentName, remoteViews);
	}
	
	
	private void enableCancel(boolean enable) {

		int R_id_enable = R.id.cancelWidgetBtn;
		int R_id_disable = R.id.idNowWidgetBtn;

		if (!enable) {
			R_id_enable = R.id.idNowWidgetBtn;
			R_id_disable = R.id.cancelWidgetBtn;
		}

		remoteViews.setViewVisibility(R_id_disable, View.GONE);
		remoteViews.setViewVisibility(R_id_enable, View.VISIBLE);
		appWidgetManager.updateAppWidget(componentName, remoteViews);

	}
	
	
	/**
	 * Helpers to read license file from assets as string
	 */
	private String getAssetAsString( String assetName ){
		
		String 		assetString = null;
		InputStream assetStream;
		
		try {
			
			assetStream = this.context.getAssets().open(assetName);
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
	 * Audio visualization adapter.
	 * Sits between GnMic and GnMusicIdStream to receive audio data as it
	 * is pulled from the microphone allowing an audio visualization to be 
	 * implemented.
	 */
	class AudioVisualizeAdapter implements IGnAudioSource {
		
		private IGnAudioSource 	audioSource;
		private int				numBitsPerSample;
		private int				numChannels;
		private static final int  	SCALE_FACTOR = 15;
		private static final int	PADDING = 20;
		private static final int 	MIN_FLIP_INTERVAL_MS = 100;
		
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

				int rms_percent = rmsPercentOfMax(buffer, bufferSize, numBitsPerSample,numChannels);
				
				// Set flipping interval on ViewFlipper
				// Note: ViewFlipper timer appears to not update flip-interval until previously-set interval has expired.
				// Here interval-padding and show/hide-view is used as a work-around
				if (rms_percent > 0 && !visDisplay_hide) {
					visThread.makeDisplayVisible(true);
					flipInterval = (MIN_FLIP_INTERVAL_MS * 100) / (SCALE_FACTOR * rms_percent + PADDING); 
					synchronized (lock) {
						lock.notify();
					}
				}
				else{
						visThread.makeDisplayVisible(false);				
				}
			 

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
	
		
	class VisDisplayThread extends Thread{
				
			public void run(){
				
				while(true){
				
				try {
					synchronized(lock){
					lock.wait();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}				
	           
	            remoteViews.setInt(R.id.viewFlipper, "setFlipInterval", flipInterval );
	            appWidgetManager.updateAppWidget(componentName, remoteViews);
				}
				
			}
			
			
		void makeDisplayVisible(boolean visible) {
			if (visible != visDisplay_currently_visible) {
				visDisplay_currently_visible = visible;
				remoteViews.setViewVisibility(R.id.viewFlipper, visible ? View.VISIBLE : View.GONE);
				appWidgetManager.updateAppWidget(componentName, remoteViews);
			}
		}
	
	}
	
	private void setStatus(String statusText){
		try {
			remoteViews.setTextViewText(R.id.artistText, statusText);
			appWidgetManager.updateAppWidget(componentName, remoteViews);
		} catch (Exception e) {
			e.printStackTrace();
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
	 * GNSDK bundle ingest status event delegate
	 */
	private class BundleIngestEvents implements IGnLookupLocalStreamIngestEvents{
		
		@Override
		public void statusEvent(GnLookupLocalStreamIngestStatus status, String bundleId, IGnCancellable canceller) {
				Log.i(appString, "Bundle ingest progress: " +  status.toString());
		}
	}
	
	
}
	
	
	

