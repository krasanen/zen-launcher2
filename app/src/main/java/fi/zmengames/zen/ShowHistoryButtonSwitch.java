package fi.zmengames.zen;


import static fi.zmengames.zen.ZEvent.State.HIDE_HISTORYBUTTON;
import static fi.zmengames.zen.ZEvent.State.SHOW_HISTORYBUTTON;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.greenrobot.eventbus.EventBus;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.SwitchPreference;

public class ShowHistoryButtonSwitch extends SwitchPreference{

    public ShowHistoryButtonSwitch(Context context) {
        super(context);
    }
    public ShowHistoryButtonSwitch(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchPreferenceStyle);

    }

    public ShowHistoryButtonSwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onClick() {
        if (!isChecked()) {
            EventBus.getDefault().postSticky(new ZEvent(SHOW_HISTORYBUTTON));
        } else {
            EventBus.getDefault().postSticky(new ZEvent(HIDE_HISTORYBUTTON));
        }
        super.onClick();
    }

}
