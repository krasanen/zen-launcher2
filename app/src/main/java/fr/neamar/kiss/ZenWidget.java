package fr.neamar.kiss;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;



/**
 * Implementation of App Widget functionality.
 */
public class ZenWidget extends AppWidgetProvider {
    private static final String MyOnClick = "myOnClickTag";
    private RemoteViews remoteViews;
    private ComponentName watchWidget;
    private static final String TAG = ZenWidget.class.getSimpleName();
    void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        if (BuildConfig.DEBUG) Log.i(TAG,"updateAppWidget:"+appWidgetId);

        // Construct the RemoteViews object
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.zen_widget);
        watchWidget = new ComponentName( context, ZenWidget.class );
        remoteViews.setOnClickPendingIntent( R.id.zenwidgetbutton, getPendingSelfIntent(context, MyOnClick));
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }
    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        MainActivity mainActivity = KissApplication.getApplication(context).getMainActivity();
        if (mainActivity!=null && MyOnClick.equals(intent.getAction())){
            remoteViews = new RemoteViews( context.getPackageName(), R.layout.zen_widget );
            mainActivity.toggleFlashLight();
            if (MainActivity.flashToggle) {
                remoteViews.setImageViewResource(R.id.zenwidgetbutton, R.drawable.lightbulp_on);
            } else {
                remoteViews.setImageViewResource(R.id.zenwidgetbutton, R.drawable.lightbulp);
            }
            watchWidget = new ComponentName( context, ZenWidget.class );
            (AppWidgetManager.getInstance(context)).updateAppWidget( watchWidget, remoteViews );
        }
    }
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }
}

