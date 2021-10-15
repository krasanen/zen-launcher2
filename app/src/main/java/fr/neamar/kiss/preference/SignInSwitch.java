package fr.neamar.kiss.preference;

import static fi.zmengames.zen.ZEvent.State.GOOGLE_SIGN_IN;
import static fi.zmengames.zen.ZEvent.State.GOOGLE_SIGN_OUT;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;

import fi.zmengames.zen.LauncherService;
import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.SwitchPreference;

public class SignInSwitch extends SwitchPreference {
    public SignInSwitch(Context context) {
        this(context, null);
    }

    public SignInSwitch(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchPreferenceStyle);
    }

    public SignInSwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }



    @Override
    protected void onClick() {
        if(BuildConfig.DEBUG) Log.w("SignInSwitch","isChecked:"+isChecked());

        if (!isChecked()){
            if(BuildConfig.DEBUG) Log.w("SignInSwitch","SIGN_IN2");
            Intent intent = new Intent(getContext(), LauncherService.class);
            intent.setAction(GOOGLE_SIGN_IN.toString());
            KissApplication.startLaucherService(intent,getContext());

        } else {
            if(BuildConfig.DEBUG) Log.w("SignInSwitch","SIGN_OUT2");
            Intent intent = new Intent(getContext(), LauncherService.class);
            intent.setAction(GOOGLE_SIGN_OUT.toString());
            KissApplication.startLaucherService(intent,getContext());
        }
        super.onClick();

    }
}
