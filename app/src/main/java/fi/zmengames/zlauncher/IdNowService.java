package fi.zmengames.zlauncher;




import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class IdNowService extends Service {
    public static final String IDNOW= "idnow";
    public static final String CANCEL= "cancel";
    private static final String LOGTAG = "GFM IdNow Widget - IdNowService";

    public IdNowService(){
        Log.i(LOGTAG, "constructing IdNow service");
    }




    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStart(intent, startId);

        idNow(intent);
        stopSelf(startId);

        return START_STICKY;
    }


    private void idNow(Intent intent) {

        if (intent != null){
            String requestedAction = intent.getAction();

            if (requestedAction != null && (requestedAction.equals(IDNOW) || requestedAction.equals(CANCEL))){

                GracenoteMusicIDWidget gnMusicId = IdNowWidgetProvider.getGnMusicIdInstance(this.getApplicationContext());

                if (requestedAction.equals(IDNOW)) {
                    gnMusicId.idNow();
                    Log.i(LOGTAG,"idNow invoked");
                } else {
                    gnMusicId.cancel();
                }

            }
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



}
