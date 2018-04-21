package fi.zmengames.zlauncher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TableLayout.LayoutParams;
import com.gracenote.gnsdk.GnAlbum;
import com.gracenote.gnsdk.GnAsset;
import com.gracenote.gnsdk.GnAssetIterator;
import com.gracenote.gnsdk.GnContent;
import com.gracenote.gnsdk.GnContentType;
import com.gracenote.gnsdk.GnDataLevel;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnImageSize;

import fr.neamar.kiss.R;

class DetailView {
	
	GracenoteMusicID gracenoteMusicID;
	GnAlbum album;
	Button buttonArtistBio;
	Button buttonAlbumReview;
	Button  buttonArtistBio_close;
	Button  buttonAlbumReview_close;

	
	DetailView(GnAlbum album, GracenoteMusicID gracenoteMusicID){
		this.album = album;	
		this.gracenoteMusicID = gracenoteMusicID;				
	}
	
	
	void show(View view){
        LayoutInflater inflater = (LayoutInflater) gracenoteMusicID.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View detailView = inflater.inflate(R.layout.detail_view, null, false);
        final PopupWindow detailPopup = new PopupWindow(detailView, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, true);
        detailPopup.setBackgroundDrawable( new BitmapDrawable() );
        detailPopup.setOutsideTouchable(true);
        final int[] upperLeftLoc = new int[2];
        view.getLocationOnScreen(upperLeftLoc);	        
        detailPopup.showAtLocation(view, Gravity.TOP,  upperLeftLoc[0],  upperLeftLoc[1] + view.getHeight());
        
        //Duration
		 final long duration = album.trackMatched().duration();
		 if(duration > 0){
			((TextView) detailView.findViewById(R.id.duration))
					.setText("Duration: "
							+ TimeUnit.MILLISECONDS.toMinutes(duration)
							+ ":"
							+ (TimeUnit.MILLISECONDS.toSeconds(duration) 
									- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))));
			 }
		 
		 
		 //Match position
		 long matchPos = album.trackMatched().matchPosition();
		 if(matchPos >= 0){
			 String matchPosStr = TimeUnit.MILLISECONDS.toMinutes(matchPos)
						+ ":"
						+ (TimeUnit.MILLISECONDS.toSeconds(matchPos) - TimeUnit.MINUTES
								.toSeconds(TimeUnit.MILLISECONDS
										.toMinutes(matchPos)));
			 ((TextView) detailView.findViewById(R.id.matchPosition)).setText("Match Position: " + matchPosStr);
		 }
		 
		 
		 //Current position
		final TextView currentPos_textView = (TextView) detailView
				.findViewById(R.id.currentPosition);

		long currentPos = album.trackMatched().currentPosition();
		if (duration > 0 && currentPos <= duration) {
			String curPosStr = TimeUnit.MILLISECONDS.toMinutes(currentPos)
					+ ":"
					+ (TimeUnit.MILLISECONDS.toSeconds(currentPos) - TimeUnit.MINUTES
							.toSeconds(TimeUnit.MILLISECONDS
									.toMinutes(currentPos)));

			currentPos_textView.setText("Current Position: " + curPosStr);
		}
			
        //Genre
		 String genre = album.trackMatched().genre(GnDataLevel.kDataLevel_1);
		 if(genre == null || genre.isEmpty()){
			 genre = album.genre(GnDataLevel.kDataLevel_1);
		 }
		 if(genre != null){
			 ((TextView) detailView.findViewById(R.id.genre)).setText("Genre: " + genre);
		 }
		 
        //Mood
		 String mood = album.trackMatched().mood(GnDataLevel.kDataLevel_1);
		 if(mood != null){
			 ((TextView) detailView.findViewById(R.id.mood)).setText("Mood: " + mood);
		 }		 			 
		 
        //Tempo
		 String tempo = album.trackMatched().tempo(GnDataLevel.kDataLevel_1);
		 if(tempo != null){
			 ((TextView) detailView.findViewById(R.id.tempo)).setText("Tempo: " + tempo);
		 }
		 			 
        //Origin
		 String origin = album.artist().contributor().origin(GnDataLevel.kDataLevel_1);
		 if(origin != null){
			 ((TextView) detailView.findViewById(R.id.origin)).setText("Origin: " + origin);
		 }
		 			 
        //Time to Match
		 if(gracenoteMusicID.lastLookup_matchTime > 0){
			  DecimalFormat df = new DecimalFormat("#,###,##0.00");
			 ((TextView) detailView.findViewById(R.id.queryTime)).setText("Time to match: " + df.format(((double)gracenoteMusicID.lastLookup_matchTime)/1000) + " s");				 
		 }
		 
        //Lookup source
		 if(gracenoteMusicID.lastLookup_local){
			 ((TextView) detailView.findViewById(R.id.lookupSource)).setText("Lookup source: local");	
		 }
		 else{
				 ((TextView) detailView.findViewById(R.id.lookupSource)).setText("Lookup source: online");					 
		 }
		 
        
			
		buttonAlbumReview = (Button) detailView.findViewById(R.id.albumReview);	
		 	
		//pre-emptively load artist bio and album review 
		final View artistBio  = inflater.inflate(R.layout.bio_review, null, false);
		artistBio.setVisibility(View.GONE);	
	
		 GnContent content_artistBio =  album.artist().contributor().biography();				
		 GnAssetIterator iter = content_artistBio.assets().getIterator();
		 if(iter.hasNext()){
				try {
					GnAsset asset = iter.next();
					loadAndDisplayText(asset.url(), (TextView) artistBio.findViewById(R.id.artistBioText));
				} catch (GnException e) {
					e.printStackTrace();
				}					
		 }		 

		 String artistImageUrl = album.artist().contributor().image().asset(GnImageSize.kImageSizeSmall).url();
		 gracenoteMusicID.loadAndDisplayCoverArt(artistImageUrl, (ImageView) artistBio.findViewById(R.id.artistImage));
			 
		ScrollView v = (ScrollView) artistBio.findViewById(R.id.artistBioScroller);
		v.setMinimumHeight(detailPopup.getHeight() - 200);
        final PopupWindow artistBioPopup = new PopupWindow(artistBio, detailPopup.getWidth(), detailPopup.getHeight(), true);	
        final View detail_view_final = view;
        artistBioPopup.setBackgroundDrawable( new BitmapDrawable() );
        artistBioPopup.setOutsideTouchable(true);
        final int[] upperLeftLoc2 = new int[2];
        gracenoteMusicID.metadataListing.getLocationOnScreen(upperLeftLoc2);
    	buttonArtistBio = (Button) detailView.findViewById(R.id.artistBio);	
		buttonArtistBio.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					artistBio.setVisibility(View.VISIBLE);
					artistBioPopup.showAtLocation(detail_view_final, Gravity.TOP,  upperLeftLoc2[0],  upperLeftLoc2[1]);
				}
			});
		buttonArtistBio_close = (Button) artistBio.findViewById(R.id.closeWindow);
		buttonArtistBio_close.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				artistBioPopup.dismiss();
			}
		});
		
		final View albumReview  = inflater.inflate(R.layout.album_review, null, false);
		albumReview.setVisibility(View.GONE);		
		 GnContent content_albumReview =  album.review();				
		 iter = content_albumReview.assets().getIterator();
		 if(iter.hasNext()){
				try {
					GnAsset asset = iter.next();
					loadAndDisplayText(asset.url(), (TextView) albumReview.findViewById(R.id.albumReviewText));
				} catch (GnException e) {
					e.printStackTrace();
				}

		 }
		 
		ScrollView v2 = (ScrollView) albumReview.findViewById(R.id.albumReviewScroller);
		v2.setMinimumHeight(detailPopup.getHeight() - 200);
	    final PopupWindow albumReviewPopup = new PopupWindow(albumReview, detailPopup.getWidth(), detailPopup.getHeight(), true);	
	    albumReviewPopup.setBackgroundDrawable( new BitmapDrawable() );
	    albumReviewPopup.setOutsideTouchable(true);
		buttonAlbumReview.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
						albumReview.setVisibility(View.VISIBLE);
						albumReviewPopup.showAtLocation(detail_view_final, Gravity.TOP,  upperLeftLoc2[0],  upperLeftLoc2[1]);
					}
				}); 
		buttonAlbumReview_close = (Button) albumReview.findViewById(R.id.closeWindow);
		buttonAlbumReview_close.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				albumReviewPopup.dismiss();
			}
		});
		
		String imageUrl = album.coverArt().asset(GnImageSize.kImageSizeSmall).url();
		gracenoteMusicID.loadAndDisplayCoverArt(imageUrl, (ImageView) albumReview.findViewById(R.id.albumReviewImage));
											 		
	}
		
	void loadAndDisplayText(String url, TextView textView){
		Thread thread = new Thread(new LoadAndDisplayTextRunnable(url, textView));
		thread.start();
	}
		
}

class LoadAndDisplayTextRunnable implements Runnable{
	String urlStr;
	TextView textView;
	
	LoadAndDisplayTextRunnable(String url, TextView textView){
		urlStr = url;
		this.textView = textView;
	}
	
	@Override
	public void run(){
		try {
		    URL url = new URL("http://"+ urlStr);
		    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		    String tempStr;
		    StringBuffer strBuf = new StringBuffer();
		    while ((tempStr = in.readLine()) != null) {
		        strBuf.append(tempStr);
		    }
		    in.close();
		    strBuf.append("\n");
		    textView.setText(strBuf.toString());

		 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
