package fr.neamar.kiss;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.UiModeManager;
import android.app.WallpaperManager;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;

import androidx.annotation.NonNull;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;


import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import fi.zmengames.zen.AppGridActivity;
import fi.zmengames.zen.DriveServiceHelper;
import fi.zmengames.zen.LauncherAppWidgetHostView;
import fi.zmengames.zen.LauncherService;
import fi.zmengames.zen.ZEvent;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.broadcast.IncomingCallHandler;
import fr.neamar.kiss.db.DBHelper;

import fr.neamar.kiss.cache.MemoryCacheHelper;
import fr.neamar.kiss.forwarder.ForwarderManager;
import fr.neamar.kiss.forwarder.Widget;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.searcher.ApplicationsSearcher;
import fr.neamar.kiss.searcher.HistorySearcher;
import fr.neamar.kiss.searcher.ContactSearcher;
import fr.neamar.kiss.searcher.QueryInterface;
import fr.neamar.kiss.searcher.QuerySearcher;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.searcher.TagsSearcher;
import fr.neamar.kiss.searcher.UntaggedSearcher;
import fr.neamar.kiss.ui.AnimatedListView;
import fr.neamar.kiss.ui.BottomPullEffectView;
import fr.neamar.kiss.ui.KeyboardScrollHider;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.ui.SearchEditText;
import fr.neamar.kiss.ui.WidgetPreferences;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.SystemUiVisibilityHelper;
import xiaofei.library.hermeseventbus.HermesEventBus;

import static android.view.HapticFeedbackConstants.LONG_PRESS;
import static fr.neamar.kiss.forwarder.Widget.WIDGET_PREFERENCE_ID;
import android.net.Uri;

