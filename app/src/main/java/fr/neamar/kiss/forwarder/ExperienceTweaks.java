package fr.neamar.kiss.forwarder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.searcher.HistorySearcher;
import fr.neamar.kiss.searcher.NullSearcher;

import static android.text.InputType.TYPE_CLASS_PHONE;
import static fr.neamar.kiss.MainActivity.isKeyboardVisible;
import static fr.neamar.kiss.MainActivity.mDebugJson;


// Deals with any settings in the "User Experience" setting sub-screen
public class ExperienceTweaks extends Forwarder {
    private static final String TAG = ExperienceTweaks.class.getSimpleName();

    /**
     * InputType that behaves as if the consuming IME is a standard-obeying
     * soft-keyboard
     * <p>
     * *Auto Complete* means "we're handling auto-completion ourselves". Then
     * we ignore whatever the IME thinks we should display.
     */
    private final static int INPUT_TYPE_STANDARD = InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    /**
     * InputType that behaves as if the consuming IME is SwiftKey
     * <p>
     * *Visible Password* fields will break many non-Latin IMEs and may show
     * unexpected behaviour in numerous ways. (#454, #517)
     */
    private final static int INPUT_TYPE_WORKAROUND = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;

    private final Runnable displayKeyboardRunnable = new Runnable() {
        @Override
        public void run() {
            mainActivity.showKeyboard();
        }
    };

    private View mainEmptyView;
    private final GestureDetector gd;
    private final ScaleGestureDetector sgd;
    private boolean scaling;
    int width, height;
    public static boolean mNumericInputTypeForced = false;

    ExperienceTweaks(final MainActivity mainActivity) {
        super(mainActivity);

        Display display = mainActivity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
        if(BuildConfig.DEBUG) Log.d(TAG, "ExperienceTweaks create");
        // Lock launcher into portrait mode
        // Do it here (before initializing the view in onCreate) to make the transition as smooth as possible
        if (prefs.getBoolean("force-portrait", true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            } else {
                mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }


        sgd = new ScaleGestureDetector(mainActivity, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (mDebugJson) {
                    if(BuildConfig.DEBUG) Log.d(TAG, "onScale");
                }
                scaling = true;
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                if (mDebugJson) {
                    if(BuildConfig.DEBUG) Log.d(TAG, "onScaleBegin");
                }
                scaling = true;
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                if (mDebugJson) {
                    if(BuildConfig.DEBUG) Log.d(TAG, "onScaleEnd");
                }
                if (prefs.getBoolean("pinch-open", false)) {
                    mainActivity.launcherButton.performClick();
                }
                scaling = false;
            }
        });

