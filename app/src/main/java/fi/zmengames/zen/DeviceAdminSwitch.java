package fi.zmengames.zen;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.SwitchPreference;

import static android.content.Context.DEVICE_POLICY_SERVICE;
import static fr.neamar.kiss.MainActivity.REQUEST_DEVICE_ADMIN_FROM_PREFERENCES;

public class DeviceAdminSwitch extends SwitchPreference implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = DeviceAdminSwitch.class.getSimpleName();
    private Context context;
    public DeviceAdminSwitch(Context context) {
        super(context);
    }
    public DeviceAdminSwitch(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchPreferenceStyle);

    }
    public DeviceAdminSwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onClick() {
        if (BuildConfig.DEBUG) Log.v(TAG, "onClick");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (isChecked()) {
            if (prefs.getBoolean("device-admin-switch", false)) {
                disableDeviceAdminPermission();
            }
        } else {
            enableDeviceAdminPermission();
        }
        super.onClick();
    }


    private void enableDeviceAdminPermission() {
        if (BuildConfig.DEBUG) Log.v(TAG, "enableDeviceAdminPermission");
        ComponentName compName = new ComponentName(getContext(), ZenAdmin.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Permission is needed to be able to lock device directly from Zen Launcher");
        KissApplication.getApplication(getContext()).getMainActivity().startActivityForResult(intent, REQUEST_DEVICE_ADMIN_FROM_PREFERENCES);

    }
    private void disableDeviceAdminPermission() {
        if (BuildConfig.DEBUG) Log.v(TAG, "disableDeviceAdminPermission");
        MainActivity.disableDeviceAdmin(context);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (BuildConfig.DEBUG) Log.v(TAG, "onSharedPreferenceChanged:"+s);
        if (s.equals("device-admin-permission")){
            this.setChecked(sharedPreferences.getBoolean("device-admin-permission", false));
            }
        else if (s.equals("proximity-switch-lock")){
            if (sharedPreferences.getBoolean("proximity-switch-lock", false)) {
                if (!isDeviceAdminActive()) {
                    enableDeviceAdminPermission();
                }
            }
        }
    }
    private boolean isDeviceAdminActive() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getContext().getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName compName = new ComponentName(getContext(), ZenAdmin.class);
        return devicePolicyManager.isAdminActive(compName);
    }
}