public class MainActivity extends Activity implements QueryInterface, KeyboardScrollHider.KeyboardHandler, View.OnTouchListener, Searcher.DataObserver, View.OnLongClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;

    private DriveServiceHelper mDriveServiceHelper;
    private String mOpenFileId;

    private EditText mFileTitleEditText;
    private EditText mDocContentEditText;

    private static final int REQUEST_LOAD_REPLACE_TAGS = 11;
    private static final int REQUEST_LOAD_REPLACE_SETTINGS = 12;
    private static final int REQUEST_LOAD_REPLACE_SETTINGS_SAVEGAME = 13;
    public static final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 14;
    public static final int MY_PERMISSIONS_RECORD_AUDIO = 15;
    private static final int MY_PERMISSIONS_OVERLAY = 16;

    // intent data that is the conflict id.  used when resolving a conflict.
    public static final String CONFLICT_ID = "conflictId";

    // intent data that is the retry count for retrying the conflict resolution.
    public static final String RETRY_COUNT = "retrycount";
    public static final int REQUEST_BIND_APPWIDGET = 17;


    /**
     * Adapter to display records
     */
    public RecordAdapter adapter;

    /**
     * Store user preferences
     */
    public SharedPreferences prefs;

    /**
     * View for the Search text
     */
    public SearchEditText searchEditText;

    /**
     * Main list view
     */
    public AnimatedListView list;
    public View listContainer;
    /**
     * View to display when list is empty
     */
    public View emptyListView;
    /**
     * Utility for automatically hiding the keyboard when scrolling down
     */
    private KeyboardScrollHider hider;
    /**
     * Menu button
     */
    public View menuButton;
    /**
     * Zen Launcher bar
     */
    public View kissBar;
    /**
     * Favorites bar. Can be either the favorites within the Zen Launcher bar,
     * or the external favorites bar (default)
     */
    public View favoritesBar;
    /**
     * Progress bar displayed when loading
     */
    private View loaderSpinner;
    /**
     * Launcher button, can be clicked to display all apps
     */
    public View launcherButton;
    /**
     * "X" button to empty the search field
     */
    private View clearButton;

    public View numericButton;

    /**
     * Task launched on text change
     */
    private Searcher searchTask;

    /**
     * SystemUiVisibility helper
     */
    private SystemUiVisibilityHelper systemUiVisibilityHelper;

    /**
     * Is the Zen Launcher bar currently displayed?
     * (flag updated before animation is over)
     */
    private boolean isDisplayingKissBar = false;

    private PopupWindow mPopup;

    private ForwarderManager forwarderManager;
    private static boolean mKeyboardVisible;
    public static boolean mDebugJson = false;
    GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SAVE_SNAPSHOT = 51;
    private static final int RC_LOAD_SNAPSHOT = 52;
    private static final int RC_LIST_SAVED_GAMES = 53;
    private static final int RC_SIGN_IN = 54;
    private int widgetAddY;
    public int action;

    /**
     * Access instance from broadcasters
     */
    public static MainActivity instance;

    public static MainActivity getInstance() {
        return instance;
    }
    public void reloadBadges(){
        Log.d(TAG,"reloadBadges");
        adapter.reloadBadges();
        this.adapter.notifyDataSetChanged();
        forwarderManager.onFavoriteChange();
    }

    public void reloadBadge(String packageName) {
        adapter.reloadBadge(packageName);
        forwarderManager.onFavoriteChange();
    }
    public void signIn(int action) {
        if (BuildConfig.DEBUG) Log.d(TAG, "signIn");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        this.action = action;
        // The Task returned from this call is always completed, no need to attach
        // a listener.
        startActivityForResult(signInIntent, RC_SIGN_IN);

    }

    public void signOut() {
        if (BuildConfig.DEBUG) Log.d(TAG, "signOut");
        mGoogleSignInClient.signOut();

        mSignedIn = false;
    }

    private static final int RC_SAVED_GAMES = 9009;

    private void openFilePicker() {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Opening file picker.");

            Intent pickerIntent = mDriveServiceHelper.createFilePickerIntent();

            // The result of the SAF Intent is handled in onActivityResult.
            startActivityForResult(pickerIntent, REQUEST_CODE_OPEN_DOCUMENT);
        }
    }


    public static byte[] objToByte(SaveGame tcpPacket) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
        objStream.writeObject(tcpPacket);

        return byteStream.toByteArray();
    }

    public static Object byteToObj(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objStream = new ObjectInputStream(byteStream);
        return objStream.readObject();
    }

    // current save game - serializable to and from the saved game
    private SaveGame mSaveGame;


    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, new Matrix(), null);
        return bmOverlay;
    }

    /**
     * Gets a screenshot to use with snapshots. Note that in practice you probably do not want to
     * use this approach because tablet screen sizes can become pretty large and because the image
     * will contain any UI and layout surrounding the area of interest.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    Bitmap getScreenShot() {
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        View root = getWindow().getDecorView().getRootView();
        Bitmap screenshot = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap coverImage = ((BitmapDrawable) wallpaperDrawable).getBitmap();
        Bitmap combo = overlay(screenshot, coverImage);
        Canvas canvas = new Canvas(combo);
        root.draw(canvas);
        return RotateBitmap(combo, 270f);


    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    ByteArrayOutputStream getScreenShotWallPaper() {
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ((BitmapDrawable) wallpaperDrawable).getBitmap().compress(Bitmap.CompressFormat.JPEG, 80, bytes);

        return bytes;
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public boolean checkPermissionReadStorage(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            return true;
        }
        return false;
    }

    public boolean checkPermissionOverlay(Activity activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MY_PERMISSIONS_OVERLAY);
                return false;
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "overlay permission ok");
            return true;
        } else {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "overlay permission ok, sdk:" + Build.VERSION.SDK_INT);
            return true;
        }
    }

    public static void checkPermissionRecordAudio(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.RECORD_AUDIO)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        if (BuildConfig.DEBUG) Log.d(TAG, "onCreate()");
        KissApplication.getApplication(this).setMainActivity(this);
        HermesEventBus.getDefault().init(this);
        HermesEventBus.getDefault().register(this);
//        KissApplication.getApplication(this).initDataHandler();

        /*
         * Initialize preferences
         */
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        MemoryCacheHelper.updatePreferences(prefs);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile.

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
// Build the api client.

        /*
         * Initialize all forwarders
         */
        forwarderManager = new ForwarderManager(this);


        /*
         * Set the view and store all useful components
         */
        setContentView(R.layout.main);
        // Apps may notify badge updates for Samsung devices
        // through a ContentResolver on the url: content://com.sec.badge/apps
        if (SamsungBadgeObserver.providerExists(this)) {

            //Content Resolver has content, so, register for updates and load its actual content
            Uri samsungBadgeUri = Uri.parse("content://com.sec.badge/apps");
            getContentResolver()
                    .registerContentObserver(samsungBadgeUri, true, new SamsungBadgeObserver(new Handler(), this));
            SamsungBadgeObserver.loadBadges(this);
        }


        this.list = this.findViewById(android.R.id.list);
        this.listContainer = (View) this.list.getParent();
        this.emptyListView = this.findViewById(android.R.id.empty);
        this.kissBar = findViewById(R.id.mainKissbar);
        this.menuButton = findViewById(R.id.menuButton);
        this.searchEditText = findViewById(R.id.searchEditText);
        this.loaderSpinner = findViewById(R.id.loaderBar);
        this.launcherButton = findViewById(R.id.launcherButton);
        this.clearButton = findViewById(R.id.clearButton);
        this.numericButton = findViewById(R.id.numericButton);
        /*
         * Initialize components behavior
         * Note that a lot of behaviors are also initialized through the forwarderManager.onCreate() call.
         */
        displayLoader(true);

        // Add touch listener for history popup to root view
        findViewById(android.R.id.content).setOnTouchListener(this);
        findViewById(android.R.id.content).setOnLongClickListener(this);

        // add history popup touch listener to empty view (prevents on not working there)
        this.emptyListView.setOnTouchListener(this);

        // Create adapter for records
        this.adapter = new RecordAdapter(this, this, new ArrayList<Result>());
        this.list.setAdapter(this.adapter);

        this.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                adapter.onClick(position, v);
            }
        });

        this.list.setLongClickable(true);
        this.list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int pos, long id) {
                ((RecordAdapter) parent.getAdapter()).onLongClick(pos, v);
                return true;
            }
        });

        // Display empty list view when having no results
        this.adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (adapter.isEmpty()) {
                    // Display help text when no results available
                    listContainer.setVisibility(View.GONE);
                    emptyListView.setVisibility(View.VISIBLE);
                } else {
                    // Otherwise, display results
                    listContainer.setVisibility(View.VISIBLE);
                    emptyListView.setVisibility(View.GONE);
                }

                forwarderManager.onDataSetChanged();

            }
        });

        // Listen to changes
        searchEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                // Auto left-trim text.
                if (s.length() > 0 && s.charAt(0) == ' ')
                    s.delete(0, 1);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isViewingAllApps()) {
                    displayKissBar(false, false, false);
                }
                String text = s.toString();
                updateSearchRecords(text);
                displayClearOnInput();
                if (searchEditText.getText().length() != 0) {
                    list.setFastScrollAlwaysVisible(false);
                    list.setFastScrollEnabled(false);
                } else {
                    list.setFastScrollAlwaysVisible(true);
                    list.setFastScrollEnabled(true);
                }
            }
        });


        // Fixes bug when dropping onto a textEdit widget which can cause a NPE
        // This fix should be on ALL TextEdit Widgets !!!
        // See : https://stackoverflow.com/a/23483957
        searchEditText.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                return true;
            }
        });


        // On validate, launch first record
        searchEditText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == android.R.id.closeButton) {
                    systemUiVisibilityHelper.onKeyboardVisibilityChanged(false);
                    if (mPopup != null) {
                        mPopup.dismiss();
                        return true;
                    }
                    systemUiVisibilityHelper.onKeyboardVisibilityChanged(false);
                    hider.fixScroll();
                    return false;
                }
                RecordAdapter adapter = ((RecordAdapter) list.getAdapter());

                adapter.onClick(adapter.getCount() - 1, v);

                return true;
            }
        });

        registerForContextMenu(menuButton);

        // When scrolling down on the list,
        // Hide the keyboard.
        this.hider = new KeyboardScrollHider(this,
                this.list,
                (BottomPullEffectView) this.findViewById(R.id.listEdgeEffect)
        );
        this.hider.start();

        // Enable/disable phone broadcast receiver
        PackageManagerUtils.enableComponent(this, IncomingCallHandler.class, prefs.getBoolean("enable-phone-history", false));

        // Hide the "X" after the text field, instead displaying the menu button
        displayClearOnInput();

        systemUiVisibilityHelper = new SystemUiVisibilityHelper(this);

        /*
         * Defer everything else to the forwarders
         */
        forwarderManager.onCreate();
        initializeKeyboardListener();
        //if(BuildConfig.DEBUG) Log.d(TAG,">setOnDragListener");

        //findViewById(R.id.main).setOnLongClickListener(new MyOnClickListener());
        //if(BuildConfig.DEBUG) Log.d(TAG,"<setOnDragListener");

    }

    private void buildWidgetPopupMenu(final View view) {
        widgetAddY = y;
        show(this, x, y);
    }

    public void show(Activity activity, final float x, final float y) {

        final int ADD_WIDGET = 0;
        final int WIDGET_SETTINGS = 1;
        final int UPDATE_WALLPAPER = 2;

        final ViewGroup root = (ViewGroup) activity.getWindow().getDecorView().findViewById(android.R.id.content);

        final View view = new View(activity.getApplicationContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(1, 1));


        root.addView(view);

        view.setX(x);
        view.setY(y);

        PopupMenu popupExcludeMenu = new PopupMenu(activity.getApplicationContext(), view);
        //Adding menu items
        popupExcludeMenu.getMenu().add(ADD_WIDGET, Menu.NONE, Menu.NONE, R.string.menu_widget_add);
        popupExcludeMenu.getMenu().add(WIDGET_SETTINGS, Menu.NONE, Menu.NONE, R.string.menu_widget_settings);
        popupExcludeMenu.getMenu().add(UPDATE_WALLPAPER, Menu.NONE, Menu.NONE, R.string.menu_wallpaper);

        //registering popup with OnMenuItemClickListener
        popupExcludeMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getGroupId()) {
                    case ADD_WIDGET:
                        hideKeyboard();
                        forwarderManager.addWidget();
                        break;
                    case WIDGET_SETTINGS:
                        hideKeyboard();
                        forwarderManager.showWidgetSettings();
                        break;
                    case UPDATE_WALLPAPER:
                        hideKeyboard();
                        Intent intent2 = new Intent(Intent.ACTION_SET_WALLPAPER);
                        startActivity(Intent.createChooser(intent2, getString(R.string.menu_wallpaper)));
                        break;
                }
                return true;
            }
        });


        popupExcludeMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                root.removeView(view);
            }
        });

        popupExcludeMenu.show();
    }

    public void onNumericKeypadClicked(View view) {
        forwarderManager.switchInputType();
    }

    // place to put widget when long clicking on widgetlayout
    public int getWidgetAddY() {
        return widgetAddY;
    }

    public void resetWidgetAddY() {
        widgetAddY = 0;
    }

    Rect r = new Rect();

    private void initializeKeyboardListener() {
        emptyListView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                emptyListView.getWindowVisibleDisplayFrame(r);
                int screenHeight = emptyListView.getRootView().getHeight();

                // r.bottom is the position above soft keypad or device button.
                // if keypad is shown, the r.bottom is smaller than that before.
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                    mKeyboardVisible = true;
                } else {
                    mKeyboardVisible = false;
                    forwarderManager.hideKeyboard();
                }
            }
        });


    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (BuildConfig.DEBUG) Log.d(TAG, "onCreateContextMenu");

        /*ImageView image;

        image = (ImageView) findViewById(R.id.imageView);
        image.setImageResource(R.drawable.call);*/
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        forwarderManager.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check for existing Google Sign In account, if the user is already signed in
// the GoogleSignInAccount will be non-null.

        forwarderManager.onStart();

        Intent intent = new Intent(this, LauncherService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (camera!=null) {
            camera.release();
        }
        forwarderManager.onStop();
        super.onStop();
        if (mServiceBound) {
            unbindService(mServiceConnection);
            mServiceBound = false;
        }
    }


    private void updateUI(boolean signedIn) {
        if (signedIn) {
            // Sign-in OK!
            mSignedIn = true;
            prefs.edit().putBoolean("wasSigned", true).apply();
            if (BuildConfig.DEBUG) Log.d(TAG, "Sign-in successful!");
        } else {
            mSignedIn = false;
            if (BuildConfig.DEBUG) Log.d(TAG, "Not signed to Google!");
        }
    }

    public static boolean flashToggle;

    Camera camera = null;
    public void toggleFlashLight() {
        if (BuildConfig.DEBUG) Log.d(TAG, "toggleFlashLight");
        flashToggle = !flashToggle;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                CameraManager cameraManager = (CameraManager) getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
                for (String id : cameraManager.getCameraIdList()) {
                    // Turn on the flash if camera has one
                    if (cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                        cameraManager.setTorchMode(id, flashToggle);
                    }
                }
            } catch (Exception e2) {
                Toast.makeText(getApplicationContext(), "Torch Failed: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {  //Lollipop and older
            toggleFlashLightPreM(flashToggle);
        }

        if (flashToggle) {
            Toast.makeText(this, R.string.flashlight_on, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.flashlight_off, Toast.LENGTH_SHORT).show();
        }

    }

    public void toggleFlashLightPreM(boolean on){
        flashToggle = on;
        try {
            if (on) {
                camera = Camera.open();
                Camera.Parameters p = camera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(p);
                camera.startPreview();
            } else {
                if (camera!=null) {
                    camera.release();
                }
            }
        } catch (RuntimeException e){

        }
    }

    public void setNightMode(Context target, boolean state) {


        UiModeManager uiManager = (UiModeManager) target.getSystemService(Context.UI_MODE_SERVICE);

        if (state) {
            if (checkPermissionOverlay(this)) {
                uiManager.setNightMode(UiModeManager.MODE_NIGHT_YES);
                Toast.makeText(this, "Night mode on", Toast.LENGTH_SHORT).show();
                //setTheme(R.style.AppThemeDark);
                //setContentView(R.layout.main);

            } else {
                Toast.makeText(getBaseContext(), "Retry after accepting permission",
                        Toast.LENGTH_SHORT).show();
            }

        } else {
            // uiManager.disableCarMode(0);
            setContentView(R.layout.main);
            uiManager.setNightMode(UiModeManager.MODE_NIGHT_NO);
            Toast.makeText(this, "Night mode off", Toast.LENGTH_SHORT).show();
        }

    }

    private void startAppGridActivity() {
        startActivity(new Intent(this, AppGridActivity.class));
    }

    /**
     * Restart if required,
     * Hide the kissbar by default
     */
    @SuppressLint("CommitPrefEdits")
    protected void onResume() {
        if (BuildConfig.DEBUG) Log.d(TAG, "onResume()");
        reloadBadges();
        if (flashToggle){
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                toggleFlashLightPreM(flashToggle);
            }
        }
        if (mDebugJson) {
            try {
                String settings = this.getSerializedSettings2();
                if (BuildConfig.DEBUG) Log.d(TAG, "settings:" + settings);
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.d(TAG, "JSONException");
            }
        }
        if (prefs.getBoolean("require-layout-update", false)) {
            super.onResume();
            Log.i(TAG, "Restarting app after setting changes");
            // Restart current activity to refresh view, since some preferences
            // may require using a new UI
            prefs.edit().putBoolean("require-layout-update", false).apply();
            this.recreate();
            return;
        }
        if (prefs.getBoolean("bluelightfilter", false)) {
            setBlueLightFilter(true);
        }

        dismissPopup();

        if (KissApplication.getApplication(this).getDataHandler().allProvidersHaveLoaded) {
            displayLoader(false);
            onFavoriteChange();
        }

        // We need to update the history in case an external event created new items
        // (for instance, installed a new app, got a phone call or simply clicked on a favorite)
        updateSearchRecords();
        displayClearOnInput();

        if (isViewingAllApps()) {
            displayKissBar(false);
        }

        forwarderManager.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HermesEventBus.getDefault().destroy();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ZEvent event) {
        Log.w(TAG, "Got message from service: " + event.getState());

        switch (event.getState()) {
            case GOOGLE_SIGNIN:
                signIn(0);
                break;
            case GOOGLE_SIGNOUT:
                signOut();
                break;
            case LOAD_OVER:
                updateSearchRecords();
                onFavoriteChange();
                break;
            case FULL_LOAD_OVER:
                Log.v(TAG, "All providers are done loading.");
                displayLoader(false);
                onFavoriteChange();
                // Run GC once to free all the garbage accumulated during provider initialization
                System.gc();
                break;
            case SHOW_TOAST:
                Log.v(TAG, "Show toast");
                Toast.makeText(getBaseContext(), event.getText(),
                        Toast.LENGTH_LONG).show();

                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // This is called when the user press Home again while already browsing MainActivity
        // onResume() will be called right after, hiding the kissbar if any.
        // http://developer.android.com/reference/android/app/Activity.html#onNewIntent(android.content.Intent)
        // Animation can't happen in this method, since the activity is not resumed yet, so they'll happen in the onResume()
        // https://github.com/Neamar/KISS/issues/569
        if (!searchEditText.getText().toString().isEmpty()) {
            Log.i(TAG, "Clearing search field");
            searchEditText.setText("");
        }

        // Hide kissbar when coming back to kiss
        if (isViewingAllApps()) {
            displayKissBar(false);
        }

        // Close the backButton context menu
        closeContextMenu();
    }

    @Override
    public void onBackPressed() {
        if (BuildConfig.DEBUG) Log.d(TAG, "onBackPressed");
        if (mPopup != null) {
            mPopup.dismiss();
        } else if (isViewingAllApps()) {
            displayKissBar(false);
        } else {
            // If no kissmenu, empty the search bar
            // (this will trigger a new event if the search bar was already empty)
            // (which means pressing back in minimalistic mode with history displayed
            // will hide history again)
            searchEditText.setText("");
        }
        // No call to super.onBackPressed(), since this would quit the launcher.
    }

    @Override
    public boolean onKeyDown(int keycode, @NonNull KeyEvent e) {
        if (keycode == KeyEvent.KEYCODE_MENU) {
            // For devices with a physical menu button, we still want to display *our* contextual menu
            menuButton.showContextMenu();
            menuButton.performHapticFeedback(LONG_PRESS);
            return true;
        }

        return super.onKeyDown(keycode, e);
    }

    private void query() {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Querying for files.");
            showSpinner(R.string.loading_from_cloud);
            mDriveServiceHelper.queryFiles()
                    .addOnSuccessListener(fileList -> {
                        dismissSpinner();
                        final ViewGroup root = (ViewGroup) this.getWindow().getDecorView().findViewById(android.R.id.content);

                        final View view = new View(MainActivity.this);
                        view.setLayoutParams(new ViewGroup.LayoutParams(1, 1));


                        root.addView(view);


                        StringBuilder builder = new StringBuilder();
                        PopupMenu popupExcludeMenu = new PopupMenu(MainActivity.this, view);
                        int i = 0;
                        for (File file : fileList.getFiles()) {
                            //mDriveServiceHelper.deleteFile(file);
                            //builder.append(file.getName()).append("\n");
                            //Adding menu items
                            // popupExcludeMenu.getMenu().add(i, Menu.NONE, Menu.NONE, file.getId());
                            final SubMenu listSubMenu = popupExcludeMenu.getMenu().addSubMenu(i, Menu.NONE, Menu.NONE, file.getName());
                            MenuItem readMenu = listSubMenu.add(i, Menu.NONE, Menu.NONE, "Load");
                            readMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                final File fileLocal = file;

                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    readFile(fileLocal.getId());
                                    return true;
                                }
                            });
                            MenuItem deleteMenu = listSubMenu.add(i + 1, Menu.NONE, Menu.NONE, "Delete");
                            //registering popup with OnMenuItemClickListener

                            deleteMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                final File fileLocal = file;

                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    mDriveServiceHelper.deleteFile(fileLocal);
                                    return true;
                                }
                            });

                            MenuItem renameMenu = listSubMenu.add(i + 1, Menu.NONE, Menu.NONE, "Rename");
                            //registering popup with OnMenuItemClickListener

                            renameMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                final File fileLocal = file;

                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    queryNewName(fileLocal.getName(), fileLocal);
                                    return true;
                                }
                            });


                            i++;
                        }
                        popupExcludeMenu.setOnDismissListener(menu -> root.removeView(view));

                        popupExcludeMenu.show();
                    /*    popupExcludeMenu.getMenu().getItem(0).getActionView().setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                deleteFile((String) popupExcludeMenu.getMenu().getItem(0).getTitle());
                                return true;
                            }
                        }); */
                        String fileNames = builder.toString();

                        Log.d(TAG, "files:" + fileNames);


                        setReadOnlyMode();
                    })
                    .addOnFailureListener(exception -> {
                        Toast.makeText(getBaseContext(), "Unable to query files.",
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Unable to query files.", exception);
                        dismissSpinner();
                    });
        }
    }


    private void queryNewName(String name, File fileLocal) {


        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Rename File");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        builder.setView(input);
        input.setText(name);
        input.selectAll();

// Set up the buttons
        builder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDriveServiceHelper.renameFile(fileLocal, String.valueOf(input.getText())).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Rename Failed");
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (forwarderManager.onOptionsItemSelected(item)) {
            return true;
        }
        Intent intent;
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                return true;
            case R.id.wallpaper:
                hideKeyboard();
                intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                startActivity(Intent.createChooser(intent, getString(R.string.menu_wallpaper)));
                return true;
            case R.id.preferences:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.shareTags:
                intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                String serializedTags;
                try {
                    serializedTags = getSerializedTags();
                } catch (JSONException e) {
                    serializedTags = e.toString();
                }
                intent.putExtra(Intent.EXTRA_TEXT, serializedTags);
                intent.putExtra(Intent.EXTRA_SUBJECT, "Zen Launcher__tags_" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date()) + ".json");
                intent.setType("application/json");
                startActivity(Intent.createChooser(intent, getString(R.string.share_tags_chooser)));
                return true;
            case R.id.loadReplaceTags:
                intent = new Intent();
                intent.setType("application/json");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, getString(R.string.share_tags_chooser)), REQUEST_LOAD_REPLACE_TAGS);
                return true;