        gd = new GestureDetector(mainActivity, new GestureDetector.SimpleOnGestureListener() {


            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return super.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1!=null&&e2!=null) {
                    float directionY = e2.getY() - e1.getY();
                    float directionX = e2.getX() - e1.getX();
                    if(BuildConfig.DEBUG) Log.d(TAG, "directionX:" + directionX);
                    if(BuildConfig.DEBUG) Log.d(TAG, "directionY:" + directionY);
                    if(BuildConfig.DEBUG) Log.d(TAG, "e1x:" + e1.getX());
                    if(BuildConfig.DEBUG) Log.d(TAG, "e2x:" + e2.getX());

                    if (!mainActivity.isViewingAllApps() && !scaling) {
                        if (Math.abs(directionX) > width / 3) {
                            if (directionX > 0) {
                                if(BuildConfig.DEBUG) Log.d(TAG, "swipeRight");
                                if (prefs.getBoolean("swipe-right", false)) {
                                    mainActivity.displayKissBar(true);
                                }
                            } else {
                                if(BuildConfig.DEBUG) Log.d(TAG, "swipeLeft");
                                if (prefs.getBoolean("swipe-left", false)) {
                                    mainActivity.displayContacts(true);
                                } else {
                                    // mainActivity.idNow(); TODO: beta feature, add or not
                                }
                            }
                            return true;
                        }
                        if (Math.abs(directionY) > height / 5) {

                            if (directionY < 0) {
                                if(BuildConfig.DEBUG) Log.d(TAG, "swipeUp");
                                // Fling up: display keyboard
                                if (prefs.getBoolean("swipe-up-opens-keyboard", false)) {
                                    mainActivity.showKeyboard();
                                }
                            } else {
                                if(BuildConfig.DEBUG) Log.d(TAG, "swipeDown");
                                // Fling down: display notifications
                                if (mainActivity.isKeyboardVisible()) {
                                    mainActivity.hideKeyboard();
                                } else {
                                    if (prefs.getBoolean("swipe-down-opens-notifications", false)) {
                                        displayNotificationDrawer();
                                    }
                                }
                            }
                        }


                    }
                }
                return true;
            }
        });
    }

    void onCreate() {
        adjustInputType(null);
        mainEmptyView = mainActivity.findViewById(R.id.main_empty);

    }

    void onResume() {
        if (mNumericInputTypeForced){
            mNumericInputTypeForced = false;
        }
        // Activity manifest specifies stateAlwaysHidden as windowSoftInputMode
        // so the keyboard will be hidden by default
        // we may want to display it if the setting is set
        if (isKeyboardOnStartEnabled()) {
            // Display keyboard
            mainActivity.showKeyboard();

            new Handler().postDelayed(displayKeyboardRunnable, 10);
            // For some weird reasons, keyboard may be hidden by the system
            // So we have to run this multiple time at different time
            // See https://github.com/Neamar/KISS/issues/119
            new Handler().postDelayed(displayKeyboardRunnable, 100);
            new Handler().postDelayed(displayKeyboardRunnable, 500);
        } else {
            // Not used (thanks windowSoftInputMode)
            // unless coming back from Zen Launcher settings
            if(MainActivity.isKeyboardVisible()) {
                mainActivity.hideKeyboard();
            }
        }

        if (isMinimalisticModeEnabled()) {
            mainEmptyView.setVisibility(View.GONE);

            mainActivity.list.setVerticalScrollBarEnabled(false);
            mainActivity.searchEditText.setHint("");
        }
        if (prefs.getBoolean("pref-hide-circle", false)) {
            ((ImageView) mainActivity.launcherButton).setImageBitmap(null);
            ((ImageView) mainActivity.menuButton).setImageBitmap(null);
        }
    }
    Handler handler = new Handler();
    int numberOfTaps = 0;
    long lastTapTimeMs = 0;
    long touchDownMs = 0;
    boolean onTouch(View view, MotionEvent event) {
        // Forward touch events to the gesture detector
        gd.onTouchEvent(event);
        sgd.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownMs = System.currentTimeMillis();

                break;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacksAndMessages(null);

                if ((System.currentTimeMillis() - touchDownMs) > ViewConfiguration.getLongPressTimeout()) {
                    //it was not a tap

                    numberOfTaps = 0;
                    lastTapTimeMs = 0;
                    break;
                }

                if (numberOfTaps >= 0
                        && (System.currentTimeMillis() - lastTapTimeMs) < ViewConfiguration.getDoubleTapTimeout()) {
                    numberOfTaps += 1;
                    if(BuildConfig.DEBUG) Log.d(TAG,"numberOfTaps += 1");
                } else {
                    numberOfTaps = 1;
                    if(BuildConfig.DEBUG) Log.d(TAG,"onetap");
                    Runnable onetap = new Runnable() {
                        @Override
                        public void run() {
                            if (numberOfTaps == 1 )
                                numberOfTaps = 0;
                                if (isMinimalisticModeEnabledForFavorites()) {
                                    mainActivity.favoritesBar.setVisibility(View.VISIBLE);
                                }
                                if (!onSingleTap()) {
                                    if(BuildConfig.DEBUG) Log.d(TAG,"no action for singletap, open keyboard");
                                    if (mainActivity.isViewingSearchResults()){
                                        mainActivity.displayKissBar(false);
                                    }
                                }
                            }
                        };
                    handler.postDelayed(onetap, ViewConfiguration.getDoubleTapTimeout());
                }

                lastTapTimeMs = System.currentTimeMillis();

                if (numberOfTaps == 3) {
                    if(BuildConfig.DEBUG) Log.d(TAG,"tripletap");
                    if (prefs.getBoolean("triple_tap_flashlight", false)) {
                        mainActivity.toggleFlashLight();
                    }
                    numberOfTaps = 0;
                    //handle triple tap
                } else if (numberOfTaps == 2) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(BuildConfig.DEBUG) Log.d(TAG,"doubletap");
                            if (numberOfTaps==2) {
                                onDoubleTap();
                            }
                        }
                    }, ViewConfiguration.getDoubleTapTimeout());
                }
                break;
            default:
                return false;
        }
        return false;
    }

    public boolean onSingleTap() {
        // if minimalistic mode is enabled,
        if (!scaling && isMinimalisticModeEnabled() && prefs.getBoolean("history-onclick", false)) {
            // and we're currently in minimalistic mode with no results,
            // and we're not looking at the app list
            if ((mainActivity.isViewingSearchResults()) && (mainActivity.searchEditText.getText().toString().isEmpty())) {
                if ((mainActivity.list.getAdapter() == null) || (mainActivity.list.getAdapter().isEmpty())) {
                    mainActivity.runTask(new HistorySearcher(mainActivity));
                    return true;
                }
            }
        }

        if (isMinimalisticModeEnabledForFavorites()) {
            mainActivity.favoritesBar.setVisibility(View.VISIBLE);
            return true;
        }
        return false;
    }

    public void onDoubleTap() {
        if (isKeyboardVisible()) {
            if (prefs.getBoolean("double-click-numeric-kb", false)) {
                if (mainActivity.searchEditText.getInputType() != TYPE_CLASS_PHONE) {
                    mainActivity.searchEditText.setInputType(TYPE_CLASS_PHONE);
                    mNumericInputTypeForced = true;
                } else {
                    mNumericInputTypeForced = false;
                    adjustInputType(mainActivity.searchEditText.getText().toString());
                }
            }
        } else {
            if (prefs.getBoolean("double-click-opens-apps", false)) {
                mainActivity.launcherButton.performClick();
            }
        }
    }
    public void toggleScreenOnOff() {
        // turn off/on screen backlight
        WindowManager.LayoutParams params = mainActivity.getWindow().getAttributes();
        if (params.screenBrightness == -1) {
            params.screenBrightness = 0;
        } else {
            params.screenBrightness = -1;
        }
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        mainActivity.getWindow().setAttributes(params);
    }


    void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && isKeyboardOnStartEnabled()) {
            mainActivity.showKeyboard();
        }
    }

    void onDisplayKissBar(Boolean display) {
        if (isMinimalisticModeEnabledForFavorites()) {
            if (display) {
                mainActivity.favoritesBar.setVisibility(View.VISIBLE);
            } else {
                mainActivity.favoritesBar.setVisibility(View.GONE);
            }
        }

        if (!display && isKeyboardOnStartEnabled()) {
            // Display keyboard
            mainActivity.showKeyboard();
        }
    }

    void updateSearchRecords(String query) {
        if (!mNumericInputTypeForced) {
            adjustInputType(query);
        }

        if (query.isEmpty()) {
            if (isMinimalisticModeEnabled()) {
                mainActivity.runTask(new NullSearcher(mainActivity));
                // By default, help text is displayed -- not in minimalistic mode.
                mainEmptyView.setVisibility(View.GONE);

                if (isMinimalisticModeEnabledForFavorites()) {
                    mainActivity.favoritesBar.setVisibility(View.GONE);
                }
            } else {
                mainActivity.runTask(new HistorySearcher(mainActivity));
            }
        }
    }

    // Ensure the keyboard uses the right input method
    public void adjustInputType(String currentText) {
        int currentInputType = mainActivity.searchEditText.getInputType();
        int requiredInputType;

        if (currentText != null && Pattern.matches("[+]\\d+", currentText)) {
            requiredInputType = TYPE_CLASS_PHONE;
        } else if (isNonCompliantKeyboard()) {
            requiredInputType = INPUT_TYPE_WORKAROUND;
        } else {
            requiredInputType = INPUT_TYPE_STANDARD;
        }
        if (currentInputType != requiredInputType) {
            mainActivity.searchEditText.setInputType(requiredInputType);
        }
    }
    // Ensure the keyboard uses the right input method
    public void switchInputType() {
        int currentInputType = mainActivity.searchEditText.getInputType();
        if (currentInputType!=  TYPE_CLASS_PHONE) {
            mNumericInputTypeForced = true;
            mainActivity.searchEditText.setInputType(TYPE_CLASS_PHONE);
        } else {
            int requiredInputType;
            if (isNonCompliantKeyboard()) {
                requiredInputType = INPUT_TYPE_WORKAROUND;
            } else {
                requiredInputType = INPUT_TYPE_STANDARD;
            }
            mNumericInputTypeForced = false;
            mainActivity.searchEditText.setInputType(requiredInputType);

        }
    }
    // Super hacky code to display notification drawer
    // Can (and will) break in any Android release.
    @SuppressLint("PrivateApi")
    private void displayNotificationDrawer() {
        @SuppressLint("WrongConstant") Object sbservice = mainActivity.getSystemService("statusbar");
        Class<?> statusbarManager = null;
        try {
            statusbarManager = Class.forName("android.app.StatusBarManager");
            Method showStatusBar;
            if (Build.VERSION.SDK_INT >= 17) {
                showStatusBar = statusbarManager.getMethod("expandNotificationsPanel");
            }
            else {
                showStatusBar = statusbarManager.getMethod("expand");
            }
            showStatusBar.invoke( sbservice );
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        }
    }

    private boolean isMinimalisticModeEnabled() {
        return prefs.getBoolean("history-hide", false);
    }

    private boolean isMinimalisticModeEnabledForFavorites() {
        return prefs.getBoolean("history-hide", false) && prefs.getBoolean("favorites-hide", false);
    }

    /**
     * Should we force the keyboard not to display suggestions?
     * (swiftkey is broken, see https://github.com/Neamar/KISS/issues/44)
     */
    private boolean isNonCompliantKeyboard() {
        String currentKeyboard = Settings.Secure.getString(mainActivity.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        return currentKeyboard.contains("swiftkey");
    }

    /**
     * Should the keyboard be displayed by default?
     */
    private boolean isKeyboardOnStartEnabled() {
        return prefs.getBoolean("display-keyboard", false);
    }

    public void hideKeyboard() {
        if(BuildConfig.DEBUG) Log.d(TAG,"hideKeyboard");
        if(mNumericInputTypeForced){
            mNumericInputTypeForced = false;
            adjustInputType(null);
        }
    }
}
