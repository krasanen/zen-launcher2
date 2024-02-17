package fr.neamar.kiss.forwarder;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.DragEvent;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.result.AppResult;
import fr.neamar.kiss.result.ContactsResult;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.result.ShortcutsResult;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.ui.ShapedContactBadge;

public class Favorites extends Forwarder implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener, View.OnDragListener {
    private static final String TAG = Favorites.class.getSimpleName();

    // Package used by Android when an Intent can be matched with more than one app
    public static final String DEFAULT_RESOLVER = "com.android.internal.app.ResolverActivity";

    /**
     * IDs for the favorites buttons
     */
    private final ArrayList<RelativeLayout> favoritesViews = new ArrayList<RelativeLayout>();

    /**
     * Currently displayed favorites
     */
    private ArrayList<Pojo> favoritesPojo = new ArrayList<>();

    /**
     * Globals for drag and drop support
     */
    private static long startTime = 0; // Start of the drag and drop, used for long press menu
    private float currentX = 0.0f; // Current X position of the drag op, this is 0 on DRAG END so we keep a copy here
    private Pojo overApp; // the view for the DRAG_END event is typically wrong, so we store a reference of the last dragged over app.

    /**
     * Configuration for drag and drop
     */
    private final int MOVE_SENSITIVITY = 5; // How much you need to move your finger to be considered "moving"
    private final int LONG_PRESS_DELAY = 250; // How long to hold your finger in place to trigger the app menu.

    // Use so we don't over process on the drag events.
    private boolean mDragEnabled = true;
    private boolean isDragging = false;
    private boolean contextMenuShown = false;
    private int favCount = -1;

    Favorites(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        if (isExternalFavoriteBarEnabled()) {
            mainActivity.favoritesBar = mainActivity.findViewById(R.id.externalFavoriteBar);
            // Hide the embedded bar
            mainActivity.findViewById(R.id.embeddedFavoritesBar).setVisibility(View.INVISIBLE);
        } else {
            mainActivity.favoritesBar = mainActivity.findViewById(R.id.embeddedFavoritesBar);
            // Hide the external bar
            mainActivity.findViewById(R.id.externalFavoriteBar).setVisibility(View.GONE);
        }

        if (prefs.getBoolean("firstRun", true)) {
            if (BuildConfig.DEBUG) Log.d(TAG,"firstRun");
            // It is the first run. Make sure this is not an update by checking if history is empty
            if (DBHelper.getHistoryLength(mainActivity) == 0) {
                new ResolveAndAddDefaultsToFavoritesTask().execute();
                checkBarCodeReaderStatus();
            }
            // set flag to false
            prefs.edit().putBoolean("firstRun", false).apply();
        }
    }

    private class ResolveAndAddDefaultsToFavoritesTask extends AsyncTask<Void, Void, String[]> {

        @Override
        protected String[] doInBackground(Void... params) {
            return addDefaultAppsToFavs();
        }

        @Override
        protected void onPostExecute(String[] favorites) {
            if (favorites != null) {
                DataHandler dataHandler =  KissApplication.getApplication(mainActivity).getDataHandler();
                for (String favorite : favorites) {
                    if (dataHandler!= null)
                        dataHandler.addToFavorites(favorite);
                }
            }
        }
    }