/*          case R.id.loadReplaceSettings:
                intent = new Intent();
                intent.setType("application/json");
                intent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(intent, getString(R.string.load_replace_tags)), REQUEST_LOAD_REPLACE_SETTINGS);
                return true;
            case R.id.shareSettings:
                intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                String serializedSettings;
                try {
                    serializedSettings = getSerializedSettings2();
                } catch (JSONException e) {
                    serializedSettings = e.toString();
                }
                String serializedWidgetSettings;
                try {
                    serializedWidgetSettings = getSerializedWidgetSettings();
                } catch (JSONException e) {
                    serializedWidgetSettings = e.toString();
                }
                intent.putExtra(Intent.EXTRA_TEXT, serializedSettings + serializedWidgetSettings);
                intent.putExtra(Intent.EXTRA_SUBJECT, "Zen Launcher_settings" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date()) + ".json");
                intent.setType("application/json");
                startActivity(Intent.createChooser(intent, getString(R.string.share_settings)));
                return true;
*/
            case R.id.saveToGoogle:
                signIn(R.id.saveToGoogle);
                return true;

            case R.id.loadFromGoogle:
                signIn(R.id.loadFromGoogle);
                return true;
            case R.id.signIn:
                if (!mSignedIn) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "signIn");
                    Intent signInIntent = new Intent(this, LauncherService.class);
                    signInIntent.setAction(LauncherService.GOOGLE_SIGN_IN);
                    KissApplication.startLaucherService(signInIntent, this);
                }
                return true;
            case R.id.nightModeOn:
                if (BuildConfig.DEBUG) Log.d(TAG, "nightModeOn");
                if (checkPermissionOverlay(this)) {
                    setBlueLightFilter(true);
                }
                return true;
            case R.id.nightModeOff:
                if (BuildConfig.DEBUG) Log.d(TAG, "nightModeOff");
                if (checkPermissionOverlay(this)) {
                    setBlueLightFilter(false);
                }
                return true;
            case R.id.appGrid:
                startAppGridActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadFromGoogle() {
        if (mSignedIn) {
            query();
            // openFilePicker();
        } else {
            Toast.makeText(getBaseContext(), "Not signed in to Google",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToGoogle() {
        if (checkPermissionReadStorage(this)) {
            if (mSignedIn) {
                createFile("" + String.valueOf(Calendar.getInstance().getTime()));
            } else {
                Toast.makeText(getBaseContext(), "Not signed in to Google",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // progress dialog we display while we're loading state from the cloud
    ProgressDialog mSpinner = null;

    void showSpinner(int resId) {
        if (mSpinner == null) {
            mSpinner = new ProgressDialog(this);
            mSpinner.setMessage(getString(resId));
        }

        mSpinner.show();

    }

    void dismissSpinner() {
        if (mSpinner != null && mSpinner.isShowing()) {
            mSpinner.dismiss();
            mSpinner = null;
        }
    }


    String getSerializedSettings2() throws JSONException {
        Map<String, ?> tags;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        tags = prefs.getAll();
        JSONObject json = new JSONObject(tags);
        for (Map.Entry<String, ?> entry : tags.entrySet()) {
            Object v = entry.getValue();
            String key = entry.getKey();
          /*  if (key.equals("favorite-apps-list")){
                json.putOpt(key, v + "_!_" + "java.util.HashSet");
            } else { */
            json.putOpt(key, v + "_!_" + v.getClass().getSimpleName());
            //}
        }


        return json.toString(1);
    }

    private String getSerializedWidgetSettings() throws JSONException {
        Map<String, ?> tags;
        SharedPreferences prefsWidget = this.getSharedPreferences(WIDGET_PREFERENCE_ID, Context.MODE_PRIVATE);
        tags = prefsWidget.getAll();

        JSONObject jsonWidget = new JSONObject(tags);
        for (Map.Entry<String, ?> entry : tags.entrySet()) {
            Object v = entry.getValue();
            String key = entry.getKey();
          /*  if (key.equals("favorite-apps-list")){
                json.putOpt(key, v + "_!_" + "java.util.HashSet");
            } else { */
            jsonWidget.putOpt(key, v + "_!_" + v.getClass().getSimpleName());
            //}
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "getSerializedWidgetSettings:" + jsonWidget.toString(1));
        return jsonWidget.toString(1);
    }


    private int loadJson(String jsonText) throws JSONException {
        int count = 0;
        TagsHandler tagsHandler = KissApplication.getApplication(this).getDataHandler().getTagsHandler();
        if (BuildConfig.DEBUG) Log.d(TAG, "jsonText:" + jsonText);
        JSONObject json = new JSONObject(jsonText);
        Iterator<String> iter = json.keys();
        SharedPreferences.Editor editor = prefs.edit();
        String booleanClassname = "Boolean";
        String stringClassname = "String";
        String hashsetClassname = "HashSet";

        while (iter.hasNext()) {
            String key = iter.next();
            String[] values = json.get(key).toString().split("_!_");
            String value = values[0];
            String classValue = values[1];

            if (BuildConfig.DEBUG)
                Log.d(TAG, "key:" + key + " value:" + value + " classValue:" + classValue);
            if (classValue.equals(booleanClassname)) {
                if (BuildConfig.DEBUG) Log.d(TAG, "putBoolean");
                editor.putBoolean(key, Boolean.parseBoolean(value));
            } else if (classValue.equals(stringClassname)) {
                if (BuildConfig.DEBUG) Log.d(TAG, "putString");
                editor.putString(key, value);
            } else if (classValue.equals(hashsetClassname)) {
                if (BuildConfig.DEBUG) Log.d(TAG, "putStringSet:" + value);
                String[] hsets = value.substring(1, value.length() - 1).split(", ");
                Set<String> hs = new HashSet<String>(Arrays.asList(hsets));
                editor.putStringSet(key, hs);
            } else if (key.equals("tags")) {
                if (BuildConfig.DEBUG) Log.d(TAG, "value:" + value);
                String toparse = value.substring(1, value.length() - 1);
                if (BuildConfig.DEBUG) Log.d(TAG, "toparse:" + toparse);
                if (!toparse.isEmpty()) {
                    String[] values2 = toparse.split(", ");
                    for (int i = 0; i < values2.length; i++) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "values2:" + values2[i]);
                        String[] app = values2[i].split("=");
                        if (BuildConfig.DEBUG) Log.d(TAG, "appId:" + app[0]);
                        if (BuildConfig.DEBUG) Log.d(TAG, "tagsForApp:" + app[1]);
                        tagsHandler.setTags(app[0], app[1]);
                    }
                }
            }
            editor.apply();
            count += 1;
        }
        editor.commit();
        //forwarderManager.onDataSetChanged();
        //KissApplication.getApplication(this).getDataHandler().getAppProvider().reload();

        return count;
    }

    private int loadWidgetJson(String jsonText) throws JSONException {
        int count = 0;
        TagsHandler tagsHandler = KissApplication.getApplication(this).getDataHandler().getTagsHandler();
        if (BuildConfig.DEBUG) Log.d(TAG, "jsonText:" + jsonText);
        JSONObject json = new JSONObject(jsonText);
        Iterator<String> iter = json.keys();
        SharedPreferences prefsWidget = this.getSharedPreferences(WIDGET_PREFERENCE_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefsWidget.edit();
        editor.clear().commit();
        String booleanClassname = "Boolean";
        String stringClassname = "String";
        String hashsetClassname = "HashSet";

        while (iter.hasNext()) {
            String key = iter.next();
            String[] values = json.get(key).toString().split("_!_");
            String value = values[0];
            String classValue = values[1];

            if (BuildConfig.DEBUG)
                Log.d(TAG, "key:" + key + " value:" + value + " classValue:" + classValue);
            if (classValue.equals(booleanClassname)) {
                if (BuildConfig.DEBUG) Log.d(TAG, "putBoolean");
                editor.putBoolean(key, Boolean.parseBoolean(value));
            } else if (classValue.equals(stringClassname)) {
                if (BuildConfig.DEBUG) Log.d(TAG, "putString");
                editor.putString(key, value);
            } else if (classValue.equals(hashsetClassname)) {
                if (BuildConfig.DEBUG) Log.d(TAG, "putStringSet:" + value);
                String[] hsets = value.substring(1, value.length() - 1).split(", ");
                Set<String> hs = new HashSet<String>(Arrays.asList(hsets));
                editor.putStringSet(key, hs);
            } else if (key.equals("tags")) {
                if (BuildConfig.DEBUG) Log.d(TAG, "value:" + value);
                String toparse = value.substring(1, value.length() - 1);
                if (BuildConfig.DEBUG) Log.d(TAG, "toparse:" + toparse);
                if (!toparse.isEmpty()) {
                    String[] values2 = toparse.split(", ");
                    for (int i = 0; i < values2.length; i++) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "values2:" + values2[i]);
                        String[] app = values2[i].split("=");
                        if (BuildConfig.DEBUG) Log.d(TAG, "appId:" + app[0]);
                        if (BuildConfig.DEBUG) Log.d(TAG, "tagsForApp:" + app[1]);
                        tagsHandler.setTags(app[0], app[1]);
                    }
                }
            }
            editor.apply();
            count += 1;
        }
        editor.commit();
        //forwarderManager.onDataSetChanged();
        //KissApplication.getApplication(this).getDataHandler().getAppProvider().reload();

        return count;
    }


    private boolean mSignedIn = false;


    /**
     * Opens a file from its {@code uri} returned from the Storage Access Framework file picker
     * initiated by
     */
    private void openFileFromFilePicker(Uri uri) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Opening " + uri.getPath());

            mDriveServiceHelper.openFileUsingStorageAccessFramework(getContentResolver(), uri)
                    .addOnSuccessListener(nameAndContent -> {
                        String name = nameAndContent.first;


                        Log.d(TAG, "name " + name);
                        getDataFromOpenedFile(nameAndContent.second);

                        // Files opened through SAF cannot be modified, except by retrieving the
                        // fileId from its metadata and updating it via the REST API. To modify
                        // files not created by your app, you will need to request the Drive
                        // Full Scope and submit your app to Google for review.
                        setReadOnlyMode();
                    })
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Unable to open file from picker.", exception));
        }
    }

    /**
     * Updates the UI to read-only mode.
     */
    private void setReadOnlyMode() {

        mOpenFileId = null;
    }

    /**
     * Updates the UI to read/write mode on the document identified by {@code fileId}.
     */
    private void setReadWriteMode(String fileId) {

        mOpenFileId = fileId;
    }

    /* Creates a new file via the Drive REST API.
     */
    private void createFile(String name) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Creating a file.");
            showSpinner(R.string.saving_to_cloud);
            mDriveServiceHelper.createFile(name)
                    .addOnSuccessListener(fileId -> saveFile(fileId, name))
                    .addOnFailureListener(exception -> {
                        Log.e(TAG, "Couldn't create file.", exception);
                        Toast.makeText(this, "Couldn't create file", Toast.LENGTH_LONG).show();
                        dismissSpinner();
                    });
        }
    }

    /**
     * Retrieves the title and content of a file identified by {@code fileId} and populates the UI.
     */
    private void readFile(String fileId) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Reading file " + fileId);
            showSpinner(R.string.loading_from_cloud);
            mDriveServiceHelper.readFile(fileId)
                    .addOnSuccessListener(nameAndContent -> {
                        String name = nameAndContent.first;
                        byte[] content = nameAndContent.second;


                        Log.d(TAG, "name " + name);


                        setReadWriteMode(fileId);

                        getDataFromOpenedFile(content);

                    })
                    .addOnSuccessListener(new OnSuccessListener<Pair<String, byte[]>>() {
                        @Override
                        public void onSuccess(Pair<String, byte[]> stringPair) {
                            dismissSpinner();
                        }
                    })
                    .addOnFailureListener(exception -> {
                        Toast.makeText(this, "Couldn't read file.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Couldn't read file.", exception);
                        dismissSpinner();
                    });

        }
    }

    private void getDataFromOpenedFile(byte[] content) {
        try {
            forwarderManager.removeWidgets();
            mSaveGame = (SaveGame) byteToObj(content);
            DBHelper.writeDatabase(mSaveGame.getDataBase(), this);
            int count = 0;
            try {
                count = loadJson(mSaveGame.getSavedSettings());
            } catch (Exception e) {
                Log.e(TAG, "can't load tags", e);
                Toast.makeText(this, "can't load tags", Toast.LENGTH_LONG).show();
            }
            Toast.makeText(this, "loaded tags for " + count + " app(s)", Toast.LENGTH_SHORT).show();
            try {
                count = loadWidgetJson(mSaveGame.getSavedWidgets());
            } catch (Exception e) {
                Log.e(TAG, "can't load widgets", e);
                Toast.makeText(this, "can't load tags", Toast.LENGTH_LONG).show();
            }
            Toast.makeText(this, "loaded widgets for " + count + " app(s)", Toast.LENGTH_LONG).show();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bMap = BitmapFactory.decodeByteArray(mSaveGame.getData(), 0, mSaveGame.getData().length);
                try {
                    getApplicationContext().setWallpaper(bMap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
        }).start();

    }

    /**
     * Saves the currently opened file created via {@link #createFile()} if one exists.
     *
     * @param fileId
     * @param unique
     */
    private void saveFile(String fileId, String unique) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Saving " + fileId);
            String fileName = unique;
            byte[] fileContent = null;
            try {
                mSaveGame = new SaveGame(getSerializedSettings2(), getSerializedWidgetSettings(), getScreenShotWallPaper(), DBHelper.getDatabaseBytes());
                fileContent = objToByte(mSaveGame);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mDriveServiceHelper.saveFile(fileId, fileName, fileContent)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            dismissSpinner();
                            Toast.makeText(getBaseContext(), "Saved",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(exception -> {
                        dismissSpinner();
                        Toast.makeText(getBaseContext(), "Unable to save",
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Unable to save file via REST.", exception);
                    });
        }
    }

    String getSerializedTags() throws JSONException {
        Map<String, String> tags = DBHelper.loadTags(this);
        JSONObject json = new JSONObject(tags);
        return json.toString(1);
    }

    String getSerializedSettings() throws JSONException {
        Map<String, ?> tags;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        tags = prefs.getAll();
        JSONObject json = new JSONObject(tags);
        for (Map.Entry<String, ?> entry : tags.entrySet()) {
            Object v = entry.getValue();
            String key = entry.getKey();
          /*  if (key.equals("favorite-apps-list")){
                json.putOpt(key, v + "_!_" + "java.util.HashSet");
            } else { */
            json.putOpt(key, v + "_!_" + v.getClass().getSimpleName());
            //}
        }
        Map<String, String> tags2 = DBHelper.loadTags(this);
        json.putOpt("tags", tags2 + "_!_" + "tags");
        return json.toString(1);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        return true;
    }

    /**
     * Display menu, on short or long press.
     *
     * @param menuButton "kebab" menu (3 dots)
     */
    public void onMenuButtonClicked(View menuButton) {
        // When the Zen Launcher bar is displayed, the button can still be clicked in a few areas (due to favorite margin)
        // To fix this, we discard any click event occurring when the kissbar is displayed
        if (!isViewingSearchResults()) {
            return;
        }
        if (!forwarderManager.onMenuButtonClicked(this.menuButton)) {
            this.menuButton.showContextMenu();
            this.menuButton.performHapticFeedback(LONG_PRESS);
        }
    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());

                    // Use the authenticated account to sign in to the Drive service.
                    GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    this, Collections.singleton(DriveScopes.DRIVE_APPDATA));
                    credential.setSelectedAccount(googleAccount.getAccount());
                    Drive googleDriveService =
                            new Drive.Builder(
                                    AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(),
                                    credential)
                                    .setApplicationName("Zen Launcher")
                                    .build();
                    updateUI(true);


                    // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                    mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                    /*mDriveServiceHelper.createFolder("Zen Launcher")
                            .addOnSuccessListener(fileId -> {
                                updateUI(true);

                            })
                            .addOnFailureListener(exception -> {
                                Log.e(TAG, "Couldn't create file.", exception);
                                updateUI(false);
                            }); */

                    if (action > 0) {
                        switch (action) {
                            case R.id.saveToGoogle:
                                saveToGoogle();
                                break;
                            case R.id.loadFromGoogle:
                                loadFromGoogle();
                                break;
                        }


                    }

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Unable to sign in.", e);
                    String error = String.format("Unable to sign in, error: %1$s", e.getMessage());

                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    updateUI(false);
                });


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);

        switch (requestCode) {

            case REQUEST_BIND_APPWIDGET:
                if (BuildConfig.DEBUG) Log.d(TAG, "REQUEST_BIND_APPWIDGET");
                if (resultCode == Activity.RESULT_OK) {
                    int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    addWidget(appWidgetId);
                }
            /*    appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(newWidgetId);
                LauncherAppWidgetHostView hostView = mAppWidgetHost.createView(mainActivity, newWidgetId, appWidgetInfo);
                hostView.setMinimumHeight(appWidgetInfo.minHeight);
                hostView.setAppWidget(newWidgetId, appWidgetInfo);
                addWidgetHostView(hostView);
                SharedPreferences.Editor widgetPrefsEditor = widgetPrefs.edit();
                widgetPrefsEditor.remove(String.valueOf(appWidgetId));
                widgetPrefsEditor.putString(String.valueOf(newWidgetId), WidgetPreferences.serialize(wp));
                widgetPrefsEditor.apply();
                refreshWidget(newWidgetId);*/
                break;
            case MY_PERMISSIONS_OVERLAY:
                if (BuildConfig.DEBUG) Log.d(TAG, "MY_PERMISSIONS_OVERLAY:" + resultCode);
                if (Build.VERSION.SDK_INT >= 23) {
                    if (Settings.canDrawOverlays(this)) {
                        setBlueLightFilter(true);
                    } else {
                        Toast.makeText(this, "Need overlay permission for this feature", Toast.LENGTH_LONG).show();
                    }
                } else {
                    setBlueLightFilter(true);
                }
                break;
            case RC_SIGN_IN:
                handleSignInResult(data);
                break;
            case REQUEST_CODE_OPEN_DOCUMENT:
                if (BuildConfig.DEBUG) Log.d(TAG, "RC_LIST_SAVED_GAMES");
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        openFileFromFilePicker(uri);
                    }
                }
                break;

            case REQUEST_LOAD_REPLACE_TAGS:
                if (BuildConfig.DEBUG) Log.d(TAG, "REQUEST_LOAD_REPLACE_TAGS");
                if (resultCode == RESULT_OK) {
                    TagsHandler tagsHandler = KissApplication.getApplication(this).getDataHandler().getTagsHandler();
                    Uri selectedFile = data.getData();
                    if (selectedFile != null) {
                        InputStream stream;
                        try {
                            stream = getContentResolver().openInputStream(selectedFile);
                        } catch (FileNotFoundException e) {
                            Log.e("TBog", "con't open file", e);
                            stream = null;
                        }
                        if (stream != null) {
                            int count = 0;
                            try {
                                String jsonText = convertStreamToString(stream);
                                JSONObject json = new JSONObject(jsonText);
                                Iterator<String> iter = json.keys();
                                while (iter.hasNext()) {
                                    String appId = iter.next();
                                    String tagsForApp = json.get(appId).toString();
                                    tagsHandler.setTags(appId, tagsForApp);
                                    count += 1;
                                }
                            } catch (Exception e) {
                                Log.e("TBog", "can't load tags", e);
                                Toast.makeText(this, "can't load tags", Toast.LENGTH_LONG).show();
                            }
                            Toast.makeText(this, "loaded tags for " + count + " app(s)", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                break;
            case REQUEST_LOAD_REPLACE_SETTINGS:
                if (BuildConfig.DEBUG) Log.d(TAG, "REQUEST_LOAD_REPLACE_SETTINGS");
                if (resultCode == RESULT_OK) {
                    Uri selectedFile = data.getData();
                    if (selectedFile != null) {
                        InputStream stream;
                        try {
                            stream = getContentResolver().openInputStream(selectedFile);
                        } catch (FileNotFoundException e) {
                            Log.e(TAG, "con't open file", e);
                            stream = null;
                        }
                        if (stream != null) {
                            int count = 0;
                            try {
                                count = loadJson(convertStreamToString(stream));
                            } catch (Exception e) {
                                Log.e(TAG, "can't load tags", e);
                                Toast.makeText(this, "can't load tags", Toast.LENGTH_LONG).show();
                            }
                            Toast.makeText(this, "loaded tags for " + count + " app(s)", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                break;

            case REQUEST_LOAD_REPLACE_SETTINGS_SAVEGAME:
                if (BuildConfig.DEBUG) Log.d(TAG, "REQUEST_LOAD_REPLACE_SETTINGS_SAVEGAME");
                int count = 0;
                try {
                    count = loadJson(data.getStringExtra("json"));
                } catch (Exception e) {
                    Log.e(TAG, "can't load tags", e);
                    Toast.makeText(this, "can't load tags", Toast.LENGTH_LONG).show();
                }
                Toast.makeText(this, "loaded tags for " + count + " app(s)", Toast.LENGTH_LONG).show();

                break;

        }
        forwarderManager.onActivityResult(requestCode, resultCode, data);
    }

    private void setBlueLightFilter(boolean b) {
        if (b) {
            prefs.edit().putBoolean("bluelightfilter", true).apply();
            Intent nighton = new Intent(this, LauncherService.class);
            nighton.setAction(LauncherService.NIGHTMODE_ON);
            KissApplication.startLaucherService(nighton, this);
        } else {
            prefs.edit().putBoolean("bluelightfilter", false).apply();
            Intent nighton = new Intent(this, LauncherService.class);
            nighton.setAction(LauncherService.NIGHTMODE_OFF);
            KissApplication.startLaucherService(nighton, this);
        }

    }


    public static String convertStreamToString(InputStream is) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } finally {
            is.close();
        }
        return sb.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        forwarderManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (forwarderManager.onTouch(view, event)) {
            return true;
        }

        if (view.getId() == searchEditText.getId()) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                searchEditText.performClick();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onLongClick(View view) {
        Log.d(TAG, "onLongClick");
        buildWidgetPopupMenu(view);
        return true;
    }

    /**
     * Clear text content when touching the cross button
     */
    @SuppressWarnings("UnusedParameters")
    public void onClearButtonClicked(View clearButton) {
        searchEditText.setText("");
    }

    /**
     * Display Zen Launcher menu
     */
    public void onLauncherButtonClicked(View launcherButton) {
        // Display or hide the Zen Launcher bar, according to current view tag (showMenu / hideMenu).
        displayKissBar(launcherButton.getTag().equals("showMenu"));
    }

    public void onContactsButtonClicked(View contactsButton) {
        // Display or hide the Zen Launcher contacts bar, according to current view tag (showMenu / hideMenu).
        displayContacts(contactsButton.getTag().equals("showMenu"));
    }

    public int x, y;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (BuildConfig.DEBUG) Log.d(TAG, "dispatchTouchEvent: " + ev.getAction());
        x = (int) ev.getX();
        y = (int) ev.getY();
        int location[] = new int[2];
        searchEditText.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];

        if (((x > viewX && x < (viewX + searchEditText.getWidth())) &&
                (y > viewY && y < (viewY + searchEditText.getHeight())))) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "dispatchTouchEvent, searchEditText " + ev.getAction());
            systemUiVisibilityHelper.onKeyboardVisibilityChanged(true);
            forwarderManager.onTouch(searchEditText, ev);
        }

        if (mPopup != null && ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            dismissPopup();
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void displayClearOnInput() {
        if (searchEditText.getText().length() > 0) {
            clearButton.setVisibility(View.VISIBLE);
            menuButton.setVisibility(View.INVISIBLE);
        } else {
            clearButton.setVisibility(View.INVISIBLE);
            menuButton.setVisibility(View.VISIBLE);
        }
    }

    public void displayLoader(Boolean display) {
        int animationDuration = getResources().getInteger(
                android.R.integer.config_longAnimTime);

        // Do not display animation if launcher button is already visible
        if (!display && launcherButton.getVisibility() == View.INVISIBLE) {
            launcherButton.setVisibility(View.VISIBLE);

            // Animate transition from loader to launch button
            launcherButton.setAlpha(0);
            launcherButton.animate()
                    .alpha(1f)
                    .setDuration(animationDuration)
                    .setListener(null);
            loaderSpinner.animate()
                    .alpha(0f)
                    .setDuration(animationDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            loaderSpinner.setVisibility(View.GONE);
                            loaderSpinner.setAlpha(1);
                        }
                    });
        } else if (display) {
            launcherButton.setVisibility(View.INVISIBLE);
            loaderSpinner.setVisibility(View.VISIBLE);
        }
    }

    public void onFavoriteChange() {
        forwarderManager.onFavoriteChange();
    }

    public void displayKissBar(Boolean display) {
        this.displayKissBar(display, true, false);
    }

    public void displayContacts(Boolean display) {
        this.displayKissBar(display, true, true);
    }

    private void displayKissBar(boolean display, boolean clearSearchText, boolean contacts) {
        dismissPopup();
        // get the center for the clipping circle
        int cx = (launcherButton.getLeft() + launcherButton.getRight()) / 2;
        int cy = (launcherButton.getTop() + launcherButton.getBottom()) / 2;

        // get the final radius for the clipping circle
        int finalRadius = Math.max(kissBar.getWidth(), kissBar.getHeight());

        if (display) {
            // Display the app list
            if (searchEditText.getText().length() != 0) {
                searchEditText.setText("");
            }
            resetTask();

            // Needs to be done after setting the text content to empty
            isDisplayingKissBar = true;

            if (contacts) {
                searchTask = new ContactSearcher(MainActivity.this);
            } else {
                searchTask = new ApplicationsSearcher(MainActivity.this);
            }
            searchTask.executeOnExecutor(Searcher.SEARCH_THREAD);

            // Reveal the bar
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !contacts) {
                int animationDuration = getResources().getInteger(
                        android.R.integer.config_shortAnimTime);

                Animator anim = ViewAnimationUtils.createCircularReveal(kissBar, cx, cy, 0, finalRadius);
                anim.setDuration(animationDuration);
                anim.start();
            }
            kissBar.setVisibility(View.VISIBLE);

            // Display the alphabet on the scrollbar (#926)
            list.setFastScrollEnabled(true);
            if (contacts) {
                list.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
                list.setFastScrollAlwaysVisible(false);
            } else {
                list.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_RIGHT);
                list.setFastScrollAlwaysVisible(true);
            }

        } else {
            isDisplayingKissBar = false;
            // Hide the bar
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int animationDuration = getResources().getInteger(
                        android.R.integer.config_shortAnimTime);

                try {
                    Animator anim = ViewAnimationUtils.createCircularReveal(kissBar, cx, cy, finalRadius, 0);
                    anim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            kissBar.setVisibility(View.GONE);
                            super.onAnimationEnd(animation);
                        }
                    });
                    anim.setDuration(animationDuration);
                    anim.start();
                } catch (IllegalStateException e) {
                    // If the view hasn't been laid out yet, we can't animate it
                    kissBar.setVisibility(View.GONE);
                }
            } else {
                // No animation before Lollipop
                kissBar.setVisibility(View.GONE);
            }

            if (clearSearchText) {
                searchEditText.setText("");
            }
        }

        forwarderManager.onDisplayKissBar(display);
    }

    public void updateSearchRecords() {
        updateSearchRecords(searchEditText.getText().toString());
    }

    /**
     * This function gets called on query changes.
     * It will ask all the providers for data
     * This function is not called for non search-related changes! Have a look at onDataSetChanged() if that's what you're looking for :)
     *
     * @param query the query on which to search
     */
    private void updateSearchRecords(String query) {
        resetTask();
        dismissPopup();

        forwarderManager.updateSearchRecords(query);

        if (query.isEmpty()) {
            systemUiVisibilityHelper.resetScroll();
        } else {
            runTask(new QuerySearcher(this, query));
        }
    }

    public void runTask(Searcher task) {
        resetTask();
        searchTask = task;
        searchTask.executeOnExecutor(Searcher.SEARCH_THREAD);
    }

    public void resetTask() {
        if (searchTask != null) {
            searchTask.cancel(true);
            searchTask = null;
        }
    }
    protected void onPause() {
        super.onPause();
        instance = null;
    }
    /**
     * Call this function when we're leaving the activity after clicking a search result
     * to clear the search list.
     * We can't use onPause(), since it may be called for a configuration change
     */
    @Override
    public void launchOccurred() {
        // We selected an item on the list,
        // now we can cleanup the filter:
        if (!searchEditText.getText().toString().isEmpty()) {
            searchEditText.setText("");
            displayClearOnInput();
            hideKeyboard();
        } else if (isViewingAllApps()) {
            displayKissBar(false);
        }
    }

    public void registerPopup(ListPopup popup) {
        if (mPopup == popup)
            return;
        dismissPopup();
        mPopup = popup;
        popup.setVisibilityHelper(systemUiVisibilityHelper);
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                MainActivity.this.mPopup = null;
            }
        });
        hider.fixScroll();
    }

    public void refreshWidget(int appWidgetId) {
        Intent intent = new Intent();
        intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        forwarderManager.onActivityResult(Widget.REQUEST_REFRESH_APPWIDGET, RESULT_OK, intent);
    }

    public void addWidget(int appWidgetId) {
        Intent intent = new Intent();
        intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        forwarderManager.onActivityResult(Widget.REQUEST_CREATE_APPWIDGET, RESULT_OK, intent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        systemUiVisibilityHelper.onWindowFocusChanged(hasFocus);
        forwarderManager.onWindowFocusChanged(hasFocus);
    }


    public void showKeyboard() {
        searchEditText.requestFocus();
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        assert mgr != null;
        mgr.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);

        systemUiVisibilityHelper.onKeyboardVisibilityChanged(true);
    }

    @Override
    public void hideKeyboard() {
        if (isKeyboardVisible()) {
            // Check if no view has focus:
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
                //noinspection ConstantConditions
                inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
            systemUiVisibilityHelper.onKeyboardVisibilityChanged(false);
            dismissPopup();
        }
    }

    @Override
    public void applyScrollSystemUi() {
        systemUiVisibilityHelper.applyScrollSystemUi();
    }

    /**
     * Check if history / search or app list is visible
     *
     * @return true of history, false on app list
     */
    public boolean isViewingSearchResults() {
        return !isDisplayingKissBar;
    }

    public boolean isViewingAllApps() {
        return isDisplayingKissBar;
    }

    @Override
    public void beforeListChange() {
        list.prepareChangeAnim();
    }

    @Override
    public void afterListChange() {
        list.animateChange();
    }

    public void dismissPopup() {
        if (mPopup != null)
            mPopup.dismiss();
    }

    public void showMatchingTags(String tag) {
        runTask(new TagsSearcher(this, tag));

        clearButton.setVisibility(View.VISIBLE);
        menuButton.setVisibility(View.INVISIBLE);
    }

    public void showUntagged() {
        runTask(new UntaggedSearcher(this));

        clearButton.setVisibility(View.VISIBLE);
        menuButton.setVisibility(View.INVISIBLE);
    }

    public void showHistory() {
        runTask(new HistorySearcher(this));

        clearButton.setVisibility(View.VISIBLE);
        menuButton.setVisibility(View.INVISIBLE);
    }

    public static boolean isKeyboardVisible() {
        return mKeyboardVisible;
    }


    public void onWallpaperScroll(float fCurrent) {
        forwarderManager.onWallpaperScroll(fCurrent);
    }

    boolean mServiceBound = false;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mServiceBound = true;
        }
    };

    public void onClick(View view) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onClick:" + view.getTag());
        searchEditText.setText((CharSequence) view.getTag());
        //displayKissBar(false,false,false);
    }


    //
    //
    // TODO: https://acoustid.org/api-key ZlN4KdPFMn
    /*Zen Launcher 1.0 FkPa1JhL2a
    private boolean saveSharedPreferencesToFile(File dst) {
        boolean res = false;
        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(new FileOutputStream(dst));
            SharedPreferences pref =
                    getSharedPreferences(prefName, MODE_PRIVATE);
            output.writeObject(pref.getAll());

            res = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return res;
    }

    @SuppressWarnings({ "unchecked" })
    private boolean loadSharedPreferencesFromFile(JSONObject src) {
        boolean res = false;
        ObjectInputStream input = null;
        try {


            prefEdit.clear();
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean)
                    prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
                else if (v instanceof Float)
                    prefEdit.putFloat(key, ((Float) v).floatValue());
                else if (v instanceof Integer)
                    prefEdit.putInt(key, ((Integer) v).intValue());
                else if (v instanceof Long)
                    prefEdit.putLong(key, ((Long) v).longValue());
                else if (v instanceof String)
                    prefEdit.putString(key, ((String) v));
            }
            prefEdit.commit();
            res = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return res;
    }
    */
}
