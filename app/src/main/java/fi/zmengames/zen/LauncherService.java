package fi.zmengames.zen;

import android.animation.Animator;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;


import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.MainActivity;

import fr.neamar.kiss.broadcast.BadgeCountHandler;

import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_TOAST;
import static fi.zmengames.zen.ZEvent.State.SHOW_TOAST;

public class LauncherService extends Service {
    public static ArrayList<ZEvent> zEventArrayList = new ArrayList<>();
    public static final String LAUNCH_INTENT = "LAUNCH_INTENT";
    public static final String SET_BADGE_COUNT = "SET_BADGE_COUNT";
    private static final String TAG = LauncherService.class.getSimpleName();
    public static final String GOOGLE_SIGN_IN = "GOOGLE_SIGN_IN";
    public static final String GOOGLE_SIGN_OUT = "GOOGLE_SIGN_OUT";
    public static final String LOAD_OVER = "PROVIDER_LOAD_OVER";
    public static final String FULL_LOAD_OVER = "PROVIDER_FULL_LOAD_OVER";
    public static final String NIGHTMODE_ON = "NIGHTMODE_ON";
    public static final String NIGHTMODE_OFF = "NIGHTMODE_OFF";
    private IBinder mBinder = new MyBinder();
    private ExecutorService serviceExecutor = Executors.newCachedThreadPool();


    // System Services
    private WindowManager mWindowManager;
    private NotificationManager mNotificationManager;
    private AccessibilityManager mAccessibilityManager;

    // Notification
    private Notification mNotification;

    // Floating Window
    private View mLayout;
    private WindowManager.LayoutParams mLayoutParams;

    // If floating window is showing
    private boolean isShowing = true;

    // Options
    private int mBrightness = 100;
    private int mAdvancedMode = Constants.AdvancedMode.NONE;
    private int mYellowFilterAlpha = 100;

    // Constants
    private static final int ANIMATE_DURATION_MILES = 250;
    private static final int NOTIFICATION_NO = 1024;

    public class MyBinder extends Binder {
        public LauncherService getService() {
            return LauncherService.this;
        }
    }

    @Override
    public void onCreate() {
        if(BuildConfig.DEBUG) Log.w(TAG, "onCreate...");
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mAccessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);


