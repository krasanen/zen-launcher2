package fi.zmengames.zen;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.Window;
import androidx.fragment.app.FragmentActivity;

import fr.neamar.kiss.BuildConfig;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;

public class AppGridActivity extends FragmentActivity {
    private static final String TAG = AppGridActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Setting the theme needs to be done before setContentView()
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("theme", "light");


        switch (theme) {
            case "dark":
                this.setTheme(R.style.AppThemeDark);
                break;
            case "transparent":
                this.setTheme(R.style.AppThemeTransparent);
                break;
            case "semi-transparent":
                this.setTheme(R.style.AppThemeSemiTransparent);
                break;
            case "semi-transparent-dark":
                this.setTheme(R.style.AppThemeSemiTransparentDark);
                break;
            case "transparent-dark":
                this.setTheme(R.style.AppThemeTransparentDark);
                break;
            case "amoled-dark":
                this.setTheme(R.style.AppThemeAmoledDark);
                break;
        }

        UIColors.updateThemePrimaryColor(this);
        this.getTheme().applyStyle(prefs.getBoolean("small-results", false) ? R.style.OverlayResultSizeSmall : R.style.OverlayResultSizeStandard, true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            int backgroundColor = getColorBasedOnTheme(this, R.attr.listBackgroundColor);
            window.setStatusBarColor(backgroundColor);
        }
        setContentView(R.layout.appgripscreen);

    }

    private int getColorBasedOnTheme(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
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