    private void checkBarCodeReaderStatus() {
        // Zen barcode reader
        String id = "setting://com.zmengames.zenlauncher.barcode_reader";
        DataHandler dataHandler = KissApplication.getApplication(mainActivity).getDataHandler();
        int pos = dataHandler.getFavoritePosition(id);
        if (BuildConfig.DEBUG) Log.d(TAG,"pos:"+pos);
        if (dataHandler.getFavoritePosition(id) == -1) {
            KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites(id);
        }
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        Bitmap bitmap;
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            // Single color bitmap will be created of 1x1 pixel
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    void onFavoriteChange() {
        favoritesPojo = KissApplication.getApplication(mainActivity).getDataHandler()
                .getFavorites();

        favCount = favoritesPojo.size();
        LayoutInflater layoutInflater = null;

        // Don't look for items after favIds length, we won't be able to display them
        for (int i = 0; i < favoritesPojo.size(); i++) {
            Pojo pojo = favoritesPojo.get(i);

            ImageView image;
            TextView textView = null;
            boolean favbarAppNames = prefs.getBoolean("enable-favbar-appnames", true);
            if (favoritesViews.size() <= i) {
                if (layoutInflater == null) {
                    layoutInflater = (LayoutInflater) mainActivity.favoritesBar.getContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                }
                assert layoutInflater != null;
                RelativeLayout layout = (RelativeLayout) layoutInflater.inflate(R.layout.favorite_item, mainActivity.favoritesBar, false);
                image = layout.findViewById(R.id.favorite_item_image);
                image.setTag(i);
                image.setOnDragListener(this);
                image.setOnTouchListener(this);
                image.setFocusable(true);
                image.setClickable(true);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                textView = layout.findViewById(R.id.favorite_item_text);
                textView.setText(pojo.getName());
                mainActivity.favoritesBar.addView(layout);
                favoritesViews.add(layout);
             } else {
                image =  favoritesViews.get(i).findViewById(R.id.favorite_item_image);
                textView = favoritesViews.get(i).findViewById(R.id.favorite_item_text);
            }

            Result result = Result.fromPojo(mainActivity, pojo);
            Drawable drawable = result.getDrawable(mainActivity);
            if (drawable != null) {
                if (result instanceof ContactsResult) {
                    drawable = ShapedContactBadge.getShapedDrawable(mainActivity, drawable);
                }
                image.setImageDrawable(drawable);
            } else {
                if (result instanceof ShortcutsResult) {
                    image.setImageResource(R.drawable.ic_open_in_browser_24px);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        image.setColorFilter(mainActivity.getColor(R.color.zenlauncher));
                    }
                } else {
                    // Use a placeholder if no drawable found
                    image.setImageResource(R.drawable.ic_z);
                }
            }
            if (favbarAppNames) {
                image.setPadding(10,0,10,0);
                textView.setVisibility(View.VISIBLE);
                int size;
                try {
                    size = Integer.parseInt(prefs.getString("favorite-text-size", "12"));
                } catch (NumberFormatException e){
                    size = 12;
                }
                textView.setTextSize(size);
                textView.setHeight(size);
                if (pojo instanceof ShortcutPojo){
                    String name = pojo.getName().replaceFirst("https://","");
                    name = name.replaceFirst("www.","");
                    textView.setText(name);
                } else {
                    textView.setText(pojo.getName());
                }

            } else {
                if (isExternalFavoriteBarEnabled()) {
                    image.setPadding(10, 0, 10, 25);
                } else {
                    image.setPadding(10, 10, 10, 10);
                }
                textView.setVisibility(View.GONE);
            }
            image.setVisibility(View.VISIBLE);
            image.setContentDescription(pojo.getName());
            favoritesViews.get(i).setVisibility(View.VISIBLE);
            TextView badgeView = (favoritesViews.get(i).findViewById(R.id.fav_item_badge_count));
            if (pojo.getBadgeCount() > 0) {
                badgeView.setText(pojo.getBadgeText());
                badgeView.setVisibility(View.VISIBLE);
                badgeView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int width = badgeView.getMeasuredWidth();
                        int height = badgeView.getMeasuredHeight();
                        if (isExternalFavoriteBarEnabled()) {
                            badgeView.setX(getBitmapOffset(image)[2] - (width));
                            badgeView.setY(getBitmapOffset(image)[3] - (height));
                        } else {
                            badgeView.setX(getBitmapOffset(image)[2] - (width>>1));
                        }
                    }
                });

            } else {
                badgeView.setVisibility(View.GONE);
            }


            TextView notificationView = (favoritesViews.get(i).findViewById(R.id.fav_item_notification_count));
            if (pojo.getNotificationCount()>0) {
                notificationView.setVisibility(View.VISIBLE);

                notificationView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int width = notificationView.getMeasuredWidth();
                        if (isExternalFavoriteBarEnabled()) {
                            notificationView.setX(getBitmapOffset(image)[2] - (width));

                        } else {
                            notificationView.setX(getBitmapOffset(image)[2] - (width>>1));
                        }
                    }
                });

            } else {
                notificationView.setVisibility(View.GONE);
            }

            if (result instanceof AppResult) {
                String packageName = ((AppResult) result).getPackageName();
                notificationView.setTag(packageName);
            } else if (result instanceof ContactsResult){
                notificationView.setTag(pojo.getNotificationPackage()+result.pojo.getName());
            }

        }

        // Hide empty favorites (not enough favorites yet)
        for (int i = favoritesPojo.size(); i < favoritesViews.size(); i++) {
            favoritesViews.get(i).setVisibility(View.GONE);
        }

        mDragEnabled = favCount > 1;
    }
    public static int[] getBitmapOffset(ImageView img) {
        int[] offset = new int[4];
        float[] values = new float[9];
        Matrix m = img.getImageMatrix();
        m.getValues(values);
        offset[0] = (int) values[Matrix.MTRANS_Y];
        offset[1] = (int) values[Matrix.MTRANS_X];

        offset[2] = offset[1] + (int) (values[Matrix.MSCALE_X]*img.getDrawable().getIntrinsicWidth());
        offset[3] = offset[0] + (int) (values[Matrix.MSCALE_Y]*img.getDrawable().getIntrinsicHeight());


        return offset;
    }
    void updateSearchRecords(String query) {
        if (query.isEmpty()) {
            mainActivity.favoritesBar.setVisibility(View.VISIBLE);
        } else {
            mainActivity.favoritesBar.setVisibility(View.GONE);
        }
    }

    void onDataSetChanged() {
        // Do not display the external bar when viewing all apps
        if(mainActivity.isViewingAllApps() && isExternalFavoriteBarEnabled()) {
            mainActivity.favoritesBar.setVisibility(View.GONE);
        }
    }

    /**
     * On first run, fill the favorite bar with sensible defaults
     */
    private String[] addDefaultAppsToFavs() {
        List<String> defaultApps = new ArrayList<>();
        {
            //Default phone call app
            Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
            phoneIntent.setData(Uri.parse("tel:0000"));
            ResolveInfo resolveInfo = mainActivity.getPackageManager().resolveActivity(phoneIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null) {
                String packageName = resolveInfo.activityInfo.packageName;
                if (BuildConfig.DEBUG) Log.i(TAG, "Dialer resolves to:" + packageName + "/" + resolveInfo.activityInfo.name);

                if ((resolveInfo.activityInfo.name != null) && (!resolveInfo.activityInfo.name.equals(DEFAULT_RESOLVER))) {
                    String activityName = resolveInfo.activityInfo.name;
                    if(packageName.equals("com.google.android.dialer")) {
                        // Default dialer has two different activities, one when calling a phone number and one when opening the app from the launcher.
                        // (com.google.android.apps.dialer.extensions.GoogleDialtactsActivity vs. com.google.android.dialer.extensions.GoogleDialtactsActivity)
                        // (notice the .apps. in the middle)
                        // The phoneIntent above resolve to the former, which isn't exposed as a Launcher activity and thus can't be displayed as a favorite
                        // This hack uses the correct resolver when the application id is the default dialer.
                        // In terms of maintenance, if Android was to change the name of the phone's main resolver, the favorite would simply not appear
                        // and we would have to update the String below to the new default resolver
                        activityName = "com.google.android.dialer.extensions.GoogleDialtactsActivity";
                    }
                    defaultApps.add("app://" + packageName + "/" + activityName);
                } else {
                    ResolveInfo resolveInfoSystem = mainActivity.getPackageManager().resolveActivity(phoneIntent, PackageManager.MATCH_SYSTEM_ONLY);
                    if (resolveInfoSystem != null) {
                        String packageNameSystem = resolveInfoSystem.activityInfo.packageName;
                        if (BuildConfig.DEBUG) Log.i(TAG, "Dialer (system) resolves to:" + packageNameSystem + "/" + resolveInfoSystem.activityInfo.name);
                        if ((resolveInfoSystem.activityInfo.name != null) && (!resolveInfoSystem.activityInfo.name.equals(DEFAULT_RESOLVER))) {
                            String activityName = resolveInfoSystem.activityInfo.name;
                            if (packageNameSystem.equals("com.google.android.dialer")) {
                                // Default dialer has two different activities, one when calling a phone number and one when opening the app from the launcher.
                                // (com.google.android.apps.dialer.extensions.GoogleDialtactsActivity vs. com.google.android.dialer.extensions.GoogleDialtactsActivity)
                                // (notice the .apps. in the middle)
                                // The phoneIntent above resolve to the former, which isn't exposed as a Launcher activity and thus can't be displayed as a favorite
                                // This hack uses the correct resolver when the application id is the default dialer.
                                // In terms of maintenance, if Android was to change the name of the phone's main resolver, the favorite would simply not appear
                                // and we would have to update the String below to the new default resolver
                                activityName = "com.google.android.dialer.extensions.GoogleDialtactsActivity";
                            }
                            defaultApps.add("app://" + packageNameSystem + "/" + activityName);
                        }
                    }
                }
            }
        }
        {
            //Default contacts app
            Intent contactsIntent = new Intent(Intent.ACTION_DEFAULT, ContactsContract.Contacts.CONTENT_URI);
            ResolveInfo resolveInfo = mainActivity.getPackageManager().resolveActivity(contactsIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null) {
                String packageName = resolveInfo.activityInfo.packageName;
                if (BuildConfig.DEBUG) Log.i(TAG, "Contacts resolves to:" + packageName);
                if (resolveInfo.activityInfo.name != null && !resolveInfo.activityInfo.name.equals(DEFAULT_RESOLVER)) {
                    defaultApps.add("app://" + packageName + "/" + resolveInfo.activityInfo.name);
                }
            }

        }
        {
            //Default browser
            Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://"));
            ResolveInfo resolveInfo = mainActivity.getPackageManager().resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null) {
                String packageName = resolveInfo.activityInfo.packageName;
                if (BuildConfig.DEBUG) Log.i(TAG, "Browser resolves to:" + packageName);

                if ((resolveInfo.activityInfo.name != null) && (!resolveInfo.activityInfo.name.equals(DEFAULT_RESOLVER))) {
                    String activityName = resolveInfo.activityInfo.name;
                    if(packageName.equalsIgnoreCase("com.android.chrome")) {
                        // Chrome has two different activities, one for Launcher and one when opening an URL.
                        // The browserIntent above resolve to the latter, which isn't exposed as a Launcher activity and thus can't be displayed
                        // This hack uses the correct resolver when the application is Chrome.
                        // In terms of maintenance, if Chrome was to change the name of the main resolver, the favorite would simply not appear
                        // and we would have to update the String below to the new default resolver
                        activityName = "com.google.android.apps.chrome.Main";
                    }
                    defaultApps.add("app://" + packageName + "/" + activityName);
                }
            }
        }
        {
            //Default camera
            Intent camera = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            ResolveInfo resolveInfo = mainActivity.getPackageManager().resolveActivity(camera, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null) {
                String packageName = resolveInfo.activityInfo.packageName;
                Log.i(TAG, "Camera resolves to:" + packageName + "/"+resolveInfo.activityInfo.name);
                if ((resolveInfo.activityInfo.name != null) && (!resolveInfo.activityInfo.name.equals(DEFAULT_RESOLVER))) {
                    Intent launchIntent = mainActivity.getPackageManager().getLaunchIntentForPackage(resolveInfo.activityInfo.packageName);
                    if (launchIntent != null && launchIntent.getComponent()!=null) {
                        String activityName = launchIntent.getComponent().getClassName();
                        if (BuildConfig.DEBUG) {
                            Log.i(TAG, "Adding camera:" + packageName + "/" + activityName);
                        }
                        defaultApps.add ("app://" + packageName + "/" + activityName);
                    }
                }
            }
        }
        return defaultApps.toArray(new String[0]);
    }



    private Result getFavResult(int favNumber) {
        if (favNumber >= favoritesPojo.size()) {
            // Clicking on a favorite before everything is loaded.
            if (BuildConfig.DEBUG) Log.i(TAG, "Clicking on an uninitialized favorite.");
            return null;
        }

        // Favorites handling
        Pojo pojo = favoritesPojo.get(favNumber);
        return Result.fromPojo(mainActivity, pojo);
    }

    @Override
    public void onClick(View v) {
        if (BuildConfig.DEBUG) Log.i(TAG,"onClick");
        int favNumber = (int) v.getTag();
        final Result result = getFavResult(favNumber);
        result.fastLaunch(mainActivity, v);
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

    }

    @Override
    public boolean onLongClick(View v) {
        int favNumber = (int) v.getTag();
        final Result result = getFavResult(favNumber);
        ListPopup popup = result.getPopupMenu(mainActivity, mainActivity.adapter, v);
        mainActivity.registerPopup(popup);
        popup.show(v);
        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        return true;
    }

    private boolean isExternalFavoriteBarEnabled() {
        return prefs.getBoolean("enable-favorites-bar", true);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            startTime = motionEvent.getEventTime();
            contextMenuShown = false;
            return true;
        }
        // No need to do the extra work
        if(isDragging) {
            return true;
        }

        // Click handlers first
        long holdTime = motionEvent.getEventTime() - startTime;
        if (holdTime < LONG_PRESS_DELAY && motionEvent.getAction() == MotionEvent.ACTION_UP) {
            this.onClick(view);
            return true;
        }
        if(!contextMenuShown && holdTime > LONG_PRESS_DELAY) {
            contextMenuShown = true;
            this.onLongClick(view);
            return true;
        }

        // Drag handlers
        int intCurrentY = Math.round(motionEvent.getY());
        int intCurrentX = Math.round(motionEvent.getX());
        int intStartY = motionEvent.getHistorySize() > 0 ? Math.round(motionEvent.getHistoricalY(0)) : intCurrentY;
        int intStartX = motionEvent.getHistorySize() > 0 ? Math.round(motionEvent.getHistoricalX(0)) : intCurrentX;
        boolean hasMoved = (Math.abs(intCurrentX - intStartX) > MOVE_SENSITIVITY) || (Math.abs(intCurrentY - intStartY) > MOVE_SENSITIVITY);

        if (hasMoved && mDragEnabled) {
            mDragEnabled = false;
            MainActivity.dismissPopup();
            mainActivity.closeContextMenu();
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            view.startDrag(null, shadowBuilder, view, 0);
            view.setVisibility(View.INVISIBLE);
            return true;
        }

        return false;
    }

    @Override
    public boolean onDrag(View v, final DragEvent event) {
        int overFavIndex;

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                isDragging = true;
                break;

            case DragEvent.ACTION_DRAG_ENTERED:
            case DragEvent.ACTION_DRAG_EXITED:
            case DragEvent.ACTION_DROP:
                if (!isDragging) {
                    return true;
                }

                overFavIndex = (int) v.getTag();
                overApp = favoritesPojo.get(overFavIndex);

                currentX = (event.getX() != 0.0f) ? event.getX() : currentX;
                break;

            case DragEvent.ACTION_DRAG_ENDED:
                // Only need to handle this action once.
                if(!isDragging) {
                    return true;
                }
                isDragging = false;

                // Reset dragging to what it should be
                mDragEnabled = favCount > 1;

                final View draggedView = (View) event.getLocalState();
                if (draggedView == null){
                    break;
                }

                // Sometimes we don't trigger onDrag over another app, in which case just drop.
                if (overApp == null) {
                    if (BuildConfig.DEBUG) Log.i(TAG, "Wasn't dragged over an app, returning app to starting position");
                    draggedView.post(new Runnable() {
                        @Override
                        public void run() {
                            draggedView.setVisibility(View.VISIBLE);
                        }
                    });
                    break;
                }

                int draggedFavIndex = (int) draggedView.getTag();
                final Pojo draggedApp = favoritesPojo.get(draggedFavIndex);

                final int pos = KissApplication.getApplication(mainActivity).getDataHandler().getFavoritePosition(overApp.id);
                draggedView.post(new Runnable() {
                    @Override
                    public void run() {
                        // Signals to a View that the drag and drop operation has concluded.
                        // If event result is set, this means the dragged view was dropped in target
                        if (event.getResult()) {
                            KissApplication.getApplication(mainActivity).getDataHandler().setFavoritePosition(mainActivity, draggedApp.id, pos);
                            mainActivity.onFavoriteChange();
                        } else {
                            draggedView.setVisibility(View.VISIBLE);
                        }
                    }
                });

                break;
            default:
                break;
        }
        return true;
    }

}

