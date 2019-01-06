package fi.zmengames.zlauncher;

import android.animation.Animator;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;



import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.MainActivity;

import xiaofei.library.hermeseventbus.HermesEventBus;

import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_TOAST;
import static fi.zmengames.zlauncher.ZEvent.State.SHOW_TOAST;

public class LauncherService extends Service {
    public static final String LAUNCH_INTENT = "LAUNCH_INTENT";
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
    private int mBrightness = 80;
    private int mAdvancedMode = Constants.AdvancedMode.NONE;
    private int mYellowFilterAlpha = 10;

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
        HermesEventBus.getDefault().register(this);
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mAccessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);


        super.onCreate();
    }
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEventMainThread(ZEvent event) {
        if(BuildConfig.DEBUG) Log.w(TAG, "in service" +
                ", Got message from service: " + event.getState());

    }
    @Nullable

    @Override
    public IBinder onBind(Intent intent) {
        if(BuildConfig.DEBUG) Log.v(TAG, "in onBind");
        HermesEventBus.getDefault().connectApp(this, getPackageName());

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

        HermesEventBus.getDefault().connectApp(this, getPackageName());
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(BuildConfig.DEBUG) Log.v(TAG, "in onUnbind");
        HermesEventBus.getDefault().destroy();
        return true;
    }

    private void sendMessageSticky(ZEvent event) {
        HermesEventBus.getDefault().postSticky(event);
    }

    private void sendMessageNotSticky(ZEvent event) {
        HermesEventBus.getDefault().post(event);
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
            }
        });

        return START_NOT_STICKY;
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
        sendMessageNotSticky(new ZEvent(ZEvent.State.FULL_LOAD_OVER));
    }

    private void handleProviderLoadOver(Intent intent) {
        if(BuildConfig.DEBUG) Log.d(TAG, "handleProviderLoadOver");
        sendMessageNotSticky(new ZEvent(ZEvent.State.LOAD_OVER));
    }

    private void handleGoogleSignIn(Intent intent) {
        if(BuildConfig.DEBUG) Log.d(TAG, "handleGoogleSignIn");
        sendMessageNotSticky(new ZEvent(ZEvent.State.GOOGLE_SIGNIN));
    }

    private void handleGoogleSignOut(Intent intent) {
        if(BuildConfig.DEBUG) Log.d(TAG, "handleGoogleSignOut");
        sendMessageNotSticky(new ZEvent(ZEvent.State.GOOGLE_SIGNOUT));
    }
    private void handleShowToast(String text) {
        if(BuildConfig.DEBUG) Log.d(TAG, "handleShowToast");
        sendMessageNotSticky(new ZEvent(SHOW_TOAST, text));
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
                    mLayout.setAlpha(0.9f);
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

            // Set mask alpha to adjust screen brightness
            float targetAlpha = (100 - mBrightness) * 0.01f;
            if (paramInt != -1) {
                if (isShowing) {
                    // Start animation when value changes a lot.
                    if (Math.abs(targetAlpha - mLayout.getAlpha()) < 0.1f) {
                        mLayout.setAlpha(targetAlpha);
                    } else {
                        mLayout.animate().alpha(targetAlpha).setDuration(100).start();
                    }
                } else {
                    mLayout.animate().alpha(targetAlpha)
                            .setDuration(ANIMATE_DURATION_MILES).start();
                }
            }
        }

        if (mLayout != null) {
            int color = Color.YELLOW;
            if (mYellowFilterAlpha > 0) {
                Log.i(TAG, "Alpha: " + mYellowFilterAlpha);
                float ratio = ((float) mYellowFilterAlpha) / 100F;
                int blend = ColorUtil.blendColors(Color.YELLOW, Color.BLUE, ratio);
                //blend = ColorUtil.blendColors(Color.RED, blend, ratio / 3F);
                color = ColorUtil.blendColors(blend, color, ratio / 3F);
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
