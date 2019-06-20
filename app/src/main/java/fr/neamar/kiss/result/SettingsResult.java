package fr.neamar.kiss.result;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import fi.zmengames.zen.LauncherService;
import fi.zmengames.zen.ZEvent;
import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.SettingsPojo;
import fr.neamar.kiss.utils.FuzzyScore;

public class SettingsResult extends Result {
    private final SettingsPojo settingPojo;
    private static final String TAG = SettingsResult.class.getSimpleName();
    SettingsResult(SettingsPojo settingPojo) {
        super(settingPojo);
        this.settingPojo = settingPojo;
    }

    @Override
    public View display(Context context, int position, View v, FuzzyScore fuzzyScore) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_setting);

        TextView settingName = v.findViewById(R.id.item_setting_name);
        displayHighlighted(settingPojo.normalizedName, settingPojo.getName(), fuzzyScore, settingName, context);

        ImageView settingIcon = v.findViewById(R.id.item_setting_icon);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean("icons-hide", false)) {
            settingIcon.setImageDrawable(getDrawable(context));
            settingIcon.setColorFilter(getThemeFillColor(context), Mode.SRC_IN);
        } else {
            settingIcon.setImageDrawable(null);
        }

        return v;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Drawable getDrawable(Context context) {
        if (settingPojo.icon != -1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return context.getDrawable(settingPojo.icon);
            } else {
                return context.getResources().getDrawable(settingPojo.icon);
            }
        }
        return null;
    }

    @Override
    public void doLaunch(Context context, View v) {
        Intent intent = new Intent(settingPojo.settingName);
        if (BuildConfig.DEBUG) Log.d(TAG,"settingPojo.settingName:"+settingPojo.settingName);
        if (BuildConfig.DEBUG) Log.d(TAG,"settingPojo.packageName:"+settingPojo.packageName);
        if (!settingPojo.packageName.isEmpty()) {
            intent.setClassName(settingPojo.packageName, settingPojo.settingName);
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            intent.setSourceBounds(v.getClipBounds());
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (settingPojo.settingName.contains("com.zmengames.zenlauncher")) {
            try {
                ZEvent event = new ZEvent(ZEvent.State.INTERNAL_EVENT, settingPojo.settingName);
                EventBus.getDefault().post(event);
            } catch (Exception e) {

                e.printStackTrace();
                Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
            }
        } else {
            try {
                Intent launchIntent = new Intent(v.getContext(), LauncherService.class);
                launchIntent.setAction(LauncherService.LAUNCH_INTENT);
                launchIntent.putExtra(Intent.EXTRA_INTENT, intent);
                KissApplication.startLaucherService(launchIntent, v.getContext());

            } catch (Exception e) {

                e.printStackTrace();
                Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
            }
        }
    }
}
