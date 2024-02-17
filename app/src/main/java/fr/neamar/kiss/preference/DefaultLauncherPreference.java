package fr.neamar.kiss.preference;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import fr.neamar.kiss.DummyActivity;
import fr.neamar.kiss.R;

/**
 * A Dialog Preference that allows the User to change the default launcher
 */
public class DefaultLauncherPreference extends DialogPreference {

    public interface AsyncTaskCompleteListener {
        void onTaskComplete(boolean result, Activity activity);
    }

    public static class ResolveDefaultLauncherTask extends AsyncTask<Activity, Void, Boolean> {
        private Activity activity;
        private AsyncTaskCompleteListener listener;

        public ResolveDefaultLauncherTask(AsyncTaskCompleteListener listener, Activity activity) {
            this.listener = listener;
            this.activity = activity;
        }

        @Override
        protected Boolean doInBackground(Activity... params) {
            if (params.length > 0) {
                activity = params[0];
            }
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            final ResolveInfo res = activity.getPackageManager().resolveActivity(intent, 0);
            return res.activityInfo != null && activity.getPackageName()
                    .equals(res.activityInfo.packageName);
        }

        @Override
        protected void onPostExecute(Boolean isDefault) {
            if (listener != null) {
                listener.onTaskComplete(isDefault, activity);
            }
        }
    }

    public DefaultLauncherPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    private static final String TAG = DefaultLauncherPreference.class.getSimpleName();

    public static void selectLauncher(Context context){
        // get context (in order to avoid multiple get() calls)


        // get packet manager
        PackageManager packageManager = context.getPackageManager();
        // get dummyActivity
        ComponentName componentName = new ComponentName(context, DummyActivity.class);
        // enable dummyActivity (it starts disabled in the manifest.xml)
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        // create a new (implicit) intent with MAIN action
        Intent intent = new Intent(Intent.ACTION_MAIN);
        // add HOME category to it
        intent.addCategory(Intent.CATEGORY_HOME);
        // launch intent

        try {
            ComponentName resolverComponent = intent.resolveActivity(packageManager);
            Log.e(TAG,"resolverComponent: "+resolverComponent);
            Log.e(TAG,"packageName: "+resolverComponent.getPackageName());
            if (resolverComponent.getPackageName().contains("android")) {
                context.startActivity(intent);
            } else {
                Toast.makeText(context, R.string.error_setting_default, Toast.LENGTH_LONG).show();
            }
            // disable dummyActivity once again
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        } catch (Exception e){
            Log.e(TAG,"exception:"+e);
            Toast.makeText(context, R.string.error_setting_default, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        if (which == DialogInterface.BUTTON_POSITIVE) {
            selectLauncher(getContext());
        }
    }
}
