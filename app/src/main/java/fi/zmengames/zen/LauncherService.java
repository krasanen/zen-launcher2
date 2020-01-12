package fi.zmengames.zen;

import android.animation.Animator;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;


import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;

import fr.neamar.kiss.R;
import fr.neamar.kiss.broadcast.BadgeCountHandler;

import static android.provider.Settings.ACTION_VOICE_CONTROL_BATTERY_SAVER_MODE;
import static android.provider.Settings.EXTRA_BATTERY_SAVER_MODE_ENABLED;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_TOAST;
import static fi.zmengames.zen.AlarmActivity.ALARM_EXTRA;
import static fi.zmengames.zen.AlarmActivity.ALARM_ID;
import static fi.zmengames.zen.AlarmActivity.ALARM_TIME;
import static fi.zmengames.zen.ZEvent.State.SHOW_TOAST;
import static fr.neamar.kiss.MainActivity.ALARM_IN_ACTION;
import static fr.neamar.kiss.MainActivity.ALARM_PICKER;
import static fr.neamar.kiss.MainActivity.LOCK_IN;

public class LauncherService extends Service {
    private static final String TAG = LauncherService.class.getSimpleName();
    public static final String LAUNCH_INTENT = "com.zmengames.zenlauncher.LAUNCH_INTENT";
    public static final String SET_BADGE_COUNT = "com.zmengames.zenlauncher.SET_BADGE_COUNT";
    public static final String ENABLE_PROXIMITY = "com.zmengames.zenlauncher.ENABLE_PROXIMITY";
    public static final String DISABLE_PROXIMITY = "com.zmengames.zenlauncher.DISABLE_PROXIMITY";
    public static final String GOOGLE_SIGN_IN = "com.zmengames.zenlauncher.GOOGLE_SIGN_IN";
    public static final String GOOGLE_SIGN_OUT = "com.zmengames.zenlauncher.GOOGLE_SIGN_OUT";
    public static final String NIGHTMODE_ON = "com.zmengames.zenlauncher.NIGHTMODE_ON";
    public static final String NIGHTMODE_OFF = "com.zmengames.zenlauncher.NIGHTMODE_OFF";
    public static final String SCREEN_ON = "com.zmengames.zenlauncher.SCREEN_ON";
    public static final String SCREEN_OFF = "com.zmengames.zenlauncher.SCREEN_OFF";
    public static final String ALARM_ENTERED_TEXT = "com.zmengames.zenlauncher.ALARM_ENTERED_TEXT";
    public static final String ALARM_DATE_PICKER_MILLIS = "com.zmengames.zenlauncher.ALARM_DATE_PICKER_MILLIS";
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
    private static SensorManager mSensorManager;
    private static Sensor mProximity;
    private static SensorEventListener sensorEventListener;
    private static final int SENSOR_SENSITIVITY = 4;
    public static boolean isProximityLockEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("proximity-switch-lock", false)) {
            return true;
        } else {
            return false;
        }
    }

    public class MyBinder extends Binder {
        public LauncherService getService() {
            return LauncherService.this;
        }

    }
    public void stopListeningProximitySensor(){
        if (mSensorManager!=null){
            handler.removeCallbacks(lockRunnable);
            if (sensorEventListener!=null) {
                mSensorManager.unregisterListener(sensorEventListener);
            }
            lastValue = -1f;
        }
    }
    public static boolean running = true;
    public static float lastValue=-1f;
    final Runnable lockRunnable = new Runnable() {
        public void run() {
            running = false;
            if (sensorEventListener!=null) {
                mSensorManager.unregisterListener(sensorEventListener);
            }
            lockScreen();
        }
    };
    Handler handler = new Handler(Looper.getMainLooper());

    private void startListeningProximitySensor() {
        if (BuildConfig.DEBUG) Log.d(TAG, "startListeningProximitySensor");
        if (lastValue == -1f) {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            sensorEventListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    float thisValue = sensorEvent.values[0];
                    if (thisValue != lastValue && sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "sensorEvent.values[0]:" + thisValue);
                        lastValue = thisValue;
                        if (sensorEvent.values[0] < sensorEvent.sensor.getMaximumRange()) {
                            //far
                            lockScreenStartTimer(true);
                        } else {
                            //near
                            lockScreenStartTimer(false);
                        }
                    }
                }




                private void lockScreenStartTimer(boolean run) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "lockScreenStartTimer: " + run);
                    if (run) {
                        if (!running)
                            handler.postDelayed(lockRunnable, 2000);
                    } else {
                        handler.removeCallbacks(lockRunnable);
                        running = false;
                    }
                }


                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {

                }
            };

            mSensorManager.registerListener(sensorEventListener, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "startListeningProximitySensor (was already running)");
        }

    }
    public void lockScreen() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (isDeviceAdminActive()) {
            devicePolicyManager.lockNow();
        } else {
            handleShowToast("Device Admin features not enabled");
        }
    }

    private boolean isDeviceAdminActive() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName compName = new ComponentName(this, ZenAdmin.class);
        return devicePolicyManager.isAdminActive(compName);
    }

    @Override
    public void onCreate() {
        if(BuildConfig.DEBUG) Log.i(TAG, "onCreate...");
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mAccessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);




        super.onCreate();
    }


    @Override
    public IBinder onBind(Intent intent) {
        if(BuildConfig.DEBUG) Log.v(TAG, "in onBind");

        if (isProximityLockEnabled(this)) {
            startListeningProximitySensor();
        }
        return mBinder;
    }

    @Override
    public void onDestroy() {
        if(BuildConfig.DEBUG) Log.v(TAG, "in onDestroy");
        super.onDestroy();
        if (isProximityLockEnabled(this)) {
            stopListeningProximitySensor();
        }
        destroyMaskView();
    }

    @Override
    public void onRebind(Intent intent) {
        if(BuildConfig.DEBUG) Log.v(TAG, "in onRebind");
        if (isProximityLockEnabled(this)) {
            startListeningProximitySensor();
        }
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(BuildConfig.DEBUG) Log.v(TAG, "in onUnbind");

        return true;
    }

    private void sendMessage(ZEvent event) {
        EventBus.getDefault().post(event);
    }

    private void sendMessageSticky2(ZEvent event) {
        EventBus.getDefault().postSticky(event);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if(BuildConfig.DEBUG) Log.i(TAG, "onStartCommand..." + intent.getAction());
        if (intent == null || intent.getAction() == null) return START_NOT_STICKY;

        serviceExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (intent.getAction().equals(GOOGLE_SIGN_IN)) handleGoogleSignIn(intent);
                else if (intent.getAction().equals(GOOGLE_SIGN_OUT)) handleGoogleSignOut(intent);

                else if (intent.getAction().equals(NIGHTMODE_ON)) createMaskView();
                else if (intent.getAction().equals(NIGHTMODE_OFF)) destroyMaskView();
                else if (intent.getAction().equals(LAUNCH_INTENT)) launchIntent(intent);
                else if (intent.getAction().equals(SET_BADGE_COUNT)) setBadgeCount(intent);
                else if (intent.getAction().equals(ENABLE_PROXIMITY)) startListeningProximitySensor();
                else if (intent.getAction().equals(DISABLE_PROXIMITY)) stopListeningProximitySensor();
                else if (intent.getAction().equals(SCREEN_ON)) screenOn();
                else if (intent.getAction().equals(SCREEN_OFF)) screenOff();
                else if (intent.getAction().equals(LOCK_IN)) lockScreenTimer(intent);
                else if (intent.getAction().equals(ALARM_IN_ACTION)) alarmIn(intent);
                else if (intent.getAction().equals(ALARM_PICKER)) alarmAtPicker(intent);
            }
        });

        return START_NOT_STICKY;
    }

    private void alarmAtPicker(Intent intent) {
        long millis = intent.getLongExtra(ALARM_DATE_PICKER_MILLIS,0);
        String enteredText = intent.getStringExtra(ALARM_ENTERED_TEXT);

        if (BuildConfig.DEBUG)  Log.d(TAG,"alarmAtPicker, millis:"+ millis);
        Calendar calAlarm = Calendar.getInstance();
        calAlarm.setTimeZone(TimeZone.getTimeZone("GMT"));
        calAlarm.setTimeInMillis(millis);
        setAlarm(calAlarm, enteredText);
    }

    private void lockScreenTimer(Intent intent) {
        if (isDeviceAdminActive()) {
            int minutes = intent.getIntExtra(ZenProvider.mMinutes, 0);
            if (BuildConfig.DEBUG) Log.d(TAG, "lockScreenTimer: " + minutes);
            handler.postDelayed(lockRunnable, minutes * 60000);
            String toast = getString(R.string.lockIn) +" "+ getString(R.string.after)+ " " + minutes + " "+ getString(R.string.minutes);
            handleShowToast(toast );
        } else {
            handleShowToast(getString(R.string.deviceadmin_switch_text));
        }
    }

    private void alarmIn(Intent intent){
        String query = intent.getStringExtra(ALARM_ENTERED_TEXT);

        long minutes = intent.getLongExtra(ZenProvider.mMinutes, 0);
        if (BuildConfig.DEBUG)  Log.d(TAG,"alarmIn, minutes:"+ minutes);
        Calendar calAlarm = Calendar.getInstance();
        calAlarm.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date alarm = new Date(System.currentTimeMillis()+minutes*60*1000);
        alarm.setSeconds(0);
        calAlarm.setTimeInMillis(alarm.getTime());
        setAlarm(calAlarm, query);
    }

    private void checkBatteryOptimizations(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                handleShowToast("Zen needs to ignore battery opt to be able to wake up for alarm.");
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("package:" + packageName));
                try {
                    startActivity(intent);
                } catch (RuntimeException e){
                    handleShowToast("Zen needs to ignore battery opt to be able to wake up for alarm.");
                }
            }
        }
    }

    private void checkDnd() {

        // Get the notification manager instance
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        checkBatteryOptimizations();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // If api level minimum 23
            /*
                boolean isNotificationPolicyAccessGranted ()
                    Checks the ability to read/modify notification policy for the calling package.
                    Returns true if the calling package can read/modify notification policy.
                    Request policy access by sending the user to the activity that matches the
                    system intent action ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS.

                    Use ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED to listen for
                    user grant or denial of this access.

                Returns
                    boolean

            */

            // If notification policy access granted for this package
            if (mNotificationManager.isNotificationPolicyAccessGranted()) {
            } else {
                handleShowToast("Zen Alarm needs notification policy access for do not disturb control");
                /*
                    String ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                        Activity Action : Show Do Not Disturb access settings.
                        Users can grant and deny access to Do Not Disturb configuration from here.

                    Input : Nothing.
                    Output : Nothing.
                    Constant Value : "android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS"
                */
                // If notification policy access not granted for this package
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                } catch (RuntimeException e){
                    handleShowToast("Zen Alarm needs notification policy access for do not disturb control");
                }
            }
        }
    }
    private void setAlarm(Calendar targetCal, String query) {
        checkDnd();
        handleShowToast("\n\n***\n" + "Alarm is set "
                + targetCal.getTime() + "\n" + query + "\n" + "***\n");
        if (BuildConfig.DEBUG) Log.d(TAG,"targetCal.getTimeInMillis():"+targetCal.getTimeInMillis());
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        long notificationId = targetCal.getTimeInMillis();
        intent.putExtra(ALARM_ID, notificationId);
        intent.putExtra(ALARM_EXTRA, query);
        intent.putExtra(ALARM_TIME, targetCal.getTime().toString());
        AlarmUtils.addAlarm(getApplicationContext(), intent, notificationId,targetCal);

    }
    private void screenOn() {
        if (BuildConfig.DEBUG) Log.d(TAG, "SCREEN_ON");
        if (isProximityLockEnabled(this)) {
            startListeningProximitySensor();
        }
    }
    private void screenOff() {
        if (BuildConfig.DEBUG) Log.d(TAG, "SCREEN_OFF");
        if (isProximityLockEnabled(this)) {
            stopListeningProximitySensor();
        }
    }

    private void setBadgeCount(Intent intent) {
        if(BuildConfig.DEBUG) Log.i(TAG, "setBadgeCount");
        sendMessageSticky2(new ZEvent(ZEvent.State.BADGE_COUNT, intent.getStringExtra(BadgeCountHandler.PACKAGENAME), intent.getIntExtra(BadgeCountHandler.BADGECOUNT,0)));
    }

    private void launchIntent(Intent intent) {
        if(BuildConfig.DEBUG) Log.i(TAG, "launchIntent");
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
        if(BuildConfig.DEBUG) Log.i(TAG, "handleProviderFullLoadOver");
        sendMessage(new ZEvent(ZEvent.State.FULL_LOAD_OVER));
    }

    private void handleProviderLoadOver(Intent intent) {
        if(BuildConfig.DEBUG) Log.i(TAG, "handleProviderLoadOver");
        sendMessageSticky2(new ZEvent(ZEvent.State.LOAD_OVER));
    }

    private void handleGoogleSignIn(Intent intent) {
        if(BuildConfig.DEBUG) Log.i(TAG, "handleGoogleSignIn");
        sendMessageSticky2(new ZEvent(ZEvent.State.GOOGLE_SIGNIN));
    }

    private void handleGoogleSignOut(Intent intent) {
        if(BuildConfig.DEBUG) Log.i(TAG, "handleGoogleSignOut");
        sendMessageSticky2(new ZEvent(ZEvent.State.GOOGLE_SIGNOUT));
    }
    private void handleShowToast(String text) {
        if(BuildConfig.DEBUG) Log.i(TAG, "handleShowToast");
        sendMessage(new ZEvent(SHOW_TOAST, text));
    }

    /// Helper Methods

    private boolean isAllowed(Intent intent) {
        return true;
    }


    private void createMaskView() {

        if(BuildConfig.DEBUG) Log.i(TAG, "createMaskView");
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
                    if(BuildConfig.DEBUG) Log.i(TAG, "createMaskView CANNOT_START");
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
        if(BuildConfig.DEBUG) Log.i(TAG, "destroyMaskView");
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
