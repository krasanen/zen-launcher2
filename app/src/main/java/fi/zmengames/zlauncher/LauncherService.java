package fi.zmengames.zlauncher;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LauncherService extends Service {
    private static final String TAG = LauncherService.class.getSimpleName();
    public static final String GOOGLE_SIGN_IN = "GOOGLE_SIGN_IN";
    public static final String GOOGLE_SIGN_OUT = "GOOGLE_SIGN_OUT";
    public static final String LOAD_OVER = "PROVIDER_LOAD_OVER" ;
    public static final String FULL_LOAD_OVER = "PROVIDER_FULL_LOAD_OVER";
    private IBinder mBinder = new MyBinder();
    private ExecutorService serviceExecutor = Executors.newCachedThreadPool();

    public class MyBinder extends Binder {
        public LauncherService getService() {
            return LauncherService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.w(TAG, "onCreate...");
        super.onCreate();
    }
    @Nullable

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "in onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v(TAG, "in onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "in onUnbind");
        return true;
    }
    private void sendMessageSticky(ZEvent event) {
        EventBus.getDefault().postSticky(event);
    }
    private void sendMessageNotSticky(ZEvent event)
    {
        EventBus.getDefault().post(event);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Log.w(TAG, "onStartCommand..."+intent.getAction());
        if (intent == null || intent.getAction() == null) return START_NOT_STICKY;

        serviceExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if      (intent.getAction().equals(GOOGLE_SIGN_IN))     handleGoogleSignIn(intent);
                else if (intent.getAction().equals(GOOGLE_SIGN_OUT))    handleGoogleSignOut(intent);
                else if (intent.getAction().equals(LOAD_OVER))    handleProviderLoadOver(intent);
                else if (intent.getAction().equals(FULL_LOAD_OVER))    handleProviderFullLoadOver(intent);
            }
        });

        return START_NOT_STICKY;
    }

    private void handleProviderFullLoadOver(Intent intent) {
        Log.d(TAG, "handleProviderFullLoadOver");
        sendMessageNotSticky(new ZEvent(ZEvent.State.FULL_LOAD_OVER));
    }

    private void handleProviderLoadOver(Intent intent) {
        Log.d(TAG, "handleProviderLoadOver");
        sendMessageNotSticky(new ZEvent(ZEvent.State.LOAD_OVER));
    }

    private void handleGoogleSignIn(Intent intent) {
        Log.d(TAG, "handleGoogleSignIn");
        sendMessageNotSticky(new ZEvent(ZEvent.State.GOOGLE_SIGNIN));
    }
    private void handleGoogleSignOut(Intent intent) {
        Log.d(TAG, "handleGoogleSignOut");
        sendMessageNotSticky(new ZEvent(ZEvent.State.GOOGLE_SIGNOUT));
    }
    /// Helper Methods

    private boolean isAllowed(Intent intent) {
        return true;
    }
}