        super.onCreate();
    }


    @Override
    public IBinder onBind(Intent intent) {
        if(BuildConfig.DEBUG) Log.v(TAG, "in onBind");

        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyMaskView();
    }

    @Override
    public void onRebind(Intent intent) {
        if(BuildConfig.DEBUG) Log.v(TAG, "in onRebind");


        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(BuildConfig.DEBUG) Log.v(TAG, "in onUnbind");

        return true;
    }

    private void sendMessageSticky(ZEvent event) {
        EventBus.getDefault().postSticky(event);
    }

    private void sendMessageSticky2(ZEvent event) {
        EventBus.getDefault().postSticky(event);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if(BuildConfig.DEBUG) Log.w(TAG, "onStartCommand..." + intent.getAction());
        if (intent == null || intent.getAction() == null) return START_NOT_STICKY;

        serviceExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (intent.getAction().equals(GOOGLE_SIGN_IN)) handleGoogleSignIn(intent);
                else if (intent.getAction().equals(GOOGLE_SIGN_OUT)) handleGoogleSignOut(intent);
                else if (intent.getAction().equals(LOAD_OVER)) handleProviderLoadOver(intent);
                else if (intent.getAction().equals(FULL_LOAD_OVER))
                    handleProviderFullLoadOver(intent);
                else if (intent.getAction().equals(NIGHTMODE_ON)) createMaskView();
                else if (intent.getAction().equals(NIGHTMODE_OFF)) destroyMaskView();
                else if (intent.getAction().equals(LAUNCH_INTENT)) launchIntent(intent);
                else if (intent.getAction().equals(SET_BADGE_COUNT)) setBadgeCount(intent);
            }
        });

        return START_NOT_STICKY;
    }

    private void setBadgeCount(Intent intent) {
        if(BuildConfig.DEBUG) Log.d(TAG, "setBadgeCount");
        sendMessageSticky2(new ZEvent(ZEvent.State.BADGE_COUNT, intent.getStringExtra(BadgeCountHandler.PACKAGENAME), intent.getIntExtra(BadgeCountHandler.BADGECOUNT,0)));
    }

    private void launchIntent(Intent intent) {
        if(BuildConfig.DEBUG) Log.d(TAG, "launchIntent");
        Intent toLaunch = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        try {
            startActivity(toLaunch);
        }
        catch(SecurityException e){
            handleShowToast(e.getMessage());
        }
        catch(Exception e) {

            e.printStackTrace();
            handleShowToast(e.getMessage());
        }

    }

    private void handleProviderFullLoadOver(Intent intent) {
        if(BuildConfig.DEBUG) Log.d(TAG, "handleProviderFullLoadOver");
        sendMessageSticky2(new ZEvent(ZEvent.State.FULL_LOAD_OVER));
    }

    private void handleProviderLoadOver(Intent intent) {
        if(BuildConfig.DEBUG) Log.d(TAG, "handleProviderLoadOver");
        sendMessageSticky2(new ZEvent(ZEvent.State.LOAD_OVER));
    }

    private void handleGoogleSignIn(Intent intent) {
        if(BuildConfig.DEBUG) Log.d(TAG, "handleGoogleSignIn");
        sendMessageSticky2(new ZEvent(ZEvent.State.GOOGLE_SIGNIN));
    }

    private void handleGoogleSignOut(Intent intent) {
        if(BuildConfig.DEBUG) Log.d(TAG, "handleGoogleSignOut");
        sendMessageSticky2(new ZEvent(ZEvent.State.GOOGLE_SIGNOUT));
    }
    private void handleShowToast(String text) {
        if(BuildConfig.DEBUG) Log.d(TAG, "handleShowToast");
        sendMessageSticky2(new ZEvent(SHOW_TOAST, text));
    }

    /// Helper Methods

    private boolean isAllowed(Intent intent) {
        return true;
    }


    private void createMaskView() {

        if(BuildConfig.DEBUG) Log.d(TAG, "createMaskView");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                if (mLayout == null) {
                    mLayout = new View(getApplicationContext());
                    mLayout.setLayoutParams(
                            new ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            )
                    );
                    mLayout.setBackgroundColor(Color.BLACK);
                    mLayout.setAlpha(0.1f);
                }
                updateLayoutParams(-1);
                try {
                    mWindowManager.addView(mLayout, mLayoutParams);
                } catch (Exception e) {
                    e.printStackTrace();
                    if(BuildConfig.DEBUG) Log.d(TAG, "createMaskView CANNOT_START");
                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction(MainActivity.class.getCanonicalName());
                    broadcastIntent.putExtra(Constants.Extra.EVENT_ID, Constants.Event.CANNOT_START);
                    sendBroadcast(broadcastIntent);
                }
            }
        });

    }

    private static float constrain(float paramFloat1, float paramFloat2, float paramFloat3) {
        if (paramFloat1 < paramFloat2) {
            return paramFloat2;
        }
        if (paramFloat1 > paramFloat3) {
            return paramFloat3;
        }
        return paramFloat1;
    }


    public int getWindowType() {
        int sdkInt = Build.VERSION.SDK_INT;
        if (sdkInt >= Build.VERSION_CODES.O) {
            // Only TYPE_APPLICATION_OVERLAY is available in O.
            return TYPE_APPLICATION_OVERLAY;
        }
        // Default window type.
        int result = TYPE_SYSTEM_OVERLAY;
        if (mAdvancedMode == Constants.AdvancedMode.NO_PERMISSION
                && sdkInt < Build.VERSION_CODES.N) {
            // Toast Mode cannot work normally after N. Window will be set 10~ secs max timeout.
            result = TYPE_TOAST;
        } else if (mAdvancedMode == Constants.AdvancedMode.OVERLAY_ALL) {
            // It seems that this mode should use TYPE_SYSTEM_ERROR as window type.
            result = TYPE_SYSTEM_ERROR;
        }
        return result;
    }

    private void updateLayoutParams(int paramInt) {
        if (mLayoutParams == null) {
            mLayoutParams = new WindowManager.LayoutParams();
        }

        // Hacky method. However, I don't know how it works.
        mAccessibilityManager.isEnabled();

        // Apply layout params type & gravity
        mLayoutParams.type = getWindowType();
        mLayoutParams.gravity = Gravity.CENTER;

        // Apply layout params attributes
        if (getWindowType() == TYPE_SYSTEM_ERROR) {
            // This is the reason why it will not affect users' application installation.
            // Mask window won't cover any views.
            mLayoutParams.width = 0;
            mLayoutParams.height = 0;
            mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            // I haven't found what this two value mean. :p (I got them from another screen filter app)
            mLayoutParams.flags &= 0xFFDFFFFF;
            mLayoutParams.flags &= 0xFFFFFF7F;
            mLayoutParams.format = PixelFormat.OPAQUE;
            // Screen is dimmed by system.
            mLayoutParams.dimAmount = constrain((100 - paramInt) / 100.0F, 0.0F, 0.9F);
        } else {
            // A dirty fix to deal with screen rotation.
            int max = Math.max(
                    Utility.getRealScreenWidth(this),
                    Utility.getRealScreenHeight(this)
            );
            mLayoutParams.height = mLayoutParams.width = max + 200;

            mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            mLayoutParams.format = PixelFormat.TRANSPARENT;


        }

        if (mLayout != null) {
            int color = Color.YELLOW;
            if (mYellowFilterAlpha > 0) {
                Log.i(TAG, "Alpha: " + mYellowFilterAlpha);
                float ratio = ((float) mYellowFilterAlpha) / 100F;
                color = ColorUtil.blendColors(Color.YELLOW, Color.TRANSPARENT, ratio);

            }
            mLayout.setBackgroundColor(color);


        }
    }

    private void cancelNotification() {
        try {
            mNotificationManager.cancel(NOTIFICATION_NO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void destroyMaskView() {
        if(BuildConfig.DEBUG) Log.d(TAG, "destroyMaskView");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {

                isShowing = false;
                //Utility.createStatusBarTiles(this, false);
                cancelNotification();
                if (mLayout != null) {
                    mLayout.animate()
                            .alpha(0f)
                            .setDuration(ANIMATE_DURATION_MILES)
                            .setListener(new Animator.AnimatorListener() {
                                private View readyToRemoveView = mLayout;

                                @Override
                                public void onAnimationStart(Animator animator) {
                                }

                                @Override
                                public void onAnimationCancel(Animator animator) {
                                }

                                @Override
                                public void onAnimationRepeat(Animator animator) {
                                }

                                @Override
                                public void onAnimationEnd(Animator animator) {
                                    try {
                                        mWindowManager.removeViewImmediate(readyToRemoveView);
                                    } catch (Exception e) {
                                        // Just do nothing
                                    }
                                }
                            }).start();
                    mLayout = null;
                }
            }

        });
    }
}
