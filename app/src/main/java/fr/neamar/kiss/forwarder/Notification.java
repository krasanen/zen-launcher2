package fr.neamar.kiss.forwarder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.notification.NotificationListener;

import static android.content.Context.MODE_PRIVATE;

class Notification extends Forwarder {
    private static final String TAG = Notification.class.getSimpleName();
    private final SharedPreferences notificationPreferences;

    private final SharedPreferences.OnSharedPreferenceChangeListener onNotificationDisplayed = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String packageAndTitle) {
            if (BuildConfig.DEBUG) Log.d(TAG,"onSharedPreferenceChanged:"+packageAndTitle);
            ListView list = mainActivity.list;

            // A new notification was received, iterate over the currently displayed results
            // if one of them is for the package that just received a notification,
            // update the notification dot visual if required.
            //
            // This implementation should be more efficient than calling notifyDataSetInvalidated()
            // since we only iterate over the items currently displayed in the list
            // and do not rebuild them all, just toggle visibility if required.
            // Also, it means we get to display an animation, and that's cool :D

            updateDots(list, list.getLastVisiblePosition() - list.getFirstVisiblePosition(), packageAndTitle);

            updateDots(mainActivity.favoritesBar, mainActivity.favoritesBar.getChildCount(), packageAndTitle);

        }
    };

    Notification(MainActivity mainActivity) {
        super(mainActivity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            notificationPreferences = mainActivity.getSharedPreferences(NotificationListener.NOTIFICATION_PREFERENCES_NAME, MODE_PRIVATE);
        } else {
            notificationPreferences = null;
        }
    }

    void onResume() {
        if (notificationPreferences != null) {
            notificationPreferences.registerOnSharedPreferenceChangeListener(onNotificationDisplayed);
        }
    }

    void onPause() {
        if (notificationPreferences != null) {
            notificationPreferences.unregisterOnSharedPreferenceChangeListener(onNotificationDisplayed);
        }
    }

    private void updateDots(ViewGroup vg, int childCount, String packageAndTitle) {
        for (int i = 0; i < childCount; i++) {
            View v = vg.getChildAt(i);
            final View notificationDot = v.findViewById(R.id.item_notification_count);
            if (notificationDot != null && packageAndTitle.equals(notificationDot.getTag())) {
                boolean hasNotification = notificationPreferences.contains(packageAndTitle);
                animateDot(notificationDot, hasNotification);
            }
        }
    }

    private void animateDot(final View notificationDot, Boolean hasNotification) {
        int currentVisibility = notificationDot.getVisibility();

        if(currentVisibility != View.VISIBLE && hasNotification) {
            // There is a notification and dot was not visible
            notificationDot.setVisibility(View.VISIBLE);
            notificationDot.setScaleX(0);
            notificationDot.setScaleY(0);
            notificationDot.animate().scaleX(1).scaleY(1).setListener(null);
        }
        else if(currentVisibility == View.VISIBLE && !hasNotification) {
            // There is no notification anymore, and dot was visible
            notificationDot.animate().scaleX(0).scaleY(0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    notificationDot.setVisibility(View.GONE);
                    notificationDot.setScaleX(1);
                    notificationDot.setScaleY(1);
                }
            });
        }
    }
}