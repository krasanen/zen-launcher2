package fi.zmengames.zen;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;

import androidx.fragment.app.FragmentActivity;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;

public class AppGridActivity extends FragmentActivity {
    private static final String TAG = AppGridActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appgripscreen);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (BuildConfig.DEBUG) Log.i(TAG, "dispatchTouchEvent: " + ev.getAction());
        super.dispatchTouchEvent(ev);
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            MainActivity.dismissPopup();
            return true;
        }
        return true;
    }
    @Override
    public void onBackPressed() {
        if (BuildConfig.DEBUG) Log.i(TAG, "onBackPressed");
        if (MainActivity.isShowingPopup()){
            MainActivity.dismissPopup();
        } else {
            super.onBackPressed();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }
}
