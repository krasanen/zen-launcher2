package fi.zmengames.zlauncher;




import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import fr.neamar.kiss.R;

public class IdNowWidgetProvider extends AppWidgetProvider {
	
	private static GracenoteMusicIDWidget gnMusicId;
	private static Context context;
	
	
	private static final String LOGTAG = "GFM IdNow Widget - IdNowWidgetProvider";
	
	
	@Override
	public void onUpdate(Context receiver_context, AppWidgetManager in_appWidgetManager,
			int[] appWidgetIds) {
		
		//receiver_context is transient, use global app context instead
		Context app_context = receiver_context.getApplicationContext();
	    
	    //initialize GnSDK and MusicID-stream
	    gnMusicId = getGnMusicIdInstance(app_context);	
	    
	    initViews(app_context);
	    		
		
	}
	
	
	static void initViews(Context in_context){
		
		context = in_context;
		ComponentName componentName = new ComponentName(context, IdNowWidgetProvider.class);
		
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widgetlayout);

		Intent intent = new Intent(context, IdNowService.class);
		intent.setAction(IdNowService.IDNOW);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
		views.setOnClickPendingIntent(R.id.idNowWidgetBtn, pendingIntent);

		Intent intent_cancel = new Intent(context, IdNowService.class);
		intent_cancel.setAction(IdNowService.CANCEL);
		PendingIntent pendingIntent_cancel = PendingIntent.getService(context, 0, intent_cancel, 0);
		views.setOnClickPendingIntent(R.id.cancelWidgetBtn, pendingIntent_cancel);
		Log.i(LOGTAG, "pending intent set");
		
		//update views for all widget instances
		AppWidgetManager.getInstance(context).updateAppWidget(componentName, views);
				 
	}

	
	 static protected GracenoteMusicIDWidget getGnMusicIdInstance(Context newContext){
				 		 
		 if(gnMusicId == null || IdNowWidgetProvider.context == null){
			 gnMusicId = new GracenoteMusicIDWidget(newContext);
			 initViews(newContext); //re-init views with the new context
			 Log.i(LOGTAG, "re-init with new context");
		 }
		 
		 return gnMusicId;

	}
						
}
